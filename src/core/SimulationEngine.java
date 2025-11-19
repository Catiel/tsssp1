package core; // Declaración del paquete que contiene la clase

// Importación de clases del modelo
import model.*;
// Importación de clases de estadísticas
import statistics.Statistics;
import statistics.LocationStats;
// Importación de utilidades de configuración
import utils.Config;
// Importación de clase Point para posiciones
import java.awt.Point;
// Importación de utilidades de colecciones
import java.util.*;
// Importación de cola de prioridad concurrente para eventos
import java.util.concurrent.PriorityBlockingQueue;
// Importación de mapa concurrente thread-safe
import java.util.concurrent.ConcurrentHashMap;
// Importación de cola concurrente thread-safe
import java.util.concurrent.ConcurrentLinkedQueue;

// Clase principal del motor de simulación
public class SimulationEngine {
    // Core simulation components
    private PriorityBlockingQueue<Event> eventQueue; // Cola de eventos ordenados por tiempo
    private double currentTime; // Tiempo actual de la simulación en horas
    private final double endTime; // Tiempo final de la simulación
    private final int weeksToSimulate; // Número de semanas a simular
    private boolean isRunning; // Indica si la simulación está ejecutándose
    private boolean isPaused; // Indica si la simulación está pausada
    private int animationSpeed = 50; // Velocidad de animación predeterminada (1-100)

    // Model components
    private Map<String, Location> locations; // Mapa de ubicaciones por nombre
    private Map<String, Double> locationHoldTimes; // Tiempos de retención por ubicación
    private Crane crane; // La grúa del sistema
    private PathNetwork pathNetwork; // Red de rutas para movimientos
    private ShiftCalendar shiftCalendar; // Calendario de turnos laborales
    private final Set<String> pendingMachineWakeups; // Máquinas pendientes de despertar al inicio del turno
    private final Queue<Valve> pendingCraneTransfers; // Válvulas pendientes de transporte por grúa

    // Statistics
    private Statistics statistics; // Objeto que recopila estadísticas
    private List<Valve> allValves; // Lista de todas las válvulas creadas
    private List<Valve> completedValves; // Lista de válvulas completadas
    private int dockToAlmacenMoves; // Contador de movimientos desde DOCK a almacenes
    private double lastOperationalTime; // Último tiempo con eventos operacionales
    private double lastSampleTime; // Último tiempo de muestreo de estadísticas
    private int completedInventoryCount; // Contador de inventario completado

    // Configuration
    private static final double SAMPLE_INTERVAL = 1.0; // Intervalo de muestreo de estadísticas (cada hora)
    private static final double SAMPLE_EPSILON = 1e-6; // Tolerancia numérica para comparaciones de tiempo
    private static final int DEFAULT_WEEKS_TO_SIMULATE = 8; // Número predeterminado de semanas a simular
    private static final double HOURS_PER_WEEK = 168.0; // Horas totales en una semana

    // Constructor de la clase SimulationEngine
    public SimulationEngine() {
        Config config = Config.getInstance(); // Obtener instancia de configuración

        this.eventQueue = new PriorityBlockingQueue<>(); // Inicializar cola de eventos
        this.currentTime = 0.0; // Inicializar tiempo actual en cero
        this.isRunning = false; // Inicializar estado de ejecución
        this.isPaused = false; // Inicializar estado de pausa

        this.locations = new ConcurrentHashMap<>(); // Inicializar mapa de ubicaciones
        this.locationHoldTimes = new ConcurrentHashMap<>(); // Inicializar mapa de tiempos de retención
        this.pathNetwork = new PathNetwork(); // Crear nueva red de rutas
        this.shiftCalendar = new ShiftCalendar(); // Crear nuevo calendario de turnos
        this.pendingMachineWakeups = ConcurrentHashMap.newKeySet(); // Inicializar conjunto de despertares pendientes
        this.pendingCraneTransfers = new ConcurrentLinkedQueue<>(); // Inicializar cola de transferencias
        this.statistics = new Statistics(); // Crear nuevo objeto de estadísticas
        this.allValves = Collections.synchronizedList(new ArrayList<>()); // Crear lista sincronizada de válvulas
        this.completedValves = Collections.synchronizedList(new ArrayList<>()); // Crear lista sincronizada de válvulas completadas
        this.dockToAlmacenMoves = 0; // Inicializar contador de movimientos
        this.lastOperationalTime = 0.0; // Inicializar último tiempo operacional
        this.lastSampleTime = 0.0; // Inicializar último tiempo de muestreo
        this.completedInventoryCount = 0; // Inicializar contador de inventario completado

        int configuredWeeks = config.getSimulationWeeks(); // Obtener semanas configuradas
        this.weeksToSimulate = configuredWeeks > 0 ? configuredWeeks : DEFAULT_WEEKS_TO_SIMULATE; // Usar semanas configuradas o valor predeterminado

        int firstWorkingHour = shiftCalendar.getFirstWorkingHourOfWeek(); // Obtener primera hora laboral de la semana
        int lastWorkingHourExclusive = shiftCalendar.getLastWorkingHourExclusive(); // Obtener última hora laboral (exclusiva)
        double weeklySpan = Math.max(0.0, lastWorkingHourExclusive - firstWorkingHour); // Calcular duración de horas laborales por semana
        if (weeklySpan <= 0.0) { // Si no hay horas laborales válidas
            weeklySpan = HOURS_PER_WEEK; // Usar semana completa
        }
        this.endTime = ((weeksToSimulate - 1) * HOURS_PER_WEEK) + weeklySpan; // Calcular tiempo final de simulación

        initializeLocations(); // Inicializar todas las ubicaciones
        initializeCrane(); // Inicializar la grúa
        scheduleArrivals(); // Programar llegadas de válvulas
        scheduleStatisticsSampling(); // Programar muestreo de estadísticas
    }

    // Método para inicializar todas las ubicaciones del sistema
    private void initializeLocations() {
        Config config = Config.getInstance(); // Obtener instancia de configuración
        locationHoldTimes.clear(); // Limpiar tiempos de retención previos
        int m1Units = Math.max(1, config.getMachineUnits("m1")); // Obtener número de unidades M1 (mínimo 1)
        int m2Units = Math.max(1, config.getMachineUnits("m2")); // Obtener número de unidades M2 (mínimo 1)
        int m3Units = Math.max(1, config.getMachineUnits("m3")); // Obtener número de unidades M3 (mínimo 1)

        // Leer capacidades de almacenes desde config
        int almacenM1Cap = config.getInt("location.almacen_m1.capacity", 20); // Capacidad almacén M1 (predeterminado 20)
        int almacenM2Cap = config.getInt("location.almacen_m2.capacity", 20); // Capacidad almacén M2 (predeterminado 20)
        int almacenM3Cap = config.getInt("location.almacen_m3.capacity", 30); // Capacidad almacén M3 (predeterminado 30)

        // Create locations with exact ProModel specifications
        locations.put("DOCK", new Location("DOCK", Integer.MAX_VALUE, 1, // Crear ubicación DOCK con capacidad infinita
            new Point(190, 140))); // Posición del DOCK
        locationHoldTimes.put("DOCK", config.getDouble("location.dock.hold_time", 0.0)); // Tiempo de retención en DOCK
        locations.put("STOCK", new Location("STOCK", Integer.MAX_VALUE, 1, // Crear ubicación STOCK con capacidad infinita
            new Point(200, 420))); // Posición del STOCK
        locationHoldTimes.put("STOCK", config.getDouble("location.stock.hold_time", 0.0)); // Tiempo de retención en STOCK

        locations.put("Almacen_M1", new Location("Almacen_M1", almacenM1Cap, 1, // Crear almacén M1 con capacidad configurada
            new Point(560, 520))); // Posición del almacén M1
        locationHoldTimes.put("Almacen_M1", config.getDouble("location.almacen_m1.hold_time", 0.0)); // Tiempo de retención en almacén M1
        // M1 parent - SOLO para enrutamiento, no procesa válvulas
        // Capacidad 0 para que las válvulas vayan directo a M1.x
        locations.put("M1", new Location("M1", 0, m1Units, // Crear ubicación padre M1 (solo enrutamiento)
            new Point(560, 380))); // Posición de M1
        // Individual M1 units - ESTAS procesan las válvulas
        for (int i = 1; i <= m1Units; i++) { // Iterar por cada unidad de M1
            locations.put("M1." + i, new Location("M1." + i, 1, 1, // Crear unidad individual M1.i
                new Point(560, 380))); // Posición de la unidad
            pathNetwork.registerLocationNode("M1." + i, pathNetwork.getNodeForLocation("M1")); // Registrar nodo en la red de rutas
        }

        locations.put("Almacen_M2", new Location("Almacen_M2", almacenM2Cap, 1, // Crear almacén M2 con capacidad configurada
            new Point(960, 320))); // Posición del almacén M2
        locationHoldTimes.put("Almacen_M2", config.getDouble("location.almacen_m2.hold_time", 0.0)); // Tiempo de retención en almacén M2
        // M2 parent - SOLO para enrutamiento
        locations.put("M2", new Location("M2", 0, m2Units, // Crear ubicación padre M2 (solo enrutamiento)
            new Point(760, 300))); // Posición de M2
        for (int i = 1; i <= m2Units; i++) { // Iterar por cada unidad de M2
            locations.put("M2." + i, new Location("M2." + i, 1, 1, // Crear unidad individual M2.i
                new Point(760, 300))); // Posición de la unidad
            pathNetwork.registerLocationNode("M2." + i, pathNetwork.getNodeForLocation("M2")); // Registrar nodo en la red de rutas
        }

        locations.put("Almacen_M3", new Location("Almacen_M3", almacenM3Cap, 1, // Crear almacén M3 con capacidad configurada
            new Point(1080, 180))); // Posición del almacén M3
        locationHoldTimes.put("Almacen_M3", config.getDouble("location.almacen_m3.hold_time", 0.0)); // Tiempo de retención en almacén M3
        // M3 parent - SOLO para enrutamiento
        locations.put("M3", new Location("M3", 0, m3Units, // Crear ubicación padre M3 (solo enrutamiento)
            new Point(900, 160))); // Posición de M3
        for (int i = 1; i <= m3Units; i++) { // Iterar por cada unidad de M3
            locations.put("M3." + i, new Location("M3." + i, 1, 1, // Crear unidad individual M3.i
                new Point(900, 160))); // Posición de la unidad
            pathNetwork.registerLocationNode("M3." + i, pathNetwork.getNodeForLocation("M3")); // Registrar nodo en la red de rutas
        }
    }

    // Método para inicializar la grúa
    private void initializeCrane() {
        Point home = locations.get("DOCK").getPosition(); // Obtener posición inicial (DOCK)
        // Speeds: empty=15.24 m/min, full=12.19 m/min (from ProModel)
        crane = new Crane("Grua", 1, 15.24, 12.19, home); // Crear grúa con velocidades vacía y cargada
    }

    // Método para programar llegadas de válvulas
    private void scheduleArrivals() {
        // Schedule arrivals every 168 hours (weekly)
        // CRÍTICO: Las válvulas llegan TODAS AL INICIO de cada semana
        for (int week = 0; week < weeksToSimulate; week++) { // Iterar por cada semana a simular
            double arrivalTime = week * HOURS_PER_WEEK; // Calcular tiempo de llegada (inicio de semana)

            // Ajustar al primer turno laboral de la semana
            if (!shiftCalendar.isWorkingTime(arrivalTime)) { // Si no es hora laboral
                arrivalTime = shiftCalendar.getNextWorkingTime(arrivalTime); // Ajustar a próxima hora laboral
            }

            // Schedule each valve type according to ProModel
            scheduleValveArrivals(Valve.Type.VALVULA_1, 10, arrivalTime); // Programar 10 válvulas tipo 1
            scheduleValveArrivals(Valve.Type.VALVULA_2, 40, arrivalTime); // Programar 40 válvulas tipo 2
            scheduleValveArrivals(Valve.Type.VALVULA_3, 10, arrivalTime); // Programar 10 válvulas tipo 3
            scheduleValveArrivals(Valve.Type.VALVULA_4, 20, arrivalTime); // Programar 20 válvulas tipo 4
        }
    }

    // Método para programar llegadas de un tipo específico de válvula
    private void scheduleValveArrivals(Valve.Type type, int quantity, double time) {
        // TODAS las válvulas llegan exactamente al mismo tiempo
        // ProModel: "Primera Vez 0" significa todas llegan al inicio
        for (int i = 0; i < quantity; i++) { // Iterar por la cantidad especificada
            Valve valve = new Valve(type, time); // Crear nueva válvula del tipo especificado
            allValves.add(valve); // Agregar válvula a la lista de todas las válvulas
            eventQueue.add(new Event(Event.Type.ARRIVAL, time, valve, null)); // Programar evento de llegada
        }
    }

    // Método para programar muestreo periódico de estadísticas
    private void scheduleStatisticsSampling() {
        for (double t = SAMPLE_INTERVAL; t <= endTime; t += SAMPLE_INTERVAL) { // Iterar desde el primer intervalo hasta el final
            eventQueue.add(new Event(Event.Type.SAMPLE_STATISTICS, t)); // Programar evento de muestreo de estadísticas
        }
    }

    // Main simulation loop
    // Método principal que ejecuta el bucle de simulación
    public void run() {
        isRunning = true; // Establecer estado de ejecución
        isPaused = false; // Establecer que no está pausado
        lastOperationalTime = 0.0; // Reiniciar último tiempo operacional
        lastSampleTime = 0.0; // Reiniciar último tiempo de muestreo

        while (!eventQueue.isEmpty() && isRunning && (currentTime < endTime || hasOperationalEvents())) { // Bucle mientras haya eventos y esté ejecutándose
            if (isPaused) { // Si está pausado
                try {
                    Thread.sleep(100); // Esperar 100 ms
                    continue; // Continuar al siguiente ciclo
                } catch (InterruptedException e) { // Capturar interrupción
                    break; // Salir del bucle
                }
            }

            // ESPERAR mientras la animación de la grúa está en progreso
            // Esto hace que la simulación se RALENTICE para que la animación sea visible
            if (crane.isMoving() && animationSpeed < 100) { // Si la grúa está en movimiento y velocidad < 100
                try {
                    Thread.sleep(getAnimationWaitMillis()); // Esperar según velocidad de animación
                    continue; // Continuar al siguiente ciclo
                } catch (InterruptedException e) { // Capturar interrupción
                    break; // Salir del bucle
                }
            }

            Event event = eventQueue.poll(); // Obtener y remover siguiente evento de la cola
            if (event != null) { // Si hay evento
                currentTime = event.getTime(); // Actualizar tiempo actual al tiempo del evento
                if (isOperationalEvent(event)) { // Si es evento operacional
                    lastOperationalTime = currentTime; // Actualizar último tiempo operacional
                }
                processEvent(event); // Procesar el evento
            }
        }

        finalizeStatistics(); // Finalizar estadísticas al terminar
    }

    // Método para ejecutar un paso individual de la simulación
    public void step() {
        if (!eventQueue.isEmpty()) { // Si hay eventos en la cola
            Event nextEvent = eventQueue.peek(); // Obtener siguiente evento sin removerlo
            if (nextEvent == null) { // Si no hay evento
                return; // Salir del método
            }
            if (currentTime >= endTime && !isOperationalEvent(nextEvent)) { // Si se alcanzó el tiempo final y no es evento operacional
                finalizeStatistics(); // Finalizar estadísticas
                return; // Salir del método
            }
            Event event = eventQueue.poll(); // Obtener y remover evento
            if (event != null) { // Si hay evento
                currentTime = event.getTime(); // Actualizar tiempo actual
                if (isOperationalEvent(event)) { // Si es evento operacional
                    lastOperationalTime = currentTime; // Actualizar último tiempo operacional
                }
                processEvent(event); // Procesar evento
            }
        }

        if (eventQueue.isEmpty() || !hasOperationalEvents()) { // Si no hay más eventos o no hay eventos operacionales
            finalizeStatistics(); // Finalizar estadísticas
        }
    }

    // Método para procesar un evento según su tipo
    private void processEvent(Event event) {
        switch (event.getType()) { // Evaluar tipo de evento
            case SAMPLE_STATISTICS: // Si es muestreo de estadísticas
                sampleStatistics(); // Ejecutar muestreo
                break; // Salir del switch
            case ARRIVAL: // Si es llegada de válvula
                handleArrival(event.getValve()); // Manejar llegada
                break; // Salir del switch
            case END_PROCESSING: // Si es fin de procesamiento
                handleEndProcessing(event.getValve()); // Manejar fin de procesamiento
                break; // Salir del switch
            case START_CRANE_MOVE: // Si es inicio de movimiento de grúa
                handleStartCraneMove(event.getValve(), (String) event.getData()); // Manejar inicio de movimiento
                break; // Salir del switch
            case END_CRANE_MOVE: // Si es fin de movimiento de grúa
                // Esperar a que la animación termine antes de procesar
                if (animationSpeed < 100) { // Si velocidad de animación < 100
                    while (crane.isMoving()) { // Mientras la grúa esté en movimiento
                        try {
                            Thread.sleep(getAnimationWaitMillis()); // Esperar según velocidad de animación
                        } catch (InterruptedException e) { // Capturar interrupción
                            break; // Salir del bucle
                        }
                    }
                }
                handleEndCraneMove(event.getValve(), (String) event.getData()); // Manejar fin de movimiento
                break; // Salir del switch
            case HOLD_RELEASE: // Si es liberación de retención
                handleHoldRelease(event.getValve(), (String) event.getData()); // Manejar liberación
                break; // Salir del switch
            case SHIFT_START: // Si es inicio de turno
                handleShiftStart((String) event.getData()); // Manejar inicio de turno
                break; // Salir del switch
            case CRANE_PICKUP: // Si es recogida de grúa
            case CRANE_RELEASE: // Si es liberación de grúa
            case SHIFT_END: // Si es fin de turno
                break; // No hacer nada (casos no implementados)
        }
    }

    // Método para manejar la llegada de una válvula
    private void handleArrival(Valve valve) {
        Location dock = locations.get("DOCK"); // Obtener ubicación DOCK
        dock.addToQueue(valve); // Agregar válvula a la cola del DOCK
        updateLocationMetrics(dock); // Actualizar métricas de la ubicación
        valve.setState(Valve.State.IN_QUEUE); // Establecer estado de la válvula en cola
        valve.setCurrentLocation(dock); // Establecer ubicación actual de la válvula
        applyHoldTime(valve, dock.getName(), currentTime); // Aplicar tiempo de retención
        valve.startWaiting(currentTime); // Comenzar a contar tiempo de espera
        statistics.recordArrival(valve); // Registrar llegada en estadísticas

        // INMEDIATAMENTE intentar programar movimiento de grúa
        tryScheduleCraneMove(); // Intentar programar movimiento de grúa

        // If crane didn't start (outside shift), schedule wakeup
        if (!crane.isBusy() && !shiftCalendar.isWorkingTime(currentTime) && dock.getQueueSize() > 0) { // Si grúa no está ocupada, fuera de turno y hay válvulas en DOCK
            scheduleCraneWakeup(); // Programar despertar de grúa
        }
    }

    // Método para manejar el fin de procesamiento de una válvula
    private void handleEndProcessing(Valve valve) {
        Location machineUnit = valve.getCurrentLocation(); // Obtener ubicación actual (máquina)
        valve.endProcessing(currentTime); // Finalizar procesamiento
        valve.advanceStep(); // Avanzar al siguiente paso del proceso

        // Incrementar contador de válvulas procesadas para esta ubicación
        if (machineUnit != null) { // Si hay ubicación
            String unitName = machineUnit.getName(); // Obtener nombre de la unidad
            String machineBaseName = unitName.contains(".") ? unitName.substring(0, unitName.indexOf(".")) : unitName; // Extraer nombre base de máquina
            LocationStats machineStats = statistics.getOrCreateLocationStats(machineBaseName); // Obtener o crear estadísticas de ubicación
            machineStats.incrementValvesProcessed(); // Incrementar contador de válvulas procesadas
        }

        // Verificar si hay espacio en destino ANTES de liberar máquina
        String destination = getNextDestination(valve); // Obtener próximo destino
        Location destLoc = destination != null ? locations.get(destination) : null; // Obtener ubicación de destino

        if (destLoc != null && !destLoc.canAccept()) { // Si destino existe pero no puede aceptar
            // BLOQUEADO: Destino lleno, válvula permanece en máquina (ProModel Blk 1)
            valve.setState(Valve.State.BLOCKED); // Establecer estado bloqueado
            valve.startBlocked(currentTime); // Comenzar tiempo de bloqueo
            // NO remover de machineUnit, permanece ocupando espacio
            return; // NO liberar máquina ni programar grúa
        }

        // Destino tiene espacio: remover de la máquina
        if (machineUnit != null) { // Si hay máquina
            machineUnit.removeValve(valve); // Remover válvula de la máquina
            updateLocationMetrics(machineUnit); // Actualizar métricas
        }

        // Machine unit holds the valve temporarily; crane will pick it up
        valve.setCurrentLocation(machineUnit); // Mantener ubicación actual temporalmente
        if (machineUnit != null) { // Si hay máquina
            applyHoldTime(valve, machineUnit.getName(), currentTime); // Aplicar tiempo de retención
        }
        valve.startWaiting(currentTime); // Comenzar tiempo de espera

        // ALTA PRIORIDAD: Válvulas que terminaron procesamiento
        pendingCraneTransfers.add(valve); // Agregar a cola de transferencias pendientes

        // Free machine capacity for next valve
        String unitName = machineUnit != null ? machineUnit.getName() : ""; // Obtener nombre de unidad
        String machineBaseName = unitName.contains(".") ? unitName.substring(0, unitName.indexOf(".")) : unitName; // Extraer nombre base
        String almacenName = "Almacen_" + machineBaseName; // Construir nombre de almacén
        Location almacen = locations.get(almacenName); // Obtener ubicación de almacén
        Location machineParent = locations.get(machineBaseName); // Obtener ubicación padre de máquina
        if (almacen != null && machineParent != null) { // Si ambas ubicaciones existen
            checkMachineQueue(machineParent, almacen); // Verificar cola de máquina
        }

        // CRÍTICO: Verificar válvulas bloqueadas SIEMPRE después de procesar
        // El almacén puede tener espacio temporalmente antes de checkMachineQueue
        checkBlockedValves(); // Verificar válvulas bloqueadas

        // INMEDIATAMENTE intentar mover con grúa
        tryScheduleCraneMove(); // Intentar programar movimiento de grúa
    }

    // Método para verificar y procesar cola de máquina
    private void checkMachineQueue(Location machineParent, Location almacen) {
        String machineBaseName = machineParent.getName(); // Obtener nombre base de máquina
        int unitCount = machineParent.getUnits(); // Obtener número de unidades

        while (almacen.getQueueSize() > 0) { // Mientras haya válvulas en el almacén
            if (!shiftCalendar.isWorkingTime(currentTime)) { // Si no es hora laboral
                Valve waitingValve = almacen.peekQueue(); // Ver primera válvula sin removerla
                if (waitingValve != null) { // Si hay válvula
                    waitingValve.startWaiting(currentTime); // Comenzar tiempo de espera
                }
                scheduleMachineWakeup(machineBaseName); // Programar despertar de máquina
                break; // Salir del bucle
            }

            // Find first available unit
            Location availableUnit = null; // Variable para unidad disponible
            for (int i = 1; i <= unitCount; i++) { // Iterar por cada unidad
                Location unit = locations.get(machineBaseName + "." + i); // Obtener unidad
                if (unit != null && unit.hasAvailableUnit()) { // Si unidad existe y está disponible
                    availableUnit = unit; // Asignar unidad disponible
                    break; // Salir del bucle
                }
            }

            if (availableUnit == null) { // Si no hay unidad disponible
                break; // Salir del bucle
            }

            Valve nextValve = findNextReadyValve(almacen); // Buscar siguiente válvula lista
            if (nextValve == null) { // Si no hay válvula lista
                break; // Salir del bucle
            }

            // CRÍTICO: Terminar tiempo de espera antes de remover
            nextValve.endWaiting(currentTime); // Finalizar tiempo de espera

            almacen.removeValve(nextValve); // Remover válvula del almacén
            updateLocationMetrics(almacen); // Actualizar métricas del almacén
            availableUnit.addToQueue(nextValve); // Agregar válvula a cola de unidad disponible
            availableUnit.moveToProcessing(nextValve); // Mover válvula a procesamiento
            updateLocationMetrics(availableUnit); // Actualizar métricas de unidad

            double processTime = nextValve.getCurrentProcessingTime(); // Obtener tiempo de procesamiento
            nextValve.setReadyTime(currentTime); // Establecer tiempo de preparación
            nextValve.startProcessing(currentTime); // Iniciar procesamiento
            nextValve.setCurrentLocation(availableUnit); // Actualizar ubicación

            eventQueue.add(new Event(Event.Type.END_PROCESSING, // Agregar evento de fin de procesamiento
                currentTime + processTime, nextValve, null)); // Con tiempo actual + tiempo de procesamiento
        }

        // CRÍTICO: SIEMPRE verificar válvulas bloqueadas y dar oportunidad a grúa
        // No importa si el almacén quedó lleno después de mover válvulas,
        // puede haber válvulas en DOCK esperando ir a OTROS almacenes (M2, M3)
        checkBlockedValves(); // Verificar válvulas bloqueadas
        if (!crane.isBusy() && shiftCalendar.isWorkingTime(currentTime)) { // Si grúa no está ocupada y es hora laboral
            tryScheduleCraneMove(); // Intentar programar movimiento de grúa
        }
    }

    // Método para programar despertar de máquina al inicio de turno
    private void scheduleMachineWakeup(String machineName) {
        if (!pendingMachineWakeups.add(machineName)) { // Si ya estaba en conjunto de despertares pendientes
            return; // Salir del método (evitar duplicados)
        }
        double wakeTime = shiftCalendar.getNextWorkingTime(currentTime); // Obtener próximo tiempo laboral
        if (wakeTime <= currentTime) { // Si tiempo de despertar no es futuro
            pendingMachineWakeups.remove(machineName); // Remover de pendientes
            return; // Salir del método
        }
        eventQueue.add(new Event(Event.Type.SHIFT_START, wakeTime, null, machineName)); // Programar evento de inicio de turno
    }

    // Método para verificar y desbloquear válvulas bloqueadas
    private void checkBlockedValves() {
        // Revisar todas las ubicaciones buscando válvulas bloqueadas
        for (Location location : locations.values()) { // Iterar por todas las ubicaciones
            List<Valve> blockedValves = new ArrayList<>(); // Lista temporal de válvulas bloqueadas
            for (Valve valve : location.getAllValves()) { // Iterar por todas las válvulas en ubicación
                if (valve.getState() == Valve.State.BLOCKED) { // Si válvula está bloqueada
                    blockedValves.add(valve); // Agregar a lista temporal
                }
            }

            for (Valve valve : blockedValves) { // Iterar por válvulas bloqueadas
                String destination = getNextDestination(valve); // Obtener destino
                Location destLoc = destination != null ? locations.get(destination) : null; // Obtener ubicación de destino

                if (destLoc != null && destLoc.canAccept()) { // Si destino existe y puede aceptar
                    // Destino ahora tiene espacio: desbloquear
                    valve.endBlocked(currentTime); // Finalizar tiempo de bloqueo

                    // Si está en DOCK, simplemente cambiar estado a IN_QUEUE
                    if (location.getName().equals("DOCK")) { // Si está en DOCK
                        valve.setState(Valve.State.IN_QUEUE); // Cambiar estado a en cola
                        // NO remover de DOCK, la grúa la recogerá
                    } else { // Si está en otra ubicación
                        // Si está en máquina, preparar para transporte
                        location.removeValve(valve); // Remover de ubicación
                        updateLocationMetrics(location); // Actualizar métricas
                        valve.setCurrentLocation(location); // Mantener ubicación temporalmente
                        applyHoldTime(valve, location.getName(), currentTime); // Aplicar tiempo de retención
                        valve.startWaiting(currentTime); // Comenzar tiempo de espera
                        pendingCraneTransfers.add(valve); // Agregar a transferencias pendientes

                        // Liberar espacio para siguiente válvula
                        String unitName = location.getName(); // Obtener nombre de ubicación
                        if (unitName.contains(".")) { // Si es unidad individual
                            String machineBaseName = unitName.substring(0, unitName.indexOf(".")); // Extraer nombre base
                            String almacenName = "Almacen_" + machineBaseName; // Construir nombre de almacén
                            Location almacen = locations.get(almacenName); // Obtener almacén
                            Location machineParent = locations.get(machineBaseName); // Obtener máquina padre
                            if (almacen != null && machineParent != null) { // Si ambas existen
                                checkMachineQueue(machineParent, almacen); // Verificar cola de máquina
                            }
                        }
                    }
                }
            }
        }
    }

    // Método para programar despertar de grúa
    private void scheduleCraneWakeup() {
        double wakeTime = shiftCalendar.getNextWorkingTime(currentTime); // Obtener próximo tiempo laboral
        if (wakeTime > currentTime) { // Si es tiempo futuro
            eventQueue.add(new Event(Event.Type.SHIFT_START, wakeTime, null, "CRANE")); // Programar evento de inicio de turno para grúa
        }
    }

    // Método para manejar inicio de turno
    private void handleShiftStart(String machineName) {
        if (machineName == null) { // Si no hay nombre
            return; // Salir del método
        }

        // Special case: crane wakeup
        if ("CRANE".equals(machineName)) { // Si es despertar de grúa
            tryScheduleCraneMove(); // Intentar programar movimiento
            return; // Salir del método
        }

        pendingMachineWakeups.remove(machineName); // Remover de despertares pendientes
        Location machineParent = locations.get(machineName); // Obtener ubicación de máquina padre
        if (machineParent == null) { // Si no existe
            return; // Salir del método
        }
        String almacenName = "Almacen_" + machineName; // Construir nombre de almacén
        Location almacen = locations.get(almacenName); // Obtener almacén
        if (almacen == null) { // Si no existe
            return; // Salir del método
        }
        checkMachineQueue(machineParent, almacen); // Verificar cola de máquina
        tryScheduleCraneMove(); // Intentar programar movimiento de grúa
    }

    // Método para intentar programar movimiento de grúa
    private void tryScheduleCraneMove() {
        if (crane.isBusy()) { // Si grúa está ocupada
            return; // Salir del método
        }

        // PRIORIDAD 1: Válvulas que terminaron procesamiento (liberan máquinas)
        Valve valveToMove = pollPendingCraneTransfer(); // Buscar válvula pendiente de transferencia
        boolean pendingFromMachine = valveToMove != null; // Indicador si proviene de máquina

        // PRIORIDAD 2: Válvulas en DOCK esperando (ProModel FIRST 1 = primera DISPONIBLE)
        if (valveToMove == null) { // Si no hay válvula de máquina
            if (!shiftCalendar.isWorkingTime(currentTime)) { // Si no es hora laboral
                return; // Fuera de turno sólo atendemos descargas de máquinas
            }
            valveToMove = findFirstAvailableValveInDock(); // Buscar primera válvula disponible en DOCK
        }

        if (valveToMove != null) { // Si hay válvula para mover
            if (!shiftCalendar.isWorkingTime(currentTime) && !pendingFromMachine) { // Si fuera de turno y no es de máquina
                return; // Salir del método
            }
            String destination = getNextDestination(valveToMove); // Obtener destino
            Location destLoc = locations.get(destination); // Obtener ubicación de destino

            if (destLoc != null && destLoc.canAccept()) { // Si destino existe y puede aceptar
                scheduleCraneMove(valveToMove, destination); // Programar movimiento de grúa
            }
        }
    }

    // Método para buscar primera válvula disponible en DOCK
    private Valve findFirstAvailableValveInDock() {
        Location dock = locations.get("DOCK"); // Obtener ubicación DOCK
        if (dock == null || dock.getQueueSize() == 0) { // Si no existe o no hay válvulas
            return null; // Retornar nulo
        }

        // Buscar PRIMERA válvula cuyo destino tenga espacio (ProModel FIRST 1)
        for (Valve valve : dock.getQueueSnapshot()) { // Iterar por válvulas en DOCK
            if (valve == null) { // Si válvula es nula
                continue; // Continuar con siguiente
            }
            if (!valve.isReady(currentTime)) { // Si válvula no está lista
                continue; // Continuar con siguiente
            }
            String destination = getNextDestination(valve); // Obtener destino
            Location destLoc = destination != null ? locations.get(destination) : null; // Obtener ubicación de destino

            if (destination == null) { // Si no hay destino
                continue; // Continuar con siguiente
            }

            // CRÍTICO: Verificar espacio ANTES de saltarse por estar bloqueada
            if (destLoc != null && destLoc.canAccept()) { // Si destino puede aceptar
                // Si estaba bloqueada, desbloquear
                if (valve.getState() == Valve.State.BLOCKED) { // Si estaba bloqueada
                    valve.endBlocked(currentTime); // Finalizar bloqueo
                    valve.setState(Valve.State.IN_QUEUE); // Cambiar estado a en cola
                }
                return valve; // Primera disponible
            } else if (destLoc != null && !destLoc.canAccept()) { // Si destino no puede aceptar
                // Destino lleno: marcar bloqueada y continuar buscando
                if (valve.getState() != Valve.State.BLOCKED) { // Si no estaba bloqueada
                    valve.setState(Valve.State.BLOCKED); // Establecer estado bloqueado
                    valve.startBlocked(currentTime); // Comenzar tiempo de bloqueo
                }
            }
        }

        return null; // Todas bloqueadas o sin destino válido
    }

    // Método para obtener y remover válvula pendiente de transferencia
    private Valve pollPendingCraneTransfer() {
        // Buscar válvula que necesita transporte desde máquinas
        Iterator<Valve> iterator = pendingCraneTransfers.iterator(); // Crear iterador
        while (iterator.hasNext()) { // Mientras haya elementos
            Valve valve = iterator.next(); // Obtener siguiente válvula
            if (valve == null || !valve.isReady(currentTime)) { // Si es nula o no está lista
                continue; // Continuar con siguiente
            }
            String destination = getNextDestination(valve); // Obtener destino
            if (destination == null) { // Si no hay destino
                iterator.remove(); // Remover de cola
                continue; // Continuar con siguiente
            }
            Location destLoc = locations.get(destination); // Obtener ubicación de destino
            if (destLoc != null && destLoc.canAccept()) { // Si destino existe y puede aceptar
                iterator.remove(); // Remover de cola
                return valve; // Retornar válvula
            }
        }
        return null; // No hay válvula disponible
    }

    // Método para obtener próximo destino de una válvula
    private String getNextDestination(Valve valve) {
        Location currentLocation = valve.getCurrentLocation(); // Obtener ubicación actual
        if (currentLocation == null) { // Si no hay ubicación
            return null; // Retornar nulo
        }

        if (currentLocation.getName().equals("DOCK")) { // Si está en DOCK
            return valve.getNextAlmacen(); // Retornar próximo almacén
        }

        if (valve.isRouteComplete()) { // Si ruta está completa
            return "STOCK"; // Retornar STOCK
        }

        return valve.getNextAlmacen(); // Retornar próximo almacén
    }

    // Método para programar movimiento de grúa
    private void scheduleCraneMove(Valve valve, String destination) {
        crane.setBusy(true); // Establecer grúa como ocupada

        Location from = valve.getCurrentLocation(); // Obtener ubicación origen
        Location to = locations.get(destination); // Obtener ubicación destino

        if (from == null || to == null) { // Si alguna ubicación no existe
            crane.setBusy(false); // Liberar grúa
            return; // Salir del método
        }

        // Si válvula estaba bloqueada, terminar bloqueo
        if (valve.getState() == Valve.State.BLOCKED) { // Si estaba bloqueada
            valve.endBlocked(currentTime); // Finalizar bloqueo
        }

        PathNetwork.PathResult pathResult = pathNetwork.getPathForLocations(from.getName(), destination); // Obtener ruta entre ubicaciones
        List<Point> pathPoints; // Lista de puntos de ruta
        List<Double> segmentDistances; // Lista de distancias de segmentos
        double totalDistanceMeters; // Distancia total en metros

        if (pathResult.isValid() && pathResult.getPoints().size() >= 2) { // Si ruta es válida y tiene al menos 2 puntos
            pathPoints = pathResult.getPoints(); // Usar puntos de ruta
            segmentDistances = pathResult.getSegmentDistances(); // Usar distancias de segmentos
            totalDistanceMeters = pathResult.getTotalDistance(); // Usar distancia total
        } else { // Si no hay ruta válida
            Point start = new Point(from.getPosition()); // Crear punto de inicio
            Point end = new Point(to.getPosition()); // Crear punto de fin
            pathPoints = Arrays.asList(start, end); // Crear lista con 2 puntos
            double euclidean = start.distance(end); // Calcular distancia euclidiana
            segmentDistances = Collections.singletonList(euclidean); // Lista con una distancia
            totalDistanceMeters = euclidean; // Distancia total = distancia euclidiana
        }

        double travelTime = crane.calculateTravelTime(totalDistanceMeters, valve != null); // Calcular tiempo de viaje

        crane.addTravelTime(travelTime); // Agregar tiempo de viaje a grúa
        valve.addMovementTime(travelTime); // Agregar tiempo de movimiento a válvula
        valve.endWaiting(currentTime); // Finalizar tiempo de espera

        // Pasar el tiempo actual de simulación y la velocidad de animación
        crane.startMove(pathPoints, segmentDistances, totalDistanceMeters, travelTime, currentTime, animationSpeed); // Iniciar movimiento de grúa

        if (from.getName().equals("DOCK")) { // Si origen es DOCK
            dockToAlmacenMoves++; // Incrementar contador de movimientos DOCK a almacén
        }

        eventQueue.add(new Event(Event.Type.START_CRANE_MOVE, // Agregar evento de inicio de movimiento
            currentTime, valve, destination)); // Con tiempo actual
        eventQueue.add(new Event(Event.Type.END_CRANE_MOVE, // Agregar evento de fin de movimiento
            currentTime + travelTime, valve, destination)); // Con tiempo actual + tiempo de viaje
    }

    // Método para manejar inicio de movimiento de grúa
    private void handleStartCraneMove(Valve valve, String destination) {
        Location from = valve.getCurrentLocation(); // Obtener ubicación origen
        if (from != null) { // Si hay ubicación origen
            from.removeValve(valve); // Remover válvula de ubicación
            updateLocationMetrics(from); // Actualizar métricas
        }

        // Finalizar tiempo de espera
        if (valve.getState() == Valve.State.IN_QUEUE ||  // Si está en cola o
            valve.getState() == Valve.State.WAITING_CRANE) { // esperando grúa
            valve.endWaiting(currentTime); // Finalizar tiempo de espera
        }

        crane.pickupValve(valve); // Grúa recoge válvula
        valve.setState(Valve.State.IN_TRANSIT); // Establecer estado en tránsito
        valve.setCurrentLocation(null); // En tránsito = sin ubicación fija
    }

    // Método para manejar fin de movimiento de grúa
    private void handleEndCraneMove(Valve valve, String destination) {
        Location destLoc = locations.get(destination); // Obtener ubicación de destino

        // Sincronizar animación antes de liberar
        crane.completeTrip(); // Completar viaje de grúa

        if (destination.equals("STOCK")) { // Si destino es STOCK
            destLoc.addToQueue(valve); // Agregar válvula a cola de STOCK
            updateLocationMetrics(destLoc); // Actualizar métricas
            destLoc.removeValve(valve); // Act as sink so it doesn't retain inventory
            updateLocationMetrics(destLoc); // Actualizar métricas nuevamente
            valve.setState(Valve.State.COMPLETED); // Establecer estado completado
            valve.setCurrentLocation(null); // Sin ubicación (completada)
            completedValves.add(valve); // Agregar a lista de válvulas completadas
            statistics.recordCompletion(valve, currentTime); // Registrar completitud en estadísticas
            completedInventoryCount++; // Incrementar contador de inventario completado
        } else if (destination.startsWith("Almacen")) { // Si destino es almacén
            // Crane drops valve at storage
            destLoc.addToQueue(valve); // Agregar válvula a almacén
            updateLocationMetrics(destLoc); // Actualizar métricas
            valve.setState(Valve.State.IN_QUEUE); // Establecer estado en cola
            valve.setCurrentLocation(destLoc); // Establecer ubicación actual
            applyHoldTime(valve, destLoc.getName(), currentTime); // Aplicar tiempo de retención

            // Immediately try to move to machine (ProModel: Almacen_M1 -> M1 is instant)
            String machineName = destination.replace("Almacen_", ""); // Extraer nombre de máquina
            Location machineParent = locations.get(machineName); // Obtener máquina padre
            if (machineParent != null) { // Si existe
                checkMachineQueue(machineParent, destLoc); // Verificar cola de máquina
            }
        }

        crane.releaseValve(); // Liberar válvula de grúa
        crane.setBusy(false); // Establecer grúa como no ocupada

        // CRÍTICO: Verificar válvulas bloqueadas que ahora pueden moverse
        checkBlockedValves(); // Verificar válvulas bloqueadas

        // CRÍTICO: Intentar siguiente movimiento INMEDIATAMENTE
        // La grúa debe estar constantemente activa
        tryScheduleCraneMove(); // Intentar programar movimiento de grúa

        // If crane can't move (shift ended), schedule wakeup
        if (!crane.isBusy() && !shiftCalendar.isWorkingTime(currentTime)) { // Si grúa no ocupada y fuera de turno
            if (!pendingCraneTransfers.isEmpty() ||  // Si hay transferencias pendientes o
                locations.get("DOCK").getQueueSize() > 0) { // hay válvulas en DOCK
                scheduleCraneWakeup(); // Programar despertar de grúa
            }
        }
    }

    // Método para muestrear estadísticas en tiempo actual
    private void sampleStatistics() {
        sampleStatisticsAt(currentTime); // Llamar muestreo con tiempo actual
    }

    // Método para muestrear estadísticas en tiempo específico
    private void sampleStatisticsAt(double sampleTime) {
        for (Location loc : locations.values()) { // Iterar por todas las ubicaciones
            String name = loc.getName(); // Obtener nombre de ubicación
            if (isMachineParent(name)) { // Si es máquina padre
                continue; // Continuar con siguiente (no muestrear padres)
            }

            // Almacenes y unidades individuales dependen de turno
            boolean countTowardsSchedule = shouldCountTowardsSchedule(name, sampleTime); // Determinar si cuenta para calendario

            loc.updateStatistics(sampleTime, countTowardsSchedule); // Actualizar estadísticas de ubicación
            statistics.updateLocationStats(name, // Actualizar estadísticas en objeto Statistics
                loc.getCurrentContents(), // Contenido actual
                loc.getUtilization(), // Utilización
                sampleTime); // Tiempo de muestreo
        }

        updateMachineAggregate("M1", sampleTime); // Actualizar agregado de M1
        updateMachineAggregate("M2", sampleTime); // Actualizar agregado de M2
        updateMachineAggregate("M3", sampleTime); // Actualizar agregado de M3

        // Update crane statistics
        crane.updateStatistics(sampleTime); // Actualizar estadísticas de grúa
        Config config = Config.getInstance(); // Obtener configuración
        int craneUnits = config.getResourceUnits(crane.getName(), crane.getUnits()); // Obtener unidades de grúa
        double weeksSimulated = Math.max(sampleTime, SAMPLE_EPSILON) / HOURS_PER_WEEK; // Calcular semanas simuladas
        double defaultScheduled = shiftCalendar.getTotalWorkingHoursPerWeek() * weeksSimulated; // Calcular horas programadas
        double scheduledHours = config.getResourceScheduledHours(crane.getName(), defaultScheduled); // Obtener horas programadas de configuración
        int totalTrips = crane.getTotalTrips(); // Obtener total de viajes

        double defaultHandleMinutes = totalTrips > 0 // Calcular minutos de manejo promedio
            ? (crane.getTotalUsageTime() * 60.0) / totalTrips // Si hay viajes
            : 0.0; // Si no hay viajes
        double avgHandleMinutes = config.getResourceAvgHandleMinutes(crane.getName(), defaultHandleMinutes); // Obtener de configuración
        double defaultTravelMinutes = totalTrips > 0 // Calcular minutos de viaje promedio
            ? (crane.getTotalTravelTime() * 60.0) / totalTrips // Si hay viajes
            : 0.0; // Si no hay viajes
        double avgTravelMinutes = config.getResourceAvgTravelMinutes(crane.getName(), defaultTravelMinutes); // Obtener de configuración
        double avgParkMinutes = config.getResourceAvgParkMinutes(crane.getName(), 0.0); // Obtener minutos de estacionamiento
        double blockedPercent = config.getResourceBlockedPercent(crane.getName(), 0.0); // Obtener porcentaje bloqueado

        double totalWorkMinutes = totalTrips * (avgHandleMinutes + avgTravelMinutes + avgParkMinutes); // Calcular minutos totales de trabajo
        double utilization = scheduledHours > 1e-9 // Calcular utilización
            ? (totalWorkMinutes / 60.0) / scheduledHours * 100.0 // Si hay horas programadas
            : 0.0; // Si no hay horas programadas

        statistics.updateCraneStats( // Actualizar estadísticas de grúa
            craneUnits, // Unidades
            scheduledHours, // Horas programadas
            totalWorkMinutes, // Minutos totales de trabajo
            totalTrips, // Total de viajes
            avgHandleMinutes, // Minutos promedio de manejo
            avgTravelMinutes, // Minutos promedio de viaje
            avgParkMinutes, // Minutos promedio de estacionamiento
            blockedPercent, // Porcentaje bloqueado
            utilization, // Utilización
            sampleTime); // Tiempo de muestreo

        lastSampleTime = sampleTime; // Actualizar último tiempo de muestreo
    }

    // Método para verificar si nombre es máquina padre
    private boolean isMachineParent(String name) {
        return "M1".equals(name) || "M2".equals(name) || "M3".equals(name); // Retornar true si es M1, M2 o M3
    }

    // Método para verificar si nombre es unidad de máquina
    private boolean isMachineUnit(String name) {
        return name.startsWith("M1.") || name.startsWith("M2.") || name.startsWith("M3."); // Retornar true si empieza con M1., M2. o M3.
    }

    // Método para actualizar agregado de máquina
    private void updateMachineAggregate(String machineBaseName, double sampleTime) {
        double busySum = 0.0; // Suma de tiempo ocupado
        int totalContents = 0; // Contenido total
        int actualUnits = 0; // Unidades reales

        String prefix = machineBaseName + "."; // Prefijo de unidades
        for (Map.Entry<String, Location> entry : locations.entrySet()) { // Iterar por ubicaciones
            String name = entry.getKey(); // Obtener nombre
            if (!name.startsWith(prefix)) { // Si no empieza con prefijo
                continue; // Continuar con siguiente
            }

            Location unit = entry.getValue(); // Obtener ubicación
            busySum += unit.getTotalBusyTime(); // Sumar tiempo ocupado
            totalContents += unit.getCurrentContents(); // Sumar contenido
            actualUnits++; // Incrementar contador de unidades
        }

        // Normalizar con stats_units (factor de ajuste)
        Config config = Config.getInstance(); // Obtener configuración
        double statsUnits = config.getMachineStatsUnits(machineBaseName, actualUnits > 0 ? actualUnits : 1); // Obtener unidades de estadísticas
        double scheduledPerUnit = shiftCalendar.getTotalWorkingHoursPerWeek(); // Horas programadas por unidad
        double weeksSimulated = Math.max(sampleTime, SAMPLE_EPSILON) / 168.0; // Semanas simuladas
        double totalScheduled = statsUnits * scheduledPerUnit * weeksSimulated; // Total programado

        double utilization = (totalScheduled > 0.0) ? (busySum / totalScheduled) * 100.0 : 0.0; // Calcular utilización
        // Limitar al 100% máximo
        utilization = Math.min(utilization, 100.0); // Limitar al 100%

        statistics.updateLocationStats(machineBaseName, totalContents, utilization, sampleTime); // Actualizar estadísticas
    }

    // Método para verificar si hay eventos operacionales pendientes
    private boolean hasOperationalEvents() {
        for (Event pending : eventQueue) { // Iterar por eventos en cola
            if (isOperationalEvent(pending)) { // Si es evento operacional
                return true; // Retornar verdadero
            }
        }
        return false; // No hay eventos operacionales
    }

    // Método para verificar si un evento es operacional
    private boolean isOperationalEvent(Event event) {
        if (event == null) { // Si evento es nulo
            return false; // Retornar falso
        }

        switch (event.getType()) { // Evaluar tipo de evento
            case ARRIVAL: // Si es llegada
            case END_PROCESSING: // Si es fin de procesamiento
            case START_CRANE_MOVE: // Si es inicio de movimiento de grúa
            case END_CRANE_MOVE: // Si es fin de movimiento de grúa
                return true; // Retornar verdadero
            case HOLD_RELEASE: // Si es liberación de retención
                return true; // Retornar verdadero
            case SHIFT_START: // Si es inicio de turno
                return hasPendingWorkForShift(event); // Verificar si hay trabajo pendiente
            default: // Cualquier otro caso
                return false; // Retornar falso
        }
    }

    // Método para determinar si debe contar para calendario
    private boolean shouldCountTowardsSchedule(String name, double time) {
        if (isMachineUnit(name)) { // Si es unidad de máquina
            double referenceTime = Math.max(0.0, time - SAMPLE_EPSILON); // Calcular tiempo de referencia
            return shiftCalendar.isWorkingTime(referenceTime); // Retornar si es hora laboral
        }
        return true; // Para otras ubicaciones siempre contar
    }

    // Método para actualizar métricas de una ubicación
    private void updateLocationMetrics(Location location) {
        if (location == null) { // Si ubicación es nula
            return; // Salir del método
        }
        boolean countTowardsSchedule = shouldCountTowardsSchedule(location.getName(), currentTime); // Determinar si cuenta para calendario
        location.updateStatistics(currentTime, countTowardsSchedule); // Actualizar estadísticas
    }

    // Método para aplicar tiempo de retención a válvula
    private void applyHoldTime(Valve valve, String locationName, double referenceTime) {
        if (valve == null || locationName == null) { // Si válvula o nombre es nulo
            return; // Salir del método
        }

        double holdTime = getHoldTimeFor(locationName); // Obtener tiempo de retención
        if (holdTime <= 0.0) { // Si no hay tiempo de retención
            valve.setReadyTime(referenceTime); // Establecer tiempo de preparación inmediato
            return; // Salir del método
        }

        double readyAt = referenceTime + holdTime; // Calcular tiempo de preparación
        valve.setReadyTime(readyAt); // Establecer tiempo de preparación
        eventQueue.add(new Event(Event.Type.HOLD_RELEASE, readyAt, valve, locationName)); // Programar evento de liberación de retención
    }

    // Método para buscar siguiente válvula lista en ubicación
    private Valve findNextReadyValve(Location location) {
        if (location == null) { // Si ubicación es nula
            return null; // Retornar nulo
        }

        for (Valve valve : location.getQueueSnapshot()) { // Iterar por válvulas en cola
            if (valve == null) { // Si válvula es nula
                continue; // Continuar con siguiente
            }
            Valve.State state = valve.getState(); // Obtener estado de válvula
            if ((state == Valve.State.IN_QUEUE || state == Valve.State.WAITING_CRANE) && valve.isReady(currentTime)) { // Si está en cola y lista
                return valve; // Retornar válvula
            }
        }
        return null; // No hay válvula lista
    }

    // Método para manejar liberación de retención
    private void handleHoldRelease(Valve valve, String locationName) {
        if (valve == null || locationName == null) { // Si válvula o nombre es nulo
            return; // Salir del método
        }

        Location currentLocation = valve.getCurrentLocation(); // Obtener ubicación actual
        if (currentLocation == null || !locationName.equals(currentLocation.getName())) { // Si no coincide ubicación
            return; // Salir del método
        }

        valve.setReadyTime(Math.max(valve.getReadyTime(), currentTime)); // Actualizar tiempo de preparación

        if ("DOCK".equals(locationName)) { // Si es DOCK
            tryScheduleCraneMove(); // Intentar programar movimiento de grúa
            return; // Salir del método
        }

        if (locationName.startsWith("Almacen_")) { // Si es almacén
            String machineName = locationName.replace("Almacen_", ""); // Extraer nombre de máquina
            Location machineParent = locations.get(machineName); // Obtener máquina padre
            if (machineParent != null) { // Si existe
                checkMachineQueue(machineParent, currentLocation); // Verificar cola de máquina
            }
        }

        tryScheduleCraneMove(); // Intentar programar movimiento de grúa
    }

    // Método para obtener tiempo de retención de ubicación
    private double getHoldTimeFor(String locationName) {
        return locationHoldTimes.getOrDefault(locationName, 0.0); // Obtener de mapa o retornar 0.0
    }

    // Método para verificar si hay trabajo pendiente para turno
    private boolean hasPendingWorkForShift(Event event) {
        Object data = event.getData(); // Obtener datos del evento
        if (!(data instanceof String)) { // Si no es String
            return false; // Retornar falso
        }

        String name = (String) data; // Convertir a String
        if ("CRANE".equals(name)) { // Si es grúa
            if (!pendingCraneTransfers.isEmpty()) { // Si hay transferencias pendientes
                return true; // Retornar verdadero
            }

            Location dock = locations.get("DOCK"); // Obtener DOCK
            if (dock != null) { // Si existe
                for (Valve valve : dock.getAllValves()) { // Iterar por válvulas en DOCK
                    String destination = getNextDestination(valve); // Obtener destino
                    Location destLoc = destination != null ? locations.get(destination) : null; // Obtener ubicación de destino
                    if (destLoc != null && destLoc.canAccept()) { // Si puede aceptar
                        return true; // Retornar verdadero
                    }
                }
            }
            return false; // No hay trabajo pendiente
        }

        Location almacen = locations.get("Almacen_" + name); // Obtener almacén
        if (almacen != null && almacen.getQueueSize() > 0) { // Si tiene válvulas en cola
            return true; // Retornar verdadero
        }

        Location machineParent = locations.get(name); // Obtener máquina padre
        if (machineParent != null) { // Si existe
            int unitCount = machineParent.getUnits(); // Obtener número de unidades
            for (int i = 1; i <= unitCount; i++) { // Iterar por unidades
                Location unit = locations.get(name + "." + i); // Obtener unidad
                if (unit != null && (unit.getQueueSize() > 0 || unit.getProcessingSize() > 0)) { // Si tiene válvulas
                    return true; // Retornar verdadero
                }
            }
        }

        return false; // No hay trabajo pendiente
    }

    // Método para finalizar estadísticas
    private void finalizeStatistics() {
        double finalTime = Math.max(lastSampleTime, lastOperationalTime); // Calcular tiempo final
        if (finalTime <= 0.0) { // Si no hay tiempo válido
            return; // Salir del método
        }

        if (finalTime > lastSampleTime + SAMPLE_EPSILON) { // Si hay tiempo significativo desde último muestreo
            sampleStatisticsAt(finalTime); // Muestrear en tiempo final
        }

        currentTime = finalTime; // Actualizar tiempo actual
    }

    // Control methods
    public void pause() { isPaused = true; } // Método para pausar simulación
    public void resume() { isPaused = false; } // Método para reanudar simulación
    public void stop() { isRunning = false; } // Método para detener simulación
    // Método para reiniciar simulación
    public void reset() {
        eventQueue.clear(); // Limpiar cola de eventos
        locations.clear(); // Limpiar ubicaciones
        allValves.clear(); // Limpiar todas las válvulas
        completedValves.clear(); // Limpiar válvulas completadas
        currentTime = 0; // Reiniciar tiempo actual
        statistics = new Statistics(); // Crear nuevo objeto de estadísticas
        initializeLocations(); // Inicializar ubicaciones
        initializeCrane(); // Inicializar grúa
        scheduleArrivals(); // Programar llegadas
        scheduleStatisticsSampling(); // Programar muestreo de estadísticas
        dockToAlmacenMoves = 0; // Reiniciar contador de movimientos
        lastOperationalTime = 0.0; // Reiniciar último tiempo operacional
        lastSampleTime = 0.0; // Reiniciar último tiempo de muestreo
        completedInventoryCount = 0; // Reiniciar contador de inventario completado
    }

    // Getters
    public double getCurrentTime() { return currentTime; } // Obtener tiempo actual
    public double getEndTime() { return endTime; } // Obtener tiempo final
    public boolean isRunning() { return isRunning; } // Verificar si está ejecutándose
    public boolean isPaused() { return isPaused; } // Verificar si está pausado
    public Map<String, Location> getLocations() { return locations; } // Obtener mapa de ubicaciones
    public Crane getCrane() { return crane; } // Obtener grúa
    public Statistics getStatistics() { return statistics; } // Obtener estadísticas
    public int getAnimationSpeed() { return animationSpeed; } // Obtener velocidad de animación
    public void setAnimationSpeed(int speed) { this.animationSpeed = Math.max(1, Math.min(100, speed)); } // Establecer velocidad de animación (limitada 1-100)
    public ShiftCalendar getShiftCalendar() { return shiftCalendar; } // Obtener calendario de turnos
    public List<Valve> getAllValves() { return new ArrayList<>(allValves); } // Obtener copia de todas las válvulas
    public List<Valve> getCompletedValves() { return new ArrayList<>(completedValves); } // Obtener copia de válvulas completadas
    // Obtener total de válvulas en sistema
    public int getTotalValvesInSystem() {
        return allValves.size() - completedValves.size(); // Total - completadas
    }
    public PathNetwork getPathNetwork() { return pathNetwork; } // Obtener red de rutas
    public int getDockToAlmacenMoves() { return dockToAlmacenMoves; } // Obtener movimientos DOCK a almacén
    public boolean hasPendingEvents() { return !eventQueue.isEmpty(); } // Verificar si hay eventos pendientes
    public int getCompletedInventoryCount() { return completedInventoryCount; } // Obtener contador de inventario completado
    public double getLastOperationalTime() { return lastOperationalTime; } // Obtener último tiempo operacional

    // Método para verificar si simulación está completa
    public boolean isSimulationComplete() {
        if (eventQueue.isEmpty()) { // Si no hay eventos
            return true; // Simulación completa
        }

        if (currentTime < endTime - SAMPLE_EPSILON) { // Si no se ha alcanzado el tiempo final
            return false; // Simulación no completa
        }

        return !hasOperationalEvents(); // Retornar negación de eventos operacionales
    }

    // Método para obtener tiempo de espera de animación en milisegundos
    private long getAnimationWaitMillis() {
        if (animationSpeed >= 90) { // Si velocidad >= 90
            return 2; // Retornar 2 ms
        }
        if (animationSpeed >= 70) { // Si velocidad >= 70
            return 6; // Retornar 6 ms
        }
        if (animationSpeed >= 40) { // Si velocidad >= 40
            return 20; // Retornar 20 ms
        }
        return 50; // Retornar 50 ms (velocidad baja)
    }

    // Debug helper to inspect valves remaining in a given location (e.g., DOCK)
    // Método auxiliar de depuración para inspeccionar válvulas en ubicación
    public List<String> getValveDetailsForLocation(String locationName) {
        Location location = locations.get(locationName); // Obtener ubicación
        if (location == null) { // Si no existe
            return Collections.emptyList(); // Retornar lista vacía
        }

        List<String> details = new ArrayList<>(); // Crear lista de detalles
        for (Valve valve : location.getAllValves()) { // Iterar por válvulas en ubicación
            String next = determineNextLocation(valve); // Determinar próxima ubicación
            details.add(String.format("%s | estado=%s | paso=%d | siguiente=%s", // Formatear detalle
                valve, // Válvula
                valve.getState(), // Estado
                valve.getCurrentStep(), // Paso actual
                next == null ? "-" : next)); // Próxima ubicación
        }
        return details; // Retornar lista de detalles
    }

    // Método para determinar próxima ubicación de válvula
    private String determineNextLocation(Valve valve) {
        Location current = valve.getCurrentLocation(); // Obtener ubicación actual
        if (current == null) { // Si no hay ubicación
            return null; // Retornar nulo
        }

        if ("DOCK".equals(current.getName())) { // Si está en DOCK
            return valve.getNextAlmacen(); // Retornar próximo almacén
        }

        if (valve.isRouteComplete()) { // Si ruta está completa
            return "STOCK"; // Retornar STOCK
        }

        return valve.getNextAlmacen(); // Retornar próximo almacén
    }
}

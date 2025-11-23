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
    private Map<String, Operator> operators; // Mapa de operadores (4 operadores + 1 camión)
    private Map<String, JoinOperation> joinOperations; // Operaciones JOIN (COCCION, FERMENTACION, EMPACADO)
    private PathNetwork pathNetwork; // Red de rutas para movimientos
    private ShiftCalendar shiftCalendar; // Calendario de turnos laborales
    private final Set<String> pendingMachineWakeups; // Locaciones pendientes de despertar al inicio del turno
    private static class TransferRequest {
        final Valve entity;
        final String origin;
        final String destination;
        final String operatorKey;

        TransferRequest(Valve entity, String origin, String destination, String operatorKey) {
            this.entity = entity;
            this.origin = origin;
            this.destination = destination;
            this.operatorKey = operatorKey;
        }

        boolean requiresOperator() {
            return operatorKey != null && !operatorKey.isEmpty();
        }
    }

    private final Queue<TransferRequest> pendingOperatorTransfers; // Transferencias pendientes (con o sin operador)
    private final Map<Valve.Type, Integer> almacenajeAccumulator; // ACCUM 6 para ALMACENAJE

    // Statistics
    private Statistics statistics; // Objeto que recopila estadísticas
    private List<Valve> allValves; // Lista de todas las válvulas creadas
    private List<Valve> completedValves; // Lista de válvulas completadas
    private int dockToAlmacenMoves; // Contador de movimientos desde DOCK a almacenes
    private double lastOperationalTime; // Último tiempo con eventos operacionales
    private double lastSampleTime; // Último tiempo de muestreo de estadísticas
    private int completedInventoryCount; // Contador de inventario completado

    // Configuration - BREWERY SIMULATION (in minutes, not hours)
    private static final double SAMPLE_INTERVAL = 60.0; // Intervalo de muestreo de estadísticas (cada 60 minutos)
    private static final double SAMPLE_EPSILON = 1e-6; // Tolerancia numérica para comparaciones de tiempo
    private static final int DEFAULT_WEEKS_TO_SIMULATE = 1; // Número predeterminado de semanas a simular (1 semana)
    private static final double MINUTES_PER_WEEK = 4200.0; // Minutos totales en una semana (10h/día * 7 días * 60 min)

    // Constructor de la clase SimulationEngine
    public SimulationEngine() {
        Config config = Config.getInstance(); // Obtener instancia de configuración

        this.eventQueue = new PriorityBlockingQueue<>(); // Inicializar cola de eventos
        this.currentTime = 0.0; // Inicializar tiempo actual en cero
        this.isRunning = false; // Inicializar estado de ejecución
        this.isPaused = false; // Inicializar estado de pausa

        this.locations = new ConcurrentHashMap<>(); // Inicializar mapa de ubicaciones
        this.locationHoldTimes = new ConcurrentHashMap<>(); // Inicializar mapa de tiempos de retención
        this.operators = new ConcurrentHashMap<>(); // Inicializar mapa de operadores
        this.joinOperations = new ConcurrentHashMap<>(); // Inicializar operaciones JOIN
        this.almacenajeAccumulator = new ConcurrentHashMap<>(); // Inicializar acumulador ALMACENAJE
        this.pathNetwork = new PathNetwork(); // Crear nueva red de rutas
        this.shiftCalendar = new ShiftCalendar(); // Crear nuevo calendario de turnos
        this.pendingMachineWakeups = ConcurrentHashMap.newKeySet(); // Inicializar conjunto de despertares pendientes
        this.pendingOperatorTransfers = new ConcurrentLinkedQueue<>(); // Inicializar cola de transferencias
        this.statistics = new Statistics(); // Crear nuevo objeto de estadísticas
        this.allValves = Collections.synchronizedList(new ArrayList<>()); // Crear lista sincronizada de entidades
        this.completedValves = Collections.synchronizedList(new ArrayList<>()); // Crear lista sincronizada de entidades completadas
        this.dockToAlmacenMoves = 0; // Inicializar contador de movimientos
        this.lastOperationalTime = 0.0; // Inicializar último tiempo operacional
        this.lastSampleTime = 0.0; // Inicializar último tiempo de muestreo
        this.completedInventoryCount = 0; // Inicializar contador de inventario completado

        int configuredWeeks = config.getSimulationWeeks(); // Obtener semanas configuradas
        this.weeksToSimulate = configuredWeeks > 0 ? configuredWeeks : DEFAULT_WEEKS_TO_SIMULATE; // Usar semanas configuradas o valor predeterminado

        // FIJO: Simulación de exactamente 70 horas = 4200 minutos (no depende de semanas)
        this.endTime = 4200.0; // 70 horas * 60 minutos = 4200 minutos FIJO

        initializeLocations(); // Inicializar todas las ubicaciones
        initializeOperators(); // Inicializar los 4 operadores + 1 camión
        initializeJoinOperations(); // Inicializar operaciones JOIN
        scheduleArrivals(); // Programar llegadas de entidades
        scheduleStatisticsSampling(); // Programar muestreo de estadísticas
    }

    // Método para inicializar todas las ubicaciones del sistema de cervecería
    private void initializeLocations() {
        Config config = Config.getInstance(); // Obtener instancia de configuración
        locationHoldTimes.clear(); // Limpiar tiempos de retención previos

        // PROCESO DE GRANOS DE CEBADA (Proceso Principal)
        locations.put("SILO_GRANDE", new Location("SILO_GRANDE", 3, 1, new Point(100, 100)));
        locationHoldTimes.put("SILO_GRANDE", 0.0);
        
        locations.put("MALTEADO", new Location("MALTEADO", 3, 1, new Point(250, 100)));
        locationHoldTimes.put("MALTEADO", 0.0);
        
        locations.put("SECADO", new Location("SECADO", 3, 1, new Point(400, 100)));
        locationHoldTimes.put("SECADO", 0.0);
        
        locations.put("MOLIENDA", new Location("MOLIENDA", 2, 1, new Point(550, 100)));
        locationHoldTimes.put("MOLIENDA", 0.0);
        
        locations.put("MACERADO", new Location("MACERADO", 3, 1, new Point(700, 100)));
        locationHoldTimes.put("MACERADO", 0.0);
        
        locations.put("FILTRADO", new Location("FILTRADO", 2, 1, new Point(850, 100)));
        locationHoldTimes.put("FILTRADO", 0.0);

        // PROCESO DE LÚPULO
        locations.put("SILO_LUPULO", new Location("SILO_LUPULO", 10, 1, new Point(100, 250)));
        locationHoldTimes.put("SILO_LUPULO", 0.0);

        // COCCIÓN (JOIN: 1 kg granos + 4 kg lúpulo = Mosto)
        locations.put("COCCION", new Location("COCCION", 10, 1, new Point(1000, 150)));
        locationHoldTimes.put("COCCION", 0.0);

        // PROCESO POST-COCCIÓN
        locations.put("ENFRIAMIENTO", new Location("ENFRIAMIENTO", 10, 1, new Point(1150, 150)));
        locationHoldTimes.put("ENFRIAMIENTO", 0.0);

        // PROCESO DE LEVADURA
        locations.put("SILO_LEVADURA", new Location("SILO_LEVADURA", 10, 1, new Point(1150, 300)));
        locationHoldTimes.put("SILO_LEVADURA", 0.0);

        // FERMENTACIÓN (JOIN: 10 L mosto + 2 kg levadura = Cerveza)
        locations.put("FERMENTACION", new Location("FERMENTACION", 10, 1, new Point(1300, 200)));
        locationHoldTimes.put("FERMENTACION", 0.0);

        // PROCESO FINAL DE CERVEZA
        locations.put("MADURACION", new Location("MADURACION", 10, 1, new Point(1450, 200)));
        locationHoldTimes.put("MADURACION", 0.0);
        
        locations.put("INSPECCION", new Location("INSPECCION", 3, 1, new Point(1600, 200)));
        locationHoldTimes.put("INSPECCION", 0.0);
        
        locations.put("EMBOTELLADO", new Location("EMBOTELLADO", 6, 1, new Point(1600, 350)));
        locationHoldTimes.put("EMBOTELLADO", 0.0);
        
        locations.put("ETIQUETADO", new Location("ETIQUETADO", 6, 1, new Point(1600, 500)));
        locationHoldTimes.put("ETIQUETADO", 0.0);

        // PROCESO DE CAJAS
        locations.put("ALMACEN_CAJAS", new Location("ALMACEN_CAJAS", 30, 1, new Point(1450, 650)));
        locationHoldTimes.put("ALMACEN_CAJAS", 0.0);

        // EMPACADO (JOIN: 6 botellas + 1 caja = Caja con cervezas)
        locations.put("EMPACADO", new Location("EMPACADO", 1, 1, new Point(1600, 650)));
        locationHoldTimes.put("EMPACADO", 0.0);

        // ALMACENAJE Y SALIDA
        locations.put("ALMACENAJE", new Location("ALMACENAJE", 6, 1, new Point(1450, 800)));
        locationHoldTimes.put("ALMACENAJE", 0.0);
        
        locations.put("MERCADO", new Location("MERCADO", Integer.MAX_VALUE, 1, new Point(1600, 950)));
        locationHoldTimes.put("MERCADO", 0.0);
    }

    // Método para inicializar los operadores del sistema de cervecería
    private void initializeOperators() {
        // OPERADOR RECEPCIÓN: Mueve granos entre MALTEADO → SECADO → MOLIENDA
        Point homeRecepcion = locations.get("MALTEADO").getPosition();
        operators.put("OPERADOR_RECEPCION", new Operator("Operador Recepcion", 1, 90.0, "RED_RECEPCION", homeRecepcion));
        
        // OPERADOR LÚPULO: Mueve lúpulo de SILO_LUPULO → COCCION
        Point homeLupulo = locations.get("SILO_LUPULO").getPosition();
        operators.put("OPERADOR_LUPULO", new Operator("Operador Lupulo", 1, 100.0, "RED_LUPULO", homeLupulo));
        
        // OPERADOR LEVADURA: Mueve levadura de SILO_LEVADURA → FERMENTACION
        Point homeLevadura = locations.get("SILO_LEVADURA").getPosition();
        operators.put("OPERADOR_LEVADURA", new Operator("Operador Levadura", 1, 100.0, "RED_LEVADURA", homeLevadura));
        
        // OPERADOR EMPACADO: Mueve cajas de EMPACADO → ALMACENAJE
        Point homeEmpacado = locations.get("EMPACADO").getPosition();
        operators.put("OPERADOR_EMPACADO", new Operator("Operador Empacado", 1, 100.0, "RED_EMPACADO", homeEmpacado));
        
        // CAMIÓN: Mueve cajas de ALMACENAJE → MERCADO (ACCUM 6)
        Point homeCamion = locations.get("ALMACENAJE").getPosition();
        operators.put("CAMION", new Operator("Camion", 1, 100.0, "RED_EMPACADO", homeCamion));
    }
    
    // Método para inicializar las operaciones JOIN
    private void initializeJoinOperations() {
        // JOIN COCCION: 1 GRANOS_CEBADA + 4 LUPULO → 1 MOSTO (ProModel: JOIN 4 LUPULO)
        Map<Valve.Type, Integer> coccionRequirements = new HashMap<>();
        coccionRequirements.put(Valve.Type.GRANOS_CEBADA, 1);
        coccionRequirements.put(Valve.Type.LUPULO, 4);
        joinOperations.put("COCCION", new JoinOperation("COCCION", coccionRequirements, Valve.Type.MOSTO));
        
        // JOIN FERMENTACION: 1 MOSTO + 2 LEVADURA → 1 CERVEZA (ProModel: JOIN 2 LEVADURA)
        Map<Valve.Type, Integer> fermentacionRequirements = new HashMap<>();
        fermentacionRequirements.put(Valve.Type.MOSTO, 1);
        fermentacionRequirements.put(Valve.Type.LEVADURA, 2);
        joinOperations.put("FERMENTACION", new JoinOperation("FERMENTACION", fermentacionRequirements, Valve.Type.CERVEZA));
        
        // JOIN EMPACADO: 1 CAJA_VACIA + 6 BOTELLA_CERVEZA → 1 CAJA_CERVEZA (ProModel: JOIN 6 BOTELLA_CON_CERVEZA)
        Map<Valve.Type, Integer> empacadoRequirements = new HashMap<>();
        empacadoRequirements.put(Valve.Type.CAJA_VACIA, 1);
        empacadoRequirements.put(Valve.Type.BOTELLA_CERVEZA, 6);
        joinOperations.put("EMPACADO", new JoinOperation("EMPACADO", empacadoRequirements, Valve.Type.CAJA_CERVEZA));
    }

    // Método para programar llegadas de entidades (arribos continuos según frecuencia)
    private void scheduleArrivals() {
        // GRANOS DE CEBADA: cada 25 minutos a SILO_GRANDE
        scheduleEntityArrivals(Valve.Type.GRANOS_CEBADA, 25.0, "SILO_GRANDE");
        
        // LÚPULO: cada 10 minutos a SILO_LUPULO
        scheduleEntityArrivals(Valve.Type.LUPULO, 10.0, "SILO_LUPULO");
        
        // LEVADURA: cada 20 minutos a SILO_LEVADURA
        scheduleEntityArrivals(Valve.Type.LEVADURA, 20.0, "SILO_LEVADURA");
        
        // CAJAS VACÍAS: cada 30 minutos a ALMACEN_CAJAS
        scheduleEntityArrivals(Valve.Type.CAJA_VACIA, 30.0, "ALMACEN_CAJAS");
    }

    // Método para programar llegadas de un tipo específico de entidad con frecuencia
    private void scheduleEntityArrivals(Valve.Type type, double frequency, String arrivalLocation) {
        // ProModel: Primera Vez 0, Ocurrencias INF, Frecuencia dada
        // Programar arribos desde tiempo 0 hasta endTime con la frecuencia dada
        double arrivalTime = 0.0; // Primera llegada en tiempo 0
        
        while (arrivalTime < endTime) {
            Valve entity = new Valve(type, arrivalTime); // Crear nueva entidad
            allValves.add(entity); // Agregar a la lista de todas las entidades
            eventQueue.add(new Event(Event.Type.ARRIVAL, arrivalTime, entity, arrivalLocation)); // Programar evento de llegada
            
            arrivalTime += frequency; // Siguiente arribo según frecuencia
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

        while (!eventQueue.isEmpty() && isRunning && currentTime < endTime) { // Bucle SOLO hasta endTime (4200 minutos = 70 horas EXACTAS)
            if (isPaused) { // Si está pausado
                try {
                    Thread.sleep(100); // Esperar 100 ms
                    continue; // Continuar al siguiente ciclo
                } catch (InterruptedException e) { // Capturar interrupción
                    break; // Salir del bucle
                }
            }

            // ESPERAR mientras algún operador está en movimiento
            // Esto hace que la simulación se RALENTICE para que la animación sea visible
            boolean anyOperatorMoving = operators.values().stream().anyMatch(Operator::isMoving);
            if (anyOperatorMoving && animationSpeed < 100) { // Si algún operador está en movimiento y velocidad < 100
                try {
                    Thread.sleep(getAnimationWaitMillis()); // Esperar según velocidad de animación
                    continue; // Continuar al siguiente ciclo
                } catch (InterruptedException e) { // Capturar interrupción
                    break; // Salir del bucle
                }
            }

            Event event = eventQueue.poll(); // Obtener y remover siguiente evento de la cola
            if (event != null) { // Si hay evento
                double eventTime = event.getTime(); // Obtener tiempo del evento
                if (eventTime >= endTime) { // Si el evento está más allá del tiempo final
                    break; // DETENER INMEDIATAMENTE - no procesar eventos después de 70 horas
                }
                currentTime = eventTime; // Actualizar tiempo actual al tiempo del evento
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
                handleStartCraneMove(event.getValve(), (TransferRequest) event.getData()); // Manejar inicio de movimiento
                break; // Salir del switch
            case END_CRANE_MOVE: // Si es fin de movimiento de operador
                // Esperar a que la animación termine antes de procesar
                if (animationSpeed < 100) { // Si velocidad de animación < 100
                    boolean anyOperatorMoving = operators.values().stream().anyMatch(Operator::isMoving);
                    while (anyOperatorMoving) { // Mientras algún operador esté en movimiento
                        try {
                            Thread.sleep(getAnimationWaitMillis()); // Esperar según velocidad de animación
                            anyOperatorMoving = operators.values().stream().anyMatch(Operator::isMoving);
                        } catch (InterruptedException e) { // Capturar interrupción
                            break; // Salir del bucle
                        }
                    }
                }
                handleEndCraneMove(event.getValve(), (TransferRequest) event.getData()); // Manejar fin de movimiento
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

    // Método para manejar la llegada de una entidad (modificado para cervecería)
    private void handleArrival(Valve entity) {
        // Obtener la locación de arribo del evento (pasada como data en Event)
        Event currentEvent = eventQueue.stream()
            .filter(e -> e.getType() == Event.Type.ARRIVAL && e.getValve() == entity)
            .findFirst().orElse(null);
        
        String arrivalLocationName = entity.getNextLocation(); // Primera locación en su ruta
        if (arrivalLocationName == null) {
            arrivalLocationName = "SILO_GRANDE"; // Por defecto
        }
        
        Location arrivalLocation = locations.get(arrivalLocationName);
        if (arrivalLocation == null) {
            return; // Ubicación no existe
        }
        
        arrivalLocation.addToQueue(entity); // Agregar entidad a la cola de la locación
        updateLocationMetrics(arrivalLocation); // Actualizar métricas de la ubicación
        entity.setState(Valve.State.IN_QUEUE); // Establecer estado en cola
        entity.setCurrentLocation(arrivalLocation); // Establecer ubicación actual
        statistics.recordArrival(entity); // Registrar llegada en estadísticas

        // Si la locación tiene procesamiento, programar inicio
        double processTime = entity.getCurrentProcessingTime();
        if (processTime > 0 && arrivalLocation.hasAvailableUnit()) {
            arrivalLocation.moveToProcessing(entity);
            entity.startProcessing(currentTime);
            updateLocationMetrics(arrivalLocation);
            eventQueue.add(new Event(Event.Type.END_PROCESSING, currentTime + processTime, entity, null));
        } else if (processTime == 0) {
            // Sin procesamiento en esta ubicación, avanzar inmediatamente
            arrivalLocation.removeValve(entity);
            updateLocationMetrics(arrivalLocation);
            entity.advanceStep();
            enqueueTransferRequest(entity, arrivalLocation);
            tryScheduleOperatorMove();
        }
    }

    // Método para manejar el fin de procesamiento de una entidad
    private void handleEndProcessing(Valve entity) {
        Location currentLoc = entity.getCurrentLocation(); // Obtener ubicación actual
        entity.endProcessing(currentTime); // Finalizar procesamiento
        
        // Incrementar contador de entidades procesadas para esta ubicación
        if (currentLoc != null) { // Si hay ubicación
            String locName = currentLoc.getName();
            LocationStats locStats = statistics.getOrCreateLocationStats(locName);
            locStats.incrementValvesProcessed();
        }

        String locName = currentLoc != null ? currentLoc.getName() : "";
        
        // Manejar casos especiales según la ubicación
        if ("COCCION".equals(locName) || "FERMENTACION".equals(locName) || "EMPACADO".equals(locName)) {
            // Operación JOIN
            handleJoinOperation(entity, locName);
        } else if ("INSPECCION".equals(locName)) {
            // Inspección con probabilidad 90%/10%
            handleInspection(entity);
        } else if ("EMBOTELLADO".equals(locName)) {
            // FIRST 6: 1 CERVEZA → 6 BOTELLA_CERVEZA (ProModel routing)
            handleEmbotellado(entity);
        } else if ("ALMACENAJE".equals(locName)) {
            // ACCUM 6 para ALMACENAJE
            handleAlmacenajeAccumulation(entity);
        } else {
            // Procesamiento normal: avanzar a siguiente ubicación
            entity.advanceStep();
            currentLoc.removeValve(entity);
            updateLocationMetrics(currentLoc);
            
            // Programar para movimiento
            enqueueTransferRequest(entity, currentLoc);
            tryScheduleOperatorMove();
        }
    }
    
    // Método para manejar operaciones JOIN
    private void handleJoinOperation(Valve entity, String locationName) {
        JoinOperation join = joinOperations.get(locationName);
        if (join == null) {
            return;
        }
        
        Location loc = locations.get(locationName);
        if (loc != null) {
            loc.removeValve(entity);
            updateLocationMetrics(loc);
        }
        
        // Agregar entidad a la operación JOIN
        boolean ready = join.addEntity(entity);
        
        if (ready) {
            // Ejecutar JOIN y crear nueva entidad
            Valve newEntity = join.execute(currentTime);
            if (newEntity != null) {
                newEntity.setCurrentLocation(loc);
                allValves.add(newEntity);
                
                // Programar procesamiento de la nueva entidad
                Double processTime = newEntity.getCurrentProcessingTime();
                if (processTime != null && processTime > 0) {
                    newEntity.startProcessing(currentTime);
                    eventQueue.add(new Event(Event.Type.END_PROCESSING, 
                        currentTime + processTime, newEntity, null));
                } else {
                    // Sin procesamiento adicional, mover directamente
                    enqueueTransferRequest(newEntity, loc);
                    tryScheduleOperatorMove();
                }
            }
        }
    }
    
    // Método para manejar inspección (90% aprueba, 10% rechaza)
    private void handleInspection(Valve entity) {
        Location inspeccionLoc = locations.get("INSPECCION");
        if (inspeccionLoc != null) {
            inspeccionLoc.removeValve(entity);
            updateLocationMetrics(inspeccionLoc);
        }
        
        double random = Math.random();
        if (random < 0.9) {
            // 90% aprobado → continúa a EMBOTELLADO
            // CERVEZA avanza a EMBOTELLADO (no se convierte aún a botella)
            entity.advanceStep(); // Avanzar paso en la ruta
            enqueueTransferRequest(entity, inspeccionLoc);
            tryScheduleOperatorMove();
        } else {
            // 10% rechazado → EXIT (registro de completitud pero rechazada)
            entity.setState(Valve.State.COMPLETED);
            completedValves.add(entity);
            statistics.recordCompletion(entity, currentTime);
        }
    }
    
    // Método para manejar EMBOTELLADO: 1 CERVEZA → 6 BOTELLA_CERVEZA (FIRST 6)
    private void handleEmbotellado(Valve entity) {
        Location embotellLoc = locations.get("EMBOTELLADO");
        if (embotellLoc != null) {
            embotellLoc.removeValve(entity);
            updateLocationMetrics(embotellLoc);
        }
        
        // Crear 6 botellas de cerveza a partir de 1 cerveza
        for (int i = 0; i < 6; i++) {
            Valve bottleEntity = new Valve(Valve.Type.BOTELLA_CERVEZA, currentTime);
            bottleEntity.setCurrentLocation(embotellLoc);
            allValves.add(bottleEntity);
            
            // Programar movimiento a ETIQUETADO
            enqueueTransferRequest(bottleEntity, embotellLoc);
        }
        
        // Marcar CERVEZA original como completada (consumida en el embotellado)
        entity.setState(Valve.State.COMPLETED);
        completedValves.add(entity);
        
        tryScheduleOperatorMove();
    }
    
    // Método para manejar acumulación en ALMACENAJE (ACCUM 6)
    private void handleAlmacenajeAccumulation(Valve entity) {
        Location almacenajeLoc = locations.get("ALMACENAJE");
        if (almacenajeLoc != null) {
            almacenajeLoc.removeValve(entity);
            updateLocationMetrics(almacenajeLoc);
        }
        
        // Acumular cajas de cerveza (ACCUM 6 según ProModel)
        Valve.Type type = entity.getType();
        int currentCount = almacenajeAccumulator.getOrDefault(type, 0) + 1;
        almacenajeAccumulator.put(type, currentCount);
        
        // Cuando alcanzamos 6 cajas, liberar el lote completo al MERCADO
        if (currentCount >= 6) {
            almacenajeAccumulator.put(type, 0); // Reiniciar acumulador
            
            // Esta entidad representa el lote de 6, avanza a MERCADO
            entity.advanceStep();
            enqueueTransferRequest(entity, almacenajeLoc);
            tryScheduleOperatorMove();
        } else {
            // Aún no alcanzamos 6, marcar como completada (consumida en el batch)
            // El lote 6 se liberará cuando llegue la sexta caja
            entity.setState(Valve.State.COMPLETED);
            completedValves.add(entity);
        }
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
        if (shiftCalendar.isWorkingTime(currentTime)) { // Si es hora laboral
            tryScheduleOperatorMove(); // Intentar programar movimiento de operador
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
                        enqueueTransferRequest(valve, location); // Agregar a transferencias pendientes

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
    // Método eliminado: scheduleCraneWakeup (obsoleto con operadores)

    // Método para manejar inicio de turno
    private void handleShiftStart(String machineName) {
        if (machineName == null) { // Si no hay nombre
            return; // Salir del método
        }

        // Special case: operator wakeup
        if ("OPERATOR".equals(machineName)) { // Si es despertar de operadores
            tryScheduleOperatorMove(); // Intentar programar movimiento
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
        tryScheduleOperatorMove(); // Intentar programar movimiento de operador
    }

    // Método para intentar programar movimiento de grúa
    private void tryScheduleOperatorMove() {
        if (!shiftCalendar.isWorkingTime(currentTime)) { // Si no es hora laboral
            return; // Los operadores no trabajan fuera de turno
        }

        TransferRequest request = pollPendingTransferRequest(); // Buscar transferencia pendiente
        if (request != null) { // Si se encontró
            scheduleOperatorMove(request); // Programar movimiento
        }
    }

    // Método eliminado: findFirstAvailableValveInDock (obsoleto con nuevo flujo)

    // Método para obtener y remover transferencia pendiente
    private TransferRequest pollPendingTransferRequest() {
        Iterator<TransferRequest> iterator = pendingOperatorTransfers.iterator();
        while (iterator.hasNext()) {
            TransferRequest request = iterator.next();
            Valve entity = request.entity;
            if (entity == null || !entity.isReady(currentTime)) {
                continue;
            }

            Location destination = locations.get(request.destination);
            if (destination == null || !destination.canAccept()) {
                continue;
            }

            if (request.requiresOperator()) {
                Operator operator = operators.get(request.operatorKey);
                if (operator == null || operator.isBusy()) {
                    continue; // Operador no disponible aún
                }
            }

            iterator.remove();
            return request;
        }
        return null;
    }

    // Método para obtener próximo destino de una entidad
    private String getNextDestination(Valve entity) {
        Location currentLocation = entity.getCurrentLocation(); // Obtener ubicación actual
        if (currentLocation == null) { // Si no hay ubicación
            return null; // Retornar nulo
        }

        if (entity.isRouteComplete()) { // Si ruta está completa
            return "MERCADO"; // Retornar MERCADO (EXIT)
        }

        return entity.getNextLocation(); // Retornar próxima ubicación de la ruta
    }

    // Método para programar movimiento de operador
    private void scheduleOperatorMove(TransferRequest request) {
        if (request == null) {
            return;
        }

        Valve entity = request.entity;
        Location from = request.origin != null ? locations.get(request.origin) : entity.getCurrentLocation();
        Location to = locations.get(request.destination);

        if (entity == null || from == null || to == null) {
            return; // Datos insuficientes
        }

        if (!request.requiresOperator()) {
            completeDirectTransfer(request);
            return;
        }

        Operator operator = operators.get(request.operatorKey);
        if (operator == null || operator.isBusy()) {
            return;
        }

        operator.setBusy(true);

        if (entity.getState() == Valve.State.BLOCKED) {
            entity.endBlocked(currentTime);
        }

        PathNetwork.PathResult pathResult = pathNetwork.getPathForLocations(from.getName(), to.getName());
        List<Point> pathPoints;
        List<Double> segmentDistances;
        double totalDistanceMeters;

        if (pathResult.isValid() && pathResult.getPoints().size() >= 2) {
            pathPoints = pathResult.getPoints();
            segmentDistances = pathResult.getSegmentDistances();
            totalDistanceMeters = pathResult.getTotalDistance();
        } else {
            Point start = new Point(from.getPosition());
            Point end = new Point(to.getPosition());
            pathPoints = Arrays.asList(start, end);
            double euclidean = start.distance(end);
            segmentDistances = Collections.singletonList(euclidean);
            totalDistanceMeters = euclidean;
        }

        double travelTime = operator.calculateTravelTime(totalDistanceMeters, true);

        operator.addTravelTime(travelTime);
        entity.addMovementTime(travelTime);
        entity.endWaiting(currentTime);

        operator.startMove(pathPoints, segmentDistances, totalDistanceMeters, travelTime, currentTime, animationSpeed);

        eventQueue.add(new Event(Event.Type.START_CRANE_MOVE, currentTime, entity, request));
        eventQueue.add(new Event(Event.Type.END_CRANE_MOVE, currentTime + travelTime, entity, request));
    }

    private void completeDirectTransfer(TransferRequest request) {
        Valve entity = request.entity;
        Location from = request.origin != null ? locations.get(request.origin) : entity.getCurrentLocation();
        Location to = locations.get(request.destination);

        if (entity == null || to == null) {
            return;
        }

        if (from != null) {
            from.removeValve(entity);
            updateLocationMetrics(from);
        }

        to.addToQueue(entity);
        updateLocationMetrics(to);
        entity.setState(Valve.State.IN_QUEUE);
        entity.setCurrentLocation(to);
        applyHoldTime(entity, to.getName(), currentTime);
        processEntityAtLocation(entity, to);
        tryScheduleOperatorMove();
    }

    private void enqueueTransferRequest(Valve entity, Location originLocation) {
        if (entity == null) {
            return;
        }

        String destination = getNextDestination(entity);
        if (destination == null) {
            return;
        }

        String originName = originLocation != null ? originLocation.getName() : (entity.getCurrentLocation() != null ? entity.getCurrentLocation().getName() : null);
        String operatorKey = requiresOperatorMove(originName, destination)
            ? getResponsibleOperator(originName != null ? originName : "", destination)
            : null;

        pendingOperatorTransfers.add(new TransferRequest(entity, originName, destination, operatorKey));
        entity.setState(Valve.State.WAITING_CRANE);
        entity.startWaiting(currentTime);
    }

    private boolean requiresOperatorMove(String origin, String destination) {
        return destination != null && !destination.equals(origin);
    }
    
    // Método para determinar qué operador es responsable de un movimiento
    private String getResponsibleOperator(String from, String to) {
        // OPERADOR_RECEPCION: Maneja flujo de granos (MALTEADO → SECADO → MOLIENDA)
        if (from.equals("MALTEADO") || from.equals("SECADO") || from.equals("MOLIENDA")) {
            return "OPERADOR_RECEPCION";
        }
        
        // OPERADOR_LUPULO: Maneja flujo de lúpulo (SILO_LUPULO → COCCION)
        if (from.equals("SILO_LUPULO") && to.equals("COCCION")) {
            return "OPERADOR_LUPULO";
        }
        
        // OPERADOR_LEVADURA: Maneja flujo de levadura (SILO_LEVADURA → FERMENTACION)
        if (from.equals("SILO_LEVADURA") && to.equals("FERMENTACION")) {
            return "OPERADOR_LEVADURA";
        }
        
        // OPERADOR_EMPACADO: Maneja flujo de empacado (EMPACADO → ALMACENAJE)
        if (from.equals("EMPACADO") && to.equals("ALMACENAJE")) {
            return "OPERADOR_EMPACADO";
        }
        
        // CAMION: Maneja flujo de ALMACENAJE → MERCADO (ACCUM 6)
        if (from.equals("ALMACENAJE") && to.equals("MERCADO")) {
            return "CAMION";
        }
        
        // Por defecto, usar OPERADOR_RECEPCION
        return "OPERADOR_RECEPCION";
    }

    // Método para manejar inicio de movimiento de operador
    private void handleStartCraneMove(Valve entity, TransferRequest request) {
        if (entity == null || request == null) {
            return;
        }

        Location from = request.origin != null ? locations.get(request.origin) : entity.getCurrentLocation();
        if (from != null) {
            from.removeValve(entity);
            updateLocationMetrics(from);
        }

        if (entity.getState() == Valve.State.IN_QUEUE || entity.getState() == Valve.State.WAITING_CRANE) {
            entity.endWaiting(currentTime);
        }

        Operator operator = request.operatorKey != null ? operators.get(request.operatorKey) : null;
        if (operator != null) {
            operator.pickupEntity(entity);
        }

        entity.setState(Valve.State.IN_TRANSIT);
        entity.setCurrentLocation(null);
    }

    // Método para manejar fin de movimiento de operador
    private void handleEndCraneMove(Valve entity, TransferRequest request) {
        if (entity == null || request == null) {
            return;
        }

        Location destLoc = locations.get(request.destination);
        Operator operator = request.operatorKey != null ? operators.get(request.operatorKey) : null;

        if (operator != null) {
            operator.completeTrip();
            operator.releaseEntity();
            operator.setBusy(false);
        }

        if (destLoc == null) {
            return;
        }

        if ("MERCADO".equals(request.destination)) {
            destLoc.addToQueue(entity);
            updateLocationMetrics(destLoc);
            destLoc.removeValve(entity);
            updateLocationMetrics(destLoc);
            entity.setState(Valve.State.COMPLETED);
            entity.setCurrentLocation(null);
            completedValves.add(entity);
            statistics.recordCompletion(entity, currentTime);
            completedInventoryCount++;
        } else {
            destLoc.addToQueue(entity);
            updateLocationMetrics(destLoc);
            entity.setState(Valve.State.IN_QUEUE);
            entity.setCurrentLocation(destLoc);
            applyHoldTime(entity, destLoc.getName(), currentTime);
            processEntityAtLocation(entity, destLoc);
        }

        tryScheduleOperatorMove();
    }
    
    // Método para procesar entidad en una ubicación
    private void processEntityAtLocation(Valve entity, Location location) {
        String locName = location.getName();
        
        // Verificar si hay procesamiento que hacer en esta ubicación
        Double processTime = Valve.LOCATION_PROCESS_TIMES.get(locName);
        if (processTime != null && processTime > 0) {
            // Mover de cola a procesamiento
            if (location.hasAvailableUnit()) {
                location.moveToProcessing(entity);
                updateLocationMetrics(location);
                entity.setReadyTime(currentTime); // Establecer tiempo de preparación
                entity.startProcessing(currentTime); // Iniciar procesamiento
                
                eventQueue.add(new Event(Event.Type.END_PROCESSING, // Agregar evento de fin de procesamiento
                    currentTime + processTime, entity, null)); // Con tiempo actual + tiempo de procesamiento
            }
        } else {
            // Sin procesamiento, programar para siguiente movimiento inmediato
            location.removeValve(entity);
            updateLocationMetrics(location);
            entity.advanceStep();
            enqueueTransferRequest(entity, location);
            tryScheduleOperatorMove();
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

        // Update operator statistics
        for (Map.Entry<String, Operator> entry : operators.entrySet()) {
            Operator operator = entry.getValue();
            operator.updateStatistics(sampleTime); // Actualizar estadísticas de operador
            
            Config config = Config.getInstance(); // Obtener configuración
            int operatorUnits = config.getResourceUnits(operator.getName(), operator.getUnits()); // Obtener unidades
            double weeksSimulated = Math.max(sampleTime, SAMPLE_EPSILON) / (MINUTES_PER_WEEK / 60.0); // Calcular semanas simuladas
            double defaultScheduled = shiftCalendar.getTotalWorkingHoursPerWeek() * weeksSimulated; // Calcular horas programadas
            double scheduledHours = config.getResourceScheduledHours(operator.getName(), defaultScheduled); // Obtener horas programadas
            int totalTrips = operator.getTotalTrips(); // Obtener total de viajes

            double defaultHandleMinutes = totalTrips > 0 // Calcular minutos de manejo promedio
                ? (operator.getTotalUsageTime() * 60.0) / totalTrips // Si hay viajes
                : 0.0; // Si no hay viajes
            double avgHandleMinutes = config.getResourceAvgHandleMinutes(operator.getName(), defaultHandleMinutes); // Obtener de configuración
            double defaultTravelMinutes = totalTrips > 0 // Calcular minutos de viaje promedio
                ? (operator.getTotalTravelTime() * 60.0) / totalTrips // Si hay viajes
                : 0.0; // Si no hay viajes
            double avgTravelMinutes = config.getResourceAvgTravelMinutes(operator.getName(), defaultTravelMinutes); // Obtener de configuración
            double avgParkMinutes = config.getResourceAvgParkMinutes(operator.getName(), 0.0); // Obtener minutos de estacionamiento
            double blockedPercent = config.getResourceBlockedPercent(operator.getName(), 0.0); // Obtener porcentaje bloqueado

            double totalWorkMinutes = totalTrips * (avgHandleMinutes + avgTravelMinutes + avgParkMinutes); // Calcular minutos totales de trabajo
            double utilization = scheduledHours > 1e-9 // Calcular utilización
                ? (totalWorkMinutes / 60.0) / scheduledHours * 100.0 // Si hay horas programadas
                : 0.0; // Si no hay horas programadas

            statistics.updateCraneStats( // Actualizar estadísticas de operador (reutilizando método crane)
                operatorUnits, // Unidades
                scheduledHours, // Horas programadas
                totalWorkMinutes, // Minutos totales de trabajo
                totalTrips, // Total de viajes
                avgHandleMinutes, // Minutos promedio de manejo
                avgTravelMinutes, // Minutos promedio de viaje
                avgParkMinutes, // Minutos promedio de estacionamiento
                blockedPercent, // Porcentaje bloqueado
                utilization, // Utilización
                sampleTime); // Tiempo de muestreo
        }

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

        // Después de liberación de retención, intentar mover entidad
        enqueueTransferRequest(valve, currentLocation);
        tryScheduleOperatorMove(); // Intentar programar movimiento de operador
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
        if ("OPERATOR".equals(name)) { // Si es operador
            if (!pendingOperatorTransfers.isEmpty()) { // Si hay transferencias pendientes
                return true; // Retornar verdadero
            }

            Location dock = locations.get("MALTEADO"); // Obtener primera ubicación
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
        initializeOperators(); // Inicializar operadores
        initializeJoinOperations(); // Inicializar operaciones JOIN
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
    public Map<String, Operator> getOperators() { return operators; } // Obtener operadores
    
    // Método temporal para compatibilidad con GUI (retorna un objeto Crane adaptado desde Operator)
    @Deprecated
    public Crane getCrane() {
        // Retornar una instancia de Crane para compatibilidad temporal con GUI
        // TODO: Actualizar GUI para usar operadores múltiples
        if (operators.isEmpty()) return null;
        
        Operator firstOp = operators.values().iterator().next();
        Point pos = firstOp.getCurrentPosition();
        if (pos == null) pos = new Point(0, 0);
        
        // Crear Crane temporal con datos del primer operador
        return new Crane("Operador Principal", 1, firstOp.getSpeed(), firstOp.getSpeed(), pos);
    }
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

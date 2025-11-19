package core;

import model.*;
import statistics.Statistics;
import statistics.LocationStats;
import utils.Config;
import java.awt.Point;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SimulationEngine {
    // Core simulation components
    private PriorityBlockingQueue<Event> eventQueue;
    private double currentTime;
    private final double endTime;
    private final int weeksToSimulate;
    private boolean isRunning;
    private boolean isPaused;
    private int animationSpeed = 50; // Default medium speed (1-100)

    // Model components
    private Map<String, Location> locations;
    private Map<String, Double> locationHoldTimes;
    private Crane crane;
    private PathNetwork pathNetwork;
    private ShiftCalendar shiftCalendar;
    private final Set<String> pendingMachineWakeups;
    private final Queue<Valve> pendingCraneTransfers;

    // Statistics
    private Statistics statistics;
    private List<Valve> allValves;
    private List<Valve> completedValves;
    private int dockToAlmacenMoves;
    private double lastOperationalTime;
    private double lastSampleTime;
    private int completedInventoryCount;

    // Configuration
    private static final double SAMPLE_INTERVAL = 1.0; // Sample stats every hour
    private static final double SAMPLE_EPSILON = 1e-6;
    private static final int DEFAULT_WEEKS_TO_SIMULATE = 8;
    private static final double HOURS_PER_WEEK = 168.0;

    public SimulationEngine() {
        Config config = Config.getInstance();

        this.eventQueue = new PriorityBlockingQueue<>();
        this.currentTime = 0.0;
        this.isRunning = false;
        this.isPaused = false;

        this.locations = new ConcurrentHashMap<>();
        this.locationHoldTimes = new ConcurrentHashMap<>();
        this.pathNetwork = new PathNetwork();
        this.shiftCalendar = new ShiftCalendar();
        this.pendingMachineWakeups = ConcurrentHashMap.newKeySet();
        this.pendingCraneTransfers = new ConcurrentLinkedQueue<>();
        this.statistics = new Statistics();
        this.allValves = Collections.synchronizedList(new ArrayList<>());
        this.completedValves = Collections.synchronizedList(new ArrayList<>());
        this.dockToAlmacenMoves = 0;
        this.lastOperationalTime = 0.0;
        this.lastSampleTime = 0.0;
        this.completedInventoryCount = 0;

        int configuredWeeks = config.getSimulationWeeks();
        this.weeksToSimulate = configuredWeeks > 0 ? configuredWeeks : DEFAULT_WEEKS_TO_SIMULATE;

        int firstWorkingHour = shiftCalendar.getFirstWorkingHourOfWeek();
        int lastWorkingHourExclusive = shiftCalendar.getLastWorkingHourExclusive();
        double weeklySpan = Math.max(0.0, lastWorkingHourExclusive - firstWorkingHour);
        if (weeklySpan <= 0.0) {
            weeklySpan = HOURS_PER_WEEK;
        }
        this.endTime = ((weeksToSimulate - 1) * HOURS_PER_WEEK) + weeklySpan;

        initializeLocations();
        initializeCrane();
        scheduleArrivals();
        scheduleStatisticsSampling();
    }

    private void initializeLocations() {
        Config config = Config.getInstance();
        locationHoldTimes.clear();
        int m1Units = Math.max(1, config.getMachineUnits("m1"));
        int m2Units = Math.max(1, config.getMachineUnits("m2"));
        int m3Units = Math.max(1, config.getMachineUnits("m3"));
        
        // Leer capacidades de almacenes desde config
        int almacenM1Cap = config.getInt("location.almacen_m1.capacity", 20);
        int almacenM2Cap = config.getInt("location.almacen_m2.capacity", 20);
        int almacenM3Cap = config.getInt("location.almacen_m3.capacity", 30);

        // Create locations with exact ProModel specifications
        locations.put("DOCK", new Location("DOCK", Integer.MAX_VALUE, 1,
            new Point(190, 140)));
        locationHoldTimes.put("DOCK", config.getDouble("location.dock.hold_time", 0.0));
        locations.put("STOCK", new Location("STOCK", Integer.MAX_VALUE, 1,
            new Point(200, 420)));
        locationHoldTimes.put("STOCK", config.getDouble("location.stock.hold_time", 0.0));

        locations.put("Almacen_M1", new Location("Almacen_M1", almacenM1Cap, 1,
            new Point(560, 520)));
        locationHoldTimes.put("Almacen_M1", config.getDouble("location.almacen_m1.hold_time", 0.0));
        // M1 parent - SOLO para enrutamiento, no procesa válvulas
        // Capacidad 0 para que las válvulas vayan directo a M1.x
        locations.put("M1", new Location("M1", 0, m1Units,
            new Point(560, 380)));
        // Individual M1 units - ESTAS procesan las válvulas
        for (int i = 1; i <= m1Units; i++) {
            locations.put("M1." + i, new Location("M1." + i, 1, 1,
                new Point(560, 380)));
            pathNetwork.registerLocationNode("M1." + i, pathNetwork.getNodeForLocation("M1"));
        }

        locations.put("Almacen_M2", new Location("Almacen_M2", almacenM2Cap, 1,
            new Point(960, 320)));
        locationHoldTimes.put("Almacen_M2", config.getDouble("location.almacen_m2.hold_time", 0.0));
        // M2 parent - SOLO para enrutamiento
        locations.put("M2", new Location("M2", 0, m2Units,
            new Point(760, 300)));
        for (int i = 1; i <= m2Units; i++) {
            locations.put("M2." + i, new Location("M2." + i, 1, 1,
                new Point(760, 300)));
            pathNetwork.registerLocationNode("M2." + i, pathNetwork.getNodeForLocation("M2"));
        }

        locations.put("Almacen_M3", new Location("Almacen_M3", almacenM3Cap, 1,
            new Point(1080, 180)));
        locationHoldTimes.put("Almacen_M3", config.getDouble("location.almacen_m3.hold_time", 0.0));
        // M3 parent - SOLO para enrutamiento
        locations.put("M3", new Location("M3", 0, m3Units,
            new Point(900, 160)));
        for (int i = 1; i <= m3Units; i++) {
            locations.put("M3." + i, new Location("M3." + i, 1, 1,
                new Point(900, 160)));
            pathNetwork.registerLocationNode("M3." + i, pathNetwork.getNodeForLocation("M3"));
        }
    }

    private void initializeCrane() {
        Point home = locations.get("DOCK").getPosition();
        // Speeds: empty=15.24 m/min, full=12.19 m/min (from ProModel)
        crane = new Crane("Grua", 1, 15.24, 12.19, home);
    }

    private void scheduleArrivals() {
        // Schedule arrivals every 168 hours (weekly)
        // CRÍTICO: Las válvulas llegan TODAS AL INICIO de cada semana
        for (int week = 0; week < weeksToSimulate; week++) {
            double arrivalTime = week * HOURS_PER_WEEK;

            // Ajustar al primer turno laboral de la semana
            if (!shiftCalendar.isWorkingTime(arrivalTime)) {
                arrivalTime = shiftCalendar.getNextWorkingTime(arrivalTime);
            }

            // Schedule each valve type according to ProModel
            scheduleValveArrivals(Valve.Type.VALVULA_1, 10, arrivalTime);
            scheduleValveArrivals(Valve.Type.VALVULA_2, 40, arrivalTime);
            scheduleValveArrivals(Valve.Type.VALVULA_3, 10, arrivalTime);
            scheduleValveArrivals(Valve.Type.VALVULA_4, 20, arrivalTime);
        }
    }

    private void scheduleValveArrivals(Valve.Type type, int quantity, double time) {
        // TODAS las válvulas llegan exactamente al mismo tiempo
        // ProModel: "Primera Vez 0" significa todas llegan al inicio
        for (int i = 0; i < quantity; i++) {
            Valve valve = new Valve(type, time);
            allValves.add(valve);
            eventQueue.add(new Event(Event.Type.ARRIVAL, time, valve, null));
        }
    }

    private void scheduleStatisticsSampling() {
        for (double t = SAMPLE_INTERVAL; t <= endTime; t += SAMPLE_INTERVAL) {
            eventQueue.add(new Event(Event.Type.SAMPLE_STATISTICS, t));
        }
    }

    // Main simulation loop
    public void run() {
        isRunning = true;
        isPaused = false;
        lastOperationalTime = 0.0;
        lastSampleTime = 0.0;

        while (!eventQueue.isEmpty() && currentTime < endTime && isRunning) {

            if (isPaused) {
                try {
                    Thread.sleep(100);
                    continue;
                } catch (InterruptedException e) {
                    break;
                }
            }
            
            // ESPERAR mientras la animación de la grúa está en progreso
            // Esto hace que la simulación se RALENTICE para que la animación sea visible
            if (crane.isMoving() && animationSpeed < 100) {
                try {
                    Thread.sleep(getAnimationWaitMillis());
                    continue;
                } catch (InterruptedException e) {
                    break;
                }
            }

            Event event = eventQueue.poll();
            if (event != null) {
                currentTime = event.getTime();
                if (isOperationalEvent(event)) {
                    lastOperationalTime = currentTime;
                }
                processEvent(event);
            }
        }

        finalizeStatistics();
    }

    public void step() {
        if (!eventQueue.isEmpty() && currentTime < endTime) {
            Event event = eventQueue.poll();
            if (event != null) {
                currentTime = event.getTime();
                if (isOperationalEvent(event)) {
                    lastOperationalTime = currentTime;
                }
                processEvent(event);
            }
        }

        if (eventQueue.isEmpty() || !hasOperationalEvents()) {
            finalizeStatistics();
        }
    }

    private void processEvent(Event event) {
        switch (event.getType()) {
            case SAMPLE_STATISTICS:
                sampleStatistics();
                break;
            case ARRIVAL:
                handleArrival(event.getValve());
                break;
            case END_PROCESSING:
                handleEndProcessing(event.getValve());
                break;
            case START_CRANE_MOVE:
                handleStartCraneMove(event.getValve(), (String) event.getData());
                break;
            case END_CRANE_MOVE:
                // Esperar a que la animación termine antes de procesar
                if (animationSpeed < 100) {
                    while (crane.isMoving()) {
                        try {
                            Thread.sleep(getAnimationWaitMillis());
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
                handleEndCraneMove(event.getValve(), (String) event.getData());
                break;
            case HOLD_RELEASE:
                handleHoldRelease(event.getValve(), (String) event.getData());
                break;
            case SHIFT_START:
                handleShiftStart((String) event.getData());
                break;
            case CRANE_PICKUP:
            case CRANE_RELEASE:
            case SHIFT_END:
                break;
        }
    }

    private void handleArrival(Valve valve) {
        Location dock = locations.get("DOCK");
        dock.addToQueue(valve);
        updateLocationMetrics(dock);
        valve.setState(Valve.State.IN_QUEUE);
        valve.setCurrentLocation(dock);
        applyHoldTime(valve, dock.getName(), currentTime);
        valve.startWaiting(currentTime); // Comenzar a contar tiempo de espera
        statistics.recordArrival(valve);

        // INMEDIATAMENTE intentar programar movimiento de grúa
        tryScheduleCraneMove();

        // If crane didn't start (outside shift), schedule wakeup
        if (!crane.isBusy() && !shiftCalendar.isWorkingTime(currentTime) && dock.getQueueSize() > 0) {
            scheduleCraneWakeup();
        }
    }

    private void handleEndProcessing(Valve valve) {
        Location machineUnit = valve.getCurrentLocation();
        valve.endProcessing(currentTime);
        valve.advanceStep();
        
        // Incrementar contador de válvulas procesadas para esta ubicación
        if (machineUnit != null) {
            String unitName = machineUnit.getName();
            String machineBaseName = unitName.contains(".") ? unitName.substring(0, unitName.indexOf(".")) : unitName;
            LocationStats machineStats = statistics.getOrCreateLocationStats(machineBaseName);
            machineStats.incrementValvesProcessed();
        }

        // Verificar si hay espacio en destino ANTES de liberar máquina
        String destination = getNextDestination(valve);
        Location destLoc = destination != null ? locations.get(destination) : null;
        
        if (destLoc != null && !destLoc.canAccept()) {
            // BLOQUEADO: Destino lleno, válvula permanece en máquina (ProModel Blk 1)
            valve.setState(Valve.State.BLOCKED);
            valve.startBlocked(currentTime);
            // NO remover de machineUnit, permanece ocupando espacio
            return; // NO liberar máquina ni programar grúa
        }

        // Destino tiene espacio: remover de la máquina
        if (machineUnit != null) {
            machineUnit.removeValve(valve);
            updateLocationMetrics(machineUnit);
        }

        // Machine unit holds the valve temporarily; crane will pick it up
        valve.setCurrentLocation(machineUnit);
        if (machineUnit != null) {
            applyHoldTime(valve, machineUnit.getName(), currentTime);
        }
        valve.startWaiting(currentTime);
        
        // ALTA PRIORIDAD: Válvulas que terminaron procesamiento
        pendingCraneTransfers.add(valve);

        // Free machine capacity for next valve
        String unitName = machineUnit != null ? machineUnit.getName() : "";
        String machineBaseName = unitName.contains(".") ? unitName.substring(0, unitName.indexOf(".")) : unitName;
        String almacenName = "Almacen_" + machineBaseName;
        Location almacen = locations.get(almacenName);
        Location machineParent = locations.get(machineBaseName);
        if (almacen != null && machineParent != null) {
            checkMachineQueue(machineParent, almacen);
        }
        
        // CRÍTICO: Verificar válvulas bloqueadas SIEMPRE después de procesar
        // El almacén puede tener espacio temporalmente antes de checkMachineQueue
        checkBlockedValves();

        // INMEDIATAMENTE intentar mover con grúa
        tryScheduleCraneMove();
    }

    private void checkMachineQueue(Location machineParent, Location almacen) {
        String machineBaseName = machineParent.getName();
        int unitCount = machineParent.getUnits();

        while (almacen.getQueueSize() > 0) {
            if (!shiftCalendar.isWorkingTime(currentTime)) {
                Valve waitingValve = almacen.peekQueue();
                if (waitingValve != null) {
                    waitingValve.startWaiting(currentTime);
                }
                scheduleMachineWakeup(machineBaseName);
                break;
            }

            // Find first available unit
            Location availableUnit = null;
            for (int i = 1; i <= unitCount; i++) {
                Location unit = locations.get(machineBaseName + "." + i);
                if (unit != null && unit.hasAvailableUnit()) {
                    availableUnit = unit;
                    break;
                }
            }

            if (availableUnit == null) {
                break;
            }

            Valve nextValve = findNextReadyValve(almacen);
            if (nextValve == null) {
                break;
            }

            // CRÍTICO: Terminar tiempo de espera antes de remover
            nextValve.endWaiting(currentTime);
            
            almacen.removeValve(nextValve);
            updateLocationMetrics(almacen);
            availableUnit.addToQueue(nextValve);
            availableUnit.moveToProcessing(nextValve);
            updateLocationMetrics(availableUnit);

            double processTime = nextValve.getCurrentProcessingTime();
            nextValve.setReadyTime(currentTime);
            nextValve.startProcessing(currentTime);
            nextValve.setCurrentLocation(availableUnit); // Actualizar ubicación

            eventQueue.add(new Event(Event.Type.END_PROCESSING,
                currentTime + processTime, nextValve, null));
        }
        
        // CRÍTICO: SIEMPRE verificar válvulas bloqueadas y dar oportunidad a grúa
        // No importa si el almacén quedó lleno después de mover válvulas,
        // puede haber válvulas en DOCK esperando ir a OTROS almacenes (M2, M3)
        checkBlockedValves();
        if (!crane.isBusy() && shiftCalendar.isWorkingTime(currentTime)) {
            tryScheduleCraneMove();
        }
    }

    private void scheduleMachineWakeup(String machineName) {
        if (!pendingMachineWakeups.add(machineName)) {
            return;
        }
        double wakeTime = shiftCalendar.getNextWorkingTime(currentTime);
        if (wakeTime <= currentTime) {
            pendingMachineWakeups.remove(machineName);
            return;
        }
        eventQueue.add(new Event(Event.Type.SHIFT_START, wakeTime, null, machineName));
    }

    private void checkBlockedValves() {
        // Revisar todas las ubicaciones buscando válvulas bloqueadas
        for (Location location : locations.values()) {
            List<Valve> blockedValves = new ArrayList<>();
            for (Valve valve : location.getAllValves()) {
                if (valve.getState() == Valve.State.BLOCKED) {
                    blockedValves.add(valve);
                }
            }
            
            for (Valve valve : blockedValves) {
                String destination = getNextDestination(valve);
                Location destLoc = destination != null ? locations.get(destination) : null;
                
                if (destLoc != null && destLoc.canAccept()) {
                    // Destino ahora tiene espacio: desbloquear
                    valve.endBlocked(currentTime);
                    
                    // Si está en DOCK, simplemente cambiar estado a IN_QUEUE
                    if (location.getName().equals("DOCK")) {
                        valve.setState(Valve.State.IN_QUEUE);
                        // NO remover de DOCK, la grúa la recogerá
                    } else {
                        // Si está en máquina, preparar para transporte
                        location.removeValve(valve);
                        updateLocationMetrics(location);
                        valve.setCurrentLocation(location);
                        applyHoldTime(valve, location.getName(), currentTime);
                        valve.startWaiting(currentTime);
                        pendingCraneTransfers.add(valve);
                        
                        // Liberar espacio para siguiente válvula
                        String unitName = location.getName();
                        if (unitName.contains(".")) {
                            String machineBaseName = unitName.substring(0, unitName.indexOf("."));
                            String almacenName = "Almacen_" + machineBaseName;
                            Location almacen = locations.get(almacenName);
                            Location machineParent = locations.get(machineBaseName);
                            if (almacen != null && machineParent != null) {
                                checkMachineQueue(machineParent, almacen);
                            }
                        }
                    }
                }
            }
        }
    }

    private void scheduleCraneWakeup() {
        double wakeTime = shiftCalendar.getNextWorkingTime(currentTime);
        if (wakeTime > currentTime) {
            eventQueue.add(new Event(Event.Type.SHIFT_START, wakeTime, null, "CRANE"));
        }
    }

    private void handleShiftStart(String machineName) {
        if (machineName == null) {
            return;
        }

        // Special case: crane wakeup
        if ("CRANE".equals(machineName)) {
            tryScheduleCraneMove();
            return;
        }

        pendingMachineWakeups.remove(machineName);
        Location machineParent = locations.get(machineName);
        if (machineParent == null) {
            return;
        }
        String almacenName = "Almacen_" + machineName;
        Location almacen = locations.get(almacenName);
        if (almacen == null) {
            return;
        }
        checkMachineQueue(machineParent, almacen);
        tryScheduleCraneMove();
    }

    private void tryScheduleCraneMove() {
        if (crane.isBusy()) {
            return;
        }

        // PRIORIDAD 1: Válvulas que terminaron procesamiento (liberan máquinas)
        Valve valveToMove = pollPendingCraneTransfer();
        boolean pendingFromMachine = valveToMove != null;
        
        // PRIORIDAD 2: Válvulas en DOCK esperando (ProModel FIRST 1 = primera DISPONIBLE)
        if (valveToMove == null) {
            if (!shiftCalendar.isWorkingTime(currentTime)) {
                return; // Fuera de turno sólo atendemos descargas de máquinas
            }
            valveToMove = findFirstAvailableValveInDock();
        }

        if (valveToMove != null) {
            if (!shiftCalendar.isWorkingTime(currentTime) && !pendingFromMachine) {
                return;
            }
            String destination = getNextDestination(valveToMove);
            Location destLoc = locations.get(destination);

            if (destLoc != null && destLoc.canAccept()) {
                scheduleCraneMove(valveToMove, destination);
            }
        }
    }
    
    private Valve findFirstAvailableValveInDock() {
        Location dock = locations.get("DOCK");
        if (dock == null || dock.getQueueSize() == 0) {
            return null;
        }

        // Buscar PRIMERA válvula cuyo destino tenga espacio (ProModel FIRST 1)
        for (Valve valve : dock.getQueueSnapshot()) {
            if (valve == null) {
                continue;
            }
            if (!valve.isReady(currentTime)) {
                continue;
            }
            String destination = getNextDestination(valve);
            Location destLoc = destination != null ? locations.get(destination) : null;

            if (destination == null) {
                continue;
            }

            // CRÍTICO: Verificar espacio ANTES de saltarse por estar bloqueada
            if (destLoc != null && destLoc.canAccept()) {
                // Si estaba bloqueada, desbloquear
                if (valve.getState() == Valve.State.BLOCKED) {
                    valve.endBlocked(currentTime);
                    valve.setState(Valve.State.IN_QUEUE);
                }
                return valve; // Primera disponible
            } else if (destLoc != null && !destLoc.canAccept()) {
                // Destino lleno: marcar bloqueada y continuar buscando
                if (valve.getState() != Valve.State.BLOCKED) {
                    valve.setState(Valve.State.BLOCKED);
                    valve.startBlocked(currentTime);
                }
            }
        }

        return null; // Todas bloqueadas o sin destino válido
    }

    private Valve pollPendingCraneTransfer() {
        // Buscar válvula que necesita transporte desde máquinas
        Iterator<Valve> iterator = pendingCraneTransfers.iterator();
        while (iterator.hasNext()) {
            Valve valve = iterator.next();
            if (valve == null || !valve.isReady(currentTime)) {
                continue;
            }
            String destination = getNextDestination(valve);
            if (destination == null) {
                iterator.remove();
                continue;
            }
            Location destLoc = locations.get(destination);
            if (destLoc != null && destLoc.canAccept()) {
                iterator.remove();
                return valve;
            }
        }
        return null;
    }

    private String getNextDestination(Valve valve) {
        Location currentLocation = valve.getCurrentLocation();
        if (currentLocation == null) {
            return null;
        }

        if (currentLocation.getName().equals("DOCK")) {
            return valve.getNextAlmacen();
        }

        if (valve.isRouteComplete()) {
            return "STOCK";
        }

        return valve.getNextAlmacen();
    }

    private void scheduleCraneMove(Valve valve, String destination) {
        crane.setBusy(true);

        Location from = valve.getCurrentLocation();
        Location to = locations.get(destination);

        if (from == null || to == null) {
            crane.setBusy(false);
            return;
        }

        // Si válvula estaba bloqueada, terminar bloqueo
        if (valve.getState() == Valve.State.BLOCKED) {
            valve.endBlocked(currentTime);
        }

        PathNetwork.PathResult pathResult = pathNetwork.getPathForLocations(from.getName(), destination);
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

        double travelTime = crane.calculateTravelTime(totalDistanceMeters, valve != null);

        crane.addTravelTime(travelTime);
        valve.addMovementTime(travelTime);
        valve.endWaiting(currentTime);

        // Pasar el tiempo actual de simulación y la velocidad de animación
        crane.startMove(pathPoints, segmentDistances, totalDistanceMeters, travelTime, currentTime, animationSpeed);

        if (from.getName().equals("DOCK")) {
            dockToAlmacenMoves++;
        }

        eventQueue.add(new Event(Event.Type.START_CRANE_MOVE,
            currentTime, valve, destination));
        eventQueue.add(new Event(Event.Type.END_CRANE_MOVE,
            currentTime + travelTime, valve, destination));
    }

    private void handleStartCraneMove(Valve valve, String destination) {
        Location from = valve.getCurrentLocation();
        if (from != null) {
            from.removeValve(valve);
            updateLocationMetrics(from);
        }
        
        // Finalizar tiempo de espera
        if (valve.getState() == Valve.State.IN_QUEUE || 
            valve.getState() == Valve.State.WAITING_CRANE) {
            valve.endWaiting(currentTime);
        }
        
        crane.pickupValve(valve);
        valve.setState(Valve.State.IN_TRANSIT);
        valve.setCurrentLocation(null); // En tránsito = sin ubicación fija
    }

    private void handleEndCraneMove(Valve valve, String destination) {
        Location destLoc = locations.get(destination);

        // Sincronizar animación antes de liberar
        crane.completeTrip();
        
        if (destination.equals("STOCK")) {
            destLoc.addToQueue(valve);
            updateLocationMetrics(destLoc);
            destLoc.removeValve(valve); // Act as sink so it doesn't retain inventory
            updateLocationMetrics(destLoc);
            valve.setState(Valve.State.COMPLETED);
            valve.setCurrentLocation(null);
            completedValves.add(valve);
            statistics.recordCompletion(valve, currentTime);
            completedInventoryCount++;
        } else if (destination.startsWith("Almacen")) {
            // Crane drops valve at storage
            destLoc.addToQueue(valve);
            updateLocationMetrics(destLoc);
            valve.setState(Valve.State.IN_QUEUE);
            valve.setCurrentLocation(destLoc);
            applyHoldTime(valve, destLoc.getName(), currentTime);

            // Immediately try to move to machine (ProModel: Almacen_M1 -> M1 is instant)
            String machineName = destination.replace("Almacen_", "");
            Location machineParent = locations.get(machineName);
            if (machineParent != null) {
                checkMachineQueue(machineParent, destLoc);
            }
        }
        
        crane.releaseValve();
        crane.setBusy(false);
        
        // CRÍTICO: Verificar válvulas bloqueadas que ahora pueden moverse
        checkBlockedValves();

        // CRÍTICO: Intentar siguiente movimiento INMEDIATAMENTE
        // La grúa debe estar constantemente activa
        tryScheduleCraneMove();

        // If crane can't move (shift ended), schedule wakeup
        if (!crane.isBusy() && !shiftCalendar.isWorkingTime(currentTime)) {
            if (!pendingCraneTransfers.isEmpty() || 
                locations.get("DOCK").getQueueSize() > 0) {
                scheduleCraneWakeup();
            }
        }
    }

    private void sampleStatistics() {
        sampleStatisticsAt(currentTime);
    }

    private void sampleStatisticsAt(double sampleTime) {
        for (Location loc : locations.values()) {
            String name = loc.getName();
            if (isMachineParent(name)) {
                continue;
            }

            // Almacenes y unidades individuales dependen de turno
            boolean countTowardsSchedule = shouldCountTowardsSchedule(name, sampleTime);

            loc.updateStatistics(sampleTime, countTowardsSchedule);
            statistics.updateLocationStats(name,
                loc.getCurrentContents(),
                loc.getUtilization(),
                sampleTime);
        }

        updateMachineAggregate("M1", sampleTime);
        updateMachineAggregate("M2", sampleTime);
        updateMachineAggregate("M3", sampleTime);

        // Update crane statistics
        crane.updateStatistics(sampleTime);
        statistics.updateCraneStats(
            crane.getUtilization(),
            crane.getTotalTrips(),
            sampleTime);

        lastSampleTime = sampleTime;
    }

    private boolean isMachineParent(String name) {
        return "M1".equals(name) || "M2".equals(name) || "M3".equals(name);
    }

    private boolean isMachineUnit(String name) {
        return name.startsWith("M1.") || name.startsWith("M2.") || name.startsWith("M3.");
    }

    private void updateMachineAggregate(String machineBaseName, double sampleTime) {
        double busySum = 0.0;
        int totalContents = 0;
        int actualUnits = 0;

        String prefix = machineBaseName + ".";
        for (Map.Entry<String, Location> entry : locations.entrySet()) {
            String name = entry.getKey();
            if (!name.startsWith(prefix)) {
                continue;
            }

            Location unit = entry.getValue();
            busySum += unit.getTotalBusyTime();
            totalContents += unit.getCurrentContents();
            actualUnits++;
        }

        // Normalizar con stats_units (factor de ajuste)
        Config config = Config.getInstance();
        double statsUnits = config.getMachineStatsUnits(machineBaseName, actualUnits > 0 ? actualUnits : 1);
        double scheduledPerUnit = shiftCalendar.getTotalWorkingHoursPerWeek();
        double weeksSimulated = Math.max(sampleTime, SAMPLE_EPSILON) / 168.0;
        double totalScheduled = statsUnits * scheduledPerUnit * weeksSimulated;

        double utilization = (totalScheduled > 0.0) ? (busySum / totalScheduled) * 100.0 : 0.0;
        // Limitar al 100% máximo
        utilization = Math.min(utilization, 100.0);

        statistics.updateLocationStats(machineBaseName, totalContents, utilization, sampleTime);
    }

    private boolean hasOperationalEvents() {
        for (Event pending : eventQueue) {
            if (isOperationalEvent(pending)) {
                return true;
            }
        }
        return false;
    }

    private boolean isOperationalEvent(Event event) {
        if (event == null) {
            return false;
        }

        switch (event.getType()) {
            case ARRIVAL:
            case END_PROCESSING:
            case START_CRANE_MOVE:
            case END_CRANE_MOVE:
                return true;
            case HOLD_RELEASE:
                return true;
            case SHIFT_START:
                return hasPendingWorkForShift(event);
            default:
                return false;
        }
    }

    private boolean shouldCountTowardsSchedule(String name, double time) {
        if (isMachineUnit(name)) {
            double referenceTime = Math.max(0.0, time - SAMPLE_EPSILON);
            return shiftCalendar.isWorkingTime(referenceTime);
        }
        return true;
    }

    private void updateLocationMetrics(Location location) {
        if (location == null) {
            return;
        }
        boolean countTowardsSchedule = shouldCountTowardsSchedule(location.getName(), currentTime);
        location.updateStatistics(currentTime, countTowardsSchedule);
    }

    private void applyHoldTime(Valve valve, String locationName, double referenceTime) {
        if (valve == null || locationName == null) {
            return;
        }

        double holdTime = getHoldTimeFor(locationName);
        if (holdTime <= 0.0) {
            valve.setReadyTime(referenceTime);
            return;
        }

        double readyAt = referenceTime + holdTime;
        valve.setReadyTime(readyAt);
        eventQueue.add(new Event(Event.Type.HOLD_RELEASE, readyAt, valve, locationName));
    }

    private Valve findNextReadyValve(Location location) {
        if (location == null) {
            return null;
        }

        for (Valve valve : location.getQueueSnapshot()) {
            if (valve == null) {
                continue;
            }
            Valve.State state = valve.getState();
            if ((state == Valve.State.IN_QUEUE || state == Valve.State.WAITING_CRANE) && valve.isReady(currentTime)) {
                return valve;
            }
        }
        return null;
    }

    private void handleHoldRelease(Valve valve, String locationName) {
        if (valve == null || locationName == null) {
            return;
        }

        Location currentLocation = valve.getCurrentLocation();
        if (currentLocation == null || !locationName.equals(currentLocation.getName())) {
            return;
        }

        valve.setReadyTime(Math.max(valve.getReadyTime(), currentTime));

        if ("DOCK".equals(locationName)) {
            tryScheduleCraneMove();
            return;
        }

        if (locationName.startsWith("Almacen_")) {
            String machineName = locationName.replace("Almacen_", "");
            Location machineParent = locations.get(machineName);
            if (machineParent != null) {
                checkMachineQueue(machineParent, currentLocation);
            }
        }

        tryScheduleCraneMove();
    }

    private double getHoldTimeFor(String locationName) {
        return locationHoldTimes.getOrDefault(locationName, 0.0);
    }

    private boolean hasPendingWorkForShift(Event event) {
        Object data = event.getData();
        if (!(data instanceof String)) {
            return false;
        }

        String name = (String) data;
        if ("CRANE".equals(name)) {
            if (!pendingCraneTransfers.isEmpty()) {
                return true;
            }

            Location dock = locations.get("DOCK");
            if (dock != null) {
                for (Valve valve : dock.getAllValves()) {
                    String destination = getNextDestination(valve);
                    Location destLoc = destination != null ? locations.get(destination) : null;
                    if (destLoc != null && destLoc.canAccept()) {
                        return true;
                    }
                }
            }
            return false;
        }

        Location almacen = locations.get("Almacen_" + name);
        if (almacen != null && almacen.getQueueSize() > 0) {
            return true;
        }

        Location machineParent = locations.get(name);
        if (machineParent != null) {
            int unitCount = machineParent.getUnits();
            for (int i = 1; i <= unitCount; i++) {
                Location unit = locations.get(name + "." + i);
                if (unit != null && (unit.getQueueSize() > 0 || unit.getProcessingSize() > 0)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void finalizeStatistics() {
        double finalTime = Math.max(lastSampleTime, lastOperationalTime);
        if (finalTime <= 0.0) {
            return;
        }

        if (finalTime > lastSampleTime + SAMPLE_EPSILON) {
            sampleStatisticsAt(finalTime);
        }

        currentTime = finalTime;
    }

    // Control methods
    public void pause() { isPaused = true; }
    public void resume() { isPaused = false; }
    public void stop() { isRunning = false; }
    public void reset() {
        eventQueue.clear();
        locations.clear();
        allValves.clear();
        completedValves.clear();
        currentTime = 0;
        statistics = new Statistics();
        initializeLocations();
        initializeCrane();
        scheduleArrivals();
        scheduleStatisticsSampling();
        dockToAlmacenMoves = 0;
        lastOperationalTime = 0.0;
        lastSampleTime = 0.0;
        completedInventoryCount = 0;
    }

    // Getters
    public double getCurrentTime() { return currentTime; }
    public double getEndTime() { return endTime; }
    public boolean isRunning() { return isRunning; }
    public boolean isPaused() { return isPaused; }
    public Map<String, Location> getLocations() { return locations; }
    public Crane getCrane() { return crane; }
    public Statistics getStatistics() { return statistics; }
    public int getAnimationSpeed() { return animationSpeed; }
    public void setAnimationSpeed(int speed) { this.animationSpeed = Math.max(1, Math.min(100, speed)); }
    public ShiftCalendar getShiftCalendar() { return shiftCalendar; }
    public List<Valve> getAllValves() { return new ArrayList<>(allValves); }
    public List<Valve> getCompletedValves() { return new ArrayList<>(completedValves); }
    public int getTotalValvesInSystem() {
        return allValves.size() - completedValves.size();
    }
    public PathNetwork getPathNetwork() { return pathNetwork; }
    public int getDockToAlmacenMoves() { return dockToAlmacenMoves; }
    public boolean hasPendingEvents() { return !eventQueue.isEmpty(); }
    public int getCompletedInventoryCount() { return completedInventoryCount; }
    public double getLastOperationalTime() { return lastOperationalTime; }

    public boolean isSimulationComplete() {
        if (currentTime >= endTime - SAMPLE_EPSILON) {
            return true;
        }
        return eventQueue.isEmpty();
    }

    private long getAnimationWaitMillis() {
        if (animationSpeed >= 90) {
            return 2;
        }
        if (animationSpeed >= 70) {
            return 6;
        }
        if (animationSpeed >= 40) {
            return 20;
        }
        return 50;
    }

    // Debug helper to inspect valves remaining in a given location (e.g., DOCK)
    public List<String> getValveDetailsForLocation(String locationName) {
        Location location = locations.get(locationName);
        if (location == null) {
            return Collections.emptyList();
        }

        List<String> details = new ArrayList<>();
        for (Valve valve : location.getAllValves()) {
            String next = determineNextLocation(valve);
            details.add(String.format("%s | estado=%s | paso=%d | siguiente=%s",
                valve,
                valve.getState(),
                valve.getCurrentStep(),
                next == null ? "-" : next));
        }
        return details;
    }

    private String determineNextLocation(Valve valve) {
        Location current = valve.getCurrentLocation();
        if (current == null) {
            return null;
        }

        if ("DOCK".equals(current.getName())) {
            return valve.getNextAlmacen();
        }

        if (valve.isRouteComplete()) {
            return "STOCK";
        }

        return valve.getNextAlmacen();
    }
}

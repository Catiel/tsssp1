package core;

import model.*;
import statistics.Statistics;
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
    private boolean isRunning;
    private boolean isPaused;

    // Model components
    private Map<String, Location> locations;
    private Crane crane;
    private PathNetwork pathNetwork;
    private ShiftCalendar shiftCalendar;
    private final Set<String> pendingMachineWakeups;
    private final Queue<Valve> pendingCraneTransfers;

    // Statistics
    private Statistics statistics;
    private List<Valve> allValves;
    private List<Valve> completedValves;

    // Configuration
    private static final double SAMPLE_INTERVAL = 1.0; // Sample stats every hour
    private static final int WEEKS_TO_SIMULATE = 8;

    public SimulationEngine() {
        this.eventQueue = new PriorityBlockingQueue<>();
        this.currentTime = 0.0;
        this.endTime = WEEKS_TO_SIMULATE * 168.0; // 8 weeks in hours
        this.isRunning = false;
        this.isPaused = false;

        this.locations = new ConcurrentHashMap<>();
        this.pathNetwork = new PathNetwork();
        this.shiftCalendar = new ShiftCalendar();
        this.pendingMachineWakeups = ConcurrentHashMap.newKeySet();
        this.pendingCraneTransfers = new ConcurrentLinkedQueue<>();
        this.statistics = new Statistics();
        this.allValves = Collections.synchronizedList(new ArrayList<>());
        this.completedValves = Collections.synchronizedList(new ArrayList<>());

        initializeLocations();
        initializeCrane();
        scheduleArrivals();
        scheduleStatisticsSampling();
    }

    private void initializeLocations() {
        // Create locations with exact ProModel specifications
        locations.put("DOCK", new Location("DOCK", Integer.MAX_VALUE, 1,
            new Point(190, 140)));
        locations.put("STOCK", new Location("STOCK", Integer.MAX_VALUE, 1,
            new Point(200, 420)));

        locations.put("Almacen_M1", new Location("Almacen_M1", 20, 1,
            new Point(560, 520)));
        // M1 parent with capacity 1 and 10 units (routing target)
        locations.put("M1", new Location("M1", 1, 10,
            new Point(560, 380)));
        // Individual M1 units
        for (int i = 1; i <= 10; i++) {
            locations.put("M1." + i, new Location("M1." + i, 1, 1,
                new Point(560, 380)));
        }

        locations.put("Almacen_M2", new Location("Almacen_M2", 20, 1,
            new Point(960, 320)));
        locations.put("M2", new Location("M2", 1, 25,
            new Point(760, 300)));
        for (int i = 1; i <= 25; i++) {
            locations.put("M2." + i, new Location("M2." + i, 1, 1,
                new Point(760, 300)));
        }

        locations.put("Almacen_M3", new Location("Almacen_M3", 30, 1,
            new Point(1080, 180)));
        locations.put("M3", new Location("M3", 1, 17,
            new Point(900, 160)));
        for (int i = 1; i <= 17; i++) {
            locations.put("M3." + i, new Location("M3." + i, 1, 1,
                new Point(900, 160)));
        }
    }

    private void initializeCrane() {
        Point home = locations.get("DOCK").getPosition();
        // Speeds: empty=15.24 m/min, full=12.19 m/min (from ProModel)
        crane = new Crane("Grua", 1, 15.24, 12.19, home);
    }

    private void scheduleArrivals() {
        // Schedule arrivals every 168 hours (weekly) for 8 weeks
        for (int week = 0; week < WEEKS_TO_SIMULATE; week++) {
            double arrivalTime = week * 168.0;

            // Schedule each valve type according to ProModel
            scheduleValveArrivals(Valve.Type.VALVULA_1, 10, arrivalTime);
            scheduleValveArrivals(Valve.Type.VALVULA_2, 40, arrivalTime);
            scheduleValveArrivals(Valve.Type.VALVULA_3, 10, arrivalTime);
            scheduleValveArrivals(Valve.Type.VALVULA_4, 20, arrivalTime);
        }
    }

    private void scheduleValveArrivals(Valve.Type type, int quantity, double time) {
        // Adjust arrival time to next working shift if outside working hours
        double arrivalTime = time;
        if (!shiftCalendar.isWorkingTime(arrivalTime)) {
            arrivalTime = shiftCalendar.getNextWorkingTime(arrivalTime);
        }
        
        for (int i = 0; i < quantity; i++) {
            Valve valve = new Valve(type, arrivalTime);
            allValves.add(valve);
            eventQueue.add(new Event(Event.Type.ARRIVAL, arrivalTime, valve, null));
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

        while (!eventQueue.isEmpty() && currentTime < endTime && isRunning) {
            if (isPaused) {
                try {
                    Thread.sleep(100);
                    continue;
                } catch (InterruptedException e) {
                    break;
                }
            }

            Event event = eventQueue.poll();
            if (event != null) {
                currentTime = event.getTime();
                processEvent(event);
            }
        }
    }

    public void step() {
        if (!eventQueue.isEmpty() && currentTime < endTime) {
            Event event = eventQueue.poll();
            if (event != null) {
                currentTime = event.getTime();
                processEvent(event);
            }
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
                handleEndCraneMove(event.getValve(), (String) event.getData());
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
        valve.setState(Valve.State.IN_QUEUE);
        statistics.recordArrival(valve);

        // Try to schedule crane movement (respects shift calendar)
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

        machineUnit.removeValve(valve);

        // Machine unit holds the valve temporarily; crane will pick it up
        valve.setCurrentLocation(machineUnit);
        valve.startWaiting(currentTime);
        pendingCraneTransfers.add(valve);

        // Free machine capacity for next valve
        String unitName = machineUnit.getName();
        String machineBaseName = unitName.contains(".") ? unitName.substring(0, unitName.indexOf(".")) : unitName;
        String almacenName = "Almacen_" + machineBaseName;
        Location almacen = locations.get(almacenName);
        Location machineParent = locations.get(machineBaseName);
        if (almacen != null && machineParent != null) {
            checkMachineQueue(machineParent, almacen);
        }

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

            Valve nextValve = almacen.peekQueue();
            if (nextValve == null) {
                break;
            }

            almacen.removeValve(nextValve);
            availableUnit.addToQueue(nextValve);
            availableUnit.moveToProcessing(nextValve);

            double processTime = nextValve.getCurrentProcessingTime();
            nextValve.startProcessing(currentTime);

            eventQueue.add(new Event(Event.Type.END_PROCESSING,
                currentTime + processTime, nextValve, null));
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

        // Crane only works during shifts
        if (!shiftCalendar.isWorkingTime(currentTime)) {
            return;
        }

        // Find valve that needs to move
        Valve valveToMove = findValveNeedingTransport();

        if (valveToMove != null) {
            String destination = getNextDestination(valveToMove);
            Location destLoc = locations.get(destination);

            if (destLoc != null && destLoc.canAccept()) {
                scheduleCraneMove(valveToMove, destination);
            }
        }
    }

    private Valve findValveNeedingTransport() {
        Valve machineValve = pollPendingCraneTransfer();
        if (machineValve != null) {
            return machineValve;
        }

        Location dock = locations.get("DOCK");
        if (dock != null && dock.getQueueSize() > 0) {
            Valve candidate = dock.peekQueue();
            if (candidate != null) {
                String destination = getNextDestination(candidate);
                Location destLoc = destination != null ? locations.get(destination) : null;
                if (destLoc != null && destLoc.canAccept()) {
                    return candidate;
                }
            }
        }

        return null;
    }

    private Valve pollPendingCraneTransfer() {
        Iterator<Valve> iterator = pendingCraneTransfers.iterator();
        while (iterator.hasNext()) {
            Valve valve = iterator.next();
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

        crane.startMove(pathPoints, segmentDistances, totalDistanceMeters, travelTime);

        eventQueue.add(new Event(Event.Type.START_CRANE_MOVE,
            currentTime, valve, destination));
        eventQueue.add(new Event(Event.Type.END_CRANE_MOVE,
            currentTime + travelTime, valve, destination));
    }

    private void handleStartCraneMove(Valve valve, String destination) {
        Location from = valve.getCurrentLocation();
        from.removeValve(valve);
        crane.pickupValve(valve);
        valve.setState(Valve.State.IN_TRANSIT);
    }

    private void handleEndCraneMove(Valve valve, String destination) {
        Location destLoc = locations.get(destination);

        crane.releaseValve();
        crane.completeTrip();
        crane.setBusy(false);

        if (destination.equals("STOCK")) {
            destLoc.addToQueue(valve);
            valve.setState(Valve.State.COMPLETED);
            completedValves.add(valve);
            statistics.recordCompletion(valve, currentTime);
        } else if (destination.startsWith("Almacen")) {
            // Crane drops valve at storage
            destLoc.addToQueue(valve);
            valve.setState(Valve.State.IN_QUEUE);

            // Immediately try to move to machine (ProModel: Almacen_M1 -> M1 is instant)
            String machineName = destination.replace("Almacen_", "");
            Location machineParent = locations.get(machineName);
            if (machineParent != null) {
                checkMachineQueue(machineParent, destLoc);
            }
        }

        // Try next crane move (respects shifts)
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
        // Update location statistics
        for (Location loc : locations.values()) {
            loc.updateStatistics(currentTime);
            statistics.updateLocationStats(loc.getName(),
                loc.getCurrentContents(),
                loc.getUtilization(currentTime),
                currentTime);
        }

        // Update crane statistics
        crane.updateStatistics(currentTime);
        statistics.updateCraneStats(
            crane.getUtilization(currentTime),
            crane.getTotalTrips(),
            currentTime);
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
    }

    // Getters
    public double getCurrentTime() { return currentTime; }
    public double getEndTime() { return endTime; }
    public boolean isRunning() { return isRunning; }
    public boolean isPaused() { return isPaused; }
    public Map<String, Location> getLocations() { return locations; }
    public Crane getCrane() { return crane; }
    public Statistics getStatistics() { return statistics; }
    public ShiftCalendar getShiftCalendar() { return shiftCalendar; }
    public List<Valve> getAllValves() { return new ArrayList<>(allValves); }
    public List<Valve> getCompletedValves() { return new ArrayList<>(completedValves); }
    public int getTotalValvesInSystem() {
        return allValves.size() - completedValves.size();
    }
    public PathNetwork getPathNetwork() { return pathNetwork; }
}

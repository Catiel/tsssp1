package core;

import model.*;
import statistics.Statistics;
import java.awt.Point;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

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
            new Point(50, 100)));
        locations.put("STOCK", new Location("STOCK", Integer.MAX_VALUE, 1,
            new Point(150, 100)));

        locations.put("Almacen_M1", new Location("Almacen_M1", 20, 1,
            new Point(300, 100)));
        locations.put("M1", new Location("M1", 1, 1,
            new Point(300, 100)));

        locations.put("Almacen_M2", new Location("Almacen_M2", 20, 1,
            new Point(500, 100)));
        locations.put("M2", new Location("M2", 1, 1,
            new Point(500, 100)));

        locations.put("Almacen_M3", new Location("Almacen_M3", 30, 1,
            new Point(650, 50)));
        locations.put("M3", new Location("M3", 1, 1,
            new Point(650, 50)));
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
            case SAMPLE_STATISTICS:
                sampleStatistics();
                break;
        }
    }

    private void handleArrival(Valve valve) {
        Location dock = locations.get("DOCK");
        dock.addToQueue(valve);
        valve.setState(Valve.State.IN_QUEUE);
        statistics.recordArrival(valve);

        // Try to schedule crane movement
        tryScheduleCraneMove();
    }

    private void handleEndProcessing(Valve valve) {
        Location machine = valve.getCurrentLocation();
        valve.endProcessing(currentTime);

        // Move from machine to its storage
        String almacenName = machine.getName().replace("M", "Almacen_M");
        Location almacen = locations.get(almacenName);

        machine.removeValve(valve);

        if (almacen.canAccept()) {
            almacen.addToQueue(valve);
            valve.advanceStep();

            // Check if there's another valve waiting for this machine
            checkMachineQueue(machine, almacen);

            // Try to schedule crane for this valve
            tryScheduleCraneMove();
        } else {
            // Blocked - valve stays in machine
            valve.startBlocked(currentTime);
        }
    }

    private void checkMachineQueue(Location machine, Location almacen) {
        if (machine.hasAvailableUnit() && almacen.getQueueSize() > 0) {
            Valve nextValve = almacen.peekQueue();
            if (nextValve != null && shiftCalendar.isWorkingTime(currentTime)) {
                almacen.removeValve(nextValve);
                machine.addToQueue(nextValve);
                machine.moveToProcessing(nextValve);

                double processTime = nextValve.getCurrentProcessingTime();
                nextValve.startProcessing(currentTime);

                eventQueue.add(new Event(Event.Type.END_PROCESSING,
                    currentTime + processTime, nextValve, null));
            }
        }
    }

    private void tryScheduleCraneMove() {
        if (crane.isBusy()) {
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
        // Priority: DOCK > Almacenes > others
        Location dock = locations.get("DOCK");
        if (dock.getQueueSize() > 0) {
            return dock.peekQueue();
        }

        for (String almacenName : Arrays.asList("Almacen_M1", "Almacen_M2", "Almacen_M3")) {
            Location almacen = locations.get(almacenName);
            if (almacen.getQueueSize() > 0) {
                Valve valve = almacen.peekQueue();
                String nextDest = getNextDestination(valve);
                if (nextDest != null && locations.get(nextDest).canAccept()) {
                    return valve;
                }
            }
        }

        return null;
    }

    private String getNextDestination(Valve valve) {
        if (valve.getCurrentLocation().getName().equals("DOCK")) {
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

        double travelTime = crane.calculateTravelTime(from.getPosition(), to.getPosition());

        crane.addTravelTime(travelTime);
        valve.addMovementTime(travelTime);
        valve.endWaiting(currentTime);

        eventQueue.add(new Event(Event.Type.START_CRANE_MOVE,
            currentTime, valve, destination));
        eventQueue.add(new Event(Event.Type.END_CRANE_MOVE,
            currentTime + travelTime, valve, destination));

        crane.startMove(to.getPosition());
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
            destLoc.addToQueue(valve);
            valve.setState(Valve.State.IN_QUEUE);

            // Try to move to machine immediately
            String machineName = destination.replace("Almacen_", "");
            Location machine = locations.get(machineName);

            if (machine.hasAvailableUnit() && shiftCalendar.isWorkingTime(currentTime)) {
                destLoc.removeValve(valve);
                machine.addToQueue(valve);
                machine.moveToProcessing(valve);

                double processTime = valve.getCurrentProcessingTime();
                valve.startProcessing(currentTime);

                eventQueue.add(new Event(Event.Type.END_PROCESSING,
                    currentTime + processTime, valve, null));
            } else {
                valve.startWaiting(currentTime);
            }
        }

        // Schedule next crane move
        tryScheduleCraneMove();
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
}

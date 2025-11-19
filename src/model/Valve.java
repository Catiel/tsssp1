package model;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import utils.Config;

public class Valve {
    public enum Type {
        VALVULA_1("Valvula 1", Color.decode("#FF1744"), new int[][]{{1, 10}, {-1, 0}, {3, 5}}),      // Rojo brillante
        VALVULA_2("Valvula 2", Color.decode("#00BCD4"), new int[][]{{2, 12}, {3, 7}, {2, 2}}),      // Cyan/Turquesa
        VALVULA_3("Valvula 3", Color.decode("#76FF03"), new int[][]{{1, 5}, {-1, 0}, {3, 10}}),     // Verde lima
        VALVULA_4("Valvula 4", Color.decode("#FF9800"), new int[][]{{1, 2}, {3, 5}, {2, 10}});

        private final String displayName;
        private final Color color;
        private final int[][] route; // [step][machineNum, processTime]

        Type(String displayName, Color color, int[][] route) {
            this.displayName = displayName;
            this.color = color;
            this.route = route;
        }

        public String getDisplayName() { return displayName; }
        public Color getColor() { return color; }
        public int[][] getRoute() { return route; }
    }

    public enum State {
        IN_DOCK,
        IN_QUEUE,
        WAITING_CRANE,
        IN_TRANSIT,
        PROCESSING,
        BLOCKED,
        COMPLETED
    }

    private static final AtomicInteger idGenerator = new AtomicInteger(1);
    private static final Map<Integer, Double> MACHINE_TIME_MULTIPLIERS = initializeMultipliers();

    private final int id;
    private final Type type;
    private final double arrivalTime;
    private State state;
    private Location currentLocation;
    private int currentStep;

    // Statistics
    private double totalProcessingTime;
    private double totalMovementTime;
    private double totalWaitingTime;
    private double totalBlockedTime;
    private double startProcessingTime;
    private double startWaitingTime;
    private double startBlockedTime;
    private double readyTime;
    private static final double READY_EPSILON = 1e-6;

    public Valve(Type type, double arrivalTime) {
        this.id = idGenerator.getAndIncrement();
        this.type = type;
        this.arrivalTime = arrivalTime;
        this.state = State.IN_DOCK;
        this.currentStep = 0;
        this.totalProcessingTime = 0;
        this.totalMovementTime = 0;
        this.totalWaitingTime = 0;
        this.totalBlockedTime = 0;
        this.readyTime = 0;
    }

    public String getNextMachine() {
        int[][] route = type.getRoute();
        // NO modificar currentStep, solo leer
        for (int step = currentStep; step < route.length; step++) {
            if (route[step][0] != -1) {
                return "M" + route[step][0];
            }
        }
        return null;
    }

    public String getNextAlmacen() {
        String machine = getNextMachine();
        return machine != null ? "Almacen_" + machine : null;
    }

    public double getCurrentProcessingTime() {
        int[][] route = type.getRoute();
        if (currentStep < route.length) {
            int machineId = route[currentStep][0];
            double baseTime = route[currentStep][1];
            double multiplier = MACHINE_TIME_MULTIPLIERS.getOrDefault(machineId, 1.0);
            return baseTime * multiplier;
        }
        return 0;
    }

    public void advanceStep() {
        currentStep++;
        // Saltar pasos con máquina -1 (pasos opcionales/vacíos)
        int[][] route = type.getRoute();
        while (currentStep < route.length && route[currentStep][0] == -1) {
            currentStep++;
        }
    }

    public boolean isRouteComplete() {
        return currentStep >= type.getRoute().length;
    }

    public void startProcessing(double currentTime) {
        state = State.PROCESSING;
        startProcessingTime = currentTime;
    }

    public void endProcessing(double currentTime) {
        if (state == State.PROCESSING) {
            totalProcessingTime += (currentTime - startProcessingTime);
        }
    }

    public void startWaiting(double currentTime) {
        if (state != State.WAITING_CRANE) {
            state = State.WAITING_CRANE;
            startWaitingTime = currentTime;
        }
    }

    public void endWaiting(double currentTime) {
        if (state == State.WAITING_CRANE) {
            totalWaitingTime += (currentTime - startWaitingTime);
        }
    }

    public void startBlocked(double currentTime) {
        state = State.BLOCKED;
        startBlockedTime = currentTime;
    }

    public void endBlocked(double currentTime) {
        if (state == State.BLOCKED) {
            totalBlockedTime += (currentTime - startBlockedTime);
        }
    }

    public void addMovementTime(double time) {
        totalMovementTime += time;
    }

    public double getTotalTimeInSystem(double currentTime) {
        return currentTime - arrivalTime;
    }

    public void setReadyTime(double time) {
        this.readyTime = Math.max(0.0, time);
    }

    public double getReadyTime() {
        return readyTime;
    }

    public boolean isReady(double currentTime) {
        return currentTime + READY_EPSILON >= readyTime;
    }

    // Getters
    public int getId() { return id; }
    public Type getType() { return type; }
    public State getState() { return state; }
    public void setState(State state) { this.state = state; }
    public double getArrivalTime() { return arrivalTime; }
    public Location getCurrentLocation() { return currentLocation; }
    public void setCurrentLocation(Location loc) { this.currentLocation = loc; }
    public int getCurrentStep() { return currentStep; }
    public double getTotalProcessingTime() { return totalProcessingTime; }
    public double getTotalMovementTime() { return totalMovementTime; }
    public double getTotalWaitingTime() { return totalWaitingTime; }
    public double getTotalBlockedTime() { return totalBlockedTime; }

    @Override
    public String toString() {
        return String.format("%s#%d", type.getDisplayName(), id);
    }

    private static Map<Integer, Double> initializeMultipliers() {
        Config config = Config.getInstance();
        Map<Integer, Double> map = new HashMap<>();
        map.put(1, config.getMachineTimeMultiplier("m1", 1.0));
        map.put(2, config.getMachineTimeMultiplier("m2", 1.0));
        map.put(3, config.getMachineTimeMultiplier("m3", 1.0));
        return map;
    }
}

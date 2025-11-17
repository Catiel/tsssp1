package model;

import java.awt.Color;
import java.util.concurrent.atomic.AtomicInteger;

public class Valve {
    public enum Type {
        VALVULA_1(Color.decode("#FF6B6B"), new int[][]{{1, 10}, {-1, 0}, {3, 5}}),
        VALVULA_2(Color.decode("#4ECDC4"), new int[][]{{2, 12}, {3, 7}, {2, 2}}),
        VALVULA_3(Color.decode("#95E1D3"), new int[][]{{1, 5}, {-1, 0}, {3, 10}}),
        VALVULA_4(Color.decode("#F38181"), new int[][]{{1, 2}, {3, 5}, {2, 10}});

        private final Color color;
        private final int[][] route; // [step][machineNum, processTime]

        Type(Color color, int[][] route) {
            this.color = color;
            this.route = route;
        }

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
    }

    public String getNextMachine() {
        int[][] route = type.getRoute();
        while (currentStep < route.length) {
            if (route[currentStep][0] != -1) {
                return "M" + route[currentStep][0];
            }
            currentStep++;
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
            return route[currentStep][1];
        }
        return 0;
    }

    public void advanceStep() {
        currentStep++;
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
        return String.format("%s#%d", type.name(), id);
    }
}

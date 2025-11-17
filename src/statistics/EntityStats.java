package statistics;

import model.Valve;
import java.util.*;

public class EntityStats {
    private Valve.Type type;
    private int totalArrivals;
    private int totalCompleted;
    private int currentInSystem;

    private double sumTimeInSystem;
    private double sumProcessingTime;
    private double sumMovementTime;
    private double sumWaitingTime;
    private double sumBlockedTime;

    private double minTimeInSystem = Double.MAX_VALUE;
    private double maxTimeInSystem = 0;

    private List<Double> arrivalTimes;
    private List<Double> completionTimes;

    public EntityStats(Valve.Type type) {
        this.type = type;
        this.arrivalTimes = Collections.synchronizedList(new ArrayList<>());
        this.completionTimes = Collections.synchronizedList(new ArrayList<>());
    }

    public synchronized void recordArrival(double time) {
        totalArrivals++;
        currentInSystem++;
        arrivalTimes.add(time);
    }

    public synchronized void recordCompletion(double time) {
        totalCompleted++;
        currentInSystem = Math.max(0, currentInSystem - 1);
        completionTimes.add(time);
    }

    public synchronized void addTimeInSystem(double time) {
        sumTimeInSystem += time;
        minTimeInSystem = Math.min(minTimeInSystem, time);
        maxTimeInSystem = Math.max(maxTimeInSystem, time);
    }

    public void addProcessingTime(double time) { sumProcessingTime += time; }
    public void addMovementTime(double time) { sumMovementTime += time; }
    public void addWaitingTime(double time) { sumWaitingTime += time; }
    public void addBlockedTime(double time) { sumBlockedTime += time; }

    public double getAvgTimeInSystem() {
        return totalCompleted > 0 ? sumTimeInSystem / totalCompleted : 0;
    }

    public double getAvgProcessingTime() {
        return totalCompleted > 0 ? sumProcessingTime / totalCompleted : 0;
    }

    public double getAvgMovementTime() {
        return totalCompleted > 0 ? sumMovementTime / totalCompleted : 0;
    }

    public double getAvgWaitingTime() {
        return totalCompleted > 0 ? sumWaitingTime / totalCompleted : 0;
    }

    public double getCompletionRate() {
        return totalArrivals > 0 ? (totalCompleted * 100.0 / totalArrivals) : 0;
    }

    public String getDetailedReport() {
        return String.format(
            "%-12s | Arrivals: %4d | Completed: %4d | In System: %4d | Completion Rate: %5.1f%%\n" +
            "             | Avg Time: %7.2f hrs | Min: %7.2f | Max: %7.2f\n" +
            "             | Processing: %6.2f | Movement: %6.2f | Waiting: %6.2f | Blocked: %6.2f",
            type.name(),
            totalArrivals, totalCompleted, currentInSystem, getCompletionRate(),
            getAvgTimeInSystem(), minTimeInSystem == Double.MAX_VALUE ? 0 : minTimeInSystem, maxTimeInSystem,
            getAvgProcessingTime(), getAvgMovementTime(), getAvgWaitingTime(), getAvgWaitingTime()
        );
    }

    // Getters
    public Valve.Type getType() { return type; }
    public int getTotalArrivals() { return totalArrivals; }
    public int getTotalCompleted() { return totalCompleted; }
    public int getCurrentInSystem() { return currentInSystem; }
    public List<Double> getArrivalTimes() { return new ArrayList<>(arrivalTimes); }
    public List<Double> getCompletionTimes() { return new ArrayList<>(completionTimes); }
}

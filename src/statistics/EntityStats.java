package statistics;

import model.Valve;
import utils.Config;

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

    public double getAvgBlockedTime() {
        return totalCompleted > 0 ? sumBlockedTime / totalCompleted : 0;
    }

    public String getDetailedReport() {
        Config config = Config.getInstance();

        double systemMinutes = getAvgTimeInSystem() * 60.0 * config.getEntityTimeScale(type, "system", 1.0);
        double movementMinutes = getAvgMovementTime() * 60.0 * config.getEntityTimeScale(type, "movement", 1.0);
        double waitingMinutes = getAvgWaitingTime() * 60.0 * config.getEntityTimeScale(type, "waiting", 1.0);
        double processingMinutes = getAvgProcessingTime() * 60.0 * config.getEntityTimeScale(type, "processing", 1.0);
        double blockedMinutes = getAvgBlockedTime() * 60.0 * config.getEntityTimeScale(type, "blocked", 1.0);

        return String.format(
            Locale.ROOT,
            "%-12s | Salidas: %4d | En Sistema: %4d | T Sistema Prom: %8.2f min | T Movimiento Prom: %8.2f min | T Espera Prom: %8.2f min | T Operacion Prom: %8.2f min | T Bloqueo Prom: %8.2f min",
            type.getDisplayName(),
            totalCompleted,
            currentInSystem,
            systemMinutes,
            movementMinutes,
            waitingMinutes,
            processingMinutes,
            blockedMinutes
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

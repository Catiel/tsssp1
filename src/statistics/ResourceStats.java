package statistics;

import java.util.*;

public class ResourceStats {
    private String name;
    private List<Double> utilizationHistory;
    private List<Integer> tripHistory;
    private List<Double> timeHistory;

    private int units;
    private double scheduledHours;
    private double totalWorkMinutes;
    private double avgHandleMinutes;
    private double avgTravelMinutes;
    private double avgParkMinutes;
    private double blockedPercent;
    private double currentUtilization;

    public ResourceStats(String name) {
        this.name = name;
        this.utilizationHistory = Collections.synchronizedList(new ArrayList<>());
        this.tripHistory = Collections.synchronizedList(new ArrayList<>());
        this.timeHistory = Collections.synchronizedList(new ArrayList<>());
        this.units = 0;
        this.scheduledHours = 0;
        this.totalWorkMinutes = 0;
        this.avgHandleMinutes = 0;
        this.avgTravelMinutes = 0;
        this.avgParkMinutes = 0;
        this.blockedPercent = 0;
        this.currentUtilization = 0;
    }

    public synchronized void update(int units,
                                    double scheduledHours,
                                    double totalWorkMinutes,
                                    int trips,
                                    double avgHandleMinutes,
                                    double avgTravelMinutes,
                                    double avgParkMinutes,
                                    double blockedPercent,
                                    double utilization,
                                    double time) {
        this.units = units;
        this.scheduledHours = scheduledHours;
        this.totalWorkMinutes = totalWorkMinutes;
        this.avgHandleMinutes = avgHandleMinutes;
        this.avgTravelMinutes = avgTravelMinutes;
        this.avgParkMinutes = avgParkMinutes;
        this.blockedPercent = blockedPercent;
        this.currentUtilization = utilization;

        utilizationHistory.add(utilization);
        tripHistory.add(trips);
        timeHistory.add(time);
    }

    public double getAverageUtilization() {
        return utilizationHistory.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    public double getCurrentUtilization() {
        return currentUtilization;
    }

    public int getTotalTrips() {
        return tripHistory.isEmpty() ? 0 : tripHistory.get(tripHistory.size() - 1);
    }

    public String getReport() {
        return String.format(
            "%-12s | Unid: %d | Prog: %6.1f h | Trabajo: %7.2f min | Usos: %4d | T Uso: %4.2f min | T Viaje: %4.2f min | T Est: %4.2f min | %% Bloq: %4.2f | Util Prom: %5.1f%%",
            name,
            units,
            scheduledHours,
            totalWorkMinutes,
            getTotalTrips(),
            avgHandleMinutes,
            avgTravelMinutes,
            avgParkMinutes,
            blockedPercent,
            getAverageUtilization());
    }

    // Getters
    public List<Double> getUtilizationHistory() { return new ArrayList<>(utilizationHistory); }
    public List<Integer> getTripHistory() { return new ArrayList<>(tripHistory); }
    public List<Double> getTimeHistory() { return new ArrayList<>(timeHistory); }
    public int getUnits() { return units; }
    public double getScheduledHours() { return scheduledHours; }
    public double getTotalWorkMinutes() { return totalWorkMinutes; }
    public double getAvgHandleMinutes() { return avgHandleMinutes; }
    public double getAvgTravelMinutes() { return avgTravelMinutes; }
    public double getAvgParkMinutes() { return avgParkMinutes; }
    public double getBlockedPercent() { return blockedPercent; }
    public String getName() { return name; }
}

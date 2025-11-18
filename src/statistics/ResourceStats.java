package statistics;

import java.util.*;

public class ResourceStats {
    private String name;
    private List<Double> utilizationHistory;
    private List<Integer> tripHistory;
    private List<Double> timeHistory;

    public ResourceStats(String name) {
        this.name = name;
        this.utilizationHistory = Collections.synchronizedList(new ArrayList<>());
        this.tripHistory = Collections.synchronizedList(new ArrayList<>());
        this.timeHistory = Collections.synchronizedList(new ArrayList<>());
    }

    public synchronized void update(double utilization, int trips, double time) {
        utilizationHistory.add(utilization);
        tripHistory.add(trips);
        timeHistory.add(time);
    }

    public double getAverageUtilization() {
        return utilizationHistory.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    public double getCurrentUtilization() {
        return utilizationHistory.isEmpty() ? 0 : utilizationHistory.get(utilizationHistory.size() - 1);
    }

    public int getTotalTrips() {
        return tripHistory.isEmpty() ? 0 : tripHistory.get(tripHistory.size() - 1);
    }

    public String getReport() {
        return String.format("%-12s | Viajes: %5d | Util Prom: %5.1f%% | Actual: %5.1f%%",
            name, getTotalTrips(), getAverageUtilization(), getCurrentUtilization());
    }

    // Getters
    public List<Double> getUtilizationHistory() { return new ArrayList<>(utilizationHistory); }
    public List<Integer> getTripHistory() { return new ArrayList<>(tripHistory); }
    public List<Double> getTimeHistory() { return new ArrayList<>(timeHistory); }
}

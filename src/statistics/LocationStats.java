package statistics;

import utils.Config;
import utils.Localization;
import java.util.*;

public class LocationStats {
    private String name;
    private List<Integer> contentHistory;
    private List<Double> utilizationHistory;
    private List<Double> timeHistory;
    private int valvesProcessed; // Contador de válvulas procesadas

    public LocationStats(String name) {
        this.name = name;
        this.contentHistory = Collections.synchronizedList(new ArrayList<>());
        this.utilizationHistory = Collections.synchronizedList(new ArrayList<>());
        this.timeHistory = Collections.synchronizedList(new ArrayList<>());
        this.valvesProcessed = 0;
    }

    public synchronized void update(int contents, double utilization, double time) {
        contentHistory.add(contents);
        utilizationHistory.add(utilization);
        timeHistory.add(time);
    }

    public synchronized double getAverageContents() {
        List<Integer> snapshot = new ArrayList<>(contentHistory);
        return snapshot.stream().mapToInt(Integer::intValue).average().orElse(0);
    }

    public synchronized double getMaxContents() {
        List<Integer> snapshot = new ArrayList<>(contentHistory);
        return snapshot.stream().mapToInt(Integer::intValue).max().orElse(0);
    }

    public synchronized double getAverageUtilization() {
        List<Double> snapshot = new ArrayList<>(utilizationHistory);
        return snapshot.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    public double getCurrentUtilization() {
        return utilizationHistory.isEmpty() ? 0 : utilizationHistory.get(utilizationHistory.size() - 1);
    }

    public synchronized void incrementValvesProcessed() {
        valvesProcessed++;
    }
    
    public String getReport() {
        Config config = Config.getInstance();
        double statsScale = config.getLocationStatsScale(name, 1.0);
        double avgContents = getAverageContents() * statsScale;
        double maxContents = getMaxContents();
        double utilization = getCurrentUtilization();

        if (name.startsWith("Almacen_")) {
            int capacity = config.getLocationCapacity(name);
            if (capacity > 0 && capacity < Integer.MAX_VALUE) {
                utilization = (avgContents / capacity) * 100.0;
            }
        }

        return String.format("%-12s | Cont Prom: %6.2f | Max: %4.0f | Utilizacion: %5.1f%% | Procesadas: %d",
            Localization.getLocationDisplayName(name),
            avgContents, maxContents, utilization, valvesProcessed);
    }

    // Getters
    public String getName() { return name; }
    public List<Integer> getContentHistory() { return new ArrayList<>(contentHistory); }
    public List<Double> getUtilizationHistory() { return new ArrayList<>(utilizationHistory); }
    public List<Double> getTimeHistory() { return new ArrayList<>(timeHistory); }
    public int getValvesProcessed() { return valvesProcessed; }
}

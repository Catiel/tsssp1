package statistics;

import java.util.*;

public class LocationStats {
    private String name;
    private List<Integer> contentHistory;
    private List<Double> utilizationHistory;
    private List<Double> timeHistory;

    public LocationStats(String name) {
        this.name = name;
        this.contentHistory = Collections.synchronizedList(new ArrayList<>());
        this.utilizationHistory = Collections.synchronizedList(new ArrayList<>());
        this.timeHistory = Collections.synchronizedList(new ArrayList<>());
    }

    public synchronized void update(int contents, double utilization, double time) {
        contentHistory.add(contents);
        utilizationHistory.add(utilization);
        timeHistory.add(time);
    }

    public double getAverageContents() {
        return contentHistory.stream().mapToInt(Integer::intValue).average().orElse(0);
    }

    public double getMaxContents() {
        return contentHistory.stream().mapToInt(Integer::intValue).max().orElse(0);
    }

    public double getAverageUtilization() {
        return utilizationHistory.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    public double getCurrentUtilization() {
        return utilizationHistory.isEmpty() ? 0 : utilizationHistory.get(utilizationHistory.size() - 1);
    }

    public String getReport() {
        return String.format("%-12s | Avg Contents: %6.2f | Max: %4.0f | Utilization: %5.1f%%",
            name, getAverageContents(), getMaxContents(), getCurrentUtilization());
    }

    // Getters
    public String getName() { return name; }
    public List<Integer> getContentHistory() { return new ArrayList<>(contentHistory); }
    public List<Double> getUtilizationHistory() { return new ArrayList<>(utilizationHistory); }
    public List<Double> getTimeHistory() { return new ArrayList<>(timeHistory); }
}

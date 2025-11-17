package statistics;

import model.Valve;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Statistics {
    private Map<Valve.Type, EntityStats> entityStats;
    private Map<String, LocationStats> locationStats;
    private ResourceStats craneStats;

    public Statistics() {
        entityStats = new ConcurrentHashMap<>();
        locationStats = new ConcurrentHashMap<>();
        craneStats = new ResourceStats("Grua");

        for (Valve.Type type : Valve.Type.values()) {
            entityStats.put(type, new EntityStats(type));
        }
    }

    public void recordArrival(Valve valve) {
        entityStats.get(valve.getType()).recordArrival(valve.getArrivalTime());
    }

    public void recordCompletion(Valve valve, double completionTime) {
        EntityStats stats = entityStats.get(valve.getType());
        stats.recordCompletion(completionTime);
        stats.addTimeInSystem(valve.getTotalTimeInSystem(completionTime));
        stats.addProcessingTime(valve.getTotalProcessingTime());
        stats.addMovementTime(valve.getTotalMovementTime());
        stats.addWaitingTime(valve.getTotalWaitingTime());
        stats.addBlockedTime(valve.getTotalBlockedTime());
    }

    public void updateLocationStats(String locationName, int contents,
                                    double utilization, double time) {
        locationStats.putIfAbsent(locationName, new LocationStats(locationName));
        locationStats.get(locationName).update(contents, utilization, time);
    }

    public void updateCraneStats(double utilization, int trips, double time) {
        craneStats.update(utilization, trips, time);
    }

    public EntityStats getEntityStats(Valve.Type type) {
        return entityStats.get(type);
    }

    public LocationStats getLocationStats(String name) {
        return locationStats.get(name);
    }

    public ResourceStats getCraneStats() {
        return craneStats;
    }

    public Map<Valve.Type, EntityStats> getAllEntityStats() {
        return new HashMap<>(entityStats);
    }

    public Map<String, LocationStats> getAllLocationStats() {
        return new HashMap<>(locationStats);
    }

    public String generateReport(double currentTime) {
        StringBuilder sb = new StringBuilder();
        sb.append("╔═══════════════════════════════════════════════════════════╗\n");
        sb.append("║          VALVE MANUFACTURING SIMULATION REPORT             ║\n");
        sb.append("╚═══════════════════════════════════════════════════════════╝\n\n");

        sb.append(String.format("Simulation Time: %.2f hours (Week %d)\n\n",
            currentTime, (int)(currentTime/168) + 1));

        sb.append("┌─────────────────────────────────────────────────────────┐\n");
        sb.append("│  ENTITY STATISTICS                                       │\n");
        sb.append("├─────────────────────────────────────────────────────────┤\n");
        for (EntityStats stats : entityStats.values()) {
            sb.append(stats.getDetailedReport()).append("\n");
        }

        sb.append("\n┌─────────────────────────────────────────────────────────┐\n");
        sb.append("│  LOCATION STATISTICS                                     │\n");
        sb.append("├─────────────────────────────────────────────────────────┤\n");
        for (LocationStats stats : locationStats.values()) {
            sb.append(stats.getReport()).append("\n");
        }

        sb.append("\n┌─────────────────────────────────────────────────────────┐\n");
        sb.append("│  RESOURCE STATISTICS                                     │\n");
        sb.append("├─────────────────────────────────────────────────────────┤\n");
        sb.append(craneStats.getReport()).append("\n");

        return sb.toString();
    }
}

package statistics;

import model.Valve;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Statistics {
    private Map<Valve.Type, EntityStats> entityStats;
    private Map<String, LocationStats> locationStats;
    private ResourceStats craneStats;
    private Map<Integer, Integer> arrivalsPerWeek;
    private Map<Integer, Integer> completionsPerArrivalWeek;

    public Statistics() {
        entityStats = new ConcurrentHashMap<>();
        locationStats = new ConcurrentHashMap<>();
        craneStats = new ResourceStats("Grua");
        arrivalsPerWeek = new ConcurrentHashMap<>();
        completionsPerArrivalWeek = new ConcurrentHashMap<>();

        for (Valve.Type type : Valve.Type.values()) {
            entityStats.put(type, new EntityStats(type));
        }
    }

    public void recordArrival(Valve valve) {
        entityStats.get(valve.getType()).recordArrival(valve.getArrivalTime());
        int week = getWeekIndex(valve.getArrivalTime());
        arrivalsPerWeek.merge(week, 1, Integer::sum);
    }

    public void recordCompletion(Valve valve, double completionTime) {
        EntityStats stats = entityStats.get(valve.getType());
        stats.recordCompletion(completionTime);
        stats.addTimeInSystem(valve.getTotalTimeInSystem(completionTime));
        stats.addProcessingTime(valve.getTotalProcessingTime());
        stats.addMovementTime(valve.getTotalMovementTime());
        stats.addWaitingTime(valve.getTotalWaitingTime());
        stats.addBlockedTime(valve.getTotalBlockedTime());

        int arrivalWeek = getWeekIndex(valve.getArrivalTime());
        completionsPerArrivalWeek.merge(arrivalWeek, 1, Integer::sum);
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

    public LocationStats getOrCreateLocationStats(String name) {
        return locationStats.computeIfAbsent(name, LocationStats::new);
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

    public Map<Integer, Integer> getArrivalsPerWeek() {
        return new HashMap<>(arrivalsPerWeek);
    }

    public Map<Integer, Integer> getCompletionsPerArrivalWeek() {
        return new HashMap<>(completionsPerArrivalWeek);
    }

    public int getArrivalsForWeek(int week) {
        return arrivalsPerWeek.getOrDefault(week, 0);
    }

    public int getCompletionsForArrivalWeek(int week) {
        return completionsPerArrivalWeek.getOrDefault(week, 0);
    }

    public String generateReport(double currentTime) {
        StringBuilder sb = new StringBuilder();
        sb.append("╔═══════════════════════════════════════════════════════════╗\n");
        sb.append("║        INFORME DE LA SIMULACION DE VALVULAS               ║\n");
        sb.append("╚═══════════════════════════════════════════════════════════╝\n\n");

        sb.append(String.format("Tiempo Simulado: %.2f horas (Semana %d)\n\n",
            currentTime, (int)(currentTime/168) + 1));

        sb.append("┌─────────────────────────────────────────────────────────┐\n");
        sb.append("│  ESTADISTICAS DE ENTIDADES                                │\n");
        sb.append("├─────────────────────────────────────────────────────────┤\n");
        for (EntityStats stats : entityStats.values()) {
            sb.append(stats.getDetailedReport()).append("\n");
        }

        sb.append("\n┌─────────────────────────────────────────────────────────┐\n");
        sb.append("│  ESTADISTICAS DE UBICACIONES                              │\n");
        sb.append("├─────────────────────────────────────────────────────────┤\n");
        
        // Ordenar: Agregados (M1, M2, M3), luego Almacenes, luego otros
        List<LocationStats> sortedStats = new ArrayList<>(locationStats.values());
        sortedStats.sort((a, b) -> {
            String nameA = a.getName();
            String nameB = b.getName();
            
            // Ocultar unidades individuales (M1.1, M2.3, etc.)
            boolean aIsUnit = nameA.matches("M[123]\\.\\d+");
            boolean bIsUnit = nameB.matches("M[123]\\.\\d+");
            if (aIsUnit || bIsUnit) {
                if (aIsUnit && bIsUnit) return 0;
                return aIsUnit ? 1 : -1; // Units al final
            }
            
            // Prioridad 1: Agregados M1, M2, M3
            boolean aIsAggregate = nameA.matches("M[123]");
            boolean bIsAggregate = nameB.matches("M[123]");
            if (aIsAggregate != bIsAggregate) {
                return aIsAggregate ? -1 : 1;
            }
            if (aIsAggregate) {
                return nameA.compareTo(nameB);
            }
            
            // Prioridad 2: Almacenes
            boolean aIsAlmacen = nameA.startsWith("Almacen_");
            boolean bIsAlmacen = nameB.startsWith("Almacen_");
            if (aIsAlmacen != bIsAlmacen) {
                return aIsAlmacen ? -1 : 1;
            }
            if (aIsAlmacen) {
                return nameA.compareTo(nameB);
            }
            
            // Resto alfabético
            return nameA.compareTo(nameB);
        });
        
        // Imprimir solo agregados, almacenes y ubicaciones principales
        for (LocationStats stats : sortedStats) {
            String name = stats.getName();
            // Saltar unidades individuales
            if (name.matches("M[123]\\.\\d+")) {
                continue;
            }
            sb.append(stats.getReport()).append("\n");
        }

        sb.append("\n┌─────────────────────────────────────────────────────────┐\n");
        sb.append("│  ESTADISTICAS DE RECURSOS                                 │\n");
        sb.append("├─────────────────────────────────────────────────────────┤\n");
        sb.append(craneStats.getReport()).append("\n");

        sb.append("\n┌─────────────────────────────────────────────────────────┐\n");
        sb.append("│  ARRIBOS VS COMPLETADOS POR SEMANA DE ARRIBO              │\n");
        sb.append("├─────────────────────────────────────────────────────────┤\n");
        int maxWeek = arrivalsPerWeek.keySet().stream()
            .mapToInt(Integer::intValue)
            .max()
            .orElse(0);
        for (int week = 1; week <= maxWeek; week++) {
            int arrivals = getArrivalsForWeek(week);
            int completions = getCompletionsForArrivalWeek(week);
            sb.append(String.format("Semana %02d | Llegadas: %3d | Completadas (segun arribo): %3d\n",
                week, arrivals, completions));
        }

        // Análisis de cuellos de botella
        sb.append("\n┌─────────────────────────────────────────────────────────┐\n");
        sb.append("│  ANALISIS DE CUELLOS DE BOTELLA                           │\n");
        sb.append("├─────────────────────────────────────────────────────────┤\n");
        
        double maxUtil = 0;
        String bottleneck = "Ninguno";
        
        // Buscar solo entre agregados M1, M2, M3
        LocationStats m1Stats = locationStats.get("M1");
        LocationStats m2Stats = locationStats.get("M2");
        LocationStats m3Stats = locationStats.get("M3");
        
        if (m1Stats != null && m1Stats.getCurrentUtilization() > maxUtil) {
            maxUtil = m1Stats.getCurrentUtilization();
            bottleneck = "Maquina 1";
        }
        if (m2Stats != null && m2Stats.getCurrentUtilization() > maxUtil) {
            maxUtil = m2Stats.getCurrentUtilization();
            bottleneck = "Maquina 2";
        }
        if (m3Stats != null && m3Stats.getCurrentUtilization() > maxUtil) {
            maxUtil = m3Stats.getCurrentUtilization();
            bottleneck = "Maquina 3";
        }
        
        sb.append(String.format("Cuello Principal: %s (%.1f%% de utilizacion)\n", bottleneck, maxUtil));

        return sb.toString();
    }

    private int getWeekIndex(double time) {
        return (int)Math.floor(time / 168.0) + 1;
    }
}

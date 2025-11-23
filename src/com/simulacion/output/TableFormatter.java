package com.simulacion.output;

public class TableFormatter {
    
    public static String formatDouble(double value, int decimals) {
        return String.format("%." + decimals + "f", value);
    }

    public static String formatEntityTable(java.util.Map<String, com.simulacion.entities.EntityStatistics> stats) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("\n=== ENTIDAD RESUMEN ===\n\n");
        sb.append(String.format("%-20s %15s %20s %30s %35s %30s %25s %25s\n",
            "Nombre", "Total Salida", "Cantidad actual en Sistema", 
            "Tiempo En Sistema Promedio (Min)", "Tiempo En lógica de movimiento Promedio (Min)",
            "Tiempo Espera Promedio (Min)", "Tiempo En Operación Promedio (Min)",
            "Tiempo de Bloqueo Promedio (Min)"));
        sb.append("-".repeat(180)).append("\n");

        for (com.simulacion.entities.EntityStatistics stat : stats.values()) {
            sb.append(String.format("%-20s %15d %20s %30s %35s %30s %25s %25s\n",
                stat.getEntityName(),
                stat.getTotalExits(),
                "0.00",
                formatDouble(stat.getAverageSystemTime(), 2),
                formatDouble(stat.getAverageNonValueAddedTime(), 2),
                formatDouble(stat.getAverageWaitTime(), 2),
                formatDouble(stat.getAverageValueAddedTime(), 2),
                "0.00"
            ));
        }
        
        return sb.toString();
    }

    public static String formatLocationTable(java.util.Map<String, com.simulacion.locations.LocationStatistics> stats) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("\n=== LOCACIÓN RESUMEN ===\n\n");
        sb.append(String.format("%-20s %20s %15s %20s %30s %25s %20s %20s %20s\n",
            "Nombre", "Tiempo Programado (Hr)", "Capacidad", "Total Entradas",
            "Tiempo Por entrada Promedio (Min)", "Contenido Promedio", "Contenido Máximo",
            "Contenido Actual", "% Utilización"));
        sb.append("-".repeat(180)).append("\n");

        for (com.simulacion.locations.LocationStatistics stat : stats.values()) {
            sb.append(String.format("%-20s %20s %15d %20d %30s %25s %20s %20s %20s\n",
                stat.getLocationName(),
                formatDouble(stat.getScheduledTime() / 60.0, 2),
                stat.getCapacity(),
                stat.getTotalEntries(),
                formatDouble(stat.getAverageTimePerEntry(), 2),
                formatDouble(stat.getAverageContents(), 2),
                formatDouble(stat.getMaxContents(), 2),
                formatDouble(stat.getCurrentContents(), 2),
                formatDouble(stat.getUtilizationPercent(), 2)
            ));
        }
        
        return sb.toString();
    }
}

package com.simulacion.output;

import com.simulacion.statistics.StatisticsCollector;
import com.simulacion.entities.EntityStatistics;
import com.simulacion.locations.LocationStatistics;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

public class ReportGenerator {
    private final StatisticsCollector statistics;

    public ReportGenerator(StatisticsCollector statistics) {
        this.statistics = statistics;
    }

    public void generateConsoleReport() {
        System.out.println("\n" + "=".repeat(100));
        System.out.println("REPORTE DE SIMULACIÓN - MODELO DE PRODUCCIÓN DE CERVEZA");
        System.out.println("=".repeat(100));

        // Tabla de entidades
        Map<String, EntityStatistics> entityStats = statistics.getEntityStats();
        System.out.println(TableFormatter.formatEntityTable(entityStats));

        // Tabla de locaciones
        Map<String, LocationStatistics> locationStats = statistics.getLocationStats();
        System.out.println(TableFormatter.formatLocationTable(locationStats));
    }

    public void generateFileReport(String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("REPORTE DE SIMULACIÓN - MODELO DE PRODUCCIÓN DE CERVEZA");
            writer.println("=".repeat(100));
            
            Map<String, EntityStatistics> entityStats = statistics.getEntityStats();
            writer.println(TableFormatter.formatEntityTable(entityStats));
            
            Map<String, LocationStatistics> locationStats = statistics.getLocationStats();
            writer.println(TableFormatter.formatLocationTable(locationStats));
            
            System.out.println("Reporte generado: " + filename);
        } catch (IOException e) {
            System.err.println("Error al generar reporte: " + e.getMessage());
        }
    }

    public void generateCSVReport(String entityFile, String locationFile) {
        generateEntityCSV(entityFile);
        generateLocationCSV(locationFile);
    }

    private void generateEntityCSV(String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("Nombre,Total Salida,Tiempo En Sistema Promedio (Min)," +
                         "Tiempo En lógica de movimiento Promedio (Min)," +
                         "Tiempo Espera Promedio (Min)," +
                         "Tiempo En Operación Promedio (Min)");
            
            for (EntityStatistics stat : statistics.getEntityStats().values()) {
                writer.printf("%s,%d,%.2f,%.2f,%.2f,%.2f\n",
                    stat.getEntityName(),
                    stat.getTotalExits(),
                    stat.getAverageSystemTime(),
                    stat.getAverageNonValueAddedTime(),
                    stat.getAverageWaitTime(),
                    stat.getAverageValueAddedTime()
                );
            }
            
            System.out.println("CSV de entidades generado: " + filename);
        } catch (IOException e) {
            System.err.println("Error al generar CSV: " + e.getMessage());
        }
    }

    private void generateLocationCSV(String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("Nombre,Tiempo Programado (Hr),Capacidad,Total Entradas," +
                         "Tiempo Por entrada Promedio (Min),Contenido Promedio," +
                         "Contenido Máximo,Contenido Actual,% Utilización");
            
            for (LocationStatistics stat : statistics.getLocationStats().values()) {
                writer.printf("%s,%.2f,%d,%d,%.2f,%.2f,%.2f,%.2f,%.2f\n",
                    stat.getLocationName(),
                    stat.getScheduledTime() / 60.0,
                    stat.getCapacity(),
                    stat.getTotalEntries(),
                    stat.getAverageTimePerEntry(),
                    stat.getAverageContents(),
                    stat.getMaxContents(),
                    stat.getCurrentContents(),
                    stat.getUtilizationPercent()
                );
            }
            
            System.out.println("CSV de locaciones generado: " + filename);
        } catch (IOException e) {
            System.err.println("Error al generar CSV: " + e.getMessage());
        }
    }
}

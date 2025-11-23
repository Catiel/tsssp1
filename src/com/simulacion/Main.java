package com.simulacion;

import com.simulacion.core.SimulationEngine;
import com.simulacion.output.ReportGenerator;
import com.simulacion.processing.ProcessingRule;
import com.simulacion.entities.Entity;

public class Main {
    public static void main(String[] args) {
        System.out.println("Iniciando simulación del modelo de producción de cerveza...\n");

        // Crear motor de simulación
        SimulationEngine engine = new SimulationEngine();

        // Configurar tipos de entidades
        setupEntityTypes(engine);

        // Configurar locaciones
        setupLocations(engine);

        // Configurar recursos
        setupResources(engine);

        // Configurar reglas de procesamiento
        setupProcessingRules(engine);

        // Configurar arribos
        setupArrivals(engine);

        // Ejecutar simulación (8 semanas = 56 días * 24 horas * 60 minutos)
        // Pero considerando horario: Lunes 6 PM - Viernes 10 PM
        // Aproximadamente 70 horas semanales * 8 semanas * 60 min = 33,600 minutos
        double simulationTime = 33600.0; // minutos
        
        System.out.println("Ejecutando simulación por " + simulationTime + " minutos...\n");
        engine.run(simulationTime);

        // Generar reportes
        ReportGenerator reportGenerator = new ReportGenerator(engine.getStatistics());
        reportGenerator.generateConsoleReport();
        reportGenerator.generateFileReport("reporte_simulacion.txt");
        reportGenerator.generateCSVReport("entidades_reporte.csv", "locaciones_reporte.csv");

        System.out.println("\n¡Simulación completada exitosamente!");
    }

    private static void setupEntityTypes(SimulationEngine engine) {
        engine.addEntityType("GRANOS_DE_CEBADA", 150.0);
        engine.addEntityType("LUPULO", 150.0);
        engine.addEntityType("LEVADURA", 150.0);
        engine.addEntityType("MOSTO", 150.0);
        engine.addEntityType("CERVEZA", 150.0);
        engine.addEntityType("BOTELLA_CON_CERVEZA", 150.0);
        engine.addEntityType("CAJA_VACIA", 150.0);
        engine.addEntityType("CAJA_CON_CERVEZAS", 150.0);
    }

    private static void setupLocations(SimulationEngine engine) {
        engine.addLocation("SILO_GRANDE", 3, 1);
        engine.addLocation("MALTEADO", 3, 1);
        engine.addLocation("SECADO", 3, 1);
        engine.addLocation("MOLIENDA", 2, 1);
        engine.addLocation("MACERADO", 3, 1);
        engine.addLocation("FILTRADO", 2, 1);
        engine.addLocation("COCCION", 10, 1);
        engine.addLocation("ALMACEN_CAJAS", 30, 1);
        engine.addLocation("SILO_LUPULO", 10, 1);
        engine.addLocation("ENFRIAMIENTO", 10, 1);
        engine.addLocation("EMPACADO", 1, 1);
        engine.addLocation("ETIQUETADO", 6, 1);
        engine.addLocation("EMBOTELLADO", 6, 1);
        engine.addLocation("INSPECCION", 3, 1);
        engine.addLocation("MADURACION", 10, 1);
        engine.addLocation("FERMENTACION", 10, 1);
        engine.addLocation("SILO_LEVADURA", 10, 1);
        engine.addLocation("ALMACENAJE", 6, 1);
        engine.addLocation("MERCADO", Integer.MAX_VALUE, 1);
    }

    private static void setupResources(SimulationEngine engine) {
        engine.addResource("OPERADOR_RECEPCION", 1, 90.0);
        engine.addResource("OPERADOR_LUPULO", 1, 100.0);
        engine.addResource("OPERADOR_LEVADURA", 1, 100.0);
        engine.addResource("OPERADOR_EMPACADO", 1, 100.0);
        engine.addResource("CAMION", 1, 100.0);
    }

    private static void setupProcessingRules(SimulationEngine engine) {
        // GRANOS_DE_CEBADA
        engine.addProcessingRule(new SimpleProcessingRule("SILO_GRANDE", "GRANOS_DE_CEBADA", 0));
        engine.addProcessingRule(new SimpleProcessingRule("MALTEADO", "GRANOS_DE_CEBADA", 60));
        engine.addProcessingRule(new SimpleProcessingRule("SECADO", "GRANOS_DE_CEBADA", 60));
        engine.addProcessingRule(new SimpleProcessingRule("MOLIENDA", "GRANOS_DE_CEBADA", 60));
        engine.addProcessingRule(new SimpleProcessingRule("MACERADO", "GRANOS_DE_CEBADA", 90));
        engine.addProcessingRule(new SimpleProcessingRule("FILTRADO", "GRANOS_DE_CEBADA", 30));
        
        // LUPULO
        engine.addProcessingRule(new SimpleProcessingRule("SILO_LUPULO", "LUPULO", 0));
        engine.addProcessingRule(new SimpleProcessingRule("COCCION", "LUPULO", 60));
        
        // LEVADURA
        engine.addProcessingRule(new SimpleProcessingRule("SILO_LEVADURA", "LEVADURA", 0));
        engine.addProcessingRule(new SimpleProcessingRule("FERMENTACION", "LEVADURA", 120));
        
        // MOSTO y CERVEZA
        engine.addProcessingRule(new SimpleProcessingRule("ENFRIAMIENTO", "MOSTO", 60));
        engine.addProcessingRule(new SimpleProcessingRule("MADURACION", "CERVEZA", 90));
        engine.addProcessingRule(new SimpleProcessingRule("INSPECCION", "CERVEZA", 30));
        engine.addProcessingRule(new SimpleProcessingRule("EMBOTELLADO", "CERVEZA", 3));
        
        // BOTELLAS
        engine.addProcessingRule(new SimpleProcessingRule("ETIQUETADO", "BOTELLA_CON_CERVEZA", 1));
        
        // CAJAS
        engine.addProcessingRule(new SimpleProcessingRule("ALMACEN_CAJAS", "CAJA_VACIA", 0));
        engine.addProcessingRule(new SimpleProcessingRule("EMPACADO", "CAJA_VACIA", 10));
        engine.addProcessingRule(new SimpleProcessingRule("ALMACENAJE", "CAJA_CON_CERVEZAS", 5));
        engine.addProcessingRule(new SimpleProcessingRule("MERCADO", "CAJA_CON_CERVEZAS", 0));
    }

    private static void setupArrivals(SimulationEngine engine) {
        // GRANOS_DE_CEBADA: Primera vez = 0, INF ocurrencias, cada 25 minutos
        engine.scheduleArrival("GRANOS_DE_CEBADA", "SILO_GRANDE", 0, 1345, 25);
        
        // LUPULO: Primera vez = 0, INF ocurrencias, cada 10 minutos
        engine.scheduleArrival("LUPULO", "SILO_LUPULO", 0, 3360, 10);
        
        // LEVADURA: Primera vez = 0, INF ocurrencias, cada 20 minutos
        engine.scheduleArrival("LEVADURA", "SILO_LEVADURA", 0, 1680, 20);
        
        // CAJA_VACIA: Primera vez = 0, INF ocurrencias, cada 30 minutos
        engine.scheduleArrival("CAJA_VACIA", "ALMACEN_CAJAS", 0, 1120, 30);
    }

    // Clase interna para reglas de procesamiento simples
    private static class SimpleProcessingRule extends ProcessingRule {
        public SimpleProcessingRule(String locationName, String entityTypeName, double processingTime) {
            super(locationName, entityTypeName, processingTime);
        }

        @Override
        public void process(Entity entity, SimulationEngine engine) {
            // Implementación básica - se puede extender según necesidades
        }
    }
}

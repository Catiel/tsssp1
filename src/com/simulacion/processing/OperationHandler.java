package com.simulacion.processing;

import com.simulacion.core.Event;
import com.simulacion.core.SimulationEngine;
import com.simulacion.entities.Entity;
import com.simulacion.locations.Location;
import com.simulacion.resources.Resource;

import java.util.Random;

public class OperationHandler {
    private final SimulationEngine engine;
    private final Random random;

    public OperationHandler(SimulationEngine engine) {
        this.engine = engine;
        this.random = new Random();
    }

    public void handleArrival(Entity entity, String locationName) {
        Location location = engine.getLocation(locationName);
        double currentTime = engine.getClock().getCurrentTime();
        
        entity.setEntryTime(currentTime);
        location.enter(entity, currentTime);
        
        // Registrar entrada
        engine.getStatistics().recordLocationEntry(locationName);
        
        // Programar procesamiento
        scheduleProcessing(entity, locationName);
    }

    public void scheduleProcessing(Entity entity, String locationName) {
        ProcessingRule rule = engine.getProcessingRule(locationName);
        if (rule != null) {
            double processingTime = rule.getProcessingTime();
            double currentTime = engine.getClock().getCurrentTime();
            
            Event processingEvent = new Event(currentTime + processingTime, 0, 
                "Process " + entity.getType().getName() + " at " + locationName) {
                @Override
                public void execute() {
                    completeProcessing(entity, locationName);
                }
            };
            
            engine.getScheduler().scheduleEvent(processingEvent);
            
            // Registrar tiempo de procesamiento
            entity.addValueAddedTime(processingTime);
            engine.getStatistics().recordLocationProcessingTime(locationName, processingTime);
        }
    }

    public void completeProcessing(Entity entity, String locationName) {
        Location location = engine.getLocation(locationName);
        double currentTime = engine.getClock().getCurrentTime();
        
        // Salir de la locación
        location.exit(currentTime);
        
        // Determinar siguiente destino
        routeEntity(entity, locationName);
    }

    public void routeEntity(Entity entity, String fromLocation) {
        RoutingRule route = getRoutingRule(fromLocation);
        
        if (route != null) {
            String destination = route.getDestinationLocation();
            
            if ("EXIT".equals(destination)) {
                handleExit(entity);
            } else {
                // Verificar si necesita recurso para moverse
                if (route.getResourceName() != null && !route.getResourceName().isEmpty()) {
                    moveWithResource(entity, destination, route.getResourceName());
                } else {
                    handleArrival(entity, destination);
                }
            }
        } else {
            handleExit(entity);
        }
    }

    private void moveWithResource(Entity entity, String destination, String resourceName) {
        Resource resource = engine.getResource(resourceName);
        double currentTime = engine.getClock().getCurrentTime();
        
        if (resource != null && resource.isAvailable()) {
            resource.acquire(currentTime);
            
            // Calcular tiempo de movimiento (simulado)
            double moveTime = 2.0; // Tiempo base de movimiento
            
            Event moveEvent = new Event(currentTime + moveTime, 0, 
                "Move " + entity.getType().getName() + " to " + destination) {
                @Override
                public void execute() {
                    resource.release(engine.getClock().getCurrentTime());
                    handleArrival(entity, destination);
                }
            };
            
            engine.getScheduler().scheduleEvent(moveEvent);
            entity.addNonValueAddedTime(moveTime);
        } else {
            // Esperar por recurso
            if (resource != null) {
                resource.addToQueue(entity);
            }
        }
    }

    private void handleExit(Entity entity) {
        double currentTime = engine.getClock().getCurrentTime();
        entity.addSystemTime(currentTime - entity.getEntryTime());
        engine.getStatistics().recordEntityExit(entity);
    }

    private RoutingRule getRoutingRule(String locationName) {
        // Esta implementación se puede extender con una tabla de rutas más compleja
        // Por ahora, retornamos reglas basadas en la lógica del modelo ProModel
        return createRoutingRuleForLocation(locationName);
    }

    private RoutingRule createRoutingRuleForLocation(String locationName) {
        // Implementar lógica de enrutamiento según el modelo ProModel
        switch (locationName) {
            case "SILO_GRANDE":
                return new RoutingRule("MALTEADO", 1.0, 1, "FIRST", null);
            case "MALTEADO":
                return new RoutingRule("SECADO", 1.0, 1, "FIRST", "OPERADOR_RECEPCION");
            case "SECADO":
                return new RoutingRule("MOLIENDA", 1.0, 1, "FIRST", "OPERADOR_RECEPCION");
            case "MOLIENDA":
                return new RoutingRule("MACERADO", 1.0, 1, "FIRST", null);
            case "MACERADO":
                return new RoutingRule("FILTRADO", 1.0, 1, "FIRST", null);
            case "FILTRADO":
                return new RoutingRule("COCCION", 1.0, 1, "FIRST", null);
            case "COCCION":
                return new RoutingRule("ENFRIAMIENTO", 1.0, 1, "FIRST", null);
            case "ENFRIAMIENTO":
                return new RoutingRule("FERMENTACION", 1.0, 1, "FIRST", null);
            case "FERMENTACION":
                return new RoutingRule("MADURACION", 1.0, 1, "FIRST", null);
            case "MADURACION":
                return new RoutingRule("INSPECCION", 1.0, 1, "FIRST", null);
            case "INSPECCION":
                return new RoutingRule("EMBOTELLADO", 0.9, 1, "FIRST", null);
            case "EMBOTELLADO":
                return new RoutingRule("ETIQUETADO", 1.0, 6, "FIRST", null);
            case "ETIQUETADO":
                return new RoutingRule("EMPACADO", 1.0, 1, "JOIN", null);
            case "EMPACADO":
                return new RoutingRule("ALMACENAJE", 1.0, 1, "FIRST", "OPERADOR_EMPACADO");
            case "ALMACENAJE":
                return new RoutingRule("MERCADO", 1.0, 1, "FIRST", "CAMION");
            case "MERCADO":
                return new RoutingRule("EXIT", 1.0, 1, "FIRST", null);
            default:
                return new RoutingRule("EXIT", 1.0, 1, "FIRST", null);
        }
    }
}

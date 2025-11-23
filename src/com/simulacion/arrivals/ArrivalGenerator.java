package com.simulacion.arrivals;

import com.simulacion.core.Event;
import com.simulacion.core.SimulationEngine;
import com.simulacion.entities.Entity;
import com.simulacion.entities.EntityType;
import com.simulacion.processing.OperationHandler;

public class ArrivalGenerator {
    private final SimulationEngine engine;
    private final OperationHandler operationHandler;

    public ArrivalGenerator(SimulationEngine engine) {
        this.engine = engine;
        this.operationHandler = new OperationHandler(engine);
    }

    public void scheduleArrivals(String entityTypeName, String locationName, 
                                double firstTime, int occurrences, double frequency) {
        EntityType entityType = engine.getEntityType(entityTypeName);
        
        if (entityType == null) {
            System.err.println("Tipo de entidad no encontrado: " + entityTypeName);
            return;
        }

        for (int i = 0; i < occurrences; i++) {
            double arrivalTime = firstTime + (i * frequency);
            
            Event arrivalEvent = new Event(arrivalTime, 0, 
                "Arrival of " + entityTypeName + " at " + locationName) {
                @Override
                public void execute() {
                    Entity entity = new Entity(entityType);
                    operationHandler.handleArrival(entity, locationName);
                }
            };
            
            engine.getScheduler().scheduleEvent(arrivalEvent);
        }
    }
}

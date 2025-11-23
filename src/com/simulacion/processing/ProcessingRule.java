package com.simulacion.processing;

import com.simulacion.entities.Entity;
import com.simulacion.core.SimulationEngine;

public abstract class ProcessingRule {
    protected final String locationName;
    protected final String entityTypeName;
    protected final double processingTime;

    public ProcessingRule(String locationName, String entityTypeName, double processingTime) {
        this.locationName = locationName;
        this.entityTypeName = entityTypeName;
        this.processingTime = processingTime;
    }

    public abstract void process(Entity entity, SimulationEngine engine);

    public String getLocationName() {
        return locationName;
    }

    public String getEntityTypeName() {
        return entityTypeName;
    }

    public double getProcessingTime() {
        return processingTime;
    }
}

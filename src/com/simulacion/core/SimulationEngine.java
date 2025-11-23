package com.simulacion.core;

import com.simulacion.entities.*;
import com.simulacion.locations.*;
import com.simulacion.resources.*;
import com.simulacion.statistics.StatisticsCollector;
import com.simulacion.processing.*;
import com.simulacion.arrivals.ArrivalGenerator;

import java.util.HashMap;
import java.util.Map;

public class SimulationEngine {
    private final SimulationClock clock;
    private final EventScheduler scheduler;
    private final StatisticsCollector statistics;
    private final Map<String, EntityType> entityTypes;
    private final Map<String, Location> locations;
    private final Map<String, Resource> resources;
    private final Map<String, ProcessingRule> processingRules;
    private final ArrivalGenerator arrivalGenerator;
    private double simulationEndTime;

    public SimulationEngine() {
        this.clock = new SimulationClock();
        this.scheduler = new EventScheduler(clock);
        this.statistics = new StatisticsCollector();
        this.entityTypes = new HashMap<>();
        this.locations = new HashMap<>();
        this.resources = new HashMap<>();
        this.processingRules = new HashMap<>();
        this.arrivalGenerator = new ArrivalGenerator(this);
    }

    public void addEntityType(String name, double speed) {
        entityTypes.put(name, new EntityType(name, speed));
    }

    public void addLocation(String name, int capacity, int units) {
        locations.put(name, new Location(new LocationType(name, capacity, units)));
    }

    public void addResource(String name, int units, double speed) {
        resources.put(name, new Resource(new ResourceType(name, units, speed)));
    }

    public void addProcessingRule(ProcessingRule rule) {
        processingRules.put(rule.getLocationName(), rule);
    }

    public void scheduleArrival(String entityTypeName, String locationName, 
                               double firstTime, int occurrences, double frequency) {
        arrivalGenerator.scheduleArrivals(entityTypeName, locationName, 
                                         firstTime, occurrences, frequency);
    }

    public void run(double endTime) {
        this.simulationEndTime = endTime;
        
        while (scheduler.hasEvents() && clock.getCurrentTime() < endTime) {
            Event event = scheduler.getNextEvent();
            clock.advanceTo(event.getScheduledTime());
            event.execute();
        }
        
        // Finalizar estadísticas
        statistics.calculateLocationStatistics(locations, clock.getCurrentTime());
    }

    // Getters
    public SimulationClock getClock() { return clock; }
    public EventScheduler getScheduler() { return scheduler; }
    public StatisticsCollector getStatistics() { return statistics; }
    public EntityType getEntityType(String name) { return entityTypes.get(name); }
    public Location getLocation(String name) { return locations.get(name); }
    public Resource getResource(String name) { return resources.get(name); }
    public ProcessingRule getProcessingRule(String location) { return processingRules.get(location); }
    public Map<String, Location> getAllLocations() { return locations; }
}

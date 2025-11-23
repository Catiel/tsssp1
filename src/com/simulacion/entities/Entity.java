package com.simulacion.entities;

import com.simulacion.locations.Location;

public class Entity {
    private static int nextId = 1;
    
    private final int id;
    private final EntityType type;
    private Location currentLocation;
    private double entryTime;
    private double totalSystemTime;
    private double totalValueAddedTime;
    private double totalNonValueAddedTime;
    private double totalWaitTime;

    public Entity(EntityType type) {
        this.id = nextId++;
        this.type = type;
        this.totalSystemTime = 0;
        this.totalValueAddedTime = 0;
        this.totalNonValueAddedTime = 0;
        this.totalWaitTime = 0;
    }

    public int getId() {
        return id;
    }

    public EntityType getType() {
        return type;
    }

    public Location getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(Location location) {
        this.currentLocation = location;
    }

    public double getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(double time) {
        this.entryTime = time;
    }

    public void addSystemTime(double time) {
        this.totalSystemTime += time;
    }

    public void addValueAddedTime(double time) {
        this.totalValueAddedTime += time;
    }

    public void addNonValueAddedTime(double time) {
        this.totalNonValueAddedTime += time;
    }

    public void addWaitTime(double time) {
        this.totalWaitTime += time;
    }

    public double getTotalSystemTime() {
        return totalSystemTime;
    }

    public double getTotalValueAddedTime() {
        return totalValueAddedTime;
    }

    public double getTotalNonValueAddedTime() {
        return totalNonValueAddedTime;
    }

    public double getTotalWaitTime() {
        return totalWaitTime;
    }
}

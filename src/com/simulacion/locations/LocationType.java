package com.simulacion.locations;

public class LocationType {
    private final String name;
    private final int capacity;
    private final int units;

    public LocationType(String name, int capacity, int units) {
        this.name = name;
        this.capacity = capacity;
        this.units = units;
    }

    public String getName() {
        return name;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getUnits() {
        return units;
    }
}

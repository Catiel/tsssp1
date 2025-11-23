package com.simulacion.resources;

public class ResourceType {
    private final String name;
    private final int units;
    private final double speedMetersPerMinute;

    public ResourceType(String name, int units, double speedMetersPerMinute) {
        this.name = name;
        this.units = units;
        this.speedMetersPerMinute = speedMetersPerMinute;
    }

    public String getName() {
        return name;
    }

    public int getUnits() {
        return units;
    }

    public double getSpeedMetersPerMinute() {
        return speedMetersPerMinute;
    }
}

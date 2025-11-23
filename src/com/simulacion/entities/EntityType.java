package com.simulacion.entities;

public class EntityType {
    private final String name;
    private final double speedMetersPerMinute;

    public EntityType(String name, double speedMetersPerMinute) {
        this.name = name;
        this.speedMetersPerMinute = speedMetersPerMinute;
    }

    public String getName() {
        return name;
    }

    public double getSpeedMetersPerMinute() {
        return speedMetersPerMinute;
    }
}

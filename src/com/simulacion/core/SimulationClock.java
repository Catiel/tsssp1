package com.simulacion.core;

public class SimulationClock {
    private double currentTime;

    public SimulationClock() {
        this.currentTime = 0.0;
    }

    public void advanceTo(double newTime) {
        if (newTime >= currentTime) {
            currentTime = newTime;
        } else {
            throw new IllegalArgumentException("No se puede retroceder el tiempo");
        }
    }

    public double getCurrentTime() {
        return currentTime;
    }

    public void reset() {
        currentTime = 0.0;
    }
}

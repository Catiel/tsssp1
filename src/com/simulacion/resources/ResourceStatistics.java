package com.simulacion.resources;

public class ResourceStatistics {
    private final String resourceName;
    private int units;
    private double utilizationPercent;
    private double averageMinutesPerTrip;
    private int totalTrips;

    public ResourceStatistics(String resourceName) {
        this.resourceName = resourceName;
    }

    public void calculate(Resource resource, double totalTime, int trips, double totalTripTime) {
        this.units = resource.getType().getUnits();
        this.utilizationPercent = resource.getUtilization(totalTime);
        this.totalTrips = trips;
        
        if (trips > 0) {
            this.averageMinutesPerTrip = totalTripTime / trips;
        }
    }

    // Getters
    public String getResourceName() { return resourceName; }
    public int getUnits() { return units; }
    public double getUtilizationPercent() { return utilizationPercent; }
    public double getAverageMinutesPerTrip() { return averageMinutesPerTrip; }
    public int getTotalTrips() { return totalTrips; }
}

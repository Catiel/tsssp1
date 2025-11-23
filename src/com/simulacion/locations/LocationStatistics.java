package com.simulacion.locations;

public class LocationStatistics {
    private final String locationName;
    private double scheduledTime;
    private int capacity;
    private int totalEntries;
    private double averageTimePerEntry;
    private double averageContents;
    private double maxContents;
    private double currentContents;
    private double utilizationPercent;

    public LocationStatistics(String locationName) {
        this.locationName = locationName;
    }

    public void calculate(Location location, double totalSimulationTime, int entries, double totalProcessingTime) {
        this.scheduledTime = totalSimulationTime;
        this.capacity = location.getType().getCapacity();
        this.totalEntries = entries;
        
        if (entries > 0) {
            this.averageTimePerEntry = totalProcessingTime / entries;
        }
        
        this.averageContents = location.getTotalOccupancyTime() / totalSimulationTime;
        this.maxContents = capacity;
        this.currentContents = location.getCurrentOccupancy();
        
        if (capacity > 0) {
            this.utilizationPercent = (averageContents / capacity) * 100.0;
        }
    }

    // Getters
    public String getLocationName() { return locationName; }
    public double getScheduledTime() { return scheduledTime; }
    public int getCapacity() { return capacity; }
    public int getTotalEntries() { return totalEntries; }
    public double getAverageTimePerEntry() { return averageTimePerEntry; }
    public double getAverageContents() { return averageContents; }
    public double getMaxContents() { return maxContents; }
    public double getCurrentContents() { return currentContents; }
    public double getUtilizationPercent() { return utilizationPercent; }
}

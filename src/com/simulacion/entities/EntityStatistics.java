package com.simulacion.entities;

public class EntityStatistics {
    private final String entityName;
    private int totalExits;
    private double totalSystemTime;
    private double totalValueAddedTime;
    private double totalNonValueAddedTime;
    private double totalWaitTime;
    private double minSystemTime;
    private double maxSystemTime;

    public EntityStatistics(String entityName) {
        this.entityName = entityName;
        this.totalExits = 0;
        this.totalSystemTime = 0;
        this.totalValueAddedTime = 0;
        this.totalNonValueAddedTime = 0;
        this.totalWaitTime = 0;
        this.minSystemTime = Double.MAX_VALUE;
        this.maxSystemTime = 0;
    }

    public void recordExit(Entity entity) {
        totalExits++;
        double systemTime = entity.getTotalSystemTime();
        totalSystemTime += systemTime;
        totalValueAddedTime += entity.getTotalValueAddedTime();
        totalNonValueAddedTime += entity.getTotalNonValueAddedTime();
        totalWaitTime += entity.getTotalWaitTime();
        
        if (systemTime < minSystemTime) {
            minSystemTime = systemTime;
        }
        if (systemTime > maxSystemTime) {
            maxSystemTime = systemTime;
        }
    }

    public String getEntityName() {
        return entityName;
    }

    public int getTotalExits() {
        return totalExits;
    }

    public double getAverageSystemTime() {
        return totalExits > 0 ? totalSystemTime / totalExits : 0;
    }

    public double getAverageValueAddedTime() {
        return totalExits > 0 ? totalValueAddedTime / totalExits : 0;
    }

    public double getAverageNonValueAddedTime() {
        return totalExits > 0 ? totalNonValueAddedTime / totalExits : 0;
    }

    public double getAverageWaitTime() {
        return totalExits > 0 ? totalWaitTime / totalExits : 0;
    }

    public double getMinSystemTime() {
        return minSystemTime == Double.MAX_VALUE ? 0 : minSystemTime;
    }

    public double getMaxSystemTime() {
        return maxSystemTime;
    }
}

package com.simulacion.resources;

import com.simulacion.entities.Entity;
import java.util.LinkedList;
import java.util.Queue;

public class Resource {
    private final ResourceType type;
    private int availableUnits;
    private final Queue<Entity> waitingQueue;
    private double totalBusyTime;
    private double lastUpdateTime;

    public Resource(ResourceType type) {
        this.type = type;
        this.availableUnits = type.getUnits();
        this.waitingQueue = new LinkedList<>();
        this.totalBusyTime = 0;
        this.lastUpdateTime = 0;
    }

    public boolean isAvailable() {
        return availableUnits > 0;
    }

    public void acquire(double currentTime) {
        if (availableUnits > 0) {
            updateBusyTime(currentTime);
            availableUnits--;
        }
    }

    public void release(double currentTime) {
        updateBusyTime(currentTime);
        availableUnits++;
    }

    public void addToQueue(Entity entity) {
        waitingQueue.add(entity);
    }

    public Entity removeFromQueue() {
        return waitingQueue.poll();
    }

    private void updateBusyTime(double currentTime) {
        double timeDelta = currentTime - lastUpdateTime;
        int busyUnits = type.getUnits() - availableUnits;
        totalBusyTime += busyUnits * timeDelta;
        lastUpdateTime = currentTime;
    }

    public ResourceType getType() {
        return type;
    }

    public int getAvailableUnits() {
        return availableUnits;
    }

    public int getQueueSize() {
        return waitingQueue.size();
    }

    public double getTotalBusyTime() {
        return totalBusyTime;
    }

    public double getUtilization(double totalTime) {
        return (totalBusyTime / (totalTime * type.getUnits())) * 100.0;
    }
}

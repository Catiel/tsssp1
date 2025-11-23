package com.simulacion.locations;

import com.simulacion.entities.Entity;
import java.util.LinkedList;
import java.util.Queue;

public class Location {
    private final LocationType type;
    private final Queue<Entity> queue;
    private final Queue<Entity> contentQueue;
    private int currentOccupancy;
    private double totalOccupancyTime;
    private double lastUpdateTime;

    public Location(LocationType type) {
        this.type = type;
        this.queue = new LinkedList<>();
        this.contentQueue = new LinkedList<>();
        this.currentOccupancy = 0;
        this.totalOccupancyTime = 0;
        this.lastUpdateTime = 0;
    }

    public boolean canAccept() {
        return currentOccupancy < type.getCapacity();
    }

    public void enter(Entity entity, double currentTime) {
        updateOccupancyTime(currentTime);
        if (canAccept()) {
            contentQueue.add(entity);
            currentOccupancy++;
            entity.setCurrentLocation(this);
        } else {
            queue.add(entity);
        }
    }

    public Entity exit(double currentTime) {
        updateOccupancyTime(currentTime);
        Entity entity = contentQueue.poll();
        if (entity != null) {
            currentOccupancy--;
            
            // Procesar siguiente en cola si hay espacio
            if (!queue.isEmpty() && canAccept()) {
                Entity nextEntity = queue.poll();
                contentQueue.add(nextEntity);
                currentOccupancy++;
                nextEntity.setCurrentLocation(this);
            }
        }
        return entity;
    }

    public void addToQueue(Entity entity) {
        queue.add(entity);
    }

    public Entity removeFromQueue() {
        return queue.poll();
    }

    private void updateOccupancyTime(double currentTime) {
        double timeDelta = currentTime - lastUpdateTime;
        totalOccupancyTime += currentOccupancy * timeDelta;
        lastUpdateTime = currentTime;
    }

    public LocationType getType() {
        return type;
    }

    public int getCurrentOccupancy() {
        return currentOccupancy;
    }

    public int getQueueSize() {
        return queue.size();
    }

    public double getTotalOccupancyTime() {
        return totalOccupancyTime;
    }

    public double getLastUpdateTime() {
        return lastUpdateTime;
    }
}

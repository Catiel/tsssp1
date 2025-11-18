package model;

import java.awt.Point;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Location {
    private final String name;
    private final int capacity;
    private final int units;
    private final Point position;

    private final Queue<Valve> queue;
    private final List<Valve> processing;

    // Statistics
    private int totalEntries;
    private int totalExits;
    private double totalBusyTime;
    private double totalBlockedTime;
    private double lastUpdateTime;
    private double totalObservedTime;
    private List<Integer> contentHistory;
    private List<Double> timeHistory;

    public Location(String name, int capacity, int units, Point position) {
        this.name = name;
        this.capacity = capacity;
        this.units = units;
        this.position = position;
        this.queue = new ConcurrentLinkedQueue<>();
        this.processing = Collections.synchronizedList(new ArrayList<>());
        this.totalEntries = 0;
        this.totalExits = 0;
        this.totalBusyTime = 0;
        this.totalBlockedTime = 0;
        this.lastUpdateTime = 0;
        this.totalObservedTime = 0;
        this.contentHistory = Collections.synchronizedList(new ArrayList<>());
        this.timeHistory = Collections.synchronizedList(new ArrayList<>());
    }

    public boolean canAccept() {
        return getCurrentContents() < capacity;
    }

    public boolean hasAvailableUnit() {
        return processing.size() < units;
    }

    public synchronized void addToQueue(Valve valve) {
        if (canAccept()) {
            queue.add(valve);
            valve.setCurrentLocation(this);
            totalEntries++;
        }
    }

    public synchronized void moveToProcessing(Valve valve) {
        if (queue.remove(valve) && hasAvailableUnit()) {
            processing.add(valve);
        }
    }

    public synchronized void removeValve(Valve valve) {
        boolean removed = queue.remove(valve) || processing.remove(valve);
        if (removed) {
            totalExits++;
        }
    }

    public Valve peekQueue() {
        return queue.peek();
    }

    public synchronized void updateStatistics(double currentTime, boolean countTowardsSchedule) {
        double deltaTime = currentTime - lastUpdateTime;
        if (deltaTime < 0) {
            deltaTime = 0;
        }

        if (!timeHistory.isEmpty()) {
            double lastTime = timeHistory.get(timeHistory.size() - 1);
            if (Math.abs(currentTime - lastTime) < 1e-9) {
                return;
            }
        }

        if (processing.size() > 0 && deltaTime > 0) {
            totalBusyTime += deltaTime;
        }

        if (!canAccept() && deltaTime > 0 && countTowardsSchedule) {
            totalBlockedTime += deltaTime;
        }

        if (deltaTime > 0 && countTowardsSchedule) {
            totalObservedTime += deltaTime;
        }

        contentHistory.add(getCurrentContents());
        timeHistory.add(currentTime);

        lastUpdateTime = currentTime;
    }

    public double getUtilization() {
        // Para almacenes (nombre empieza con "Almacen_"), usar contenido vs capacidad
        if (name.startsWith("Almacen_") && capacity > 0 && capacity < Integer.MAX_VALUE) {
            return (getAverageContents() / capacity) * 100.0;
        }
        
        // Para máquinas, usar tiempo ocupado vs tiempo observado
        double denominator = totalObservedTime * units;
        if (denominator <= 0) {
            return 0;
        }
        double util = (totalBusyTime / denominator) * 100.0;
        // Cap at 100% for individual units
        return Math.min(util, 100.0);
    }

    public synchronized double getTotalBusyTime() {
        return totalBusyTime;
    }

    public synchronized double getTotalObservedTime() {
        return totalObservedTime * units;
    }

    public synchronized double getAverageContents() {
        if (contentHistory.isEmpty()) return 0;
        List<Integer> snapshot = new ArrayList<>(contentHistory);
        return snapshot.stream()
            .mapToInt(Integer::intValue)
            .average()
            .orElse(0.0);
    }

    public synchronized double getMaxContents() {
        List<Integer> snapshot = new ArrayList<>(contentHistory);
        return snapshot.stream()
            .mapToInt(Integer::intValue)
            .max()
            .orElse(0);
    }

    public List<Integer> getContentHistory() {
        return new ArrayList<>(contentHistory);
    }

    public List<Double> getTimeHistory() {
        return new ArrayList<>(timeHistory);
    }

    // Getters
    public String getName() { return name; }
    public int getCapacity() { return capacity; }
    public int getUnits() { return units; }
    public Point getPosition() { return position; }
    public int getCurrentContents() { return queue.size() + processing.size(); }
    public int getQueueSize() { return queue.size(); }
    public int getProcessingSize() { return processing.size(); }
    public int getTotalEntries() { return totalEntries; }
    public int getTotalExits() { return totalExits; }
    public double getTotalBlockedTime() { return totalBlockedTime; }

    public List<Valve> getAllValves() {
        List<Valve> all = new ArrayList<>(queue);
        all.addAll(processing);
        return all;
    }

    public List<Valve> getProcessingValves() {
        return new ArrayList<>(processing);
    }
}

package model;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.*;

public class Crane {
    private final String name;
    private final int units;
    private final double emptySpeed;  // meters per minute
    private final double fullSpeed;   // meters per minute
    private final Point homePosition;

    private Point currentPosition;
    private Point targetPosition;
    private Valve carryingValve;
    private boolean isBusy;
    private boolean isMoving;

    // Animation
    private double animationProgress; // 0.0 to 1.0

    // Statistics
    private int totalTrips;
    private double totalTravelTime;
    private double totalUsageTime;
    private double lastUpdateTime;
    private List<Double> utilizationHistory;
    private List<Double> timeHistory;

    public Crane(String name, int units, double emptySpeed, double fullSpeed, Point home) {
        this.name = name;
        this.units = units;
        this.emptySpeed = emptySpeed;
        this.fullSpeed = fullSpeed;
        this.homePosition = new Point(home);
        this.currentPosition = new Point(home);
        this.targetPosition = null;
        this.carryingValve = null;
        this.isBusy = false;
        this.isMoving = false;
        this.animationProgress = 0;
        this.totalTrips = 0;
        this.totalTravelTime = 0;
        this.totalUsageTime = 0;
        this.lastUpdateTime = 0;
        this.utilizationHistory = Collections.synchronizedList(new ArrayList<>());
        this.timeHistory = Collections.synchronizedList(new ArrayList<>());
    }

    public double calculateTravelTime(Point from, Point to) {
        double distance = from.distance(to);
        double speed = (carryingValve != null) ? fullSpeed : emptySpeed;
        return (distance / speed) / 60.0; // Convert to hours
    }

    public void startMove(Point target) {
        this.targetPosition = new Point(target);
        this.isMoving = true;
        this.animationProgress = 0;
    }

    public void updateAnimation(double deltaProgress) {
        if (isMoving && targetPosition != null) {
            animationProgress += deltaProgress;
            if (animationProgress >= 1.0) {
                animationProgress = 1.0;
                currentPosition = new Point(targetPosition);
                isMoving = false;
            }
        }
    }

    public Point getInterpolatedPosition() {
        if (!isMoving || targetPosition == null) {
            return currentPosition;
        }

        int x = (int)(currentPosition.x + (targetPosition.x - currentPosition.x) * animationProgress);
        int y = (int)(currentPosition.y + (targetPosition.y - currentPosition.y) * animationProgress);
        return new Point(x, y);
    }

    public void pickupValve(Valve valve) {
        this.carryingValve = valve;
        if (valve != null) {
            valve.setState(Valve.State.IN_TRANSIT);
        }
    }

    public Valve releaseValve() {
        Valve valve = this.carryingValve;
        this.carryingValve = null;
        return valve;
    }

    public void completeTrip() {
        totalTrips++;
        isMoving = false;
        animationProgress = 0;
    }

    public void addTravelTime(double time) {
        totalTravelTime += time;
        totalUsageTime += time;
    }

    public void updateStatistics(double currentTime) {
        double utilization = getUtilization(currentTime);
        utilizationHistory.add(utilization);
        timeHistory.add(currentTime);
        lastUpdateTime = currentTime;
    }

    public double getUtilization(double totalTime) {
        if (totalTime == 0) return 0;
        return (totalUsageTime / totalTime) * 100.0;
    }

    // Getters
    public String getName() { return name; }
    public boolean isBusy() { return isBusy; }
    public void setBusy(boolean busy) { this.isBusy = busy; }
    public boolean isMoving() { return isMoving; }
    public Valve getCarryingValve() { return carryingValve; }
    public Point getCurrentPosition() { return currentPosition; }
    public Point getHomePosition() { return homePosition; }
    public int getTotalTrips() { return totalTrips; }
    public double getTotalTravelTime() { return totalTravelTime; }
    public double getEmptySpeed() { return emptySpeed; }
    public double getFullSpeed() { return fullSpeed; }
    public List<Double> getUtilizationHistory() { return new ArrayList<>(utilizationHistory); }
    public List<Double> getTimeHistory() { return new ArrayList<>(timeHistory); }
}

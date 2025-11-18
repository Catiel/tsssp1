package model;

import java.awt.Point;
import java.util.*;

public class Crane {
    private final String name;
    private final int units;
    private final double emptySpeed;  // meters per minute
    private final double fullSpeed;   // meters per minute
    private final Point homePosition;

    // Logical position (for simulation)
    private Point logicalPosition;

    // Visual path (for animation) - usa tiempo REAL para animación suave
    private List<Point> currentPathPoints;
    private List<Double> currentSegmentDistances;
    private double totalPathDistance;
    private double animationDurationSeconds;
    private double visualProgress; // 0.0 to 1.0

    private Valve carryingValve;
    private boolean isBusy;
    private boolean isMoving;

    // Statistics
    private int totalTrips;
    private double totalTravelTime;
    private double totalUsageTime;
    private List<Double> utilizationHistory;
    private List<Double> timeHistory;
    private double lastStatsUpdateTime;
    private double totalObservedTime;

    public Crane(String name, int units, double emptySpeed, double fullSpeed, Point home) {
        this.name = name;
        this.units = units;
        this.emptySpeed = emptySpeed;
        this.fullSpeed = fullSpeed;
        this.homePosition = new Point(home);
        this.logicalPosition = new Point(home);
        this.currentPathPoints = new ArrayList<>();
        this.currentSegmentDistances = new ArrayList<>();
        this.totalPathDistance = 0;
        this.animationDurationSeconds = 0.5;
        this.visualProgress = 1.0; // Start at destination
        this.carryingValve = null;
        this.isBusy = false;
        this.isMoving = false;
        this.totalTrips = 0;
        this.totalTravelTime = 0;
        this.totalUsageTime = 0;
        this.utilizationHistory = Collections.synchronizedList(new ArrayList<>());
        this.timeHistory = Collections.synchronizedList(new ArrayList<>());
        this.lastStatsUpdateTime = 0;
        this.totalObservedTime = 0;
    }

    public double calculateTravelTime(double distanceMeters, boolean loaded) {
        if (distanceMeters <= 0) {
            return 0;
        }
        double speed = loaded ? fullSpeed : emptySpeed;
        if (speed <= 0) {
            return 0;
        }
        return (distanceMeters / speed) / 60.0; // Convert to hours
    }

    public synchronized void startMove(List<Point> pathPoints,
                          List<Double> segmentDistances,
                          double totalDistanceMeters,
                          double travelTimeHours) {
        startMove(pathPoints, segmentDistances, totalDistanceMeters, travelTimeHours, 0, 50);
    }
    
    public synchronized void startMove(List<Point> pathPoints,
                          List<Double> segmentDistances,
                          double totalDistanceMeters,
                          double travelTimeHours,
                          double currentSimTime) {
        startMove(pathPoints, segmentDistances, totalDistanceMeters, travelTimeHours, currentSimTime, 50);
    }
    
    public synchronized void startMove(List<Point> pathPoints,
                          List<Double> segmentDistances,
                          double totalDistanceMeters,
                          double travelTimeHours,
                          double currentSimTime,
                          int animationSpeed) {
        if (pathPoints == null || pathPoints.size() < 2) {
            this.isMoving = false;
            this.visualProgress = 1.0;
            if (pathPoints != null && !pathPoints.isEmpty()) {
                this.logicalPosition = new Point(pathPoints.get(pathPoints.size() - 1));
            }
            return;
        }

        // NO actualizar logicalPosition aquí - esperar a completeTrip()
        this.currentPathPoints = new ArrayList<>(pathPoints);
        this.currentSegmentDistances = new ArrayList<>(segmentDistances);
        this.totalPathDistance = totalDistanceMeters <= 0 ? 1 : totalDistanceMeters;
        
        // ANIMACIÓN BASADA EN TIEMPO REAL - ajustada según la velocidad de simulación
        // Speed 1-20: Animación lenta (8-4 segundos) - para ver detalles
        // Speed 21-50: Animación normal (4-2 segundos) - balance
        // Speed 51-80: Animación rápida (2-0.5 segundos) - más rápido
        // Speed 81-100: Animación muy rápida (0.5-0.1 segundos) - máxima velocidad
        double baseDuration;
        if (animationSpeed <= 20) {
            // Muy lento: 8 a 4 segundos
            baseDuration = 8.0 - (animationSpeed / 20.0) * 4.0;
        } else if (animationSpeed <= 50) {
            // Normal: 4 a 2 segundos
            baseDuration = 4.0 - ((animationSpeed - 20.0) / 30.0) * 2.0;
        } else if (animationSpeed <= 80) {
            // Rápido: 2 a 0.5 segundos
            baseDuration = 2.0 - ((animationSpeed - 50.0) / 30.0) * 1.5;
        } else {
            // Muy rápido: 0.5 a 0.1 segundos
            baseDuration = 0.5 - ((animationSpeed - 80.0) / 20.0) * 0.4;
        }
        
        // Ajustar por distancia (movimientos más largos toman más tiempo)
        double distanceFactor = Math.sqrt(totalDistanceMeters / 100.0);
        double duration = Math.max(0.1, baseDuration * (0.7 + distanceFactor * 0.3));
        
        this.animationDurationSeconds = duration;
        this.visualProgress = 0.0;
        this.isMoving = true;
    }

    public synchronized void updateVisualPosition(double deltaSeconds) {
        if (!isMoving || currentPathPoints.size() < 2) {
            return;
        }

        if (animationDurationSeconds <= 0) {
            isMoving = false;
            visualProgress = 1.0;
            return;
        }

        // Actualizar progreso basado en tiempo real transcurrido
        // Esto hace que la animación sea suave y predecible
        double progressPerSecond = 1.0 / animationDurationSeconds;
        visualProgress += progressPerSecond * deltaSeconds;
        
        if (visualProgress >= 1.0) {
            visualProgress = 1.0;
            isMoving = false;
        }
    }
    


    public synchronized Point getInterpolatedPosition() {
        if (currentPathPoints.isEmpty()) {
            return new Point(logicalPosition);
        }
        
        // Si no está en movimiento o ya terminó, retornar la posición final
        if (!isMoving || visualProgress >= 1.0) {
            return new Point(currentPathPoints.get(currentPathPoints.size() - 1));
        }
        
        // Si apenas está comenzando, retornar la posición inicial
        if (visualProgress <= 0.0) {
            return new Point(currentPathPoints.get(0));
        }

        if (currentSegmentDistances.isEmpty() || totalPathDistance <= 0) {
            // Sin datos de segmentos, interpolar linealmente entre inicio y fin
            Point from = currentPathPoints.get(0);
            Point to = currentPathPoints.get(currentPathPoints.size() - 1);
            int x = (int)Math.round(from.x + (to.x - from.x) * visualProgress);
            int y = (int)Math.round(from.y + (to.y - from.y) * visualProgress);
            return new Point(x, y);
        }

        // Calcular la distancia objetivo basada en el progreso visual
        double targetDistance = totalPathDistance * visualProgress;
        double accumulatedDistance = 0;

        // Encontrar en qué segmento del path estamos
        for (int i = 0; i < currentSegmentDistances.size(); i++) {
            double segmentLength = currentSegmentDistances.get(i);
            double nextAccumulated = accumulatedDistance + segmentLength;
            
            if (targetDistance <= nextAccumulated || i == currentSegmentDistances.size() - 1) {
                // Estamos en este segmento
                double distanceInSegment = targetDistance - accumulatedDistance;
                double ratio = segmentLength == 0 ? 1.0 : Math.min(1.0, distanceInSegment / segmentLength);
                
                Point from = currentPathPoints.get(i);
                Point to = currentPathPoints.get(i + 1);
                int x = (int)Math.round(from.x + (to.x - from.x) * ratio);
                int y = (int)Math.round(from.y + (to.y - from.y) * ratio);
                return new Point(x, y);
            }
            
            accumulatedDistance = nextAccumulated;
        }

        // Fallback: retornar la última posición
        return new Point(currentPathPoints.get(currentPathPoints.size() - 1));
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

    public synchronized void completeTrip() {
        totalTrips++;
        // Forzar completado de animación
        visualProgress = 1.0;
        isMoving = false;
        
        // Actualizar posición lógica al destino final
        if (!currentPathPoints.isEmpty()) {
            logicalPosition = new Point(currentPathPoints.get(currentPathPoints.size() - 1));
        }
        
        // Limpiar ruta después de actualizar posición
        totalPathDistance = 0;
        currentPathPoints.clear();
        currentSegmentDistances.clear();
    }

    public void addTravelTime(double time) {
        totalTravelTime += time;
        totalUsageTime += time;
    }

    public synchronized void updateStatistics(double currentTime) {
        double delta = currentTime - lastStatsUpdateTime;
        if (delta < 0) {
            delta = 0;
        }

        if (delta > 0) {
            totalObservedTime += delta;
        }

        double utilization = getUtilization();
        utilizationHistory.add(utilization);
        timeHistory.add(currentTime);
        lastStatsUpdateTime = currentTime;
    }

    public double getUtilization() {
        double denominator = totalObservedTime;
        if (denominator <= 0) {
            return 0;
        }
        return (totalUsageTime / denominator) * 100.0;
    }

    public double getUtilization(double ignoredTotalTime) {
        return getUtilization();
    }

    // Getters
    public String getName() { return name; }
    public boolean isBusy() { return isBusy; }
    public void setBusy(boolean busy) { this.isBusy = busy; }
    public boolean isMoving() { return isMoving; }
    public double getVisualProgress() { return visualProgress; }
    public Valve getCarryingValve() { return carryingValve; }
    public Point getCurrentPosition() { return logicalPosition; }
    public Point getHomePosition() { return homePosition; }
    public int getTotalTrips() { return totalTrips; }
    public double getTotalTravelTime() { return totalTravelTime; }
    public double getTotalUsageTime() { return totalUsageTime; }
    public double getTotalObservedTime() { return totalObservedTime; }
    public double getEmptySpeed() { return emptySpeed; }
    public double getFullSpeed() { return fullSpeed; }
    public List<Double> getUtilizationHistory() { return new ArrayList<>(utilizationHistory); }
    public List<Double> getTimeHistory() { return new ArrayList<>(timeHistory); }
    public List<Point> getCurrentPathPoints() { 
        synchronized(this) {
            return new ArrayList<>(currentPathPoints); 
        }
    }
}

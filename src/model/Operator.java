package model; // Declaración del paquete model donde se encuentra la clase

import java.awt.Point; // Importa Point de AWT para representar coordenadas (x,y)
import java.util.*; // Importa clases de utilidades

public class Operator { // Clase que representa un operador en la cervecería
    private final String name; // Nombre del operador
    private final int units; // Número de unidades (siempre 1)
    private final double speed; // Velocidad en pasos por minuto
    private final Point homePosition; // Posición inicial/home
    private final String network; // Red de rutas asignada

    private Point logicalPosition; // Posición lógica actual
    private List<Point> currentPathPoints; // Puntos del camino actual
    private List<Double> currentSegmentDistances; // Distancias de cada segmento
    private double totalPathDistance; // Distancia total del camino
    private double animationDurationSeconds; // Duración de la animación
    private double visualProgress; // Progreso visual 0.0-1.0

    private Valve carryingEntity; // Entidad que está transportando
    private boolean isBusy; // Si está ocupado
    private boolean isMoving; // Si está en movimiento

    private int totalTrips; // Total de viajes completados
    private double totalTravelTime; // Tiempo total de viaje en minutos
    private double totalUsageTime; // Tiempo total de uso en minutos
    private List<Double> utilizationHistory; // Historial de utilización
    private List<Double> timeHistory; // Historial de tiempos
    private double lastStatsUpdateTime; // Última actualización de estadísticas
    private double totalObservedTime; // Tiempo total observado

    public Operator(String name, int units, double speed, String network, Point home) {
        this.name = name;
        this.units = units;
        this.speed = speed;
        this.network = network;
        this.homePosition = new Point(home);
        this.logicalPosition = new Point(home);
        this.currentPathPoints = new ArrayList<>();
        this.currentSegmentDistances = new ArrayList<>();
        this.totalPathDistance = 0;
        this.animationDurationSeconds = 0.5;
        this.visualProgress = 1.0;
        this.carryingEntity = null;
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
        // En el sistema de cervecería, la velocidad es constante independiente de la carga
        if (distanceMeters <= 0) return 0;
        if (speed <= 0) return 0;
        // Tiempo en minutos = distancia / velocidad
        return distanceMeters / speed;
    }

    public synchronized void startMove(List<Point> pathPoints,
                          List<Double> segmentDistances,
                          double totalDistanceMeters,
                          double travelTimeMinutes,
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

        this.currentPathPoints = new ArrayList<>(pathPoints);
        this.currentSegmentDistances = new ArrayList<>(segmentDistances);
        this.totalPathDistance = totalDistanceMeters <= 0 ? 1 : totalDistanceMeters;

        double baseDuration;
        if (animationSpeed <= 20) {
            baseDuration = 8.0 - (animationSpeed / 20.0) * 4.0;
        } else if (animationSpeed <= 50) {
            baseDuration = 4.0 - ((animationSpeed - 20.0) / 30.0) * 2.0;
        } else if (animationSpeed <= 80) {
            baseDuration = 2.0 - ((animationSpeed - 50.0) / 30.0) * 1.5;
        } else {
            baseDuration = 0.5 - ((animationSpeed - 80.0) / 20.0) * 0.4;
        }

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

        if (!isMoving || visualProgress >= 1.0) {
            return new Point(currentPathPoints.get(currentPathPoints.size() - 1));
        }

        if (visualProgress <= 0.0) {
            return new Point(currentPathPoints.get(0));
        }

        if (currentSegmentDistances.isEmpty() || totalPathDistance <= 0) {
            Point from = currentPathPoints.get(0);
            Point to = currentPathPoints.get(currentPathPoints.size() - 1);
            int x = (int)Math.round(from.x + (to.x - from.x) * visualProgress);
            int y = (int)Math.round(from.y + (to.y - from.y) * visualProgress);
            return new Point(x, y);
        }

        double targetDistance = totalPathDistance * visualProgress;
        double accumulatedDistance = 0;

        for (int i = 0; i < currentSegmentDistances.size(); i++) {
            double segmentLength = currentSegmentDistances.get(i);
            double nextAccumulated = accumulatedDistance + segmentLength;

            if (targetDistance <= nextAccumulated || i == currentSegmentDistances.size() - 1) {
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

        return new Point(currentPathPoints.get(currentPathPoints.size() - 1));
    }

    public void pickupEntity(Valve entity) {
        this.carryingEntity = entity;
        if (entity != null) {
            entity.setState(Valve.State.IN_TRANSIT);
        }
    }

    public Valve releaseEntity() {
        Valve entity = this.carryingEntity;
        this.carryingEntity = null;
        return entity;
    }

    public synchronized void completeTrip() {
        totalTrips++;
        visualProgress = 1.0;
        isMoving = false;

        if (!currentPathPoints.isEmpty()) {
            logicalPosition = new Point(currentPathPoints.get(currentPathPoints.size() - 1));
        }

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
        if (delta < 0) delta = 0;

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
        if (denominator <= 0) return 0;
        return (totalUsageTime / denominator) * 100.0;
    }

    // Getters
    public String getName() { return name; }
    public boolean isBusy() { return isBusy; }
    public void setBusy(boolean busy) { this.isBusy = busy; }
    public boolean isMoving() { return isMoving; }
    public double getVisualProgress() { return visualProgress; }
    public Valve getCarryingEntity() { return carryingEntity; }
    public Point getCurrentPosition() { return logicalPosition; }
    public Point getHomePosition() { return homePosition; }
    public int getTotalTrips() { return totalTrips; }
    public double getTotalTravelTime() { return totalTravelTime; }
    public double getTotalUsageTime() { return totalUsageTime; }
    public double getTotalObservedTime() { return totalObservedTime; }
    public double getSpeed() { return speed; }
    public String getNetwork() { return network; }
    public List<Double> getUtilizationHistory() { return new ArrayList<>(utilizationHistory); }
    public List<Double> getTimeHistory() { return new ArrayList<>(timeHistory); }
    public int getUnits() { return units; }
    public List<Point> getCurrentPathPoints() {
        synchronized(this) {
            return new ArrayList<>(currentPathPoints);
        }
    }
}

package com.simulacion.routing;

public class PathSegment {
    private final PathNode fromNode;
    private final PathNode toNode;
    private final double distance;
    private final boolean bidirectional;
    private final double speedFactor;

    public PathSegment(PathNode fromNode, PathNode toNode, double distance,
                      boolean bidirectional, double speedFactor) {
        this.fromNode = fromNode;
        this.toNode = toNode;
        this.distance = distance;
        this.bidirectional = bidirectional;
        this.speedFactor = speedFactor;
    }

    public PathSegment(PathNode fromNode, PathNode toNode, boolean bidirectional) {
        this.fromNode = fromNode;
        this.toNode = toNode;
        this.distance = fromNode.distanceTo(toNode);
        this.bidirectional = bidirectional;
        this.speedFactor = 1.0;
    }

    public double calculateTravelTime(double baseSpeed) {
        if (baseSpeed <= 0) {
            throw new IllegalArgumentException("La velocidad debe ser positiva");
        }
        double effectiveSpeed = baseSpeed * speedFactor;
        return distance / effectiveSpeed;
    }

    public PathNode getFromNode() {
        return fromNode;
    }

    public PathNode getToNode() {
        return toNode;
    }

    public double getDistance() {
        return distance;
    }

    public boolean isBidirectional() {
        return bidirectional;
    }

    public double getSpeedFactor() {
        return speedFactor;
    }

    public boolean connectsNodes(PathNode node1, PathNode node2) {
        if (fromNode.equals(node1) && toNode.equals(node2)) {
            return true;
        }
        return bidirectional && fromNode.equals(node2) && toNode.equals(node1);
    }

    @Override
    public String toString() {
        return "PathSegment{" +
                "from=" + fromNode.getNodeId() +
                ", to=" + toNode.getNodeId() +
                ", distance=" + distance +
                ", bidirectional=" + bidirectional +
                ", speedFactor=" + speedFactor +
                '}';
    }
}

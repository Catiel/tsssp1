package com.simulacion.routing;

import com.simulacion.locations.Location;

public class PathNode {
    private final String nodeId;
    private final Location associatedLocation;
    private double xCoordinate;
    private double yCoordinate;

    public PathNode(String nodeId, Location associatedLocation) {
        this.nodeId = nodeId;
        this.associatedLocation = associatedLocation;
        this.xCoordinate = 0.0;
        this.yCoordinate = 0.0;
    }

    public PathNode(String nodeId, Location associatedLocation, double x, double y) {
        this.nodeId = nodeId;
        this.associatedLocation = associatedLocation;
        this.xCoordinate = x;
        this.yCoordinate = y;
    }

    public String getNodeId() {
        return nodeId;
    }

    public Location getAssociatedLocation() {
        return associatedLocation;
    }

    public double getXCoordinate() {
        return xCoordinate;
    }

    public void setXCoordinate(double xCoordinate) {
        this.xCoordinate = xCoordinate;
    }

    public double getYCoordinate() {
        return yCoordinate;
    }

    public void setYCoordinate(double yCoordinate) {
        this.yCoordinate = yCoordinate;
    }

    public double distanceTo(PathNode other) {
        double dx = this.xCoordinate - other.xCoordinate;
        double dy = this.yCoordinate - other.yCoordinate;
        return Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public String toString() {
        return "PathNode{" +
                "nodeId='" + nodeId + '\'' +
                ", location=" + (associatedLocation != null ? associatedLocation.getType().getName() : "null") +
                ", coordinates=(" + xCoordinate + ", " + yCoordinate + ")" +
                '}';
    }
}

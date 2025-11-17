package model;

import java.awt.Point;
import java.util.*;

public class PathNetwork {
    private Map<String, Point> nodes;
    private Map<String, Map<String, Double>> distances;

    public PathNetwork() {
        nodes = new HashMap<>();
        distances = new HashMap<>();
        initializeNetwork();
    }

    private void initializeNetwork() {
        // Define node positions (meters)
        nodes.put("N1", new Point(50, 100));   // DOCK
        nodes.put("N2", new Point(150, 100));  // STOCK
        nodes.put("N3", new Point(300, 100));  // Almacen_M1 / M1
        nodes.put("N4", new Point(500, 100));  // Almacen_M2 / M2
        nodes.put("N5", new Point(650, 50));   // Almacen_M3 / M3

        // Define distances based on ProModel configuration
        addPath("N1", "N2", 10.0);
        addPath("N1", "N3", 14.14);
        addPath("N1", "N4", 22.36);
        addPath("N1", "N5", 20.0);
        addPath("N2", "N3", 10.0);
        addPath("N2", "N4", 20.0);
        addPath("N2", "N5", 22.36);
        addPath("N3", "N4", 10.0);
        addPath("N3", "N5", 14.14);
        addPath("N4", "N5", 10.0);
    }

    private void addPath(String from, String to, double distance) {
        distances.putIfAbsent(from, new HashMap<>());
        distances.putIfAbsent(to, new HashMap<>());
        distances.get(from).put(to, distance);
        distances.get(to).put(from, distance); // Bidirectional
    }

    public Point getNodePosition(String node) {
        return nodes.get(node);
    }

    public double getDistance(String from, String to) {
        if (distances.containsKey(from) && distances.get(from).containsKey(to)) {
            return distances.get(from).get(to);
        }
        // Direct distance if not defined
        Point p1 = nodes.get(from);
        Point p2 = nodes.get(to);
        if (p1 != null && p2 != null) {
            return p1.distance(p2);
        }
        return 0;
    }

    public String getNodeForLocation(String locationName) {
        switch (locationName) {
            case "DOCK": return "N1";
            case "STOCK": return "N2";
            case "Almacen_M1":
            case "M1": return "N3";
            case "Almacen_M2":
            case "M2": return "N4";
            case "Almacen_M3":
            case "M3": return "N5";
            default: return null;
        }
    }
}

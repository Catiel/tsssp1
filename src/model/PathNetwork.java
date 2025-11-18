package model;

import java.awt.Point;
import java.util.*;

public class PathNetwork {
    private Map<String, Point> nodes;
    private Map<String, Map<String, Double>> distances;
    private Map<String, String> locationToNode;
    private List<PathEdge> edges;
    private Set<String> edgeKeys;

    public PathNetwork() {
        nodes = new HashMap<>();
        distances = new HashMap<>();
        locationToNode = new HashMap<>();
        edges = new ArrayList<>();
        edgeKeys = new HashSet<>();
        initializeNetwork();
    }

    private void initializeNetwork() {
        // Node positions follow the plant layout from ProModel screenshot
        nodes.put("N1", new Point(200, 140));  // DOCK / Crane Home
        nodes.put("N2", new Point(210, 420));  // STOCK
        nodes.put("N3", new Point(560, 420));  // M1 / Almacen_M1
        nodes.put("N4", new Point(760, 300));  // M2 / Almacen_M2
        nodes.put("N5", new Point(940, 180));  // M3 / Almacen_M3

        // Location to node mapping (per ProModel interfaces)
        locationToNode.put("DOCK", "N1");
        locationToNode.put("STOCK", "N2");
        locationToNode.put("Almacen_M1", "N3");
        locationToNode.put("M1", "N3");
        locationToNode.put("Almacen_M2", "N4");
        locationToNode.put("M2", "N4");
        locationToNode.put("Almacen_M3", "N5");
        locationToNode.put("M3", "N5");

        // Define distances (meters) based on ProModel configuration
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

        String key = from.compareTo(to) < 0 ? from + ":" + to : to + ":" + from;
        if (edgeKeys.add(key)) {
            edges.add(new PathEdge(from, to, distance));
        }
    }

    public Point getNodePosition(String node) {
        return nodes.get(node);
    }

    public Map<String, Point> getNodePositions() {
        return Collections.unmodifiableMap(nodes);
    }

    public List<PathEdge> getEdges() {
        return Collections.unmodifiableList(edges);
    }

    public String getNodeForLocation(String locationName) {
        return locationToNode.get(locationName);
    }

    public PathResult getPathForLocations(String fromLocation, String toLocation) {
        String startNode = getNodeForLocation(fromLocation);
        String endNode = getNodeForLocation(toLocation);
        if (startNode == null || endNode == null) {
            return PathResult.empty();
        }
        return getPathBetweenNodes(startNode, endNode);
    }

    private PathResult getPathBetweenNodes(String startNode, String endNode) {
        if (startNode.equals(endNode)) {
            Point pos = getNodePosition(startNode);
            List<Point> points = List.of(new Point(pos));
            return new PathResult(List.of(startNode), points,
                Collections.emptyList(), 0.0);
        }

        Map<String, Double> distanceMap = new HashMap<>();
        Map<String, String> previous = new HashMap<>();
        PriorityQueue<NodeDistance> queue = new PriorityQueue<>(Comparator.comparingDouble(nd -> nd.distance));

        for (String node : nodes.keySet()) {
            distanceMap.put(node, Double.POSITIVE_INFINITY);
        }
        distanceMap.put(startNode, 0.0);
        queue.add(new NodeDistance(startNode, 0.0));

        while (!queue.isEmpty()) {
            NodeDistance current = queue.poll();
            if (current.node.equals(endNode)) {
                break;
            }

            if (current.distance > distanceMap.get(current.node)) {
                continue;
            }

            Map<String, Double> neighbors = distances.getOrDefault(current.node, Collections.emptyMap());
            for (Map.Entry<String, Double> entry : neighbors.entrySet()) {
                double newDist = current.distance + entry.getValue();
                if (newDist < distanceMap.get(entry.getKey())) {
                    distanceMap.put(entry.getKey(), newDist);
                    previous.put(entry.getKey(), current.node);
                    queue.add(new NodeDistance(entry.getKey(), newDist));
                }
            }
        }

        if (!previous.containsKey(endNode) && !startNode.equals(endNode)) {
            return PathResult.empty();
        }

        List<String> nodePath = new ArrayList<>();
        String current = endNode;
        nodePath.add(current);
        while (previous.containsKey(current)) {
            current = previous.get(current);
            nodePath.add(current);
            if (current.equals(startNode)) {
                break;
            }
        }
        if (!nodePath.get(nodePath.size() - 1).equals(startNode)) {
            nodePath.add(startNode);
        }
        Collections.reverse(nodePath);

        List<Point> points = new ArrayList<>();
        List<Double> segmentDistances = new ArrayList<>();
        double totalDistance = 0.0;
        for (int i = 0; i < nodePath.size(); i++) {
            points.add(new Point(nodes.get(nodePath.get(i))));
            if (i < nodePath.size() - 1) {
                double segment = distances.get(nodePath.get(i)).get(nodePath.get(i + 1));
                segmentDistances.add(segment);
                totalDistance += segment;
            }
        }

        return new PathResult(nodePath, points, segmentDistances, totalDistance);
    }

    private static class NodeDistance {
        final String node;
        final double distance;

        NodeDistance(String node, double distance) {
            this.node = node;
            this.distance = distance;
        }
    }

    public static class PathEdge {
        private final String from;
        private final String to;
        private final double distance;

        public PathEdge(String from, String to, double distance) {
            this.from = from;
            this.to = to;
            this.distance = distance;
        }

        public String getFrom() { return from; }
        public String getTo() { return to; }
        public double getDistance() { return distance; }
    }

    public static class PathResult {
        private final List<String> nodes;
        private final List<Point> points;
        private final List<Double> segmentDistances;
        private final double totalDistance;

        public PathResult(List<String> nodes, List<Point> points,
                          List<Double> segmentDistances, double totalDistance) {
            this.nodes = nodes;
            this.points = points;
            this.segmentDistances = segmentDistances;
            this.totalDistance = totalDistance;
        }

        public static PathResult empty() {
            return new PathResult(Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), 0.0);
        }

        public boolean isValid() { return nodes != null && !nodes.isEmpty(); }
        public List<Point> getPoints() { return points; }
        public List<Double> getSegmentDistances() { return segmentDistances; }
        public double getTotalDistance() { return totalDistance; }
        public List<String> getNodes() { return nodes; }
    }
}

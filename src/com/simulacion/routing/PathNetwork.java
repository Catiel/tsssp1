package com.simulacion.routing;

import java.util.*;

public class PathNetwork {
    private final String networkName;
    private final String networkType; // "Sobrepasar", "No Sobrepasar", etc.
    private final Map<String, PathNode> nodes;
    private final List<PathSegment> segments;
    private final PathNode homeNode;

    public PathNetwork(String networkName, String networkType, PathNode homeNode) {
        this.networkName = networkName;
        this.networkType = networkType;
        this.nodes = new HashMap<>();
        this.segments = new ArrayList<>();
        this.homeNode = homeNode;
    }

    public void addNode(PathNode node) {
        nodes.put(node.getNodeId(), node);
    }

    public void addSegment(PathSegment segment) {
        segments.add(segment);

        // Asegurar que los nodos estén en la red
        if (!nodes.containsKey(segment.getFromNode().getNodeId())) {
            addNode(segment.getFromNode());
        }
        if (!nodes.containsKey(segment.getToNode().getNodeId())) {
            addNode(segment.getToNode());
        }
    }

    public PathNode getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    public List<PathSegment> getSegmentsFromNode(PathNode node) {
        List<PathSegment> result = new ArrayList<>();
        for (PathSegment segment : segments) {
            if (segment.getFromNode().equals(node)) {
                result.add(segment);
            } else if (segment.isBidirectional() && segment.getToNode().equals(node)) {
                result.add(segment);
            }
        }
        return result;
    }

    public PathSegment findSegment(PathNode from, PathNode to) {
        for (PathSegment segment : segments) {
            if (segment.connectsNodes(from, to)) {
                return segment;
            }
        }
        return null;
    }

    public List<PathNode> findShortestPath(PathNode start, PathNode end) {
        if (start == null || end == null) {
            return new ArrayList<>();
        }

        Map<PathNode, Double> distances = new HashMap<>();
        Map<PathNode, PathNode> previous = new HashMap<>();
        PriorityQueue<NodeDistancePair> queue = new PriorityQueue<>();

        // Inicializar distancias
        for (PathNode node : nodes.values()) {
            distances.put(node, Double.MAX_VALUE);
        }
        distances.put(start, 0.0);
        queue.add(new NodeDistancePair(start, 0.0));

        while (!queue.isEmpty()) {
            NodeDistancePair current = queue.poll();
            PathNode currentNode = current.node;

            if (currentNode.equals(end)) {
                break;
            }

            for (PathSegment segment : getSegmentsFromNode(currentNode)) {
                PathNode neighbor = segment.getFromNode().equals(currentNode)
                    ? segment.getToNode()
                    : segment.getFromNode();

                double newDistance = distances.get(currentNode) + segment.getDistance();

                if (newDistance < distances.get(neighbor)) {
                    distances.put(neighbor, newDistance);
                    previous.put(neighbor, currentNode);
                    queue.add(new NodeDistancePair(neighbor, newDistance));
                }
            }
        }

        // Reconstruir camino
        List<PathNode> path = new ArrayList<>();
        PathNode current = end;

        while (current != null) {
            path.add(0, current);
            current = previous.get(current);
        }

        return path.isEmpty() || !path.get(0).equals(start) ? new ArrayList<>() : path;
    }

    public double calculatePathDistance(List<PathNode> path) {
        double totalDistance = 0.0;

        for (int i = 0; i < path.size() - 1; i++) {
            PathSegment segment = findSegment(path.get(i), path.get(i + 1));
            if (segment != null) {
                totalDistance += segment.getDistance();
            }
        }

        return totalDistance;
    }

    public String getNetworkName() {
        return networkName;
    }

    public String getNetworkType() {
        return networkType;
    }

    public PathNode getHomeNode() {
        return homeNode;
    }

    public Map<String, PathNode> getNodes() {
        return nodes;
    }

    public List<PathSegment> getSegments() {
        return segments;
    }

    // Clase auxiliar para el algoritmo de Dijkstra
    private static class NodeDistancePair implements Comparable<NodeDistancePair> {
        PathNode node;
        double distance;

        NodeDistancePair(PathNode node, double distance) {
            this.node = node;
            this.distance = distance;
        }

        @Override
        public int compareTo(NodeDistancePair other) {
            return Double.compare(this.distance, other.distance);
        }
    }

    @Override
    public String toString() {
        return "PathNetwork{" +
                "name='" + networkName + '\'' +
                ", type='" + networkType + '\'' +
                ", nodes=" + nodes.size() +
                ", segments=" + segments.size() +
                ", home=" + (homeNode != null ? homeNode.getNodeId() : "null") +
                '}';
    }
}

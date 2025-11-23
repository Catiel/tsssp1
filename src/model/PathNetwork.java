package model; // Declaración del paquete model donde se encuentra la clase

import java.awt.Point; // Importa Point de AWT para representar coordenadas (x,y) de posiciones
import java.util.*; // Importa todas las clases de utilidades de Java (Map, List, Set, HashMap, ArrayList, etc.)

public class PathNetwork { // Declaración de la clase pública PathNetwork que representa una red de caminos entre nodos
    private Map<String, Point> nodes; // Mapa que asocia nombres de nodos con sus posiciones (x,y) en el layout
    private Map<String, Map<String, Double>> distances; // Mapa anidado que almacena distancias entre nodos: nodo1 -> (nodo2 -> distancia)
    private Map<String, String> locationToNode; // Mapa que asocia nombres de locaciones con sus nodos correspondientes
    private List<PathEdge> edges; // Lista que almacena todos los bordes/aristas de la red para visualización
    private Set<String> edgeKeys; // Conjunto que almacena claves únicas de aristas para evitar duplicados

    public PathNetwork() { // Constructor que inicializa la red de caminos
        nodes = new HashMap<>(); // Inicializa el mapa de nodos vacío
        distances = new HashMap<>(); // Inicializa el mapa de distancias vacío
        locationToNode = new HashMap<>(); // Inicializa el mapa de locaciones a nodos vacío
        edges = new ArrayList<>(); // Inicializa la lista de aristas vacía
        edgeKeys = new HashSet<>(); // Inicializa el conjunto de claves de aristas vacío
        initializeNetwork(); // Llama al método que configura la red con nodos y conexiones predefinidas
    }

    private void initializeNetwork() { // Método privado que inicializa la red de cervecería con 4 redes independientes según ProModel
        // RED_RECEPCION: Flujo de granos (MALTEADO → SECADO → MOLIENDA)
        // ProModel: N1-N2: 24.67m, N2-N3: 26.43m
        nodes.put("RR1", new Point(100, 100)); // MALTEADO - N1
        nodes.put("RR2", new Point(250, 100)); // SECADO - N2
        nodes.put("RR3", new Point(400, 100)); // MOLIENDA - N3
        
        locationToNode.put("MALTEADO", "RR1");
        locationToNode.put("SECADO", "RR2");
        locationToNode.put("MOLIENDA", "RR3");
        locationToNode.put("MACERADO", "RR3"); // MACERADO conectado a MOLIENDA
        
        addPath("RR1", "RR2", 24.67); // MALTEADO → SECADO (24.67 metros)
        addPath("RR2", "RR3", 26.43); // SECADO → MOLIENDA (26.43 metros)
        
        // RED_LUPULO: Flujo de lúpulo (SILO_LUPULO → COCCION)
        // ProModel: N1-N2: 51.36m
        nodes.put("RL1", new Point(600, 200)); // SILO_LUPULO - N1
        nodes.put("RL2", new Point(750, 200)); // COCCION - N2
        
        locationToNode.put("SILO_LUPULO", "RL1");
        locationToNode.put("COCCION", "RL2");
        locationToNode.put("FILTRADO", "RL2"); // FILTRADO conectado a COCCION
        
        addPath("RL1", "RL2", 51.36); // SILO_LUPULO → COCCION (51.36 metros)
        
        // RED_LEVADURA: Flujo de levadura (SILO_LEVADURA → FERMENTACION)
        // ProModel: N1-N2: 25.49m
        nodes.put("RV1", new Point(950, 300)); // SILO_LEVADURA - N1
        nodes.put("RV2", new Point(1100, 300)); // FERMENTACION - N2
        
        locationToNode.put("SILO_LEVADURA", "RV1");
        locationToNode.put("FERMENTACION", "RV2");
        
        addPath("RV1", "RV2", 25.49); // SILO_LEVADURA → FERMENTACION (25.49 metros)
        
        // RED_EMPACADO: Flujo de empacado (EMPACADO → ALMACENAJE → MERCADO)
        // ProModel: N1-N2: 27.43m, N2-N3: 38.00m
        nodes.put("RE1", new Point(1600, 650)); // EMPACADO - N1
        nodes.put("RE2", new Point(1450, 800)); // ALMACENAJE - N2
        nodes.put("RE3", new Point(1600, 950)); // MERCADO - N3
        
        locationToNode.put("EMPACADO", "RE1");
        locationToNode.put("ALMACENAJE", "RE2");
        locationToNode.put("MERCADO", "RE3");
        
        addPath("RE1", "RE2", 27.43); // EMPACADO → ALMACENAJE (27.43 metros)
        addPath("RE2", "RE3", 38.00); // ALMACENAJE → MERCADO (38.00 metros)
    }

    private void addPath(String from, String to, double distance) { // Método privado que agrega un camino bidireccional entre dos nodos
        distances.putIfAbsent(from, new HashMap<>()); // Crea mapa de distancias para nodo 'from' si no existe
        distances.putIfAbsent(to, new HashMap<>()); // Crea mapa de distancias para nodo 'to' si no existe
        distances.get(from).put(to, distance); // Almacena distancia desde 'from' hacia 'to'
        distances.get(to).put(from, distance); // Almacena distancia desde 'to' hacia 'from' (bidireccional)

        String key = from.compareTo(to) < 0 ? from + ":" + to : to + ":" + from; // Crea clave única ordenada alfabéticamente para evitar duplicados (N1:N2 mismo que N2:N1)
        if (edgeKeys.add(key)) { // Intenta agregar la clave al conjunto, retorna true si es nueva (no duplicada)
            edges.add(new PathEdge(from, to, distance)); // Agrega la arista a la lista si es única
        }
    }

    public Point getNodePosition(String node) { // Método público que retorna la posición de un nodo dado su nombre
        return nodes.get(node); // Retorna el Point asociado al nodo o null si no existe
    }

    public Map<String, Point> getNodePositions() { // Método público que retorna todos los nodos y sus posiciones
        return Collections.unmodifiableMap(nodes); // Retorna una vista inmutable del mapa de nodos para evitar modificaciones externas
    }

    public List<PathEdge> getEdges() { // Método público que retorna todas las aristas de la red
        return Collections.unmodifiableList(edges); // Retorna una vista inmutable de la lista de aristas
    }

    public String getNodeForLocation(String locationName) { // Método público que retorna el nodo asociado a una locación
        return locationToNode.get(locationName); // Retorna el nombre del nodo o null si la locación no está mapeada
    }

    public void registerLocationNode(String locationName, String nodeKey) { // Método público que registra dinámicamente una locación con un nodo
        if (locationName == null || nodeKey == null) { // Verifica si algún parámetro es null
            return; // Sale del método sin hacer nada si hay parámetros null
        }
        if (nodes.containsKey(nodeKey)) { // Verifica si el nodo existe en la red
            locationToNode.put(locationName, nodeKey); // Mapea la locación al nodo solo si el nodo existe
        }
    }

    public PathResult getPathForLocations(String fromLocation, String toLocation) { // Método público que calcula el camino entre dos locaciones
        String startNode = getNodeForLocation(fromLocation); // Obtiene el nodo correspondiente a la locación origen
        String endNode = getNodeForLocation(toLocation); // Obtiene el nodo correspondiente a la locación destino
        if (startNode == null || endNode == null) { // Verifica si alguna de las locaciones no tiene nodo asociado
            return PathResult.empty(); // Retorna resultado vacío si falta algún nodo
        }
        return getPathBetweenNodes(startNode, endNode); // Calcula y retorna el camino más corto entre los nodos usando Dijkstra
    }

    private PathResult getPathBetweenNodes(String startNode, String endNode) { // Método privado que implementa el algoritmo de Dijkstra para encontrar el camino más corto
        if (startNode.equals(endNode)) { // Verifica si origen y destino son el mismo nodo
            Point pos = getNodePosition(startNode); // Obtiene la posición del nodo
            List<Point> points = List.of(new Point(pos)); // Crea lista inmutable con una copia del punto
            return new PathResult(List.of(startNode), points, Collections.emptyList(), 0.0); // Retorna resultado con distancia 0 y sin segmentos
        }

        Map<String, Double> distanceMap = new HashMap<>(); // Mapa que almacena la distancia mínima conocida desde el inicio a cada nodo
        Map<String, String> previous = new HashMap<>(); // Mapa que almacena el nodo previo en el camino más corto
        PriorityQueue<NodeDistance> queue = new PriorityQueue<>(Comparator.comparingDouble(nd -> nd.distance)); // Cola de prioridad ordenada por distancia (menor primero)

        for (String node : nodes.keySet()) { // Itera sobre todos los nodos de la red
            distanceMap.put(node, Double.POSITIVE_INFINITY); // Inicializa la distancia de cada nodo como infinito
        }
        distanceMap.put(startNode, 0.0); // Establece la distancia del nodo inicial como 0
        queue.add(new NodeDistance(startNode, 0.0)); // Agrega el nodo inicial a la cola con distancia 0

        while (!queue.isEmpty()) { // Bucle principal de Dijkstra - procesa nodos hasta vaciar la cola
            NodeDistance current = queue.poll(); // Extrae el nodo con menor distancia de la cola
            if (current.node.equals(endNode)) { // Verifica si alcanzamos el nodo destino
                break; // Sale del bucle si encontramos el destino (camino más corto encontrado)
            }

            if (current.distance > distanceMap.get(current.node)) { // Verifica si esta distancia es mayor que la ya conocida (nodo ya procesado)
                continue; // Salta este nodo porque ya encontramos un camino mejor
            }

            Map<String, Double> neighbors = distances.getOrDefault(current.node, Collections.emptyMap()); // Obtiene los vecinos del nodo actual y sus distancias
            for (Map.Entry<String, Double> entry : neighbors.entrySet()) { // Itera sobre cada vecino
                double newDist = current.distance + entry.getValue(); // Calcula nueva distancia: distancia actual + distancia al vecino
                if (newDist < distanceMap.get(entry.getKey())) { // Verifica si encontramos un camino más corto al vecino
                    distanceMap.put(entry.getKey(), newDist); // Actualiza la distancia mínima al vecino
                    previous.put(entry.getKey(), current.node); // Actualiza el nodo previo en el camino más corto
                    queue.add(new NodeDistance(entry.getKey(), newDist)); // Agrega el vecino a la cola con su nueva distancia
                }
            }
        }

        if (!previous.containsKey(endNode) && !startNode.equals(endNode)) { // Verifica si no se encontró camino al destino
            return PathResult.empty(); // Retorna resultado vacío si no hay camino posible
        }

        List<String> nodePath = new ArrayList<>(); // Lista para reconstruir el camino de nodos
        String current = endNode; // Comienza desde el nodo destino
        nodePath.add(current); // Agrega el nodo destino al camino
        while (previous.containsKey(current)) { // Bucle que retrocede desde destino hasta origen
            current = previous.get(current); // Obtiene el nodo previo en el camino
            nodePath.add(current); // Agrega el nodo previo al camino
            if (current.equals(startNode)) { // Verifica si alcanzamos el nodo inicial
                break; // Sale del bucle si llegamos al origen
            }
        }
        if (!nodePath.get(nodePath.size() - 1).equals(startNode)) { // Verifica si el último nodo no es el inicial (caso edge)
            nodePath.add(startNode); // Agrega el nodo inicial si falta
        }
        Collections.reverse(nodePath); // Invierte la lista para que vaya de origen a destino

        List<Point> points = new ArrayList<>(); // Lista para almacenar las posiciones de los puntos del camino
        List<Double> segmentDistances = new ArrayList<>(); // Lista para almacenar las distancias de cada segmento
        double totalDistance = 0.0; // Variable para acumular la distancia total del camino
        for (int i = 0; i < nodePath.size(); i++) { // Itera sobre cada nodo del camino
            points.add(new Point(nodes.get(nodePath.get(i)))); // Agrega copia de la posición del nodo a la lista de puntos
            if (i < nodePath.size() - 1) { // Verifica si no es el último nodo (hay segmento siguiente)
                double segment = distances.get(nodePath.get(i)).get(nodePath.get(i + 1)); // Obtiene la distancia del segmento entre nodo actual y siguiente
                segmentDistances.add(segment); // Agrega la distancia del segmento a la lista
                totalDistance += segment; // Acumula la distancia del segmento al total
            }
        }

        return new PathResult(nodePath, points, segmentDistances, totalDistance); // Retorna el resultado completo con nodos, puntos, distancias y total
    }

    private static class NodeDistance { // Clase interna privada estática que representa un nodo con su distancia (para la cola de prioridad)
        final String node; // Nombre del nodo (final porque es inmutable)
        final double distance; // Distancia desde el origen (final porque es inmutable)

        NodeDistance(String node, double distance) { // Constructor que inicializa el nodo y su distancia
            this.node = node; // Asigna el nombre del nodo
            this.distance = distance; // Asigna la distancia
        }
    }

    public static class PathEdge { // Clase interna pública estática que representa una arista entre dos nodos
        private final String from; // Nodo origen de la arista (final porque es inmutable)
        private final String to; // Nodo destino de la arista (final porque es inmutable)
        private final double distance; // Distancia entre los nodos (final porque es inmutable)

        public PathEdge(String from, String to, double distance) { // Constructor que inicializa una arista
            this.from = from; // Asigna el nodo origen
            this.to = to; // Asigna el nodo destino
            this.distance = distance; // Asigna la distancia
        }

        public String getFrom() { return from; } // Método getter que retorna el nodo origen
        public String getTo() { return to; } // Método getter que retorna el nodo destino
        public double getDistance() { return distance; } // Método getter que retorna la distancia de la arista
    }

    public static class PathResult { // Clase interna pública estática que representa el resultado de un cálculo de camino
        private final List<String> nodes; // Lista de nombres de nodos en el camino (final porque es inmutable)
        private final List<Point> points; // Lista de posiciones (x,y) de los puntos del camino (final porque es inmutable)
        private final List<Double> segmentDistances; // Lista de distancias de cada segmento del camino (final porque es inmutable)
        private final double totalDistance; // Distancia total del camino completo (final porque es inmutable)

        public PathResult(List<String> nodes, List<Point> points, List<Double> segmentDistances, double totalDistance) { // Constructor que inicializa un resultado de camino
            this.nodes = nodes; // Asigna la lista de nodos
            this.points = points; // Asigna la lista de puntos
            this.segmentDistances = segmentDistances; // Asigna la lista de distancias de segmentos
            this.totalDistance = totalDistance; // Asigna la distancia total
        }

        public static PathResult empty() { // Método estático factory que crea un resultado vacío
            return new PathResult(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), 0.0); // Retorna PathResult con listas vacías y distancia 0
        }

        public boolean isValid() { return nodes != null && !nodes.isEmpty(); } // Método que verifica si el resultado es válido (tiene nodos)
        public List<Point> getPoints() { return points; } // Método getter que retorna la lista de puntos del camino
        public List<Double> getSegmentDistances() { return segmentDistances; } // Método getter que retorna la lista de distancias de segmentos
        public double getTotalDistance() { return totalDistance; } // Método getter que retorna la distancia total del camino
        public List<String> getNodes() { return nodes; } // Método getter que retorna la lista de nombres de nodos del camino
    }
}

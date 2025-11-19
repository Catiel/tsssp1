package model; // Declaración del paquete model donde se encuentra la clase

import java.awt.Point; // Importa Point de AWT para representar coordenadas (x,y) de posiciones
import java.util.*; // Importa todas las clases de utilidades de Java (List, ArrayList, Collections, etc.)

public class Crane { // Declaración de la clase pública Crane que representa una grúa en la simulación
    private final String name; // Variable final que almacena el nombre identificador de la grúa
    private final int units; // Variable final que almacena el número de unidades de la grúa
    private final double emptySpeed; // Variable final que almacena la velocidad de la grúa vacía en metros por minuto
    private final double fullSpeed; // Variable final que almacena la velocidad de la grúa cargada en metros por minuto
    private final Point homePosition; // Variable final que almacena la posición inicial/home de la grúa

    private Point logicalPosition; // Variable que almacena la posición lógica actual de la grúa en la simulación

    private List<Point> currentPathPoints; // Lista que almacena los puntos del camino actual que sigue la grúa
    private List<Double> currentSegmentDistances; // Lista que almacena las distancias de cada segmento del camino en metros
    private double totalPathDistance; // Variable que almacena la distancia total del camino actual en metros
    private double animationDurationSeconds; // Variable que almacena la duración de la animación en segundos de tiempo real
    private double visualProgress; // Variable que almacena el progreso visual de la animación de 0.0 (inicio) a 1.0 (fin)

    private Valve carryingValve; // Variable que almacena la válvula que está transportando actualmente (null si no lleva nada)
    private boolean isBusy; // Variable booleana que indica si la grúa está ocupada
    private boolean isMoving; // Variable booleana que indica si la grúa está en movimiento actualmente

    private int totalTrips; // Variable que acumula el número total de viajes completados por la grúa
    private double totalTravelTime; // Variable que acumula el tiempo total de viaje en horas
    private double totalUsageTime; // Variable que acumula el tiempo total de uso de la grúa en horas
    private List<Double> utilizationHistory; // Lista sincronizada que almacena el historial de porcentajes de utilización
    private List<Double> timeHistory; // Lista sincronizada que almacena los tiempos correspondientes a cada medición de utilización
    private double lastStatsUpdateTime; // Variable que almacena el tiempo de la última actualización de estadísticas
    private double totalObservedTime; // Variable que acumula el tiempo total observado para calcular utilización

    public Crane(String name, int units, double emptySpeed, double fullSpeed, Point home) { // Constructor que inicializa una grúa con sus parámetros
        this.name = name; // Asigna el nombre recibido a la variable de instancia
        this.units = units; // Asigna las unidades recibidas a la variable de instancia
        this.emptySpeed = emptySpeed; // Asigna la velocidad vacía recibida a la variable de instancia
        this.fullSpeed = fullSpeed; // Asigna la velocidad cargada recibida a la variable de instancia
        this.homePosition = new Point(home); // Crea una copia del punto home y lo asigna como posición inicial
        this.logicalPosition = new Point(home); // Crea una copia del punto home y lo asigna como posición lógica inicial
        this.currentPathPoints = new ArrayList<>(); // Inicializa la lista de puntos del camino vacía
        this.currentSegmentDistances = new ArrayList<>(); // Inicializa la lista de distancias de segmentos vacía
        this.totalPathDistance = 0; // Inicializa la distancia total del camino en 0
        this.animationDurationSeconds = 0.5; // Inicializa la duración de animación por defecto en 0.5 segundos
        this.visualProgress = 1.0; // Inicializa el progreso visual en 1.0 (completado) para comenzar en destino
        this.carryingValve = null; // Inicializa sin válvula transportada (null)
        this.isBusy = false; // Inicializa la grúa como no ocupada
        this.isMoving = false; // Inicializa la grúa como no en movimiento
        this.totalTrips = 0; // Inicializa el contador de viajes en 0
        this.totalTravelTime = 0; // Inicializa el tiempo total de viaje en 0
        this.totalUsageTime = 0; // Inicializa el tiempo total de uso en 0
        this.utilizationHistory = Collections.synchronizedList(new ArrayList<>()); // Crea lista sincronizada para thread-safety del historial de utilización
        this.timeHistory = Collections.synchronizedList(new ArrayList<>()); // Crea lista sincronizada para thread-safety del historial de tiempos
        this.lastStatsUpdateTime = 0; // Inicializa el tiempo de última actualización de estadísticas en 0
        this.totalObservedTime = 0; // Inicializa el tiempo total observado en 0
    }

    public double calculateTravelTime(double distanceMeters, boolean loaded) { // Método que calcula el tiempo de viaje basado en distancia y si está cargada
        if (distanceMeters <= 0) { // Verifica si la distancia es cero o negativa
            return 0; // Retorna 0 horas si no hay distancia a recorrer
        }
        double speed = loaded ? fullSpeed : emptySpeed; // Selecciona velocidad según si está cargada (fullSpeed) o vacía (emptySpeed)
        if (speed <= 0) { // Verifica si la velocidad es cero o negativa
            return 0; // Retorna 0 horas si la velocidad es inválida
        }
        return (distanceMeters / speed) / 60.0; // Calcula tiempo: (metros / metros_por_minuto) / 60 = horas
    }

    public synchronized void startMove(List<Point> pathPoints, // Método sincronizado sobrecargado con parámetros mínimos para iniciar movimiento
                          List<Double> segmentDistances, // Segundo parámetro: lista de distancias de cada segmento
                          double totalDistanceMeters, // Tercer parámetro: distancia total del camino
                          double travelTimeHours) { // Cuarto parámetro: tiempo de viaje en horas (no usado en animación)
        startMove(pathPoints, segmentDistances, totalDistanceMeters, travelTimeHours, 0, 50); // Llama a la versión completa con tiempo 0 y velocidad 50 por defecto
    }

    public synchronized void startMove(List<Point> pathPoints, // Método sincronizado sobrecargado que agrega tiempo de simulación
                          List<Double> segmentDistances, // Lista de distancias de segmentos
                          double totalDistanceMeters, // Distancia total del camino
                          double travelTimeHours, // Tiempo de viaje en horas
                          double currentSimTime) { // Tiempo actual de simulación
        startMove(pathPoints, segmentDistances, totalDistanceMeters, travelTimeHours, currentSimTime, 50); // Llama a la versión completa con velocidad 50 por defecto
    }

    public synchronized void startMove(List<Point> pathPoints, // Método sincronizado completo que inicia el movimiento de la grúa con todos los parámetros
                          List<Double> segmentDistances, // Lista de distancias de cada segmento del camino
                          double totalDistanceMeters, // Distancia total del camino en metros
                          double travelTimeHours, // Tiempo de viaje en horas (usado solo para estadísticas)
                          double currentSimTime, // Tiempo actual de simulación en horas
                          int animationSpeed) { // Velocidad de animación (1-100) que afecta duración visual
        if (pathPoints == null || pathPoints.size() < 2) { // Verifica si no hay camino válido (null o menos de 2 puntos)
            this.isMoving = false; // Marca que no está en movimiento
            this.visualProgress = 1.0; // Establece progreso en 1.0 (completado)
            if (pathPoints != null && !pathPoints.isEmpty()) { // Verifica si hay al menos un punto
                this.logicalPosition = new Point(pathPoints.get(pathPoints.size() - 1)); // Actualiza posición lógica al último punto disponible
            }
            return; // Sale del método sin iniciar movimiento
        }

        this.currentPathPoints = new ArrayList<>(pathPoints); // Crea copia de la lista de puntos del camino
        this.currentSegmentDistances = new ArrayList<>(segmentDistances); // Crea copia de la lista de distancias de segmentos
        this.totalPathDistance = totalDistanceMeters <= 0 ? 1 : totalDistanceMeters; // Establece distancia total, mínimo 1 para evitar división por cero

        double baseDuration; // Declara variable para almacenar la duración base de la animación
        if (animationSpeed <= 20) { // Velocidad muy lenta (1-20)
            baseDuration = 8.0 - (animationSpeed / 20.0) * 4.0; // Calcula duración entre 8 y 4 segundos (interpolación lineal)
        } else if (animationSpeed <= 50) { // Velocidad normal (21-50)
            baseDuration = 4.0 - ((animationSpeed - 20.0) / 30.0) * 2.0; // Calcula duración entre 4 y 2 segundos
        } else if (animationSpeed <= 80) { // Velocidad rápida (51-80)
            baseDuration = 2.0 - ((animationSpeed - 50.0) / 30.0) * 1.5; // Calcula duración entre 2 y 0.5 segundos
        } else { // Velocidad muy rápida (81-100)
            baseDuration = 0.5 - ((animationSpeed - 80.0) / 20.0) * 0.4; // Calcula duración entre 0.5 y 0.1 segundos
        }

        double distanceFactor = Math.sqrt(totalDistanceMeters / 100.0); // Calcula factor de ajuste basado en distancia usando raíz cuadrada normalizada a 100 metros
        double duration = Math.max(0.1, baseDuration * (0.7 + distanceFactor * 0.3)); // Ajusta duración base por distancia (70% base + 30% factor) con mínimo de 0.1s

        this.animationDurationSeconds = duration; // Asigna la duración calculada a la variable de instancia
        this.visualProgress = 0.0; // Reinicia el progreso visual a 0.0 (inicio de animación)
        this.isMoving = true; // Marca la grúa como en movimiento
    }

    public synchronized void updateVisualPosition(double deltaSeconds) { // Método sincronizado que actualiza la posición visual basado en tiempo real transcurrido
        if (!isMoving || currentPathPoints.size() < 2) { // Verifica si no está en movimiento o no hay camino válido
            return; // Sale del método sin actualizar nada
        }

        if (animationDurationSeconds <= 0) { // Verifica si la duración de animación es inválida
            isMoving = false; // Marca como no en movimiento
            visualProgress = 1.0; // Establece progreso en 1.0 (completado)
            return; // Sale del método
        }

        double progressPerSecond = 1.0 / animationDurationSeconds; // Calcula el progreso por segundo (1.0 dividido por duración total)
        visualProgress += progressPerSecond * deltaSeconds; // Incrementa el progreso visual multiplicando progreso por segundo por tiempo transcurrido

        if (visualProgress >= 1.0) { // Verifica si el progreso alcanzó o superó 1.0 (completado)
            visualProgress = 1.0; // Limita el progreso exactamente a 1.0
            isMoving = false; // Marca la grúa como no en movimiento (animación completada)
        }
    }

    public synchronized Point getInterpolatedPosition() { // Método sincronizado que retorna la posición interpolada actual de la grúa para animación suave
        if (currentPathPoints.isEmpty()) { // Verifica si no hay puntos en el camino actual
            return new Point(logicalPosition); // Retorna una copia de la posición lógica actual
        }

        if (!isMoving || visualProgress >= 1.0) { // Verifica si no está en movimiento o la animación ya terminó
            return new Point(currentPathPoints.get(currentPathPoints.size() - 1)); // Retorna copia de la posición final del camino
        }

        if (visualProgress <= 0.0) { // Verifica si apenas está comenzando la animación
            return new Point(currentPathPoints.get(0)); // Retorna copia de la posición inicial del camino
        }

        if (currentSegmentDistances.isEmpty() || totalPathDistance <= 0) { // Verifica si no hay información de segmentos o distancia inválida
            Point from = currentPathPoints.get(0); // Obtiene el punto inicial del camino
            Point to = currentPathPoints.get(currentPathPoints.size() - 1); // Obtiene el punto final del camino
            int x = (int)Math.round(from.x + (to.x - from.x) * visualProgress); // Calcula coordenada X interpolada linealmente entre inicio y fin
            int y = (int)Math.round(from.y + (to.y - from.y) * visualProgress); // Calcula coordenada Y interpolada linealmente entre inicio y fin
            return new Point(x, y); // Retorna el punto interpolado
        }

        double targetDistance = totalPathDistance * visualProgress; // Calcula la distancia objetivo basada en el progreso actual
        double accumulatedDistance = 0; // Inicializa variable para acumular distancia recorrida

        for (int i = 0; i < currentSegmentDistances.size(); i++) { // Itera sobre cada segmento del camino
            double segmentLength = currentSegmentDistances.get(i); // Obtiene la longitud del segmento actual
            double nextAccumulated = accumulatedDistance + segmentLength; // Calcula la distancia acumulada hasta el final de este segmento

            if (targetDistance <= nextAccumulated || i == currentSegmentDistances.size() - 1) { // Verifica si la distancia objetivo está en este segmento o es el último segmento
                double distanceInSegment = targetDistance - accumulatedDistance; // Calcula cuánta distancia se ha recorrido dentro de este segmento
                double ratio = segmentLength == 0 ? 1.0 : Math.min(1.0, distanceInSegment / segmentLength); // Calcula el ratio de progreso en el segmento (0.0 a 1.0)

                Point from = currentPathPoints.get(i); // Obtiene el punto inicial del segmento
                Point to = currentPathPoints.get(i + 1); // Obtiene el punto final del segmento
                int x = (int)Math.round(from.x + (to.x - from.x) * ratio); // Calcula coordenada X interpolada en el segmento
                int y = (int)Math.round(from.y + (to.y - from.y) * ratio); // Calcula coordenada Y interpolada en el segmento
                return new Point(x, y); // Retorna el punto interpolado
            }

            accumulatedDistance = nextAccumulated; // Actualiza la distancia acumulada para el siguiente segmento
        }

        return new Point(currentPathPoints.get(currentPathPoints.size() - 1)); // Fallback: retorna copia del último punto del camino
    }

    public void pickupValve(Valve valve) { // Método que hace que la grúa recoja una válvula
        this.carryingValve = valve; // Asigna la válvula recibida a la variable de instancia
        if (valve != null) { // Verifica si la válvula no es null
            valve.setState(Valve.State.IN_TRANSIT); // Cambia el estado de la válvula a IN_TRANSIT (en tránsito)
        }
    }

    public Valve releaseValve() { // Método que libera la válvula que está transportando la grúa
        Valve valve = this.carryingValve; // Guarda referencia a la válvula actual en variable local
        this.carryingValve = null; // Limpia la referencia de la grúa (ya no transporta nada)
        return valve; // Retorna la válvula liberada
    }

    public synchronized void completeTrip() { // Método sincronizado que marca el viaje como completado
        totalTrips++; // Incrementa el contador total de viajes
        visualProgress = 1.0; // Fuerza el progreso visual a 1.0 (completado)
        isMoving = false; // Marca la grúa como no en movimiento

        if (!currentPathPoints.isEmpty()) { // Verifica si hay puntos en el camino actual
            logicalPosition = new Point(currentPathPoints.get(currentPathPoints.size() - 1)); // Actualiza la posición lógica al último punto del camino
        }

        totalPathDistance = 0; // Reinicia la distancia total del camino a 0
        currentPathPoints.clear(); // Limpia la lista de puntos del camino
        currentSegmentDistances.clear(); // Limpia la lista de distancias de segmentos
    }

    public void addTravelTime(double time) { // Método que agrega tiempo de viaje a las estadísticas
        totalTravelTime += time; // Incrementa el tiempo total de viaje con el tiempo recibido
        totalUsageTime += time; // Incrementa el tiempo total de uso con el tiempo recibido
    }

    public synchronized void updateStatistics(double currentTime) { // Método sincronizado que actualiza las estadísticas de la grúa
        double delta = currentTime - lastStatsUpdateTime; // Calcula el tiempo transcurrido desde la última actualización
        if (delta < 0) { // Verifica si el delta es negativo (error de tiempo)
            delta = 0; // Establece delta en 0 para evitar valores negativos
        }

        if (delta > 0) { // Verifica si transcurrió tiempo desde la última actualización
            totalObservedTime += delta; // Incrementa el tiempo total observado con el delta
        }

        double utilization = getUtilization(); // Calcula el porcentaje de utilización actual
        utilizationHistory.add(utilization); // Agrega la utilización calculada al historial
        timeHistory.add(currentTime); // Agrega el tiempo actual al historial de tiempos
        lastStatsUpdateTime = currentTime; // Actualiza la marca de tiempo de última actualización
    }

    public double getUtilization() { // Método que calcula y retorna el porcentaje de utilización de la grúa
        double denominator = totalObservedTime; // Obtiene el tiempo total observado como denominador
        if (denominator <= 0) { // Verifica si el denominador es cero o negativo
            return 0; // Retorna 0% de utilización si no hay tiempo observado
        }
        return (totalUsageTime / denominator) * 100.0; // Calcula utilización: (tiempo usado / tiempo observado) * 100
    }

    public double getUtilization(double ignoredTotalTime) { // Método sobrecargado que ignora el parámetro y llama a getUtilization()
        return getUtilization(); // Retorna el resultado de getUtilization() sin parámetros
    }

    // Getters
    public String getName() { return name; } // Método getter que retorna el nombre de la grúa
    public boolean isBusy() { return isBusy; } // Método getter que retorna si la grúa está ocupada
    public void setBusy(boolean busy) { this.isBusy = busy; } // Método setter que establece si la grúa está ocupada
    public boolean isMoving() { return isMoving; } // Método getter que retorna si la grúa está en movimiento
    public double getVisualProgress() { return visualProgress; } // Método getter que retorna el progreso visual de la animación (0.0 a 1.0)
    public Valve getCarryingValve() { return carryingValve; } // Método getter que retorna la válvula que está transportando (null si no transporta)
    public Point getCurrentPosition() { return logicalPosition; } // Método getter que retorna la posición lógica actual de la grúa
    public Point getHomePosition() { return homePosition; } // Método getter que retorna la posición home inicial de la grúa
    public int getTotalTrips() { return totalTrips; } // Método getter que retorna el número total de viajes completados
    public double getTotalTravelTime() { return totalTravelTime; } // Método getter que retorna el tiempo total de viaje en horas
    public double getTotalUsageTime() { return totalUsageTime; } // Método getter que retorna el tiempo total de uso en horas
    public double getTotalObservedTime() { return totalObservedTime; } // Método getter que retorna el tiempo total observado en horas
    public double getEmptySpeed() { return emptySpeed; } // Método getter que retorna la velocidad de la grúa vacía en metros por minuto
    public double getFullSpeed() { return fullSpeed; } // Método getter que retorna la velocidad de la grúa cargada en metros por minuto
    public List<Double> getUtilizationHistory() { return new ArrayList<>(utilizationHistory); } // Método getter que retorna copia del historial de utilización
    public List<Double> getTimeHistory() { return new ArrayList<>(timeHistory); } // Método getter que retorna copia del historial de tiempos
    public int getUnits() { return units; } // Método getter que retorna el número de unidades de la grúa
    public List<Point> getCurrentPathPoints() { // Método getter sincronizado que retorna copia de los puntos del camino actual
        synchronized(this) { // Bloque sincronizado para thread-safety
            return new ArrayList<>(currentPathPoints); // Retorna copia de la lista de puntos del camino
        }
    }
}

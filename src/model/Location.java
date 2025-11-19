package model; // Declaración del paquete model donde se encuentra la clase

import java.awt.Point; // Importa Point de AWT para representar coordenadas (x,y) de posiciones
import java.util.*; // Importa todas las clases de utilidades de Java (List, ArrayList, Queue, Collections, etc.)
import java.util.concurrent.ConcurrentLinkedQueue; // Importa ConcurrentLinkedQueue para cola thread-safe sin bloqueos

public class Location { // Declaración de la clase pública Location que representa una locación en la simulación
    private final String name; // Variable final que almacena el nombre identificador de la locación
    private final int capacity; // Variable final que almacena la capacidad máxima de válvulas en la locación
    private final int units; // Variable final que almacena el número de unidades de procesamiento disponibles
    private final Point position; // Variable final que almacena la posición (x,y) de la locación en el layout

    private final Queue<Valve> queue; // Cola thread-safe que almacena válvulas esperando ser procesadas
    private final List<Valve> processing; // Lista sincronizada que almacena válvulas actualmente en procesamiento

    private int totalEntries; // Variable que acumula el número total de entradas de válvulas a esta locación
    private int totalExits; // Variable que acumula el número total de salidas de válvulas de esta locación
    private double totalBusyTime; // Variable que acumula el tiempo total que la locación estuvo ocupada en horas
    private double totalBlockedTime; // Variable que acumula el tiempo total que la locación estuvo bloqueada (llena) en horas
    private double lastUpdateTime; // Variable que almacena el tiempo de la última actualización de estadísticas en horas
    private double totalObservedTime; // Variable que acumula el tiempo total observado para estadísticas en horas
    private double cumulativeContentTime; // Variable que acumula el producto de contenido por tiempo para calcular promedio
    private int lastSampleContents; // Variable que almacena el contenido de la última muestra tomada
    private List<Integer> contentHistory; // Lista sincronizada que almacena el historial de contenidos en cada momento
    private List<Double> timeHistory; // Lista sincronizada que almacena los tiempos correspondientes a cada muestra de contenido

    public Location(String name, int capacity, int units, Point position) { // Constructor que inicializa una locación con sus parámetros
        this.name = name; // Asigna el nombre recibido a la variable de instancia
        this.capacity = capacity; // Asigna la capacidad recibida a la variable de instancia
        this.units = units; // Asigna las unidades recibidas a la variable de instancia
        this.position = position; // Asigna la posición recibida a la variable de instancia
        this.queue = new ConcurrentLinkedQueue<>(); // Inicializa la cola concurrente vacía para thread-safety
        this.processing = Collections.synchronizedList(new ArrayList<>()); // Inicializa la lista de procesamiento sincronizada vacía
        this.totalEntries = 0; // Inicializa el contador de entradas en 0
        this.totalExits = 0; // Inicializa el contador de salidas en 0
        this.totalBusyTime = 0; // Inicializa el tiempo ocupado acumulado en 0
        this.totalBlockedTime = 0; // Inicializa el tiempo bloqueado acumulado en 0
        this.lastUpdateTime = 0; // Inicializa el tiempo de última actualización en 0
        this.totalObservedTime = 0; // Inicializa el tiempo total observado en 0
        this.cumulativeContentTime = 0; // Inicializa el tiempo acumulado de contenido en 0
        this.lastSampleContents = 0; // Inicializa el último contenido muestreado en 0
        this.contentHistory = Collections.synchronizedList(new ArrayList<>()); // Inicializa lista sincronizada de historial de contenidos vacía
        this.timeHistory = Collections.synchronizedList(new ArrayList<>()); // Inicializa lista sincronizada de historial de tiempos vacía
    }

    public boolean canAccept() { // Método que verifica si la locación puede aceptar más válvulas
        return getCurrentContents() < capacity; // Retorna true si el contenido actual es menor que la capacidad
    }

    public boolean hasAvailableUnit() { // Método que verifica si hay unidades de procesamiento disponibles
        return processing.size() < units; // Retorna true si el número de válvulas en procesamiento es menor que el número de unidades
    }

    public synchronized void addToQueue(Valve valve) { // Método sincronizado que agrega una válvula a la cola si hay capacidad
        if (canAccept()) { // Verifica si la locación puede aceptar más válvulas
            queue.add(valve); // Agrega la válvula a la cola de espera
            valve.setCurrentLocation(this); // Establece esta locación como la locación actual de la válvula
            totalEntries++; // Incrementa el contador total de entradas
        }
    }

    public synchronized void moveToProcessing(Valve valve) { // Método sincronizado que mueve una válvula de la cola a procesamiento
        if (queue.remove(valve) && hasAvailableUnit()) { // Intenta remover la válvula de la cola y verifica si hay unidad disponible
            processing.add(valve); // Agrega la válvula a la lista de procesamiento
        }
    }

    public synchronized void removeValve(Valve valve) { // Método sincronizado que remueve una válvula de la locación (cola o procesamiento)
        boolean removed = queue.remove(valve) || processing.remove(valve); // Intenta remover de la cola o de procesamiento, retorna true si se removió
        if (removed) { // Verifica si la válvula fue removida exitosamente
            totalExits++; // Incrementa el contador total de salidas
        }
    }

    public Valve peekQueue() { // Método que obtiene la primera válvula de la cola sin removerla
        return queue.peek(); // Retorna la primera válvula de la cola o null si está vacía
    }

    public synchronized void updateStatistics(double currentTime, boolean countTowardsSchedule) { // Método sincronizado que actualiza las estadísticas de la locación
        double deltaTime = currentTime - lastUpdateTime; // Calcula el tiempo transcurrido desde la última actualización
        if (deltaTime < 0) { // Verifica si el delta es negativo (error de tiempo)
            deltaTime = 0; // Establece delta en 0 para evitar valores negativos
        }

        if (deltaTime > 0) { // Verifica si transcurrió tiempo desde la última actualización
            cumulativeContentTime += lastSampleContents * deltaTime; // Acumula el producto de contenido por tiempo (para calcular promedio ponderado)
        }

        if (!timeHistory.isEmpty()) { // Verifica si hay datos en el historial de tiempos
            double lastTime = timeHistory.get(timeHistory.size() - 1); // Obtiene el último tiempo registrado
            if (Math.abs(currentTime - lastTime) < 1e-9) { // Verifica si el tiempo actual es casi igual al último (diferencia menor a 1e-9)
                return; // Sale del método para evitar duplicar entradas con el mismo tiempo
            }
        }

        if (processing.size() > 0 && deltaTime > 0) { // Verifica si hay válvulas en procesamiento y transcurrió tiempo
            totalBusyTime += deltaTime; // Acumula el tiempo ocupado (cuando hay procesamiento activo)
        }

        if (!canAccept() && deltaTime > 0 && countTowardsSchedule) { // Verifica si la locación está llena, transcurrió tiempo y cuenta para el horario
            totalBlockedTime += deltaTime; // Acumula el tiempo bloqueado (cuando está a capacidad máxima)
        }

        if (deltaTime > 0 && countTowardsSchedule) { // Verifica si transcurrió tiempo y cuenta para el horario programado
            totalObservedTime += deltaTime; // Acumula el tiempo total observado
        }

        int currentContents = getCurrentContents(); // Obtiene el contenido actual de la locación (cola + procesamiento)
        contentHistory.add(currentContents); // Agrega el contenido actual al historial
        timeHistory.add(currentTime); // Agrega el tiempo actual al historial

        lastSampleContents = currentContents; // Actualiza la última muestra de contenido con el valor actual
        lastUpdateTime = currentTime; // Actualiza el tiempo de última actualización con el tiempo actual
    }

    public double getUtilization() { // Método que calcula y retorna el porcentaje de utilización de la locación
        if (name.startsWith("Almacen_") && capacity > 0 && capacity < Integer.MAX_VALUE) { // Verifica si es un almacén con capacidad finita
            return (getAverageContents() / capacity) * 100.0; // Para almacenes: calcula utilización como (contenido promedio / capacidad) * 100
        }

        double denominator = totalObservedTime * units; // Calcula denominador: tiempo observado multiplicado por número de unidades
        if (denominator <= 0) { // Verifica si el denominador es cero o negativo
            return 0; // Retorna 0% de utilización si no hay tiempo observado
        }
        double util = (totalBusyTime / denominator) * 100.0; // Calcula utilización: (tiempo ocupado / tiempo disponible) * 100
        return Math.min(util, 100.0); // Retorna el mínimo entre la utilización calculada y 100% para limitarlo
    }

    public synchronized double getTotalBusyTime() { // Método sincronizado que retorna el tiempo total ocupado
        return totalBusyTime; // Retorna el tiempo total que la locación estuvo ocupada
    }

    public synchronized double getTotalObservedTime() { // Método sincronizado que retorna el tiempo total observado multiplicado por unidades
        return totalObservedTime * units; // Retorna el tiempo observado multiplicado por número de unidades (tiempo disponible total)
    }

    public synchronized double getAverageContents() { // Método sincronizado que calcula y retorna el contenido promedio de la locación
        if (timeHistory.size() < 2) { // Verifica si hay menos de 2 muestras en el historial
            return 0.0; // Retorna 0.0 si no hay suficientes datos para calcular promedio
        }
        double totalTime = timeHistory.get(timeHistory.size() - 1) - timeHistory.get(0); // Calcula el tiempo total del periodo observado (último - primero)
        if (totalTime <= 1e-9) { // Verifica si el tiempo total es prácticamente cero
            return 0.0; // Retorna 0.0 si no hay tiempo significativo observado
        }
        return cumulativeContentTime / totalTime; // Calcula promedio ponderado: tiempo acumulado de contenido / tiempo total
    }

    public synchronized double getMaxContents() { // Método sincronizado que retorna el contenido máximo observado
        List<Integer> snapshot = new ArrayList<>(contentHistory); // Crea una copia del historial de contenidos para thread-safety
        return snapshot.stream() // Crea un stream del snapshot
            .mapToInt(Integer::intValue) // Convierte los Integer a int primitivos
            .max() // Encuentra el valor máximo
            .orElse(0); // Retorna el máximo o 0 si el stream está vacío
    }

    public List<Integer> getContentHistory() { // Método que retorna copia del historial de contenidos
        return new ArrayList<>(contentHistory); // Retorna una nueva ArrayList con copia del historial de contenidos
    }

    public List<Double> getTimeHistory() { // Método que retorna copia del historial de tiempos
        return new ArrayList<>(timeHistory); // Retorna una nueva ArrayList con copia del historial de tiempos
    }

    public synchronized double getTotalResidenceTime() { // Método sincronizado que retorna el tiempo total de residencia acumulado
        return cumulativeContentTime; // Retorna el tiempo acumulado de contenido (suma de contenido * delta tiempo)
    }

    // Getters
    public String getName() { return name; } // Método getter que retorna el nombre de la locación
    public int getCapacity() { return capacity; } // Método getter que retorna la capacidad máxima de la locación
    public int getUnits() { return units; } // Método getter que retorna el número de unidades de procesamiento
    public Point getPosition() { return position; } // Método getter que retorna la posición (x,y) de la locación
    public int getCurrentContents() { return queue.size() + processing.size(); } // Método getter que retorna el contenido actual sumando cola y procesamiento
    public int getQueueSize() { return queue.size(); } // Método getter que retorna el tamaño actual de la cola de espera
    public int getProcessingSize() { return processing.size(); } // Método getter que retorna el número de válvulas actualmente en procesamiento
    public int getTotalEntries() { return totalEntries; } // Método getter que retorna el total de entradas registradas
    public int getTotalExits() { return totalExits; } // Método getter que retorna el total de salidas registradas
    public double getTotalBlockedTime() { return totalBlockedTime; } // Método getter que retorna el tiempo total bloqueado (a capacidad máxima)

    public List<Valve> getAllValves() { // Método que retorna todas las válvulas en la locación (cola y procesamiento)
        List<Valve> all = new ArrayList<>(queue); // Crea nueva lista e inicializa con todas las válvulas de la cola
        all.addAll(processing); // Agrega todas las válvulas de procesamiento a la lista
        return all; // Retorna la lista combinada de todas las válvulas
    }

    public List<Valve> getQueueSnapshot() { // Método que retorna copia de las válvulas en la cola
        return new ArrayList<>(queue); // Retorna nueva ArrayList con copia de las válvulas en cola
    }

    public List<Valve> getProcessingValves() { // Método que retorna copia de las válvulas en procesamiento
        return new ArrayList<>(processing); // Retorna nueva ArrayList con copia de las válvulas en procesamiento
    }
}

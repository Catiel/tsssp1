package statistics; // Declaración del paquete statistics donde se encuentran las clases de estadísticas

import model.Valve; // Importa la clase Valve para acceder a los tipos de válvulas
import utils.Config; // Importa la clase Config para acceder a la configuración de escalas de tiempo

import java.util.*; // Importa todas las clases de utilidades de Java (List, ArrayList, Collections, Locale)

public class EntityStats { // Declaración de la clase pública EntityStats que almacena estadísticas de válvulas por tipo
    private Valve.Type type; // Variable que almacena el tipo de válvula al que pertenecen estas estadísticas
    private int totalArrivals; // Variable que acumula el número total de arribos de válvulas de este tipo
    private int totalCompleted; // Variable que acumula el número total de válvulas completadas de este tipo
    private int currentInSystem; // Variable que almacena el número actual de válvulas de este tipo en el sistema

    private double sumTimeInSystem; // Variable que acumula la suma de tiempos totales en el sistema en horas
    private double sumProcessingTime; // Variable que acumula la suma de tiempos de procesamiento en horas
    private double sumMovementTime; // Variable que acumula la suma de tiempos de movimiento en horas
    private double sumWaitingTime; // Variable que acumula la suma de tiempos de espera en horas
    private double sumBlockedTime; // Variable que acumula la suma de tiempos bloqueados en horas

    private double minTimeInSystem = Double.MAX_VALUE; // Variable que almacena el tiempo mínimo en sistema (inicializado con valor máximo posible)
    private double maxTimeInSystem = 0; // Variable que almacena el tiempo máximo en sistema (inicializado en 0)

    private List<Double> arrivalTimes; // Lista sincronizada que almacena los tiempos de arribo de cada válvula
    private List<Double> completionTimes; // Lista sincronizada que almacena los tiempos de completación de cada válvula

    public EntityStats(Valve.Type type) { // Constructor que inicializa las estadísticas para un tipo específico de válvula
        this.type = type; // Asigna el tipo de válvula recibido a la variable de instancia
        this.arrivalTimes = Collections.synchronizedList(new ArrayList<>()); // Inicializa lista sincronizada de tiempos de arribo para thread-safety
        this.completionTimes = Collections.synchronizedList(new ArrayList<>()); // Inicializa lista sincronizada de tiempos de completación para thread-safety
    }

    public synchronized void recordArrival(double time) { // Método sincronizado que registra el arribo de una válvula
        totalArrivals++; // Incrementa el contador total de arribos
        currentInSystem++; // Incrementa el contador de válvulas actualmente en el sistema
        arrivalTimes.add(time); // Agrega el tiempo de arribo a la lista de tiempos
    }

    public synchronized void recordCompletion(double time) { // Método sincronizado que registra la completación de una válvula
        totalCompleted++; // Incrementa el contador total de válvulas completadas
        currentInSystem = Math.max(0, currentInSystem - 1); // Decrementa el contador de válvulas en sistema asegurando que no sea negativo
        completionTimes.add(time); // Agrega el tiempo de completación a la lista de tiempos
    }

    public synchronized void addTimeInSystem(double time) { // Método sincronizado que agrega un tiempo en sistema y actualiza mín/máx
        sumTimeInSystem += time; // Acumula el tiempo en sistema recibido a la suma total
        minTimeInSystem = Math.min(minTimeInSystem, time); // Actualiza el tiempo mínimo si el tiempo recibido es menor
        maxTimeInSystem = Math.max(maxTimeInSystem, time); // Actualiza el tiempo máximo si el tiempo recibido es mayor
    }

    public void addProcessingTime(double time) { sumProcessingTime += time; } // Método que acumula tiempo de procesamiento a la suma total
    public void addMovementTime(double time) { sumMovementTime += time; } // Método que acumula tiempo de movimiento a la suma total
    public void addWaitingTime(double time) { sumWaitingTime += time; } // Método que acumula tiempo de espera a la suma total
    public void addBlockedTime(double time) { sumBlockedTime += time; } // Método que acumula tiempo bloqueado a la suma total

    public double getAvgTimeInSystem() { // Método que calcula el tiempo promedio en sistema
        return totalCompleted > 0 ? sumTimeInSystem / totalCompleted : 0; // Retorna suma de tiempos dividido por completadas o 0 si no hay completadas
    }

    public double getAvgProcessingTime() { // Método que calcula el tiempo promedio de procesamiento
        return totalCompleted > 0 ? sumProcessingTime / totalCompleted : 0; // Retorna suma de procesamiento dividido por completadas o 0 si no hay completadas
    }

    public double getAvgMovementTime() { // Método que calcula el tiempo promedio de movimiento
        return totalCompleted > 0 ? sumMovementTime / totalCompleted : 0; // Retorna suma de movimiento dividido por completadas o 0 si no hay completadas
    }

    public double getAvgWaitingTime() { // Método que calcula el tiempo promedio de espera
        return totalCompleted > 0 ? sumWaitingTime / totalCompleted : 0; // Retorna suma de espera dividido por completadas o 0 si no hay completadas
    }

    public double getCompletionRate() { // Método que calcula la tasa de completación como porcentaje
        return totalArrivals > 0 ? (totalCompleted * 100.0 / totalArrivals) : 0; // Retorna porcentaje de completadas sobre arribos o 0 si no hay arribos
    }

    public double getAvgBlockedTime() { // Método que calcula el tiempo promedio bloqueado
        return totalCompleted > 0 ? sumBlockedTime / totalCompleted : 0; // Retorna suma de tiempo bloqueado dividido por completadas o 0 si no hay completadas
    }

    public String getDetailedReport() { // Método que genera un reporte detallado formateado de las estadísticas
        Config config = Config.getInstance(); // Obtiene la instancia singleton de la configuración

        double systemMinutes = getAvgTimeInSystem() * 60.0 * config.getEntityTimeScale(type, "system", 1.0); // Convierte tiempo promedio en sistema de horas a minutos y aplica escala configurada
        double movementMinutes = getAvgMovementTime() * 60.0 * config.getEntityTimeScale(type, "movement", 1.0); // Convierte tiempo promedio de movimiento de horas a minutos y aplica escala configurada
        double waitingMinutes = getAvgWaitingTime() * 60.0 * config.getEntityTimeScale(type, "waiting", 1.0); // Convierte tiempo promedio de espera de horas a minutos y aplica escala configurada
        double processingMinutes = getAvgProcessingTime() * 60.0 * config.getEntityTimeScale(type, "processing", 1.0); // Convierte tiempo promedio de procesamiento de horas a minutos y aplica escala configurada
        double blockedMinutes = getAvgBlockedTime() * 60.0 * config.getEntityTimeScale(type, "blocked", 1.0); // Convierte tiempo promedio bloqueado de horas a minutos y aplica escala configurada

        return String.format( // Retorna string formateado con todas las estadísticas
            Locale.ROOT, // Usa locale ROOT para formato consistente (punto decimal independiente del sistema)
            "%-12s | Salidas: %4d | En Sistema: %4d | T Sistema Prom: %8.2f min | T Movimiento Prom: %8.2f min | T Espera Prom: %8.2f min | T Operacion Prom: %8.2f min | T Bloqueo Prom: %8.2f min", // Formato con alineación y decimales específicos
            type.getDisplayName(), // Inserta nombre del tipo de válvula alineado a izquierda en 12 caracteres
            totalCompleted, // Inserta total de válvulas completadas alineado en 4 dígitos
            currentInSystem, // Inserta número actual de válvulas en sistema alineado en 4 dígitos
            systemMinutes, // Inserta tiempo promedio en sistema en minutos con 2 decimales alineado en 8 caracteres
            movementMinutes, // Inserta tiempo promedio de movimiento en minutos con 2 decimales alineado en 8 caracteres
            waitingMinutes, // Inserta tiempo promedio de espera en minutos con 2 decimales alineado en 8 caracteres
            processingMinutes, // Inserta tiempo promedio de procesamiento en minutos con 2 decimales alineado en 8 caracteres
            blockedMinutes // Inserta tiempo promedio bloqueado en minutos con 2 decimales alineado en 8 caracteres
        );
    }

    // Getters
    public Valve.Type getType() { return type; } // Método getter que retorna el tipo de válvula de estas estadísticas
    public int getTotalArrivals() { return totalArrivals; } // Método getter que retorna el total de arribos registrados
    public int getTotalCompleted() { return totalCompleted; } // Método getter que retorna el total de válvulas completadas
    public int getCurrentInSystem() { return currentInSystem; } // Método getter que retorna el número actual de válvulas en sistema
    public List<Double> getArrivalTimes() { return new ArrayList<>(arrivalTimes); } // Método getter que retorna copia de la lista de tiempos de arribo
    public List<Double> getCompletionTimes() { return new ArrayList<>(completionTimes); } // Método getter que retorna copia de la lista de tiempos de completación
}

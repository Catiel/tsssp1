package statistics; // Declaración del paquete statistics donde se encuentran las clases de estadísticas

import java.util.*; // Importa todas las clases de utilidades de Java (List, ArrayList, Collections)

public class ResourceStats { // Declaración de la clase pública ResourceStats que almacena estadísticas de recursos (grúas)
    private String name; // Variable que almacena el nombre identificador del recurso
    private List<Double> utilizationHistory; // Lista sincronizada que almacena el historial de porcentajes de utilización
    private List<Integer> tripHistory; // Lista sincronizada que almacena el historial de número de viajes
    private List<Double> timeHistory; // Lista sincronizada que almacena los tiempos correspondientes a cada muestra

    private int units; // Variable que almacena el número de unidades del recurso
    private double scheduledHours; // Variable que almacena las horas programadas de trabajo
    private double totalWorkMinutes; // Variable que almacena el total de minutos trabajados
    private double avgHandleMinutes; // Variable que almacena el tiempo promedio de manejo/uso en minutos
    private double avgTravelMinutes; // Variable que almacena el tiempo promedio de viaje en minutos
    private double avgParkMinutes; // Variable que almacena el tiempo promedio estacionado/en reposo en minutos
    private double blockedPercent; // Variable que almacena el porcentaje de tiempo bloqueado
    private double currentUtilization; // Variable que almacena el porcentaje de utilización actual

    public ResourceStats(String name) { // Constructor que inicializa las estadísticas para un recurso específico
        this.name = name; // Asigna el nombre del recurso recibido a la variable de instancia
        this.utilizationHistory = Collections.synchronizedList(new ArrayList<>()); // Inicializa lista sincronizada de historial de utilización para thread-safety
        this.tripHistory = Collections.synchronizedList(new ArrayList<>()); // Inicializa lista sincronizada de historial de viajes para thread-safety
        this.timeHistory = Collections.synchronizedList(new ArrayList<>()); // Inicializa lista sincronizada de historial de tiempos para thread-safety
        this.units = 0; // Inicializa el número de unidades en 0
        this.scheduledHours = 0; // Inicializa las horas programadas en 0
        this.totalWorkMinutes = 0; // Inicializa los minutos totales trabajados en 0
        this.avgHandleMinutes = 0; // Inicializa el tiempo promedio de manejo en 0
        this.avgTravelMinutes = 0; // Inicializa el tiempo promedio de viaje en 0
        this.avgParkMinutes = 0; // Inicializa el tiempo promedio estacionado en 0
        this.blockedPercent = 0; // Inicializa el porcentaje bloqueado en 0
        this.currentUtilization = 0; // Inicializa la utilización actual en 0
    }

    public synchronized void update(int units, // Método sincronizado que actualiza todas las estadísticas del recurso - Parámetro 1: número de unidades
                                    double scheduledHours, // Parámetro 2: horas programadas de trabajo
                                    double totalWorkMinutes, // Parámetro 3: total de minutos trabajados
                                    int trips, // Parámetro 4: número de viajes realizados
                                    double avgHandleMinutes, // Parámetro 5: tiempo promedio de manejo en minutos
                                    double avgTravelMinutes, // Parámetro 6: tiempo promedio de viaje en minutos
                                    double avgParkMinutes, // Parámetro 7: tiempo promedio estacionado en minutos
                                    double blockedPercent, // Parámetro 8: porcentaje de tiempo bloqueado
                                    double utilization, // Parámetro 9: porcentaje de utilización actual
                                    double time) { // Parámetro 10: tiempo de simulación actual
        this.units = units; // Actualiza el número de unidades con el valor recibido
        this.scheduledHours = scheduledHours; // Actualiza las horas programadas con el valor recibido
        this.totalWorkMinutes = totalWorkMinutes; // Actualiza los minutos totales trabajados con el valor recibido
        this.avgHandleMinutes = avgHandleMinutes; // Actualiza el tiempo promedio de manejo con el valor recibido
        this.avgTravelMinutes = avgTravelMinutes; // Actualiza el tiempo promedio de viaje con el valor recibido
        this.avgParkMinutes = avgParkMinutes; // Actualiza el tiempo promedio estacionado con el valor recibido
        this.blockedPercent = blockedPercent; // Actualiza el porcentaje bloqueado con el valor recibido
        this.currentUtilization = utilization; // Actualiza la utilización actual con el valor recibido

        utilizationHistory.add(utilization); // Agrega el porcentaje de utilización actual al historial
        tripHistory.add(trips); // Agrega el número de viajes actual al historial
        timeHistory.add(time); // Agrega el tiempo actual al historial
    }

    public double getAverageUtilization() { // Método que calcula la utilización promedio del recurso
        synchronized (utilizationHistory) { // Evita ConcurrentModificationException durante la lectura
            return utilizationHistory.stream().mapToDouble(Double::doubleValue).average().orElse(0); // Convierte historial a stream de double, calcula promedio, retorna 0 si está vacío
        }
    }

    public double getCurrentUtilization() { // Método que retorna la utilización actual del recurso
        return currentUtilization; // Retorna el valor de la utilización actual almacenada
    }

    public int getTotalTrips() { // Método que retorna el número total de viajes realizados
        return tripHistory.isEmpty() ? 0 : tripHistory.get(tripHistory.size() - 1); // Retorna 0 si el historial está vacío, sino retorna el último valor registrado (acumulado)
    }

    public String getReport() { // Método que genera un reporte formateado de las estadísticas del recurso
        return String.format( // Retorna string formateado con todas las estadísticas del recurso
            "%-12s | Unid: %d | Prog: %6.1f h | Trabajo: %7.2f min | Usos: %4d | T Uso: %4.2f min | T Viaje: %4.2f min | T Est: %4.2f min | %% Bloq: %4.2f | Util Prom: %5.1f%%", // Formato con alineación y decimales específicos
            name, // Inserta nombre del recurso alineado a izquierda en 12 caracteres
            units, // Inserta número de unidades como entero
            scheduledHours, // Inserta horas programadas con 1 decimal alineado en 6 caracteres
            totalWorkMinutes, // Inserta minutos totales trabajados con 2 decimales alineado en 7 caracteres
            getTotalTrips(), // Inserta número total de viajes (usos) alineado en 4 dígitos
            avgHandleMinutes, // Inserta tiempo promedio de uso con 2 decimales alineado en 4 caracteres
            avgTravelMinutes, // Inserta tiempo promedio de viaje con 2 decimales alineado en 4 caracteres
            avgParkMinutes, // Inserta tiempo promedio estacionado con 2 decimales alineado en 4 caracteres
            blockedPercent, // Inserta porcentaje bloqueado con 2 decimales alineado en 4 caracteres
            getAverageUtilization()); // Inserta utilización promedio con 1 decimal alineado en 5 caracteres
    }

    // Getters
    public List<Double> getUtilizationHistory() { return new ArrayList<>(utilizationHistory); } // Método getter que retorna copia del historial de utilización
    public List<Integer> getTripHistory() { return new ArrayList<>(tripHistory); } // Método getter que retorna copia del historial de viajes
    public List<Double> getTimeHistory() { return new ArrayList<>(timeHistory); } // Método getter que retorna copia del historial de tiempos
    public int getUnits() { return units; } // Método getter que retorna el número de unidades
    public double getScheduledHours() { return scheduledHours; } // Método getter que retorna las horas programadas
    public double getTotalWorkMinutes() { return totalWorkMinutes; } // Método getter que retorna los minutos totales trabajados
    public double getAvgHandleMinutes() { return avgHandleMinutes; } // Método getter que retorna el tiempo promedio de manejo
    public double getAvgTravelMinutes() { return avgTravelMinutes; } // Método getter que retorna el tiempo promedio de viaje
    public double getAvgParkMinutes() { return avgParkMinutes; } // Método getter que retorna el tiempo promedio estacionado
    public double getBlockedPercent() { return blockedPercent; } // Método getter que retorna el porcentaje bloqueado
    public String getName() { return name; } // Método getter que retorna el nombre del recurso
}

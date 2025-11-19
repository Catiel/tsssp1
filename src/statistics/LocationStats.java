package statistics; // Declaración del paquete statistics donde se encuentran las clases de estadísticas

import utils.Config; // Importa la clase Config para acceder a la configuración de escalas y capacidades
import utils.Localization; // Importa la clase Localization para obtener nombres localizados de locaciones
import java.util.*; // Importa todas las clases de utilidades de Java (List, ArrayList, Collections)

public class LocationStats { // Declaración de la clase pública LocationStats que almacena estadísticas de una locación
    private String name; // Variable que almacena el nombre identificador de la locación
    private List<Integer> contentHistory; // Lista sincronizada que almacena el historial de contenidos de la locación
    private List<Double> utilizationHistory; // Lista sincronizada que almacena el historial de porcentajes de utilización
    private List<Double> timeHistory; // Lista sincronizada que almacena los tiempos correspondientes a cada muestra
    private int valvesProcessed; // Variable que cuenta el número total de válvulas procesadas en esta locación

    public LocationStats(String name) { // Constructor que inicializa las estadísticas para una locación específica
        this.name = name; // Asigna el nombre de la locación recibido a la variable de instancia
        this.contentHistory = Collections.synchronizedList(new ArrayList<>()); // Inicializa lista sincronizada de historial de contenidos para thread-safety
        this.utilizationHistory = Collections.synchronizedList(new ArrayList<>()); // Inicializa lista sincronizada de historial de utilización para thread-safety
        this.timeHistory = Collections.synchronizedList(new ArrayList<>()); // Inicializa lista sincronizada de historial de tiempos para thread-safety
        this.valvesProcessed = 0; // Inicializa el contador de válvulas procesadas en 0
    }

    public synchronized void update(int contents, double utilization, double time) { // Método sincronizado que actualiza las estadísticas con una nueva muestra
        contentHistory.add(contents); // Agrega el contenido actual al historial de contenidos
        utilizationHistory.add(utilization); // Agrega la utilización actual al historial de utilización
        timeHistory.add(time); // Agrega el tiempo actual al historial de tiempos
    }

    public synchronized double getAverageContents() { // Método sincronizado que calcula el contenido promedio de la locación
        List<Integer> snapshot = new ArrayList<>(contentHistory); // Crea una copia del historial de contenidos para thread-safety
        return snapshot.stream().mapToInt(Integer::intValue).average().orElse(0); // Convierte a stream de int, calcula promedio, retorna 0 si está vacío
    }

    public synchronized double getMaxContents() { // Método sincronizado que encuentra el contenido máximo registrado
        List<Integer> snapshot = new ArrayList<>(contentHistory); // Crea una copia del historial de contenidos para thread-safety
        return snapshot.stream().mapToInt(Integer::intValue).max().orElse(0); // Convierte a stream de int, encuentra máximo, retorna 0 si está vacío
    }

    public synchronized double getAverageUtilization() { // Método sincronizado que calcula la utilización promedio
        List<Double> snapshot = new ArrayList<>(utilizationHistory); // Crea una copia del historial de utilización para thread-safety
        return snapshot.stream().mapToDouble(Double::doubleValue).average().orElse(0); // Convierte a stream de double, calcula promedio, retorna 0 si está vacío
    }

    public double getCurrentUtilization() { // Método que retorna la utilización actual (última registrada)
        return utilizationHistory.isEmpty() ? 0 : utilizationHistory.get(utilizationHistory.size() - 1); // Retorna 0 si la lista está vacía, sino retorna el último elemento
    }

    public synchronized void incrementValvesProcessed() { // Método sincronizado que incrementa el contador de válvulas procesadas
        valvesProcessed++; // Incrementa el contador de válvulas procesadas en 1
    }

    public String getReport() { // Método que genera un reporte formateado de las estadísticas de la locación
        Config config = Config.getInstance(); // Obtiene la instancia singleton de la configuración
        double statsScale = config.getLocationStatsScale(name, 1.0); // Obtiene la escala de estadísticas para esta locación (por defecto 1.0)
        double avgContents = getAverageContents() * statsScale; // Calcula contenido promedio y aplica la escala configurada
        double maxContents = getMaxContents(); // Obtiene el contenido máximo registrado
        double utilization = getCurrentUtilization(); // Obtiene la utilización actual

        if (name.startsWith("Almacen_")) { // Verifica si la locación es un almacén (buffer)
            int capacity = config.getLocationCapacity(name); // Obtiene la capacidad del almacén desde la configuración
            if (capacity > 0 && capacity < Integer.MAX_VALUE) { // Verifica si la capacidad es finita (no infinita)
                utilization = (avgContents / capacity) * 100.0; // Calcula utilización como porcentaje: (contenido promedio / capacidad) * 100
            }
        }

        return String.format("%-12s | Cont Prom: %6.2f | Max: %4.0f | Utilizacion: %5.1f%% | Procesadas: %d", // Formato con alineación y decimales específicos
            Localization.getLocationDisplayName(name), // Inserta nombre localizado de la locación alineado a izquierda en 12 caracteres
            avgContents, maxContents, utilization, valvesProcessed); // Inserta contenido promedio (6 caracteres, 2 decimales), máximo (4 enteros), utilización (5 caracteres, 1 decimal), y procesadas
    }

    // Getters
    public String getName() { return name; } // Método getter que retorna el nombre de la locación
    public List<Integer> getContentHistory() { return new ArrayList<>(contentHistory); } // Método getter que retorna copia del historial de contenidos
    public List<Double> getUtilizationHistory() { return new ArrayList<>(utilizationHistory); } // Método getter que retorna copia del historial de utilización
    public List<Double> getTimeHistory() { return new ArrayList<>(timeHistory); } // Método getter que retorna copia del historial de tiempos
    public int getValvesProcessed() { return valvesProcessed; } // Método getter que retorna el número de válvulas procesadas
}

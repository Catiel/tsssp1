package core; // Declaración del paquete core donde se encuentran las clases centrales de la simulación

import model.Valve; // Importa la clase Valve para asociar válvulas con eventos

public class Event implements Comparable<Event> { // Declaración de clase pública Event que representa un evento de simulación e implementa Comparable para ordenamiento
    public enum Type { // Enumeración pública que define los tipos de eventos posibles en la simulación
        ARRIVAL, // Evento de arribo de una nueva válvula al sistema
        END_PROCESSING, // Evento de finalización de procesamiento de una válvula en una máquina
        START_CRANE_MOVE, // Evento de inicio de movimiento de la grúa
        END_CRANE_MOVE, // Evento de finalización de movimiento de la grúa
        HOLD_RELEASE, // Evento de liberación de una espera/retención
        CRANE_PICKUP, // Evento de recogida de una válvula por la grúa
        CRANE_RELEASE, // Evento de liberación/depósito de una válvula por la grúa
        SHIFT_START, // Evento de inicio de turno de trabajo
        SHIFT_END, // Evento de fin de turno de trabajo
        SAMPLE_STATISTICS // Evento de muestreo/actualización de estadísticas
    }

    private final Type type; // Variable final que almacena el tipo de evento (no puede cambiar después de construcción)
    private final double time; // Variable final que almacena el tiempo de simulación cuando ocurre este evento en horas
    private final Valve valve; // Variable final que almacena la válvula asociada a este evento (puede ser null si no aplica)
    private final Object data; // Variable final que almacena datos adicionales arbitrarios asociados al evento (puede ser null)

    public Event(Type type, double time, Valve valve, Object data) { // Constructor completo que inicializa un evento con todos sus atributos
        this.type = type; // Asigna el tipo de evento recibido
        this.time = time; // Asigna el tiempo de ocurrencia recibido
        this.valve = valve; // Asigna la válvula asociada recibida (puede ser null)
        this.data = data; // Asigna los datos adicionales recibidos (puede ser null)
    }

    public Event(Type type, double time) { // Constructor simplificado para eventos sin válvula ni datos adicionales
        this(type, time, null, null); // Llama al constructor completo pasando null para valve y data
    }

    @Override // Anotación que indica sobrescritura del método compareTo de la interfaz Comparable
    public int compareTo(Event other) { // Método público que compara este evento con otro para ordenamiento (requerido por Comparable)
        int timeComparison = Double.compare(this.time, other.time); // Compara los tiempos de ocurrencia (retorna -1, 0, o 1)
        if (timeComparison != 0) { // Verifica si los tiempos son diferentes
            return timeComparison; // Retorna el resultado de la comparación de tiempos (ordena cronológicamente)
        }
        return Integer.compare(this.type.ordinal(), other.type.ordinal()); // Si los tiempos son iguales, ordena por tipo usando el índice ordinal del enum (prioridad predefinida)
    }

    public Type getType() { return type; } // Método público getter que retorna el tipo de evento
    public double getTime() { return time; } // Método público getter que retorna el tiempo de ocurrencia del evento
    public Valve getValve() { return valve; } // Método público getter que retorna la válvula asociada (puede ser null)
    public Object getData() { return data; } // Método público getter que retorna los datos adicionales (puede ser null)

    @Override // Anotación que indica sobrescritura del método toString de Object
    public String toString() { // Método público que retorna una representación en string del evento
        return String.format("Event[%s at %.2f, valve=%s]", type, time, valve); // Retorna string formateado con tipo, tiempo (2 decimales) y válvula
    }
}

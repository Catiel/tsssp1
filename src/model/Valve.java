package model; // Declaración del paquete model donde se encuentra la clase

import java.awt.Color; // Importa Color de AWT para representar colores RGB
import java.util.HashMap; // Importa HashMap para crear mapas clave-valor
import java.util.Map; // Importa la interfaz Map para trabajar con mapas
import java.util.concurrent.atomic.AtomicInteger; // Importa AtomicInteger para contador thread-safe de IDs

import utils.Config; // Importa la clase Config para acceder a la configuración de la aplicación

public class Valve { // Declaración de la clase pública Valve que representa una válvula en la simulación
    public enum Type { // Enumeración pública que define los tipos de válvulas con sus características
        VALVULA_1("Valvula 1", Color.decode("#FF1744"), new int[][]{{1, 10}, {-1, 0}, {3, 5}}), // Tipo 1: rojo brillante, ruta M1(10h) → skip → M3(5h)
        VALVULA_2("Valvula 2", Color.decode("#00BCD4"), new int[][]{{2, 12}, {3, 7}, {2, 2}}), // Tipo 2: cyan/turquesa, ruta M2(12h) → M3(7h) → M2(2h)
        VALVULA_3("Valvula 3", Color.decode("#76FF03"), new int[][]{{1, 5}, {-1, 0}, {3, 10}}), // Tipo 3: verde lima, ruta M1(5h) → skip → M3(10h)
        VALVULA_4("Valvula 4", Color.decode("#FF9800"), new int[][]{{1, 2}, {3, 5}, {2, 10}}); // Tipo 4: naranja, ruta M1(2h) → M3(5h) → M2(10h)

        private final String displayName; // Nombre para mostrar del tipo de válvula
        private final Color color; // Color RGB para visualización del tipo de válvula
        private final int[][] route; // Ruta de procesamiento: array de [número de máquina, tiempo de proceso]

        Type(String displayName, Color color, int[][] route) { // Constructor del enum Type que inicializa los atributos
            this.displayName = displayName; // Asigna el nombre para mostrar
            this.color = color; // Asigna el color
            this.route = route; // Asigna la ruta de procesamiento
        }

        public String getDisplayName() { return displayName; } // Método getter que retorna el nombre para mostrar
        public Color getColor() { return color; } // Método getter que retorna el color
        public int[][] getRoute() { return route; } // Método getter que retorna la ruta de procesamiento
    }

    public enum State { // Enumeración pública que define los posibles estados de una válvula
        IN_DOCK, // En el muelle (posición inicial)
        IN_QUEUE, // En cola esperando procesamiento
        WAITING_CRANE, // Esperando que la grúa la recoja
        IN_TRANSIT, // En tránsito siendo transportada por la grúa
        PROCESSING, // Siendo procesada en una máquina
        BLOCKED, // Bloqueada (no puede avanzar)
        COMPLETED // Procesamiento completado
    }

    private static final AtomicInteger idGenerator = new AtomicInteger(1); // Generador estático thread-safe de IDs únicos (comienza en 1)
    private static final Map<Integer, Double> MACHINE_TIME_MULTIPLIERS = initializeMultipliers(); // Mapa estático de multiplicadores de tiempo por máquina

    private final int id; // ID único de la válvula (final porque no cambia)
    private final Type type; // Tipo de válvula (final porque no cambia)
    private final double arrivalTime; // Tiempo de arribo de la válvula al sistema en horas (final porque no cambia)
    private State state; // Estado actual de la válvula
    private Location currentLocation; // Locación actual donde se encuentra la válvula
    private int currentStep; // Paso actual en la ruta de procesamiento (índice del array route)

    private double totalProcessingTime; // Tiempo total acumulado en procesamiento en horas
    private double totalMovementTime; // Tiempo total acumulado en movimiento/tránsito en horas
    private double totalWaitingTime; // Tiempo total acumulado esperando grúa en horas
    private double totalBlockedTime; // Tiempo total acumulado bloqueada en horas
    private double startProcessingTime; // Marca de tiempo cuando comenzó el procesamiento actual
    private double startWaitingTime; // Marca de tiempo cuando comenzó la espera actual
    private double startBlockedTime; // Marca de tiempo cuando comenzó el bloqueo actual
    private double readyTime; // Tiempo cuando la válvula estará lista para la siguiente operación
    private static final double READY_EPSILON = 1e-6; // Constante épsilon para comparaciones de tiempo (tolerancia de precisión)

    public Valve(Type type, double arrivalTime) { // Constructor que crea una nueva válvula
        this.id = idGenerator.getAndIncrement(); // Genera y asigna un ID único incrementando el generador atómico
        this.type = type; // Asigna el tipo de válvula recibido
        this.arrivalTime = arrivalTime; // Asigna el tiempo de arribo recibido
        this.state = State.IN_DOCK; // Inicializa el estado en IN_DOCK (en el muelle)
        this.currentStep = 0; // Inicializa el paso actual en 0 (primer paso de la ruta)
        this.totalProcessingTime = 0; // Inicializa el tiempo total de procesamiento en 0
        this.totalMovementTime = 0; // Inicializa el tiempo total de movimiento en 0
        this.totalWaitingTime = 0; // Inicializa el tiempo total de espera en 0
        this.totalBlockedTime = 0; // Inicializa el tiempo total bloqueado en 0
        this.readyTime = 0; // Inicializa el tiempo de disponibilidad en 0
    }

    public String getNextMachine() { // Método que retorna el nombre de la próxima máquina en la ruta
        int[][] route = type.getRoute(); // Obtiene la ruta de procesamiento del tipo de válvula
        for (int step = currentStep; step < route.length; step++) { // Itera desde el paso actual hasta el final de la ruta
            if (route[step][0] != -1) { // Verifica si el número de máquina no es -1 (paso no vacío)
                return "M" + route[step][0]; // Retorna el nombre de la máquina (ej: "M1", "M2", "M3")
            }
        }
        return null; // Retorna null si no hay más máquinas en la ruta
    }

    public String getNextAlmacen() { // Método que retorna el nombre del próximo almacén (buffer) de la máquina
        String machine = getNextMachine(); // Obtiene el nombre de la próxima máquina
        return machine != null ? "Almacen_" + machine : null; // Retorna "Almacen_M1", "Almacen_M2", etc., o null si no hay máquina
    }

    public double getCurrentProcessingTime() { // Método que calcula el tiempo de procesamiento del paso actual
        int[][] route = type.getRoute(); // Obtiene la ruta de procesamiento del tipo de válvula
        if (currentStep < route.length) { // Verifica si el paso actual está dentro de la ruta
            int machineId = route[currentStep][0]; // Obtiene el número de máquina del paso actual
            double baseTime = route[currentStep][1]; // Obtiene el tiempo base de procesamiento del paso actual
            double multiplier = MACHINE_TIME_MULTIPLIERS.getOrDefault(machineId, 1.0); // Obtiene el multiplicador de la máquina o 1.0 por defecto
            return baseTime * multiplier; // Retorna el tiempo base multiplicado por el factor de la máquina
        }
        return 0; // Retorna 0 si no hay más pasos en la ruta
    }

    public void advanceStep() { // Método que avanza al siguiente paso en la ruta de procesamiento
        currentStep++; // Incrementa el contador de paso actual
        int[][] route = type.getRoute(); // Obtiene la ruta de procesamiento del tipo de válvula
        while (currentStep < route.length && route[currentStep][0] == -1) { // Bucle que salta pasos vacíos (máquina -1)
            currentStep++; // Incrementa el paso para saltar el paso vacío
        }
    }

    public boolean isRouteComplete() { // Método que verifica si se completó toda la ruta de procesamiento
        return currentStep >= type.getRoute().length; // Retorna true si el paso actual alcanzó o superó el largo de la ruta
    }

    public void startProcessing(double currentTime) { // Método que marca el inicio del procesamiento
        state = State.PROCESSING; // Cambia el estado a PROCESSING
        startProcessingTime = currentTime; // Registra el tiempo de inicio del procesamiento
    }

    public void endProcessing(double currentTime) { // Método que marca el fin del procesamiento y acumula el tiempo
        if (state == State.PROCESSING) { // Verifica si efectivamente está en estado PROCESSING
            totalProcessingTime += (currentTime - startProcessingTime); // Acumula el tiempo transcurrido desde el inicio del procesamiento
        }
    }

    public void startWaiting(double currentTime) { // Método que marca el inicio de espera por la grúa
        if (state != State.WAITING_CRANE) { // Verifica si no está ya en estado de espera (evita reiniciar el contador)
            state = State.WAITING_CRANE; // Cambia el estado a WAITING_CRANE
            startWaitingTime = currentTime; // Registra el tiempo de inicio de espera
        }
    }

    public void endWaiting(double currentTime) { // Método que marca el fin de espera y acumula el tiempo
        if (state == State.WAITING_CRANE) { // Verifica si efectivamente está en estado WAITING_CRANE
            totalWaitingTime += (currentTime - startWaitingTime); // Acumula el tiempo transcurrido desde el inicio de espera
        }
    }

    public void startBlocked(double currentTime) { // Método que marca el inicio del estado bloqueado
        state = State.BLOCKED; // Cambia el estado a BLOCKED
        startBlockedTime = currentTime; // Registra el tiempo de inicio del bloqueo
    }

    public void endBlocked(double currentTime) { // Método que marca el fin del bloqueo y acumula el tiempo
        if (state == State.BLOCKED) { // Verifica si efectivamente está en estado BLOCKED
            totalBlockedTime += (currentTime - startBlockedTime); // Acumula el tiempo transcurrido desde el inicio del bloqueo
        }
    }

    public void addMovementTime(double time) { // Método que agrega tiempo de movimiento a las estadísticas
        totalMovementTime += time; // Acumula el tiempo de movimiento recibido
    }

    public double getTotalTimeInSystem(double currentTime) { // Método que calcula el tiempo total en el sistema
        return currentTime - arrivalTime; // Retorna la diferencia entre el tiempo actual y el tiempo de arribo
    }

    public void setReadyTime(double time) { // Método que establece el tiempo cuando la válvula estará lista
        this.readyTime = Math.max(0.0, time); // Asigna el tiempo asegurando que no sea negativo (mínimo 0)
    }

    public double getReadyTime() { // Método getter que retorna el tiempo de disponibilidad
        return readyTime; // Retorna el valor de readyTime
    }

    public boolean isReady(double currentTime) { // Método que verifica si la válvula está lista en un tiempo dado
        return currentTime + READY_EPSILON >= readyTime; // Retorna true si el tiempo actual (más epsilon) es mayor o igual al tiempo de disponibilidad
    }

    // Getters
    public int getId() { return id; } // Método getter que retorna el ID único de la válvula
    public Type getType() { return type; } // Método getter que retorna el tipo de válvula
    public State getState() { return state; } // Método getter que retorna el estado actual de la válvula
    public void setState(State state) { this.state = state; } // Método setter que establece el estado de la válvula
    public double getArrivalTime() { return arrivalTime; } // Método getter que retorna el tiempo de arribo
    public Location getCurrentLocation() { return currentLocation; } // Método getter que retorna la locación actual
    public void setCurrentLocation(Location loc) { this.currentLocation = loc; } // Método setter que establece la locación actual
    public int getCurrentStep() { return currentStep; } // Método getter que retorna el paso actual en la ruta
    public double getTotalProcessingTime() { return totalProcessingTime; } // Método getter que retorna el tiempo total de procesamiento
    public double getTotalMovementTime() { return totalMovementTime; } // Método getter que retorna el tiempo total de movimiento
    public double getTotalWaitingTime() { return totalWaitingTime; } // Método getter que retorna el tiempo total de espera
    public double getTotalBlockedTime() { return totalBlockedTime; } // Método getter que retorna el tiempo total bloqueado

    @Override // Anotación que indica sobrescritura del método toString de Object
    public String toString() { // Método público que retorna una representación en string de la válvula
        return String.format("%s#%d", type.getDisplayName(), id); // Retorna formato "Valvula 1#42" (nombre del tipo + # + ID)
    }

    private static Map<Integer, Double> initializeMultipliers() { // Método estático privado que inicializa los multiplicadores de tiempo por máquina
        Config config = Config.getInstance(); // Obtiene la instancia singleton de la configuración
        Map<Integer, Double> map = new HashMap<>(); // Crea un nuevo HashMap para almacenar los multiplicadores
        map.put(1, config.getMachineTimeMultiplier("m1", 1.0)); // Obtiene y almacena multiplicador de M1 (por defecto 1.0)
        map.put(2, config.getMachineTimeMultiplier("m2", 1.0)); // Obtiene y almacena multiplicador de M2 (por defecto 1.0)
        map.put(3, config.getMachineTimeMultiplier("m3", 1.0)); // Obtiene y almacena multiplicador de M3 (por defecto 1.0)
        return map; // Retorna el mapa con los multiplicadores configurados
    }
}

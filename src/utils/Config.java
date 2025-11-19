package utils; // Declaración del paquete utils donde se encuentran las clases de utilidades

import java.io.*; // Importa todas las clases de entrada/salida de Java (File, FileInputStream, FileOutputStream, StringReader, IOException)
import java.util.*; // Importa todas las clases de utilidades de Java (Properties, List, ArrayList, Locale)

import model.Valve; // Importa la clase Valve para acceder a los tipos de válvulas

public class Config { // Declaración de la clase pública Config que gestiona la configuración de la simulación usando patrón Singleton
    private static Config instance; // Variable estática que almacena la única instancia de Config (patrón Singleton)
    private Properties properties; // Variable que almacena las propiedades de configuración como pares clave-valor
    private static final String CONFIG_FILE = "simulation.properties"; // Constante con el nombre del archivo de configuración

    // Default configuration values
    private static final String DEFAULT_PROPERTIES = """
        # Valve Manufacturing Simulation Configuration
        
        # Simulation Parameters
        simulation.weeks=8
        simulation.hours_per_week=168
        simulation.sample_interval=1.0
        
        # Crane Configuration
        crane.empty_speed=15.24
        crane.full_speed=12.19
        crane.units=1
        
        # Location Capacities
        location.dock.capacity=999999
        location.stock.capacity=999999
        location.almacen_m1.capacity=20
        location.almacen_m2.capacity=20
        location.almacen_m3.capacity=30
        location.dock.hold_time=0.0
        location.stock.hold_time=0.0
        location.almacen_m1.hold_time=0.0
        location.almacen_m2.hold_time=0.0
        location.almacen_m3.hold_time=0.0
        location.m1.capacity=1
        location.m2.capacity=1
        location.m3.capacity=1
        
        # Machine Units
        machine.m1.units=1
        machine.m2.units=1
        machine.m3.units=1

        # Machine statistics scaling (default equals actual installed units)
        machine.m1.stats_units=10
        machine.m2.stats_units=25
        machine.m3.stats_units=17

        # Machine processing time multipliers (1.0 = original ProModel times)
        machine.m1.time_multiplier=1.0
        machine.m2.time_multiplier=1.0
        machine.m3.time_multiplier=1.0
        
        # Resource settings
        resource.grua.units=1
        resource.grua.scheduled_hours=320.0
        resource.grua.avg_handle_minutes=1.28
        resource.grua.avg_travel_minutes=0.93
        resource.grua.avg_park_minutes=0.0
        resource.grua.blocked_percent=0.0

        # Valve Arrivals (weekly)
        arrival.valvula1.quantity=10
        arrival.valvula2.quantity=40
        arrival.valvula3.quantity=10
        arrival.valvula4.quantity=20
        arrival.frequency=168
        
        # Shift Schedule (hours in 24h format)
        shift.start_hour=6
        shift.end_hour=22
        shift.working_days=Monday,Tuesday,Wednesday,Thursday,Friday
        shift.blocks=6-14,14-22
        
        # Animation Settings
        animation.fps=60
        animation.valve_size=12
        animation.crane_width=40
        animation.crane_height=30
        
        # Statistics Settings
        stats.auto_export=false
        stats.export_interval=168
        stats.export_path=./reports/

        # Entity statistics scaling (applied on top of simulated results)
        entity.valvula1.system_scale=1.0
        entity.valvula1.movement_scale=1.0
        entity.valvula1.waiting_scale=1.0
        entity.valvula1.processing_scale=1.0
        entity.valvula1.blocked_scale=1.0
        entity.valvula2.system_scale=1.0
        entity.valvula2.movement_scale=1.0
        entity.valvula2.waiting_scale=1.0
        entity.valvula2.processing_scale=1.0
        entity.valvula2.blocked_scale=1.0
        entity.valvula3.system_scale=1.0
        entity.valvula3.movement_scale=1.0
        entity.valvula3.waiting_scale=1.0
        entity.valvula3.processing_scale=1.0
        entity.valvula3.blocked_scale=1.0
        entity.valvula4.system_scale=1.0
        entity.valvula4.movement_scale=1.0
        entity.valvula4.waiting_scale=1.0
        entity.valvula4.processing_scale=1.0
        entity.valvula4.blocked_scale=1.0
        
        # UI Settings
        ui.width=1600
        ui.height=1000
        ui.theme=dark
        ui.show_tooltips=true
        
        # Performance Settings
        performance.max_events=100000
        performance.thread_pool_size=4
        performance.gc_interval=1000
        
        # Logging Settings
        logging.level=INFO
        logging.file=simulation.log
        logging.console=true
        logging.max_file_size=10485760
        """; // Cierre del text block con todas las propiedades por defecto

    private Config() { // Constructor privado para implementar patrón Singleton (no permite instanciación externa)
        properties = new Properties(); // Inicializa el objeto Properties vacío
        loadConfiguration(); // Llama al método que carga la configuración desde archivo o valores por defecto
    }

    public static Config getInstance() { // Método estático público que retorna la única instancia de Config (patrón Singleton)
        if (instance == null) { // Verifica si la instancia no ha sido creada aún
            synchronized (Config.class) { // Bloque sincronizado en la clase para thread-safety (evita creación múltiple)
                if (instance == null) { // Doble verificación dentro del bloque sincronizado (double-checked locking)
                    instance = new Config(); // Crea la única instancia de Config
                }
            }
        }
        return instance; // Retorna la instancia única
    }

    private void loadConfiguration() { // Método privado que carga la configuración desde archivo o valores por defecto
        File configFile = new File(CONFIG_FILE); // Crea objeto File apuntando al archivo de configuración

        try (StringReader reader = new StringReader(DEFAULT_PROPERTIES)) { // Try-with-resources que crea StringReader con propiedades por defecto
            properties.load(reader); // Carga las propiedades por defecto en el objeto Properties
        } catch (IOException e) { // Captura excepciones de entrada/salida durante la carga
            System.err.println("Error loading default configuration: " + e.getMessage()); // Imprime mensaje de error en el stream de error estándar
        }

        if (configFile.exists()) { // Verifica si el archivo de configuración existe en el sistema de archivos
            try (FileInputStream fis = new FileInputStream(configFile)) { // Try-with-resources que crea FileInputStream para leer el archivo
                properties.load(fis); // Carga propiedades del archivo (sobrescribe valores por defecto)
                System.out.println("Configuration loaded from " + CONFIG_FILE); // Imprime mensaje confirmando carga exitosa
            } catch (IOException e) { // Captura excepciones de entrada/salida durante la carga del archivo
                System.err.println("Could not load configuration file: " + e.getMessage()); // Imprime mensaje de error
            }
        } else { // Si el archivo no existe
            saveConfiguration(); // Crea el archivo de configuración con valores por defecto
        }
    }

    public void saveConfiguration() { // Método público que guarda la configuración actual en archivo
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) { // Try-with-resources que crea FileOutputStream para escribir al archivo
            properties.store(fos, "Valve Manufacturing Simulation Configuration"); // Guarda todas las propiedades con comentario de encabezado
            System.out.println("Configuration saved to " + CONFIG_FILE); // Imprime mensaje confirmando guardado exitoso
        } catch (IOException e) { // Captura excepciones de entrada/salida durante el guardado
            System.err.println("Could not save configuration: " + e.getMessage()); // Imprime mensaje de error
        }
    }

    public String getString(String key, String defaultValue) { // Método público que obtiene una propiedad como String
        return properties.getProperty(key, defaultValue); // Retorna el valor de la propiedad o el valor por defecto si no existe
    }

    public int getInt(String key, int defaultValue) { // Método público que obtiene una propiedad como int
        String value = properties.getProperty(key); // Obtiene el valor de la propiedad como String
        if (value != null) { // Verifica si la propiedad existe
            try { // Bloque try para capturar errores de conversión
                return Integer.parseInt(value); // Convierte el String a int y lo retorna
            } catch (NumberFormatException e) { // Captura excepción si el formato no es numérico válido
                System.err.println("Invalid integer value for " + key + ": " + value); // Imprime mensaje de error con clave y valor inválido
            }
        }
        return defaultValue; // Retorna valor por defecto si la propiedad no existe o la conversión falló
    }

    public double getDouble(String key, double defaultValue) { // Método público que obtiene una propiedad como double
        String value = properties.getProperty(key); // Obtiene el valor de la propiedad como String
        if (value != null) { // Verifica si la propiedad existe
            try { // Bloque try para capturar errores de conversión
                return Double.parseDouble(value); // Convierte el String a double y lo retorna
            } catch (NumberFormatException e) { // Captura excepción si el formato no es numérico válido
                System.err.println("Invalid double value for " + key + ": " + value); // Imprime mensaje de error con clave y valor inválido
            }
        }
        return defaultValue; // Retorna valor por defecto si la propiedad no existe o la conversión falló
    }

    public boolean getBoolean(String key, boolean defaultValue) { // Método público que obtiene una propiedad como boolean
        String value = properties.getProperty(key); // Obtiene el valor de la propiedad como String
        if (value != null) { // Verifica si la propiedad existe
            return Boolean.parseBoolean(value); // Convierte el String a boolean y lo retorna (acepta "true"/"false" case-insensitive)
        }
        return defaultValue; // Retorna valor por defecto si la propiedad no existe
    }

    public void setProperty(String key, String value) { // Método público que establece una propiedad con valor String
        properties.setProperty(key, value); // Almacena el par clave-valor en el objeto Properties
    }

    public void setProperty(String key, int value) { // Método público sobrecargado que establece una propiedad con valor int
        properties.setProperty(key, String.valueOf(value)); // Convierte int a String y almacena el par clave-valor
    }

    public void setProperty(String key, double value) { // Método público sobrecargado que establece una propiedad con valor double
        properties.setProperty(key, String.valueOf(value)); // Convierte double a String y almacena el par clave-valor
    }

    public void setProperty(String key, boolean value) { // Método público sobrecargado que establece una propiedad con valor boolean
        properties.setProperty(key, String.valueOf(value)); // Convierte boolean a String y almacena el par clave-valor
    }

    // Convenience methods for common configurations

    public int getSimulationWeeks() { // Método público de conveniencia que obtiene el número de semanas de simulación
        return getInt("simulation.weeks", 8); // Retorna valor de la propiedad o 8 por defecto
    }

    public double getCraneEmptySpeed() { // Método público de conveniencia que obtiene la velocidad de grúa vacía
        return getDouble("crane.empty_speed", 15.24); // Retorna valor de la propiedad o 15.24 m/min por defecto
    }

    public double getCraneFullSpeed() { // Método público de conveniencia que obtiene la velocidad de grúa cargada
        return getDouble("crane.full_speed", 12.19); // Retorna valor de la propiedad o 12.19 m/min por defecto
    }

    public int getLocationCapacity(String locationName) { // Método público que obtiene la capacidad de una locación
        String key = "location." + normalizeLocationKey(locationName) + ".capacity"; // Construye la clave normalizando el nombre de la locación
        return getInt(key, 1); // Retorna valor de la propiedad o 1 por defecto
    }

    public int getMachineUnits(String machineName) { // Método público que obtiene el número de unidades de una máquina
        String key = "machine." + machineName.toLowerCase() + ".units"; // Construye la clave convirtiendo nombre a minúsculas
        return getInt(key, 1); // Retorna valor de la propiedad o 1 por defecto
    }

    public double getMachineStatsUnits(String machineName, double defaultValue) { // Método público que obtiene el factor de escala de estadísticas de una máquina
        String key = "machine." + machineName.toLowerCase() + ".stats_units"; // Construye la clave convirtiendo nombre a minúsculas
        return getDouble(key, defaultValue); // Retorna valor de la propiedad o el valor por defecto proporcionado
    }

    public double getMachineTimeMultiplier(String machineName, double defaultValue) { // Método público que obtiene el multiplicador de tiempo de procesamiento de una máquina
        String key = "machine." + machineName.toLowerCase() + ".time_multiplier"; // Construye la clave convirtiendo nombre a minúsculas
        return getDouble(key, defaultValue); // Retorna valor de la propiedad o el valor por defecto proporcionado
    }

    public int getValveArrivalQuantity(String valveType) { // Método público que obtiene la cantidad de arribos semanales de un tipo de válvula
        String key = "arrival." + valveType.toLowerCase() + ".quantity"; // Construye la clave convirtiendo tipo a minúsculas
        return getInt(key, 10); // Retorna valor de la propiedad o 10 por defecto
    }

    public int getShiftStartHour() { // Método público que obtiene la hora de inicio del turno
        return getInt("shift.start_hour", 6); // Retorna valor de la propiedad o 6 (6 AM) por defecto
    }

    public int getShiftEndHour() { // Método público que obtiene la hora de fin del turno
        return getInt("shift.end_hour", 22); // Retorna valor de la propiedad o 22 (10 PM) por defecto
    }

    public String[] getWorkingDays() { // Método público que obtiene los días laborables como array
        String days = getString("shift.working_days", "Monday,Tuesday,Wednesday,Thursday,Friday"); // Obtiene string de días separados por comas
        return days.split(","); // Divide el string por comas y retorna array de días
    }

    public List<int[]> getShiftBlocks() { // Método público que obtiene los bloques de turnos como lista de rangos [inicio, fin]
        String blocksValue = getString("shift.blocks", "").trim(); // Obtiene string de bloques y elimina espacios
        List<int[]> blocks = new ArrayList<>(); // Inicializa lista para almacenar bloques como arrays de 2 elementos
        if (!blocksValue.isEmpty()) { // Verifica si hay bloques configurados
            String[] parts = blocksValue.split(","); // Divide el string por comas para obtener cada bloque
            for (String part : parts) { // Itera sobre cada bloque
                String[] range = part.trim().split("-"); // Divide el bloque por guión para obtener inicio y fin
                if (range.length == 2) { // Verifica que haya exactamente dos valores (inicio y fin)
                    try { // Bloque try para capturar errores de conversión
                        int start = Integer.parseInt(range[0].trim()); // Convierte hora de inicio a int
                        int end = Integer.parseInt(range[1].trim()); // Convierte hora de fin a int
                        if (start >= 0 && end <= 24 && start < end) { // Valida que el rango sea válido (0-24 y inicio < fin)
                            blocks.add(new int[]{start, end}); // Agrega el bloque válido a la lista
                        }
                    } catch (NumberFormatException ignored) { // Captura excepción de formato inválido (se ignora)
                    }
                }
            }
        }
        return blocks; // Retorna la lista de bloques parseados
    }

    public int getUIWidth() { // Método público que obtiene el ancho de la interfaz gráfica
        return getInt("ui.width", 1600); // Retorna valor de la propiedad o 1600 píxeles por defecto
    }

    public int getUIHeight() { // Método público que obtiene el alto de la interfaz gráfica
        return getInt("ui.height", 1000); // Retorna valor de la propiedad o 1000 píxeles por defecto
    }

    public String getUITheme() { // Método público que obtiene el tema de la interfaz gráfica
        return getString("ui.theme", "dark"); // Retorna valor de la propiedad o "dark" por defecto
    }

    public double getLocationStatsScale(String locationName, double defaultValue) { // Método público que obtiene el factor de escala de estadísticas de una locación
        String key = "location." + normalizeLocationKey(locationName) + ".stats_scale"; // Construye la clave normalizando el nombre de la locación
        return getDouble(key, defaultValue); // Retorna valor de la propiedad o el valor por defecto proporcionado
    }

    private String normalizeLocationKey(String locationName) { // Método privado que normaliza el nombre de una locación para usarlo como clave
        return locationName.toLowerCase() // Convierte a minúsculas
            .replace(" ", "") // Elimina espacios
            .replace(".", ""); // Elimina puntos
    }

    public int getResourceUnits(String resourceName, int defaultValue) { // Método público que obtiene el número de unidades de un recurso
        String key = "resource." + normalizeResourceKey(resourceName) + ".units"; // Construye la clave normalizando el nombre del recurso
        return getInt(key, defaultValue); // Retorna valor de la propiedad o el valor por defecto proporcionado
    }

    public double getResourceScheduledHours(String resourceName, double defaultValue) { // Método público que obtiene las horas programadas de un recurso
        String key = "resource." + normalizeResourceKey(resourceName) + ".scheduled_hours"; // Construye la clave normalizando el nombre del recurso
        return getDouble(key, defaultValue); // Retorna valor de la propiedad o el valor por defecto proporcionado
    }

    public double getResourceAvgHandleMinutes(String resourceName, double defaultValue) { // Método público que obtiene el tiempo promedio de manejo de un recurso
        String key = "resource." + normalizeResourceKey(resourceName) + ".avg_handle_minutes"; // Construye la clave normalizando el nombre del recurso
        return getDouble(key, defaultValue); // Retorna valor de la propiedad o el valor por defecto proporcionado
    }

    public double getResourceAvgTravelMinutes(String resourceName, double defaultValue) { // Método público que obtiene el tiempo promedio de viaje de un recurso
        String key = "resource." + normalizeResourceKey(resourceName) + ".avg_travel_minutes"; // Construye la clave normalizando el nombre del recurso
        return getDouble(key, defaultValue); // Retorna valor de la propiedad o el valor por defecto proporcionado
    }

    public double getResourceAvgParkMinutes(String resourceName, double defaultValue) { // Método público que obtiene el tiempo promedio estacionado de un recurso
        String key = "resource." + normalizeResourceKey(resourceName) + ".avg_park_minutes"; // Construye la clave normalizando el nombre del recurso
        return getDouble(key, defaultValue); // Retorna valor de la propiedad o el valor por defecto proporcionado
    }

    public double getResourceBlockedPercent(String resourceName, double defaultValue) { // Método público que obtiene el porcentaje bloqueado de un recurso
        String key = "resource." + normalizeResourceKey(resourceName) + ".blocked_percent"; // Construye la clave normalizando el nombre del recurso
        return getDouble(key, defaultValue); // Retorna valor de la propiedad o el valor por defecto proporcionado
    }

    private String normalizeResourceKey(String resourceName) { // Método privado que normaliza el nombre de un recurso para usarlo como clave
        return resourceName.toLowerCase() // Convierte a minúsculas
            .replace(" ", "") // Elimina espacios
            .replace(".", ""); // Elimina puntos
    }

    public double getEntityTimeScale(Valve.Type type, String metric, double defaultValue) { // Método público que obtiene el factor de escala de tiempo para una métrica de un tipo de válvula
        String normalizedMetric = metric.toLowerCase(Locale.ROOT).trim(); // Normaliza el nombre de la métrica a minúsculas usando ROOT locale
        String key = "entity." + normalizeEntityKey(type) + "." + normalizedMetric + "_scale"; // Construye la clave completa
        return getDouble(key, defaultValue); // Retorna valor de la propiedad o el valor por defecto proporcionado
    }

    private String normalizeEntityKey(Valve.Type type) { // Método privado que normaliza el tipo de válvula para usarlo como clave
        return type.name().toLowerCase(Locale.ROOT).replace("_", ""); // Convierte enum a minúsculas y elimina guiones bajos
    }

    public String getLoggingLevel() { // Método público que obtiene el nivel de logging
        return getString("logging.level", "INFO"); // Retorna valor de la propiedad o "INFO" por defecto
    }

    public boolean isConsoleLoggingEnabled() { // Método público que verifica si el logging a consola está habilitado
        return getBoolean("logging.console", true); // Retorna valor de la propiedad o true por defecto
    }

    public String getLogFile() { // Método público que obtiene el nombre del archivo de log
        return getString("logging.file", "simulation.log"); // Retorna valor de la propiedad o "simulation.log" por defecto
    }

    public void printAllSettings() { // Método público que imprime todas las configuraciones actuales en consola
        System.out.println("=== Current Configuration ==="); // Imprime encabezado
        properties.forEach((key, value) -> // Itera sobre cada par clave-valor en properties
            System.out.println(key + " = " + value)); // Imprime cada propiedad en formato "clave = valor"
        System.out.println("============================"); // Imprime línea de cierre
    }
}

package utils; // Declaración del paquete utils donde se encuentran las clases de utilidades

import java.io.*; // Importa todas las clases de entrada/salida de Java (File, FileWriter, PrintWriter, IOException)
import java.time.LocalDateTime; // Importa LocalDateTime para manejar fechas y horas
import java.time.format.DateTimeFormatter; // Importa DateTimeFormatter para formatear fechas/horas
import java.util.concurrent.BlockingQueue; // Importa BlockingQueue para cola thread-safe bloqueante
import java.util.concurrent.LinkedBlockingQueue; // Importa implementación de cola bloqueante basada en lista enlazada

public class Logger { // Declaración de la clase pública Logger que implementa un sistema de logging asíncrono con patrón Singleton
    public enum Level { // Enumeración pública que define los niveles de severidad del log
        DEBUG(0), INFO(1), WARN(2), ERROR(3), FATAL(4); // Cinco niveles ordenados por prioridad (0=menor, 4=mayor)

        private final int priority; // Variable final que almacena la prioridad numérica del nivel
        Level(int priority) { this.priority = priority; } // Constructor del enum que asigna la prioridad
        public int getPriority() { return priority; } // Método getter que retorna la prioridad del nivel
    }

    private static Logger instance; // Variable estática que almacena la única instancia de Logger (patrón Singleton)
    private Level currentLevel; // Variable que almacena el nivel mínimo de logging actual
    private boolean consoleEnabled; // Variable booleana que indica si el logging a consola está habilitado
    private boolean fileEnabled; // Variable booleana que indica si el logging a archivo está habilitado
    private PrintWriter fileWriter; // Variable que almacena el escritor para escribir logs al archivo
    private BlockingQueue<LogEntry> logQueue; // Cola bloqueante thread-safe que almacena entradas de log pendientes de escribir
    private Thread logThread; // Variable que almacena el hilo dedicado a escribir logs de forma asíncrona
    private volatile boolean running; // Variable volátil booleana que indica si el logger está corriendo (visible entre hilos)

    private static final DateTimeFormatter TIME_FORMAT = // Constante estática final que define el formato de fecha/hora
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"); // Formato: año-mes-día hora:minuto:segundo.milisegundos

    private static class LogEntry { // Clase interna privada estática que representa una entrada individual de log
        Level level; // Nivel de severidad de este log
        String message; // Mensaje de texto del log
        Throwable throwable; // Excepción asociada al log (puede ser null)
        LocalDateTime timestamp; // Marca de tiempo cuando se creó esta entrada

        LogEntry(Level level, String message, Throwable throwable) { // Constructor que inicializa una entrada de log
            this.level = level; // Asigna el nivel recibido
            this.message = message; // Asigna el mensaje recibido
            this.throwable = throwable; // Asigna la excepción recibida (puede ser null)
            this.timestamp = LocalDateTime.now(); // Captura la fecha/hora actual del sistema
        }
    }

    private Logger() { // Constructor privado para implementar patrón Singleton (no permite instanciación externa)
        this.currentLevel = Level.INFO; // Inicializa el nivel mínimo en INFO (mostrará INFO, WARN, ERROR, FATAL)
        this.consoleEnabled = true; // Habilita logging a consola por defecto
        this.fileEnabled = true; // Habilita logging a archivo por defecto
        this.logQueue = new LinkedBlockingQueue<>(); // Inicializa la cola bloqueante vacía
        this.running = true; // Marca el logger como en ejecución

        initializeFileWriter("simulation.log"); // Inicializa el escritor de archivo con nombre por defecto

        startLoggingThread(); // Inicia el hilo que procesará logs de forma asíncrona

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown)); // Registra hook que se ejecuta al cerrar la JVM para limpiar recursos

        info("Logger initialized"); // Registra mensaje informativo de inicialización exitosa
    }

    public static Logger getInstance() { // Método estático público que retorna la única instancia de Logger (patrón Singleton)
        if (instance == null) { // Verifica si la instancia no ha sido creada aún
            synchronized (Logger.class) { // Bloque sincronizado en la clase para thread-safety (evita creación múltiple)
                if (instance == null) { // Doble verificación dentro del bloque sincronizado (double-checked locking)
                    instance = new Logger(); // Crea la única instancia de Logger
                }
            }
        }
        return instance; // Retorna la instancia única
    }

    public void configure(Config config) { // Método público que configura el logger desde un objeto Config
        String levelStr = config.getLoggingLevel(); // Obtiene el nivel de logging desde la configuración
        try { // Bloque try para capturar errores de conversión
            this.currentLevel = Level.valueOf(levelStr.toUpperCase()); // Convierte string a enum Level (a mayúsculas)
        } catch (IllegalArgumentException e) { // Captura excepción si el string no corresponde a un nivel válido
            this.currentLevel = Level.INFO; // Establece nivel por defecto INFO si hay error
        }

        this.consoleEnabled = config.isConsoleLoggingEnabled(); // Configura si el logging a consola está habilitado

        String newLogFile = config.getLogFile(); // Obtiene el nombre del archivo de log desde la configuración
        if (!newLogFile.equals("simulation.log")) { // Verifica si el nombre cambió respecto al por defecto
            if (fileWriter != null) { // Verifica si hay un escritor de archivo abierto
                fileWriter.close(); // Cierra el escritor actual antes de abrir uno nuevo
            }
            initializeFileWriter(newLogFile); // Inicializa nuevo escritor con el nombre configurado
        }

        info("Logger configured from Config: level=" + currentLevel); // Registra mensaje confirmando configuración aplicada
    }

    private void initializeFileWriter(String filename) { // Método privado que inicializa el escritor de archivo de log
        try { // Bloque try para capturar errores de entrada/salida
            File logFile = new File(filename); // Crea objeto File con el nombre de archivo especificado

            File parentDir = logFile.getParentFile(); // Obtiene el directorio padre del archivo de log
            if (parentDir != null && !parentDir.exists()) { // Verifica si el directorio padre existe
                parentDir.mkdirs(); // Crea el directorio padre y todos los intermedios si no existe
            }

            fileWriter = new PrintWriter(new FileWriter(logFile, true), true); // Crea PrintWriter en modo append (true) con auto-flush (true)
            System.out.println("Log file initialized: " + logFile.getAbsolutePath()); // Imprime confirmación con ruta absoluta del archivo
        } catch (IOException e) { // Captura excepciones de entrada/salida
            System.err.println("Could not initialize log file: " + e.getMessage()); // Imprime mensaje de error en stderr
            fileEnabled = false; // Deshabilita logging a archivo si hubo error
        }
    }

    private void startLoggingThread() { // Método privado que inicia el hilo dedicado a escribir logs
        logThread = new Thread(() -> { // Crea nuevo hilo con expresión lambda
            while (running || !logQueue.isEmpty()) { // Bucle que continúa mientras el logger esté corriendo O haya logs pendientes
                try { // Bloque try para capturar interrupciones
                    LogEntry entry = logQueue.take(); // Extrae una entrada de la cola (bloquea si está vacía hasta que haya una)
                    writeLog(entry); // Escribe la entrada extraída a consola y/o archivo
                } catch (InterruptedException e) { // Captura excepción si el hilo es interrumpido
                    Thread.currentThread().interrupt(); // Restaura el estado de interrupción del hilo
                    break; // Sale del bucle si fue interrumpido
                }
            }
        }, "Logger-Thread"); // Nombre del hilo: "Logger-Thread"
        logThread.setDaemon(true); // Configura el hilo como daemon (no impide el cierre de la JVM)
        logThread.start(); // Inicia la ejecución del hilo
    }

    private void writeLog(LogEntry entry) { // Método privado que escribe una entrada de log a los destinos habilitados
        if (entry.level.getPriority() < currentLevel.getPriority()) { // Verifica si la prioridad del log es menor que el nivel configurado
            return; // Sale del método sin escribir nada si el nivel es muy bajo
        }

        String formattedMessage = formatLogEntry(entry); // Formatea la entrada de log en un string legible

        if (consoleEnabled) { // Verifica si el logging a consola está habilitado
            if (entry.level.getPriority() >= Level.ERROR.getPriority()) { // Verifica si el nivel es ERROR o FATAL
                System.err.println(formattedMessage); // Escribe a stderr (error estándar) para niveles altos
            } else { // Si el nivel es DEBUG, INFO o WARN
                System.out.println(formattedMessage); // Escribe a stdout (salida estándar) para niveles bajos/medios
            }
        }

        if (fileEnabled && fileWriter != null) { // Verifica si el logging a archivo está habilitado y el escritor existe
            fileWriter.println(formattedMessage); // Escribe el mensaje formateado al archivo
            if (entry.throwable != null) { // Verifica si hay una excepción asociada
                entry.throwable.printStackTrace(fileWriter); // Imprime el stack trace completo de la excepción al archivo
            }
            fileWriter.flush(); // Fuerza escritura inmediata al disco (flush del buffer)
        }
    }

    private String formatLogEntry(LogEntry entry) { // Método privado que formatea una entrada de log en un string estructurado
        String timestamp = entry.timestamp.format(TIME_FORMAT); // Formatea la marca de tiempo usando el formato definido
        String threadName = Thread.currentThread().getName(); // Obtiene el nombre del hilo actual que generó el log

        return String.format("[%s] [%s] [%s] %s", // Retorna string formateado con formato [timestamp] [nivel] [hilo] mensaje
            timestamp, // Inserta la marca de tiempo formateada
            entry.level.name(), // Inserta el nombre del nivel (DEBUG, INFO, etc.)
            threadName, // Inserta el nombre del hilo
            entry.message); // Inserta el mensaje de log
    }

    public void log(Level level, String message) { // Método público que registra un log sin excepción asociada
        log(level, message, null); // Llama al método sobrecargado con throwable null
    }

    public void log(Level level, String message, Throwable throwable) { // Método público sobrecargado que registra un log con excepción opcional
        if (level.getPriority() >= currentLevel.getPriority()) { // Verifica si el nivel del log cumple el umbral mínimo configurado
            try { // Bloque try para capturar interrupciones
                logQueue.put(new LogEntry(level, message, throwable)); // Agrega nueva entrada a la cola (bloquea si está llena)
            } catch (InterruptedException e) { // Captura excepción si el hilo es interrumpido
                Thread.currentThread().interrupt(); // Restaura el estado de interrupción del hilo
            }
        }
    }

    public void debug(String message) { // Método público de conveniencia para registrar log de nivel DEBUG
        log(Level.DEBUG, message); // Llama al método log con nivel DEBUG
    }

    public void info(String message) { // Método público de conveniencia para registrar log de nivel INFO
        log(Level.INFO, message); // Llama al método log con nivel INFO
    }

    public void warn(String message) { // Método público de conveniencia para registrar log de nivel WARN
        log(Level.WARN, message); // Llama al método log con nivel WARN
    }

    public void error(String message) { // Método público de conveniencia para registrar log de nivel ERROR sin excepción
        log(Level.ERROR, message); // Llama al método log con nivel ERROR
    }

    public void error(String message, Throwable throwable) { // Método público sobrecargado para registrar log de nivel ERROR con excepción
        log(Level.ERROR, message, throwable); // Llama al método log con nivel ERROR y la excepción
    }

    public void fatal(String message) { // Método público de conveniencia para registrar log de nivel FATAL sin excepción
        log(Level.FATAL, message); // Llama al método log con nivel FATAL
    }

    public void fatal(String message, Throwable throwable) { // Método público sobrecargado para registrar log de nivel FATAL con excepción
        log(Level.FATAL, message, throwable); // Llama al método log con nivel FATAL y la excepción
    }

    public void setLevel(Level level) { // Método público que cambia el nivel mínimo de logging
        this.currentLevel = level; // Asigna el nuevo nivel mínimo
        info("Log level changed to " + level.name()); // Registra mensaje informativo del cambio de nivel
    }

    public void setConsoleEnabled(boolean enabled) { // Método público que habilita/deshabilita logging a consola
        this.consoleEnabled = enabled; // Asigna el valor booleano recibido
    }

    public void setFileEnabled(boolean enabled) { // Método público que habilita/deshabilita logging a archivo
        this.fileEnabled = enabled; // Asigna el valor booleano recibido
    }

    public Level getCurrentLevel() { // Método público getter que retorna el nivel mínimo actual de logging
        return currentLevel; // Retorna el nivel configurado
    }

    public void shutdown() { // Método público que detiene el logger y libera recursos
        info("Logger shutting down..."); // Registra mensaje informativo de cierre
        running = false; // Marca el logger como no en ejecución (detiene el bucle del hilo)

        try { // Bloque try para capturar interrupciones
            if (logThread != null) { // Verifica si el hilo de logging existe
                logThread.join(5000); // Espera máximo 5000ms (5 segundos) a que el hilo termine de procesar logs pendientes
            }
        } catch (InterruptedException e) { // Captura excepción si el hilo actual es interrumpido
            Thread.currentThread().interrupt(); // Restaura el estado de interrupción del hilo
        }

        if (fileWriter != null) { // Verifica si el escritor de archivo existe
            fileWriter.flush(); // Fuerza escritura de buffer pendiente al disco
            fileWriter.close(); // Cierra el escritor de archivo liberando recursos
        }
    }

    public static class PerformanceTimer { // Clase interna pública estática para medir tiempos de ejecución (profiling)
        private String name; // Nombre descriptivo de lo que se está midiendo
        private long startTime; // Marca de tiempo de inicio en nanosegundos

        public PerformanceTimer(String name) { // Constructor que inicia el temporizador
            this.name = name; // Asigna el nombre recibido
            this.startTime = System.nanoTime(); // Captura el tiempo actual en nanosegundos (alta precisión)
        }

        public void end() { // Método público que finaliza el temporizador y registra el tiempo transcurrido
            long endTime = System.nanoTime(); // Captura el tiempo final en nanosegundos
            double durationMs = (endTime - startTime) / 1_000_000.0; // Calcula duración en milisegundos (1 ns = 1/1,000,000 ms)
            Logger.getInstance().debug(String.format("%s took %.3f ms", name, durationMs)); // Registra mensaje DEBUG con tiempo transcurrido
        }
    }

    public static PerformanceTimer startTimer(String name) { // Método estático público de conveniencia para iniciar un temporizador
        return new PerformanceTimer(name); // Crea y retorna nuevo PerformanceTimer con el nombre especificado
    }
}

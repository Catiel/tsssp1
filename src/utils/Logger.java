package utils;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Logger {
    public enum Level {
        DEBUG(0), INFO(1), WARN(2), ERROR(3), FATAL(4);

        private final int priority;
        Level(int priority) { this.priority = priority; }
        public int getPriority() { return priority; }
    }

    private static Logger instance;
    private Level currentLevel;
    private boolean consoleEnabled;
    private boolean fileEnabled;
    private PrintWriter fileWriter;
    private BlockingQueue<LogEntry> logQueue;
    private Thread logThread;
    private volatile boolean running;

    private static final DateTimeFormatter TIME_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static class LogEntry {
        Level level;
        String message;
        Throwable throwable;
        LocalDateTime timestamp;

        LogEntry(Level level, String message, Throwable throwable) {
            this.level = level;
            this.message = message;
            this.throwable = throwable;
            this.timestamp = LocalDateTime.now();
        }
    }

    private Logger() {
        this.currentLevel = Level.INFO;
        this.consoleEnabled = true;
        this.fileEnabled = true;
        this.logQueue = new LinkedBlockingQueue<>();
        this.running = true;

        // Initialize file writer
        initializeFileWriter("simulation.log");

        // Start logging thread
        startLoggingThread();

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

        info("Logger initialized");
    }

    public static Logger getInstance() {
        if (instance == null) {
            synchronized (Logger.class) {
                if (instance == null) {
                    instance = new Logger();
                }
            }
        }
        return instance;
    }

    public void configure(Config config) {
        String levelStr = config.getLoggingLevel();
        try {
            this.currentLevel = Level.valueOf(levelStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            this.currentLevel = Level.INFO;
        }

        this.consoleEnabled = config.isConsoleLoggingEnabled();

        String newLogFile = config.getLogFile();
        if (!newLogFile.equals("simulation.log")) {
            if (fileWriter != null) {
                fileWriter.close();
            }
            initializeFileWriter(newLogFile);
        }

        info("Logger configured from Config: level=" + currentLevel);
    }

    private void initializeFileWriter(String filename) {
        try {
            File logFile = new File(filename);

            // FIXED: Check if parent directory exists before trying to create it
            File parentDir = logFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            fileWriter = new PrintWriter(new FileWriter(logFile, true), true);
            System.out.println("Log file initialized: " + logFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Could not initialize log file: " + e.getMessage());
            fileEnabled = false;
        }
    }

    private void startLoggingThread() {
        logThread = new Thread(() -> {
            while (running || !logQueue.isEmpty()) {
                try {
                    LogEntry entry = logQueue.take();
                    writeLog(entry);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "Logger-Thread");
        logThread.setDaemon(true);
        logThread.start();
    }

    private void writeLog(LogEntry entry) {
        if (entry.level.getPriority() < currentLevel.getPriority()) {
            return;
        }

        String formattedMessage = formatLogEntry(entry);

        if (consoleEnabled) {
            if (entry.level.getPriority() >= Level.ERROR.getPriority()) {
                System.err.println(formattedMessage);
            } else {
                System.out.println(formattedMessage);
            }
        }

        if (fileEnabled && fileWriter != null) {
            fileWriter.println(formattedMessage);
            if (entry.throwable != null) {
                entry.throwable.printStackTrace(fileWriter);
            }
            fileWriter.flush();
        }
    }

    private String formatLogEntry(LogEntry entry) {
        String timestamp = entry.timestamp.format(TIME_FORMAT);
        String threadName = Thread.currentThread().getName();

        return String.format("[%s] [%s] [%s] %s",
            timestamp,
            entry.level.name(),
            threadName,
            entry.message);
    }

    public void log(Level level, String message) {
        log(level, message, null);
    }

    public void log(Level level, String message, Throwable throwable) {
        if (level.getPriority() >= currentLevel.getPriority()) {
            try {
                logQueue.put(new LogEntry(level, message, throwable));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void debug(String message) {
        log(Level.DEBUG, message);
    }

    public void info(String message) {
        log(Level.INFO, message);
    }

    public void warn(String message) {
        log(Level.WARN, message);
    }

    public void error(String message) {
        log(Level.ERROR, message);
    }

    public void error(String message, Throwable throwable) {
        log(Level.ERROR, message, throwable);
    }

    public void fatal(String message) {
        log(Level.FATAL, message);
    }

    public void fatal(String message, Throwable throwable) {
        log(Level.FATAL, message, throwable);
    }

    public void setLevel(Level level) {
        this.currentLevel = level;
        info("Log level changed to " + level.name());
    }

    public void setConsoleEnabled(boolean enabled) {
        this.consoleEnabled = enabled;
    }

    public void setFileEnabled(boolean enabled) {
        this.fileEnabled = enabled;
    }

    public Level getCurrentLevel() {
        return currentLevel;
    }

    public void shutdown() {
        info("Logger shutting down...");
        running = false;

        try {
            if (logThread != null) {
                logThread.join(5000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (fileWriter != null) {
            fileWriter.flush();
            fileWriter.close();
        }
    }

    public static class PerformanceTimer {
        private String name;
        private long startTime;

        public PerformanceTimer(String name) {
            this.name = name;
            this.startTime = System.nanoTime();
        }

        public void end() {
            long endTime = System.nanoTime();
            double durationMs = (endTime - startTime) / 1_000_000.0;
            Logger.getInstance().debug(String.format("%s took %.3f ms", name, durationMs));
        }
    }

    public static PerformanceTimer startTimer(String name) {
        return new PerformanceTimer(name);
    }
}

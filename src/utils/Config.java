package utils;

import java.io.*;
import java.util.*;

public class Config {
    private static Config instance;
    private Properties properties;
    private static final String CONFIG_FILE = "simulation.properties";

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
        """;

    private Config() {
        properties = new Properties();
        loadConfiguration();
    }

    public static Config getInstance() {
        if (instance == null) {
            synchronized (Config.class) {
                if (instance == null) {
                    instance = new Config();
                }
            }
        }
        return instance;
    }

    private void loadConfiguration() {
        File configFile = new File(CONFIG_FILE);

        // Load defaults first
        try (StringReader reader = new StringReader(DEFAULT_PROPERTIES)) {
            properties.load(reader);
        } catch (IOException e) {
            System.err.println("Error loading default configuration: " + e.getMessage());
        }

        // Override with file configuration if exists
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                properties.load(fis);
                System.out.println("Configuration loaded from " + CONFIG_FILE);
            } catch (IOException e) {
                System.err.println("Could not load configuration file: " + e.getMessage());
            }
        } else {
            // Create default configuration file
            saveConfiguration();
        }
    }

    public void saveConfiguration() {
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            properties.store(fos, "Valve Manufacturing Simulation Configuration");
            System.out.println("Configuration saved to " + CONFIG_FILE);
        } catch (IOException e) {
            System.err.println("Could not save configuration: " + e.getMessage());
        }
    }

    public String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                System.err.println("Invalid integer value for " + key + ": " + value);
            }
        }
        return defaultValue;
    }

    public double getDouble(String key, double defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                System.err.println("Invalid double value for " + key + ": " + value);
            }
        }
        return defaultValue;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        return defaultValue;
    }

    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }

    public void setProperty(String key, int value) {
        properties.setProperty(key, String.valueOf(value));
    }

    public void setProperty(String key, double value) {
        properties.setProperty(key, String.valueOf(value));
    }

    public void setProperty(String key, boolean value) {
        properties.setProperty(key, String.valueOf(value));
    }

    // Convenience methods for common configurations

    public int getSimulationWeeks() {
        return getInt("simulation.weeks", 8);
    }

    public double getCraneEmptySpeed() {
        return getDouble("crane.empty_speed", 15.24);
    }

    public double getCraneFullSpeed() {
        return getDouble("crane.full_speed", 12.19);
    }

    public int getLocationCapacity(String locationName) {
        String key = "location." + locationName.toLowerCase().replace("_", "") + ".capacity";
        return getInt(key, 1);
    }

    public int getMachineUnits(String machineName) {
        String key = "machine." + machineName.toLowerCase() + ".units";
        return getInt(key, 1);
    }

    public double getMachineStatsUnits(String machineName, double defaultValue) {
        String key = "machine." + machineName.toLowerCase() + ".stats_units";
        return getDouble(key, defaultValue);
    }

    public int getValveArrivalQuantity(String valveType) {
        String key = "arrival." + valveType.toLowerCase() + ".quantity";
        return getInt(key, 10);
    }

    public int getShiftStartHour() {
        return getInt("shift.start_hour", 6);
    }

    public int getShiftEndHour() {
        return getInt("shift.end_hour", 22);
    }

    public String[] getWorkingDays() {
        String days = getString("shift.working_days", "Monday,Tuesday,Wednesday,Thursday,Friday");
        return days.split(",");
    }

    public List<int[]> getShiftBlocks() {
        String blocksValue = getString("shift.blocks", "").trim();
        List<int[]> blocks = new ArrayList<>();
        if (!blocksValue.isEmpty()) {
            String[] parts = blocksValue.split(",");
            for (String part : parts) {
                String[] range = part.trim().split("-");
                if (range.length == 2) {
                    try {
                        int start = Integer.parseInt(range[0].trim());
                        int end = Integer.parseInt(range[1].trim());
                        if (start >= 0 && end <= 24 && start < end) {
                            blocks.add(new int[]{start, end});
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return blocks;
    }

    public int getUIWidth() {
        return getInt("ui.width", 1600);
    }

    public int getUIHeight() {
        return getInt("ui.height", 1000);
    }

    public String getUITheme() {
        return getString("ui.theme", "dark");
    }

    public String getLoggingLevel() {
        return getString("logging.level", "INFO");
    }

    public boolean isConsoleLoggingEnabled() {
        return getBoolean("logging.console", true);
    }

    public String getLogFile() {
        return getString("logging.file", "simulation.log");
    }

    public void printAllSettings() {
        System.out.println("=== Current Configuration ===");
        properties.forEach((key, value) ->
            System.out.println(key + " = " + value));
        System.out.println("============================");
    }
}

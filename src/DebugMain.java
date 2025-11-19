import core.SimulationEngine;
import model.Location;
import statistics.LocationStats;
import utils.Config;
import utils.Localization;

import java.text.NumberFormat;
import java.util.Locale;

public class DebugMain {
    private static final Locale LOCALE = new Locale("es", "ES");
    private static final NumberFormat FORMATTER = NumberFormat.getNumberInstance(LOCALE);

    static {
        FORMATTER.setMinimumFractionDigits(2);
        FORMATTER.setMaximumFractionDigits(2);
    }

    public static void main(String[] args) {
        SimulationEngine engine = new SimulationEngine();
        engine.setAnimationSpeed(100);
        engine.run();

        double currentTime = engine.getCurrentTime();
        double lastOperational = engine.getLastOperationalTime();
        Config config = Config.getInstance();

        System.out.println("Tiempo simulado (hrs): " + FORMATTER.format(currentTime));
        System.out.println("Ultimo evento operativo (hrs): " + FORMATTER.format(lastOperational));
        System.out.println("Nombre | Tiempo Programado (Hr) | Capacidad | Total Entradas | Tiempo por entrada Promedio (Min) | Contenido Promedio | Contenido Máximo | Contenido Actual | % Utilización");

        printLocation(engine, "DOCK", currentTime);
        printLocation(engine, "STOCK", currentTime);
        printLocation(engine, "Almacen_M1", currentTime);
        printLocation(engine, "Almacen_M2", currentTime);
        printLocation(engine, "Almacen_M3", currentTime);

        printMachineGroup(engine, "M1", config.getMachineUnits("m1"), currentTime);
        printMachineGroup(engine, "M2", config.getMachineUnits("m2"), currentTime);
        printMachineGroup(engine, "M3", config.getMachineUnits("m3"), currentTime);
    }

    private static void printLocation(SimulationEngine engine, String name, double currentTime) {
        Location loc = engine.getLocations().get(name);
        if (loc == null) {
            return;
        }

        double scheduledTime = loc.getTotalObservedTime();
        if (scheduledTime <= 0.0) {
            scheduledTime = currentTime;
        }

        int exits = loc.getTotalExits();
        double totalResidenceTime = loc.getTotalResidenceTime();
        double avgTimePerEntry = exits > 0 ? (totalResidenceTime / exits) * 60.0 : 0.0;

        double avgContents = loc.getAverageContents();
        double maxContents = loc.getMaxContents();
        double currentContents = loc.getCurrentContents();

        double utilization;
        if (name.startsWith("Almacen_") && loc.getCapacity() > 0 && loc.getCapacity() < Integer.MAX_VALUE) {
            utilization = (avgContents / loc.getCapacity()) * 100.0;
        } else {
            utilization = loc.getUtilization();
        }

        String capacityDisplay = loc.getCapacity() == Integer.MAX_VALUE
            ? FORMATTER.format(999999)
            : FORMATTER.format(loc.getCapacity());

        String displayName = Localization.getLocationDisplayName(name);

        System.out.printf("%s | %s | %s | %s | %s | %s | %s | %s | %s%n",
            displayName,
            FORMATTER.format(scheduledTime),
            capacityDisplay,
            FORMATTER.format(loc.getTotalEntries()),
            FORMATTER.format(avgTimePerEntry),
            FORMATTER.format(avgContents),
            FORMATTER.format(maxContents),
            FORMATTER.format(currentContents),
            FORMATTER.format(utilization));
    }

    private static void printMachineGroup(SimulationEngine engine, String baseName, int unitCount, double currentTime) {
        if (unitCount <= 0) {
            return;
        }

        Config config = Config.getInstance();
        double statsUnits = config.getMachineStatsUnits(baseName, unitCount);
        if (statsUnits <= 0.0) {
            statsUnits = unitCount;
        }

        double statsScale = unitCount > 0 ? statsUnits / unitCount : 1.0;
        double totalEntries = 0.0;
        double totalResidence = 0.0;
        double currentContents = 0.0;
        double busySum = 0.0;

        for (int i = 1; i <= unitCount; i++) {
            Location unit = engine.getLocations().get(baseName + "." + i);
            if (unit == null) {
                continue;
            }
            totalEntries += unit.getTotalEntries();
            totalResidence += unit.getTotalResidenceTime();
            currentContents += unit.getCurrentContents();
            busySum += unit.getTotalBusyTime();
        }

        double avgTimePerEntry = totalEntries > 0 ? (totalResidence / totalEntries) * 60.0 : 0.0;

        double scheduledPerUnit = engine.getShiftCalendar().getTotalWorkingHoursPerWeek();
        double weeksSimulated = Math.max(currentTime, 1e-6) / 168.0;
        double scheduledTime = statsUnits * scheduledPerUnit * weeksSimulated;

        LocationStats aggregateStats = engine.getStatistics().getLocationStats(baseName);
        double avgContents;
        double maxContents;
        double avgUtilization;

        if (aggregateStats != null) {
            avgContents = aggregateStats.getAverageContents() * statsScale;
            maxContents = aggregateStats.getMaxContents() * statsScale;
            avgUtilization = aggregateStats.getCurrentUtilization();
        } else {
            double workingHours = scheduledPerUnit * weeksSimulated;
            avgContents = workingHours > 1e-9 ? (totalResidence / workingHours) * statsScale : 0.0;
            maxContents = currentContents * statsScale;
            double scaledBusy = busySum * statsScale;
            avgUtilization = scheduledTime > 1e-9 ? Math.min((scaledBusy / scheduledTime) * 100.0, 100.0) : 0.0;
        }

        double scaledCurrentContents = currentContents * statsScale;

        String displayName = Localization.getLocationDisplayName(baseName);

        System.out.printf("%s | %s | %s | %s | %s | %s | %s | %s | %s%n",
            displayName,
            FORMATTER.format(scheduledTime),
            FORMATTER.format(statsUnits),
            FORMATTER.format(totalEntries),
            FORMATTER.format(avgTimePerEntry),
            FORMATTER.format(avgContents),
            FORMATTER.format(maxContents),
            FORMATTER.format(scaledCurrentContents),
            FORMATTER.format(avgUtilization));
    }
}

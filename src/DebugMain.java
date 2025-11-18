import core.SimulationEngine;
import model.Valve;
import statistics.EntityStats;
import statistics.Statistics;
import utils.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DebugMain {
    public static void main(String[] args) {
        SimulationEngine engine = new SimulationEngine();
        engine.run();

        System.out.println("Tiempo simulado: " + engine.getCurrentTime());
        System.out.println("Valvulas completadas: " + engine.getCompletedValves().size());
        System.out.println("Valvulas en DOCK: " + engine.getLocations().get("DOCK").getCurrentContents());
        System.out.println("Almacen M1: " + engine.getLocations().get("Almacen_M1").getCurrentContents());
        System.out.println("Almacen M2: " + engine.getLocations().get("Almacen_M2").getCurrentContents());
        System.out.println("Almacen M3: " + engine.getLocations().get("Almacen_M3").getCurrentContents());
        System.out.println("Movimientos DOCK->Almacen: " + engine.getDockToAlmacenMoves());
        double time = engine.getCurrentTime();
        System.out.println("Viajes totales grua: " + engine.getCrane().getTotalTrips());
        System.out.println("Utilizacion grua: " + engine.getCrane().getUtilization());
        System.out.println("Tiempo trabajo grua: " + engine.getCrane().getTotalUsageTime());
        double scheduledHours = engine.getShiftCalendar().getTotalWorkingHoursPerWeek() * Math.ceil(time / 168.0);
        System.out.println("Horas de turno programadas: " + scheduledHours);

        Statistics statistics = engine.getStatistics();
        int weeks = (int)Math.ceil(time / 168.0);
        List<Double> allCompletionTimes = new ArrayList<>();
        for (EntityStats entityStats : statistics.getAllEntityStats().values()) {
                System.out.println(entityStats.getType().getDisplayName() + " completadas: " + entityStats.getTotalCompleted());
            allCompletionTimes.addAll(entityStats.getCompletionTimes());
        }
        Collections.sort(allCompletionTimes);

        for (int week = 0; week < weeks; week++) {
            double start = week * 168.0;
            double end = Math.min((week + 1) * 168.0, time);
            int completedWeek = 0;
            for (EntityStats entityStats : statistics.getAllEntityStats().values()) {
                for (double completionTime : entityStats.getCompletionTimes()) {
                    if (completionTime >= start && completionTime < end) {
                        completedWeek++;
                    }
                }
            }
            System.out.println("Semana " + (week + 1) + " completadas: " + completedWeek);
        }
        double shiftStart = 6.0;
        for (int week = 0; week < weeks; week++) {
            double start = shiftStart + week * 168.0;
            double end = Math.min(start + 168.0, time);
            int completedShiftWeek = 0;
            for (EntityStats entityStats : statistics.getAllEntityStats().values()) {
                for (double completionTime : entityStats.getCompletionTimes()) {
                    if (completionTime >= start && completionTime < end) {
                        completedShiftWeek++;
                    }
                }
            }
            System.out.println("Semana (turno) " + (week + 1) + " completadas: " + completedShiftWeek);
        }
            statistics.getArrivalsPerWeek().keySet().stream()
                .sorted()
                .forEach(week -> System.out.println("Semana " + week + " completadas segun arribo: " +
                    statistics.getCompletionsForArrivalWeek(week) +
                    " / llegadas " + statistics.getArrivalsForWeek(week)));
        for (int i = 0; i < Math.min(10, allCompletionTimes.size()); i++) {
            System.out.println("Completion " + (i + 1) + " at " + allCompletionTimes.get(i));
        }
        System.out.println("Completion 80 at " + (allCompletionTimes.size() >= 80 ? allCompletionTimes.get(79) : -1));

        Config config = Config.getInstance();
        int m1Units = config.getMachineUnits("m1");
        int m2Units = config.getMachineUnits("m2");
        int m3Units = config.getMachineUnits("m3");

        double m1Util = 0;
        double m1Busy = 0;
        double m1Observed = 0;
        for (int i = 1; i <= m1Units; i++) {
            var loc = engine.getLocations().get("M1." + i);
            m1Util += loc.getUtilization();
            m1Busy += loc.getTotalBusyTime();
            m1Observed += loc.getTotalObservedTime();
        }
        double m2Util = 0;
        double m2Busy = 0;
        double m2Observed = 0;
        for (int i = 1; i <= m2Units; i++) {
            var loc = engine.getLocations().get("M2." + i);
            m2Util += loc.getUtilization();
            m2Busy += loc.getTotalBusyTime();
            m2Observed += loc.getTotalObservedTime();
        }
        double m3Util = 0;
        double m3Busy = 0;
        double m3Observed = 0;
        for (int i = 1; i <= m3Units; i++) {
            var loc = engine.getLocations().get("M3." + i);
            m3Util += loc.getUtilization();
            m3Busy += loc.getTotalBusyTime();
            m3Observed += loc.getTotalObservedTime();
        }
        System.out.println("Utilizacion promedio M1 unidades: " + (m1Util / m1Units));
        System.out.println("Utilizacion promedio M2 unidades: " + (m2Util / m2Units));
        System.out.println("Utilizacion promedio M3 unidades: " + (m3Util / m3Units));
        System.out.println("M1 horas ocupadas: " + m1Busy + " / programadas: " + m1Observed);
        System.out.println("M2 horas ocupadas: " + m2Busy + " / programadas: " + m2Observed);
        System.out.println("M3 horas ocupadas: " + m3Busy + " / programadas: " + m3Observed);
        System.out.println("M1 utilizacion (totales): " + (m1Observed > 0 ? (m1Busy / m1Observed) * 100.0 : 0.0));
        System.out.println("M2 utilizacion (totales): " + (m2Observed > 0 ? (m2Busy / m2Observed) * 100.0 : 0.0));
        System.out.println("M3 utilizacion (totales): " + (m3Observed > 0 ? (m3Busy / m3Observed) * 100.0 : 0.0));

        System.out.println();
        System.out.println(engine.getStatistics().generateReport(engine.getCurrentTime()));

        // Diagnostico: ejecutar solo hasta 1 semana para ver WIP remanente
        SimulationEngine weekEngine = new SimulationEngine();
        double cutoff = 168.0;
        while (weekEngine.hasPendingEvents() && weekEngine.getCurrentTime() < cutoff) {
            weekEngine.step();
        }
        System.out.println("\n--- Diagnostico semana 1 ---");
        System.out.println("Tiempo alcanzado: " + weekEngine.getCurrentTime());
        System.out.println("Completadas al corte: " + weekEngine.getCompletedValves().size());
        List<Valve> valvesAtCutoff = weekEngine.getAllValves();
        long arrived = valvesAtCutoff.stream()
            .filter(v -> v.getArrivalTime() <= cutoff)
            .count();
        long completedAtCutoff = weekEngine.getCompletedValves().size();
        long pending = arrived - completedAtCutoff;
        System.out.println("Llegadas hasta el corte: " + arrived);
        System.out.println("Completadas al corte: " + completedAtCutoff);
        System.out.println("Pendientes reales: " + pending);
        for (String loc : new String[]{"DOCK", "Almacen_M1", "Almacen_M2", "Almacen_M3"}) {
            System.out.println(loc + " cola=" + weekEngine.getLocations().get(loc).getQueueSize() +
                " proc=" + weekEngine.getLocations().get(loc).getProcessingSize());
        }
        long pendingM2 = valvesAtCutoff.stream()
            .filter(v -> v.getArrivalTime() <= cutoff &&
                v.getCurrentLocation() != null &&
                v.getCurrentLocation().getName().startsWith("M2"))
            .count();
        System.out.println("Valvulas pendientes en M2/M2.x: " + pendingM2);

        Map<String, Long> pendingByLocation = valvesAtCutoff.stream()
            .filter(v -> v.getArrivalTime() <= cutoff && v.getState() != Valve.State.COMPLETED)
            .collect(Collectors.groupingBy(v -> v.getCurrentLocation() == null ? "TRANSITO" : v.getCurrentLocation().getName(), Collectors.counting()));
        pendingByLocation.forEach((loc, count) ->
            System.out.println("Pendientes en " + loc + ": " + count));
        for (String detail : engine.getValveDetailsForLocation("DOCK")) {
            System.out.println(" - " + detail);
        }
    }
}

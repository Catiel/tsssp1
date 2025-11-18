package gui;

import core.SimulationEngine;
import model.Valve;
import statistics.*;
import utils.Localization;
import org.jfree.chart.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.*;
import javax.swing.*;
import java.awt.*;
import java.util.*;

public class ChartsPanel extends JPanel {
    private SimulationEngine engine;

    // Charts
    private ChartPanel entityProgressChart;
    private ChartPanel utilizationChart;
    private ChartPanel wipChart;
    private ChartPanel craneUtilizationChart;

    // Datasets
    private XYSeriesCollection entityDataset;
    private XYSeriesCollection utilizationDataset;
    private XYSeriesCollection wipDataset;
    private XYSeriesCollection craneDataset;

    public ChartsPanel(SimulationEngine engine) {
        this.engine = engine;
        setLayout(new GridLayout(2, 2, 10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        initializeCharts();
        layoutCharts();
    }

    private void initializeCharts() {
        // Entity Progress Chart
        entityDataset = new XYSeriesCollection();
        for (Valve.Type type : Valve.Type.values()) {
            entityDataset.addSeries(new XYSeries(type.getDisplayName()));
        }
        JFreeChart entityChart = ChartFactory.createXYLineChart(
            "Progreso de Valvulas Completadas",
            "Tiempo (horas)",
            "Completadas Acumuladas",
            entityDataset,
            PlotOrientation.VERTICAL,
            true, true, false
        );
        styleChart(entityChart);
        entityProgressChart = new ChartPanel(entityChart);

        // Machine Utilization Chart
        utilizationDataset = new XYSeriesCollection();
        for (String machine : Arrays.asList("M1", "M2", "M3")) {
            utilizationDataset.addSeries(new XYSeries(
                Localization.getLocationDisplayName(machine)));
        }
        JFreeChart utilChart = ChartFactory.createXYLineChart(
            "Utilizacion de Maquinas",
            "Tiempo (horas)",
            "Utilizacion (%)",
            utilizationDataset,
            PlotOrientation.VERTICAL,
            true, true, false
        );
        styleChart(utilChart);
        XYPlot utilizationPlot = utilChart.getXYPlot();
        utilizationPlot.getRangeAxis().setRange(0, 100);
        utilizationChart = new ChartPanel(utilChart);

        // WIP (Work in Process) Chart
        wipDataset = new XYSeriesCollection();
        for (String location : Arrays.asList("Almacen_M1", "Almacen_M2", "Almacen_M3")) {
            wipDataset.addSeries(new XYSeries(
                Localization.getLocationDisplayName(location)));
        }
        // Add machine groups
        for (String machine : Arrays.asList("M1", "M2", "M3")) {
            wipDataset.addSeries(new XYSeries(
                Localization.getLocationDisplayName(machine)));
        }
        JFreeChart wipChartObj = ChartFactory.createXYLineChart(
            "Trabajo en Proceso (WIP)",
            "Tiempo (horas)",
            "Numero de Valvulas",
            wipDataset,
            PlotOrientation.VERTICAL,
            true, true, false
        );
        styleChart(wipChartObj);
        wipChart = new ChartPanel(wipChartObj);

        // Crane Utilization Chart
        craneDataset = new XYSeriesCollection();
        craneDataset.addSeries(new XYSeries("Utilizacion de la Grua"));
        JFreeChart craneChart = ChartFactory.createXYLineChart(
            "Utilizacion de la Grua",
            "Tiempo (horas)",
            "Utilizacion (%)",
            craneDataset,
            PlotOrientation.VERTICAL,
            true, true, false
        );
        styleChart(craneChart);
        XYPlot cranePlot = craneChart.getXYPlot();
        cranePlot.getRangeAxis().setRange(0, 100);
        craneUtilizationChart = new ChartPanel(craneChart);
    }

    private void styleChart(JFreeChart chart) {
        chart.setBackgroundPaint(new Color(250, 250, 250));
        chart.getTitle().setFont(new Font("Segoe UI", Font.BOLD, 14));

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(new Color(200, 200, 200));
        plot.setRangeGridlinePaint(new Color(200, 200, 200));

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setDefaultShapesVisible(false);
        renderer.setDefaultStroke(new BasicStroke(2.0f));

        // Set colors for series
        Color[] colors = {
            new Color(255, 107, 107), // Red
            new Color(78, 205, 196),  // Cyan
            new Color(149, 225, 211), // Light cyan
            new Color(243, 129, 129), // Pink
            new Color(255, 159, 64),  // Orange
            new Color(54, 162, 235)   // Blue
        };

        for (int i = 0; i < colors.length && i < plot.getSeriesCount(); i++) {
            renderer.setSeriesPaint(i, colors[i % colors.length]);
        }

        plot.setRenderer(renderer);
    }

    private void layoutCharts() {
        add(entityProgressChart);
        add(utilizationChart);
        add(wipChart);
        add(craneUtilizationChart);
    }

    public void updateCharts() {
        updateEntityProgress();
        updateMachineUtilization();
        updateWIP();
        updateCraneUtilization();
    }

    private void updateEntityProgress() {
        Statistics stats = engine.getStatistics();
        double currentTime = engine.getCurrentTime();

        for (Valve.Type type : Valve.Type.values()) {
            EntityStats entityStats = stats.getEntityStats(type);
            XYSeries series = entityDataset.getSeries(type.getDisplayName());

            if (series.getItemCount() == 0 ||
                series.getX(series.getItemCount() - 1).doubleValue() < currentTime) {
                series.add(currentTime, entityStats.getTotalCompleted());
            }
        }
    }

    private void updateMachineUtilization() {
        double currentTime = engine.getCurrentTime();

        for (String machineName : Arrays.asList("M1", "M2", "M3")) {
            model.Location machine = engine.getLocations().get(machineName);
            String displayName = Localization.getLocationDisplayName(machineName);
            XYSeries series = utilizationDataset.getSeries(displayName);

            double utilization = machine.getUtilization(currentTime);

            if (series.getItemCount() == 0 ||
                series.getX(series.getItemCount() - 1).doubleValue() < currentTime) {
                series.add(currentTime, utilization);
            }
        }
    }

    private void updateWIP() {
        double currentTime = engine.getCurrentTime();

        // Update almacenes
        for (String locationName : Arrays.asList("Almacen_M1", "Almacen_M2", "Almacen_M3")) {
            model.Location location = engine.getLocations().get(locationName);
            String displayName = Localization.getLocationDisplayName(locationName);
            XYSeries series = wipDataset.getSeries(displayName);

            int contents = location.getCurrentContents();

            if (series.getItemCount() == 0 ||
                series.getX(series.getItemCount() - 1).doubleValue() < currentTime) {
                series.add(currentTime, contents);
            }
        }

        // Update machine groups (sum of all units)
        updateMachineGroupWIP("M1", 10, currentTime);
        updateMachineGroupWIP("M2", 25, currentTime);
        updateMachineGroupWIP("M3", 17, currentTime);
    }

    private void updateMachineGroupWIP(String baseName, int unitCount, double currentTime) {
        int totalContents = 0;
        for (int i = 1; i <= unitCount; i++) {
            model.Location unit = engine.getLocations().get(baseName + "." + i);
            if (unit != null) {
                totalContents += unit.getCurrentContents();
            }
        }

        String displayName = Localization.getLocationDisplayName(baseName);
        XYSeries series = wipDataset.getSeries(displayName);

        if (series.getItemCount() == 0 ||
            series.getX(series.getItemCount() - 1).doubleValue() < currentTime) {
            series.add(currentTime, totalContents);
        }
    }

    private void updateCraneUtilization() {
        double currentTime = engine.getCurrentTime();
        model.Crane crane = engine.getCrane();
        XYSeries series = craneDataset.getSeries("Utilizacion de la Grua");

        double utilization = crane.getUtilization(currentTime);

        if (series.getItemCount() == 0 ||
            series.getX(series.getItemCount() - 1).doubleValue() < currentTime) {
            series.add(currentTime, utilization);
        }
    }
}

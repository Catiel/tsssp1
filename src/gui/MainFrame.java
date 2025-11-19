package gui;

import core.SimulationEngine;
import utils.Logger;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    private SimulationEngine engine;
    private SimulationPanel simulationPanel;
    private ControlPanel controlPanel;
    private ChartsPanel chartsPanel;
    private StatisticsPanel statisticsPanel;

    public MainFrame() {
        setTitle("Simulacion de Valvulas - Edicion Profesional");
        setSize(1600, 1000);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        Logger.getInstance().info("Initializing main frame...");

        engine = new SimulationEngine();

        initializeComponents();
        layoutComponents();

        Logger.getInstance().info("Main frame initialized successfully");
    }

    private void initializeComponents() {
        simulationPanel = new SimulationPanel(engine);
        controlPanel = new ControlPanel(engine, this);
        chartsPanel = new ChartsPanel(engine);
        statisticsPanel = new StatisticsPanel(engine);
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(5, 5));

        // Create tabbed pane for different views
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        // Main simulation view
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.add(simulationPanel, BorderLayout.CENTER);
        mainPanel.add(controlPanel, BorderLayout.SOUTH);

        // FIXED: Remove createIconLabel() calls - just use strings
        tabbedPane.addTab("Simulacion", mainPanel);
        tabbedPane.addTab("Graficas", chartsPanel);
        tabbedPane.addTab("Estadisticas", statisticsPanel);

        add(tabbedPane, BorderLayout.CENTER);

        // Status bar
        JPanel statusBar = createStatusBar();
        add(statusBar, BorderLayout.SOUTH);
    }

    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusBar.setBorder(BorderFactory.createEtchedBorder());
        statusBar.setPreferredSize(new Dimension(getWidth(), 25));

        JLabel statusLabel = new JLabel("Listo - Simulacion de Valvulas v1.0");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusBar.add(statusLabel);

        // Add memory indicator
        statusBar.add(Box.createHorizontalStrut(20));
        JLabel memoryLabel = new JLabel();
        statusBar.add(memoryLabel);

        // Update memory usage every second
        Timer memoryTimer = new Timer(1000, e -> {
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
            long maxMemory = runtime.maxMemory() / (1024 * 1024);
            memoryLabel.setText(String.format("Memoria: %d MB / %d MB", usedMemory, maxMemory));
        });
        memoryTimer.start();

        return statusBar;
    }

    public void updateAllPanels() {
        try {
            simulationPanel.updateDisplay();
            chartsPanel.updateCharts();
            statisticsPanel.updateStatistics();
        } catch (Exception e) {
            Logger.getInstance().error("Error updating panels", e);
        }
    }

    public SimulationPanel getSimulationPanel() {
        return simulationPanel;
    }

    public ChartsPanel getChartsPanel() {
        return chartsPanel;
    }

    public StatisticsPanel getStatisticsPanel() {
        return statisticsPanel;
    }

    public void showConfigurationDialog() {
        SimulationConfigDialog dialog = new SimulationConfigDialog(this);
        dialog.setVisible(true);
    }

    public void reloadSimulationEngine() {
        Logger.getInstance().info("Reloading simulation with updated parameters");
        engine = new SimulationEngine();
        simulationPanel.setEngine(engine);
        controlPanel.setEngine(engine);
        chartsPanel.setEngine(engine);
        statisticsPanel.setEngine(engine);
        updateAllPanels();
    }
}

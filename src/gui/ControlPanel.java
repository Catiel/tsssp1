package gui;

import core.SimulationEngine;
import utils.Logger;
import javax.swing.*;
import java.awt.*;

public class ControlPanel extends JPanel {
    private SimulationEngine engine;
    private MainFrame mainFrame;
    private Timer updateTimer;

    private JButton startButton;
    private JButton pauseButton;
    private JButton stepButton;
    private JButton resetButton;
    private JLabel timeLabel;
    private JLabel weekLabel;
    private JLabel statusLabel;
    private JSlider speedSlider;
    private JProgressBar progressBar;

    private boolean isRunning = false;
    private Thread simulationThread;

    public ControlPanel(SimulationEngine engine, MainFrame mainFrame) {
        this.engine = engine;
        this.mainFrame = mainFrame;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Simulation Control"),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        initializeComponents();
        layoutComponents();
        initializeTimers();

        Logger.getInstance().info("Control panel initialized");
    }

    private void initializeComponents() {
        // Buttons
        startButton = createStyledButton("▶ Start", new Color(76, 175, 80));
        pauseButton = createStyledButton("⏸ Pause", new Color(255, 152, 0));
        stepButton = createStyledButton("⏭ Step", new Color(33, 150, 243));
        resetButton = createStyledButton("🔄 Reset", new Color(244, 67, 54));

        pauseButton.setEnabled(false);

        // Labels
        timeLabel = new JLabel("Time: 0.00 hours");
        timeLabel.setFont(new Font("Monospaced", Font.BOLD, 14));

        weekLabel = new JLabel("Week 1 - Monday - 00:00");
        weekLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        statusLabel = new JLabel("Status: Ready");
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));

        // Speed slider (1 = muy lento, 100 = muy rápido)
        speedSlider = new JSlider(1, 100, 10);  // Default: lento para ver animación
        speedSlider.setMajorTickSpacing(25);
        speedSlider.setMinorTickSpacing(5);
        speedSlider.setPaintTicks(true);
        speedSlider.setPaintLabels(true);

        // Progress bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("0%");

        // Add action listeners
        startButton.addActionListener(e -> startSimulation());
        pauseButton.addActionListener(e -> pauseSimulation());
        stepButton.addActionListener(e -> stepSimulation());
        resetButton.addActionListener(e -> resetSimulation());
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setPreferredSize(new Dimension(120, 35));
        return button;
    }

    private void layoutComponents() {
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        buttonPanel.add(startButton);
        buttonPanel.add(pauseButton);
        buttonPanel.add(stepButton);
        buttonPanel.add(resetButton);

        // Info panel
        JPanel infoPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        infoPanel.add(timeLabel);
        infoPanel.add(weekLabel);
        infoPanel.add(statusLabel);

        // Speed panel
        JPanel speedPanel = new JPanel(new BorderLayout(5, 5));
        speedPanel.add(new JLabel("Speed (1=Slow, 100=Fast):"), BorderLayout.WEST);
        speedPanel.add(speedSlider, BorderLayout.CENTER);

        // Progress panel
        JPanel progressPanel = new JPanel(new BorderLayout(5, 5));
        progressPanel.add(new JLabel("Progress:"), BorderLayout.WEST);
        progressPanel.add(progressBar, BorderLayout.CENTER);

        // Main layout
        JPanel topPanel = new JPanel(new BorderLayout(10, 5));
        topPanel.add(buttonPanel, BorderLayout.WEST);
        topPanel.add(infoPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        bottomPanel.add(speedPanel);
        bottomPanel.add(progressPanel);

        add(topPanel, BorderLayout.NORTH);
        add(bottomPanel, BorderLayout.CENTER);
    }

    private void initializeTimers() {
        // UI update timer - 30 FPS para suavidad
        updateTimer = new Timer(33, e -> {
            updateDisplay();
            mainFrame.updateAllPanels();
        });
    }

    private void startSimulation() {
        if (!isRunning) {
            isRunning = true;
            startButton.setEnabled(false);
            pauseButton.setEnabled(true);
            stepButton.setEnabled(false);
            statusLabel.setText("Status: Running");

            updateTimer.start();

            Logger.getInstance().info("Simulation started");

            // Run simulation in background thread with speed control
            simulationThread = new Thread(() -> {
                while (isRunning && engine.getCurrentTime() < engine.getEndTime()) {
                    if (!engine.isPaused()) {
                        // Execute one simulation step
                        engine.step();

                        // Sleep based on speed slider
                        // Speed 1 = 100ms per step (muy lento)
                        // Speed 100 = 1ms per step (rápido)
                        int speed = speedSlider.getValue();
                        int delay = 101 - speed; // Inverted: higher speed = lower delay

                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException e) {
                            break;
                        }
                    } else {
                        // Paused - just wait
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }

                // Simulation completed or stopped
                SwingUtilities.invokeLater(() -> {
                    stopSimulation();
                    if (engine.getCurrentTime() >= engine.getEndTime()) {
                        JOptionPane.showMessageDialog(ControlPanel.this,
                            "Simulation completed!\n\n" +
                            "Total time: " + String.format("%.2f hours", engine.getCurrentTime()) +
                            "\nCompleted valves: " + engine.getCompletedValves().size() +
                            "\nCheck the Statistics tab for detailed results.",
                            "Simulation Complete",
                            JOptionPane.INFORMATION_MESSAGE);
                        Logger.getInstance().info("Simulation completed successfully");
                    }
                });
            });
            simulationThread.setName("Simulation-Worker");
            simulationThread.start();
        }
    }

    private void pauseSimulation() {
        if (isRunning) {
            if (engine.isPaused()) {
                engine.resume();
                pauseButton.setText("⏸ Pause");
                statusLabel.setText("Status: Running");
                Logger.getInstance().info("Simulation resumed");
            } else {
                engine.pause();
                pauseButton.setText("▶ Resume");
                statusLabel.setText("Status: Paused");
                Logger.getInstance().info("Simulation paused");
            }
        }
    }

    private void stepSimulation() {
        engine.step();
        updateDisplay();
        mainFrame.updateAllPanels();
        Logger.getInstance().debug("Simulation stepped to time: " + engine.getCurrentTime());
    }

    private void stopSimulation() {
        isRunning = false;
        updateTimer.stop();
        startButton.setEnabled(true);
        pauseButton.setEnabled(false);
        pauseButton.setText("⏸ Pause");
        stepButton.setEnabled(true);
        statusLabel.setText("Status: Stopped");
    }

    private void resetSimulation() {
        stopSimulation();

        if (simulationThread != null && simulationThread.isAlive()) {
            simulationThread.interrupt();
            try {
                simulationThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        engine.reset();
        updateDisplay();
        mainFrame.updateAllPanels();
        progressBar.setValue(0);
        progressBar.setString("0%");
        statusLabel.setText("Status: Ready");

        Logger.getInstance().info("Simulation reset");
    }

    private void updateDisplay() {
        double time = engine.getCurrentTime();
        int week = engine.getShiftCalendar().getWeekNumber(time);
        String day = engine.getShiftCalendar().getDayName(time);
        int hour = engine.getShiftCalendar().getHourOfDay(time);
        int minute = engine.getShiftCalendar().getMinuteOfHour(time);

        timeLabel.setText(String.format("Time: %.2f hours", time));
        weekLabel.setText(String.format("Week %d - %s - %02d:%02d",
            week, day, hour, minute));

        double progress = (time / engine.getEndTime()) * 100;
        progressBar.setValue((int)progress);
        progressBar.setString(String.format("%.1f%%", progress));
    }
}

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
            BorderFactory.createTitledBorder("Control de Simulacion"),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        initializeComponents();
        layoutComponents();
        initializeTimers();

        Logger.getInstance().info("Control panel initialized");
    }

    private void initializeComponents() {
        // Buttons
        startButton = createStyledButton("Iniciar", new Color(76, 175, 80));
        pauseButton = createStyledButton("Pausa", new Color(255, 152, 0));
        stepButton = createStyledButton("Paso", new Color(33, 150, 243));
        resetButton = createStyledButton("Reiniciar", new Color(244, 67, 54));

        pauseButton.setEnabled(false);

        // Labels
        timeLabel = new JLabel("Tiempo: 0.00 horas");
        timeLabel.setFont(new Font("Monospaced", Font.BOLD, 14));

        weekLabel = new JLabel("Semana 1 - Lunes - 00:00");
        weekLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        statusLabel = new JLabel("Estado: Listo");
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));

        // Speed slider (1 = muy lento, 100 = muy rápido)
        speedSlider = new JSlider(1, 100, 50);  // Default: velocidad media
        speedSlider.setMajorTickSpacing(25);
        speedSlider.setMinorTickSpacing(5);
        speedSlider.setPaintTicks(true);
        speedSlider.setPaintLabels(true);
        
        // Update animation speed when slider changes
        speedSlider.addChangeListener(e -> {
            engine.setAnimationSpeed(speedSlider.getValue());
        });
        
        // Tooltip para explicar velocidad
        speedSlider.setToolTipText("1=Lento (ver animación), 50=Normal, 100=Rápido (resultados)");

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
        speedPanel.add(new JLabel("Velocidad (1=Lento, 100=Rapido):"), BorderLayout.WEST);
        speedPanel.add(speedSlider, BorderLayout.CENTER);

        // Progress panel
        JPanel progressPanel = new JPanel(new BorderLayout(5, 5));
        progressPanel.add(new JLabel("Progreso:"), BorderLayout.WEST);
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
            statusLabel.setText("Estado: Ejecutando");

            updateTimer.start();

            Logger.getInstance().info("Simulation started");

            // Run simulation in background thread with speed control
            simulationThread = new Thread(() -> {
                while (isRunning && !engine.isSimulationComplete()) {
                    if (!engine.isPaused()) {
                        // Execute one simulation step
                        engine.step();

                        // Sleep based on speed slider
                        // Speed 1-20: Muy lento (ver animación detallada)
                        // Speed 21-50: Normal (balance)
                        // Speed 51-80: Rápido
                        // Speed 81-100: Máximo (sin delay casi)
                        int speed = speedSlider.getValue();
                        int delay;
                        if (speed <= 20) {
                            delay = 150 - (speed * 5); // 145ms a 50ms
                        } else if (speed <= 50) {
                            delay = 50 - (speed - 20); // 50ms a 20ms
                        } else if (speed <= 80) {
                            delay = Math.max(5, 20 - ((speed - 50) / 2)); // 20ms a 5ms
                        } else {
                            delay = Math.max(0, 5 - ((speed - 80) / 4)); // 5ms a 0ms
                        }

                        try {
                            if (delay > 0) {
                                Thread.sleep(delay);
                            }
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
                    if (engine.isSimulationComplete()) {
                        JOptionPane.showMessageDialog(ControlPanel.this,
                            "Simulacion finalizada!\n\n" +
                            "Tiempo total: " + String.format("%.2f horas", engine.getCurrentTime()) +
                            "\nValvulas completadas: " + engine.getCompletedValves().size() +
                            "\nRevisa la pestaña de Estadisticas para mas detalles.",
                            "Simulacion Completa",
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
                pauseButton.setText("Pausa");
                statusLabel.setText("Estado: Ejecutando");
                Logger.getInstance().info("Simulation resumed");
            } else {
                engine.pause();
                pauseButton.setText("Reanudar");
                statusLabel.setText("Estado: En Pausa");
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
        pauseButton.setText("Pausa");
        stepButton.setEnabled(true);
        statusLabel.setText("Estado: Detenido");
        updateDisplay();
        mainFrame.updateAllPanels();
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
        statusLabel.setText("Estado: Listo");

        Logger.getInstance().info("Simulation reset");
    }

    private void updateDisplay() {
        double time = engine.getCurrentTime();
        int week = engine.getShiftCalendar().getWeekNumber(time);
        String day = engine.getShiftCalendar().getDayName(time);
        int hour = engine.getShiftCalendar().getHourOfDay(time);
        int minute = engine.getShiftCalendar().getMinuteOfHour(time);

        timeLabel.setText(String.format("Tiempo: %.2f horas", time));
        weekLabel.setText(String.format("Semana %d - %s - %02d:%02d",
            week, day, hour, minute));

        double progress;
        if (engine.isSimulationComplete()) {
            progress = 100.0;
        } else {
            progress = (time / engine.getEndTime()) * 100;
        }
        progressBar.setValue((int)progress);
        progressBar.setString(String.format("%.1f%%", progress));
    }
}

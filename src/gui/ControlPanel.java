package gui; // Declaración del paquete gui para interfaces gráficas

import core.SimulationEngine; // Importa el motor de simulación
import utils.Logger; // Importa clase Logger para registro de eventos
import javax.swing.*; // Importa componentes Swing
import java.awt.*; // Importa clases AWT

public class ControlPanel extends JPanel { // Clase que extiende JPanel para controlar la simulación
    private SimulationEngine engine; // Referencia al motor de simulación
    private MainFrame mainFrame; // Referencia al frame principal
    private Timer updateTimer; // Temporizador para actualizar la interfaz

    private JButton startButton; // Botón para iniciar simulación
    private JButton pauseButton; // Botón para pausar/reanudar simulación
    private JButton stepButton; // Botón para ejecutar un paso de simulación
    private JButton resetButton; // Botón para reiniciar simulación
    private JButton configButton; // Botón para abrir configuración
    private JLabel timeLabel; // Etiqueta que muestra el tiempo actual
    private JLabel weekLabel; // Etiqueta que muestra semana, día y hora
    private JLabel statusLabel; // Etiqueta que muestra el estado actual
    private JSlider speedSlider; // Control deslizante para velocidad de simulación
    private JProgressBar progressBar; // Barra de progreso de la simulación

    private boolean isRunning = false; // Bandera que indica si la simulación está corriendo
    private Thread simulationThread; // Hilo para ejecutar simulación en segundo plano

    public ControlPanel(SimulationEngine engine, MainFrame mainFrame) { // Constructor que inicializa el panel de control
        this.engine = engine; // Asigna motor recibido
        this.mainFrame = mainFrame; // Asigna frame principal recibido

        setLayout(new BorderLayout(10, 10)); // Establece layout BorderLayout con espaciado de 10 píxeles
        setBorder(BorderFactory.createCompoundBorder( // Crea borde compuesto
            BorderFactory.createTitledBorder("Control de Simulacion"), // Borde con título
            BorderFactory.createEmptyBorder(10, 10, 10, 10) // Borde vacío interno de 10 píxeles
        ));

        initializeComponents(); // Inicializa componentes de interfaz
        layoutComponents(); // Organiza componentes en el panel
        initializeTimers(); // Inicializa temporizadores

        Logger.getInstance().info("Control panel initialized"); // Registra inicialización en log
    }

    private void initializeComponents() { // Método que inicializa todos los componentes de interfaz
        // Buttons
        startButton = createStyledButton("Iniciar", new Color(76, 175, 80)); // Crea botón de inicio verde
        pauseButton = createStyledButton("Pausa", new Color(255, 152, 0)); // Crea botón de pausa naranja
        stepButton = createStyledButton("Paso", new Color(33, 150, 243)); // Crea botón de paso azul
        resetButton = createStyledButton("Reiniciar", new Color(244, 67, 54)); // Crea botón de reinicio rojo
        configButton = createStyledButton("Parametros", new Color(63, 81, 181)); // Crea botón de configuración índigo

        pauseButton.setEnabled(false); // Deshabilita botón de pausa inicialmente

        // Labels
        timeLabel = new JLabel("Tiempo: 0.00 horas"); // Crea etiqueta de tiempo con valor inicial
        timeLabel.setFont(new Font("Monospaced", Font.BOLD, 14)); // Establece fuente monoespaciada negrita tamaño 14

        weekLabel = new JLabel("Semana 1 - Lunes - 00:00"); // Crea etiqueta de semana con valor inicial
        weekLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12)); // Establece fuente Segoe UI normal tamaño 12

        statusLabel = new JLabel("Estado: Listo"); // Crea etiqueta de estado con valor inicial
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11)); // Establece fuente Segoe UI itálica tamaño 11

        // Speed slider (1 = muy lento, 100 = muy rápido)
        speedSlider = new JSlider(1, 100, 50); // Crea slider de velocidad con rango 1-100 y valor inicial 50
        speedSlider.setMajorTickSpacing(25); // Establece espaciado de marcas mayores cada 25 unidades
        speedSlider.setMinorTickSpacing(5); // Establece espaciado de marcas menores cada 5 unidades
        speedSlider.setPaintTicks(true); // Activa visualización de marcas
        speedSlider.setPaintLabels(true); // Activa visualización de etiquetas numéricas

        // Update animation speed when slider changes
        speedSlider.addChangeListener(e -> { // Agrega listener que se ejecuta cuando cambia el slider
            engine.setAnimationSpeed(speedSlider.getValue()); // Actualiza velocidad de animación en el motor
        });

        // Tooltip para explicar velocidad
        speedSlider.setToolTipText("1=Lento (ver animación), 50=Normal, 100=Rápido (resultados)"); // Establece tooltip explicativo

        // Progress bar
        progressBar = new JProgressBar(0, 100); // Crea barra de progreso con rango 0-100
        progressBar.setStringPainted(true); // Activa visualización de texto en la barra
        progressBar.setString("0%"); // Establece texto inicial

        // Add action listeners
        startButton.addActionListener(e -> startSimulation()); // Asocia botón de inicio con método startSimulation
        pauseButton.addActionListener(e -> pauseSimulation()); // Asocia botón de pausa con método pauseSimulation
        stepButton.addActionListener(e -> stepSimulation()); // Asocia botón de paso con método stepSimulation
        resetButton.addActionListener(e -> resetSimulation()); // Asocia botón de reinicio con método resetSimulation
        configButton.addActionListener(e -> openConfigurationDialog()); // Asocia botón de configuración con método openConfigurationDialog
    }

    private JButton createStyledButton(String text, Color color) { // Método que crea botón con estilo personalizado
        JButton button = new JButton(text); // Crea botón con texto especificado
        button.setFont(new Font("Segoe UI", Font.BOLD, 12)); // Establece fuente Segoe UI negrita tamaño 12
        button.setBackground(color); // Establece color de fondo recibido
        button.setForeground(Color.WHITE); // Establece color de texto blanco
        button.setFocusPainted(false); // Desactiva pintado de borde de foco
        button.setBorderPainted(false); // Desactiva pintado de borde
        button.setOpaque(true); // Hace el botón opaco para que se vea el color de fondo
        button.setPreferredSize(new Dimension(120, 35)); // Establece tamaño preferido de 120x35 píxeles
        return button; // Retorna botón configurado
    }

    private void layoutComponents() { // Método que organiza componentes en el panel
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5)); // Crea panel con FlowLayout alineado a izquierda
        buttonPanel.add(startButton); // Agrega botón de inicio
        buttonPanel.add(pauseButton); // Agrega botón de pausa
        buttonPanel.add(stepButton); // Agrega botón de paso
        buttonPanel.add(resetButton); // Agrega botón de reinicio
        buttonPanel.add(configButton); // Agrega botón de configuración

        // Info panel
        JPanel infoPanel = new JPanel(new GridLayout(3, 1, 5, 5)); // Crea panel con 3 filas, 1 columna, espaciado 5
        infoPanel.add(timeLabel); // Agrega etiqueta de tiempo
        infoPanel.add(weekLabel); // Agrega etiqueta de semana
        infoPanel.add(statusLabel); // Agrega etiqueta de estado

        // Speed panel
        JPanel speedPanel = new JPanel(new BorderLayout(5, 5)); // Crea panel con BorderLayout
        speedPanel.add(new JLabel("Velocidad (1=Lento, 100=Rapido):"), BorderLayout.WEST); // Agrega etiqueta al oeste
        speedPanel.add(speedSlider, BorderLayout.CENTER); // Agrega slider al centro

        // Progress panel
        JPanel progressPanel = new JPanel(new BorderLayout(5, 5)); // Crea panel con BorderLayout
        progressPanel.add(new JLabel("Progreso:"), BorderLayout.WEST); // Agrega etiqueta al oeste
        progressPanel.add(progressBar, BorderLayout.CENTER); // Agrega barra de progreso al centro

        // Main layout
        JPanel topPanel = new JPanel(new BorderLayout(10, 5)); // Crea panel superior
        topPanel.add(buttonPanel, BorderLayout.WEST); // Agrega panel de botones al oeste
        topPanel.add(infoPanel, BorderLayout.CENTER); // Agrega panel de información al centro

        JPanel bottomPanel = new JPanel(new GridLayout(2, 1, 5, 5)); // Crea panel inferior con 2 filas
        bottomPanel.add(speedPanel); // Agrega panel de velocidad
        bottomPanel.add(progressPanel); // Agrega panel de progreso

        add(topPanel, BorderLayout.NORTH); // Agrega panel superior al norte del panel principal
        add(bottomPanel, BorderLayout.CENTER); // Agrega panel inferior al centro del panel principal
    }

    private void initializeTimers() { // Método que inicializa temporizadores
        // UI update timer - 30 FPS para suavidad
        updateTimer = new Timer(33, e -> { // Crea temporizador que se ejecuta cada 33ms (~30 FPS)
            updateDisplay(); // Actualiza visualización de información
            mainFrame.updateAllPanels(); // Actualiza todos los paneles del frame principal
        });
    }

    private void startSimulation() { // Método que inicia la simulación
        if (!isRunning) { // Verifica si no está corriendo
            isRunning = true; // Marca como corriendo
            startButton.setEnabled(false); // Deshabilita botón de inicio
            pauseButton.setEnabled(true); // Habilita botón de pausa
            stepButton.setEnabled(false); // Deshabilita botón de paso
            statusLabel.setText("Estado: Ejecutando"); // Actualiza etiqueta de estado

            updateTimer.start(); // Inicia temporizador de actualización de interfaz

            Logger.getInstance().info("Simulation started"); // Registra inicio en log

            // Run simulation in background thread with speed control
            simulationThread = new Thread(() -> { // Crea nuevo hilo para ejecutar simulación
                while (isRunning && !engine.isSimulationComplete()) { // Bucle mientras esté corriendo y no haya terminado
                    if (!engine.isPaused()) { // Verifica si no está pausada
                        // Execute one simulation step
                        engine.step(); // Ejecuta un paso de simulación

                        // Sleep based on speed slider
                        // Speed 1-20: Muy lento (ver animación detallada)
                        // Speed 21-50: Normal (balance)
                        // Speed 51-80: Rápido
                        // Speed 81-100: Máximo (sin delay casi)
                        int speed = speedSlider.getValue(); // Obtiene valor actual del slider
                        int delay; // Variable para almacenar delay en milisegundos
                        if (speed <= 20) { // Si velocidad muy lenta
                            delay = 150 - (speed * 5); // Calcula delay: 145ms a 50ms
                        } else if (speed <= 50) { // Si velocidad normal
                            delay = 50 - (speed - 20); // Calcula delay: 50ms a 20ms
                        } else if (speed <= 80) { // Si velocidad rápida
                            delay = Math.max(5, 20 - ((speed - 50) / 2)); // Calcula delay: 20ms a 5ms
                        } else { // Si velocidad máxima
                            delay = Math.max(0, 5 - ((speed - 80) / 4)); // Calcula delay: 5ms a 0ms
                        }

                        try {
                            if (delay > 0) { // Si hay delay
                                Thread.sleep(delay); // Espera el tiempo calculado
                            }
                        } catch (InterruptedException e) { // Captura interrupción
                            break; // Sale del bucle
                        }
                    } else { // Si está pausada
                        // Paused - just wait
                        try {
                            Thread.sleep(100); // Espera 100ms mientras está pausada
                        } catch (InterruptedException e) { // Captura interrupción
                            break; // Sale del bucle
                        }
                    }
                }

                // Simulation completed or stopped
                SwingUtilities.invokeLater(() -> { // Ejecuta en el hilo de eventos de Swing
                    stopSimulation(); // Detiene simulación
                    if (engine.isSimulationComplete()) { // Verifica si completó normalmente
                        JOptionPane.showMessageDialog(ControlPanel.this, // Muestra diálogo de información
                            "Simulacion finalizada!\n\n" + // Mensaje de finalización
                            "Tiempo total: " + String.format("%.2f horas", engine.getCurrentTime() / 60.0) + // Tiempo total (convertir minutos a horas)
                            "\nValvulas completadas: " + engine.getCompletedValves().size() + // Válvulas completadas
                            "\nRevisa la pestaña de Estadisticas para mas detalles.", // Instrucción adicional
                            "Simulacion Completa", // Título del diálogo
                            JOptionPane.INFORMATION_MESSAGE); // Tipo de mensaje
                        Logger.getInstance().info("Simulation completed successfully"); // Registra completación en log
                    }
                });
            });
            simulationThread.setName("Simulation-Worker"); // Establece nombre del hilo
            simulationThread.start(); // Inicia ejecución del hilo
        }
    }

    private void pauseSimulation() { // Método que pausa o reanuda la simulación
        if (isRunning) { // Verifica si está corriendo
            if (engine.isPaused()) { // Verifica si está pausada
                engine.resume(); // Reanuda simulación
                pauseButton.setText("Pausa"); // Cambia texto del botón a "Pausa"
                statusLabel.setText("Estado: Ejecutando"); // Actualiza estado
                Logger.getInstance().info("Simulation resumed"); // Registra reanudación en log
            } else { // Si no está pausada
                engine.pause(); // Pausa simulación
                pauseButton.setText("Reanudar"); // Cambia texto del botón a "Reanudar"
                statusLabel.setText("Estado: En Pausa"); // Actualiza estado
                Logger.getInstance().info("Simulation paused"); // Registra pausa en log
            }
        }
    }

    private void stepSimulation() { // Método que ejecuta un solo paso de simulación
        engine.step(); // Ejecuta un paso en el motor
        updateDisplay(); // Actualiza visualización
        mainFrame.updateAllPanels(); // Actualiza todos los paneles
        Logger.getInstance().debug("Simulation stepped to time: " + engine.getCurrentTime()); // Registra paso en log
    }

    private void stopSimulation() { // Método que detiene la simulación
        isRunning = false; // Marca como no corriendo
        updateTimer.stop(); // Detiene temporizador de actualización
        startButton.setEnabled(true); // Habilita botón de inicio
        pauseButton.setEnabled(false); // Deshabilita botón de pausa
        pauseButton.setText("Pausa"); // Restablece texto del botón
        stepButton.setEnabled(true); // Habilita botón de paso
        statusLabel.setText("Estado: Detenido"); // Actualiza estado
        updateDisplay(); // Actualiza visualización
        mainFrame.updateAllPanels(); // Actualiza todos los paneles
    }

    private void resetSimulation() { // Método que reinicia la simulación
        stopSimulation(); // Detiene simulación

        if (simulationThread != null && simulationThread.isAlive()) { // Verifica si el hilo existe y está vivo
            simulationThread.interrupt(); // Interrumpe el hilo
            try {
                simulationThread.join(1000); // Espera máximo 1 segundo a que termine
            } catch (InterruptedException e) { // Captura interrupción
                Thread.currentThread().interrupt(); // Restablece estado de interrupción
            }
        }

        engine.reset(); // Reinicia el motor de simulación
        updateDisplay(); // Actualiza visualización
        mainFrame.updateAllPanels(); // Actualiza todos los paneles
        progressBar.setValue(0); // Reinicia barra de progreso a 0
        progressBar.setString("0%"); // Reinicia texto de progreso
        statusLabel.setText("Estado: Listo"); // Actualiza estado a "Listo"

        Logger.getInstance().info("Simulation reset"); // Registra reinicio en log
    }

    private void openConfigurationDialog() { // Método que abre el diálogo de configuración
        if (isRunning) { // Verifica si está corriendo
            int choice = JOptionPane.showConfirmDialog(this, // Muestra diálogo de confirmación
                "La simulacion se detendra para modificar parametros. Continuar?", // Mensaje de advertencia
                "Modificar parametros", // Título
                JOptionPane.OK_CANCEL_OPTION, // Opciones OK/Cancelar
                JOptionPane.WARNING_MESSAGE); // Tipo de mensaje
            if (choice != JOptionPane.OK_OPTION) { // Si no eligió OK
                return; // Sale sin hacer nada
            }
            stopSimulation(); // Detiene simulación
        }

        mainFrame.showConfigurationDialog(); // Muestra diálogo de configuración del frame principal
    }

    private void updateDisplay() { // Método que actualiza la visualización de información
        double time = engine.getCurrentTime(); // Obtiene tiempo actual de simulación
        int week = engine.getShiftCalendar().getWeekNumber(time); // Obtiene número de semana
        String day = engine.getShiftCalendar().getDayName(time); // Obtiene nombre del día
        int hour = engine.getShiftCalendar().getHourOfDay(time); // Obtiene hora del día
        int minute = engine.getShiftCalendar().getMinuteOfHour(time); // Obtiene minuto de la hora

        timeLabel.setText(String.format("Tiempo: %.2f horas", time)); // Actualiza etiqueta de tiempo
        weekLabel.setText(String.format("Semana %d - %s - %02d:%02d", // Actualiza etiqueta de semana
            week, day, hour, minute)); // Formatea con semana, día, hora y minuto

        double progress; // Variable para calcular progreso
        if (engine.isSimulationComplete()) { // Si la simulación está completa
            progress = 100.0; // Progreso al 100%
        } else { // Si no está completa
            progress = (time / engine.getEndTime()) * 100; // Calcula porcentaje basado en tiempo
        }
        progressBar.setValue((int)progress); // Actualiza valor de barra de progreso
        progressBar.setString(String.format("%.1f%%", progress)); // Actualiza texto de barra de progreso
    }

    public void setEngine(SimulationEngine newEngine) { // Método público que cambia el motor de simulación
        stopSimulation(); // Detiene simulación actual
        this.engine = newEngine; // Asigna nuevo motor
        engine.setAnimationSpeed(speedSlider.getValue()); // Establece velocidad de animación según slider
        progressBar.setValue(0); // Reinicia barra de progreso
        progressBar.setString("0%"); // Reinicia texto de progreso
        statusLabel.setText("Estado: Listo"); // Actualiza estado
        updateDisplay(); // Actualiza visualización
    }
}

package gui; // Declaración del paquete gui para interfaces gráficas

import core.SimulationEngine; // Importa el motor de simulación
import utils.Logger; // Importa clase Logger para registro de eventos

import javax.swing.*; // Importa componentes Swing
import java.awt.*; // Importa clases AWT

public class MainFrame extends JFrame { // Clase principal que extiende JFrame, es la ventana principal de la aplicación
    private SimulationEngine engine; // Referencia al motor de simulación
    private SimulationPanel simulationPanel; // Panel que muestra la simulación visual
    private ControlPanel controlPanel; // Panel que contiene los controles de la simulación
    private ChartsPanel chartsPanel; // Panel que muestra gráficos estadísticos
    private StatisticsPanel statisticsPanel; // Panel que muestra estadísticas detalladas

    public MainFrame() { // Constructor que inicializa el frame principal
        setTitle("Simulacion de Produccion de Cerveza Artesanal"); // Establece título de la ventana
        setSize(1600, 1000); // Establece tamaño de la ventana en píxeles
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Configura para cerrar aplicación al cerrar ventana
        setLocationRelativeTo(null); // Centra la ventana en la pantalla

        Logger.getInstance().info("Initializing brewery simulation..."); // Registra inicio de inicialización en log

        engine = new SimulationEngine(); // Crea nueva instancia del motor de simulación

        initializeComponents(); // Inicializa todos los paneles de la interfaz
        layoutComponents(); // Organiza los paneles en el frame

        Logger.getInstance().info("Main frame initialized successfully"); // Registra finalización exitosa en log
    }

    private void initializeComponents() { // Método que inicializa todos los paneles
        simulationPanel = new SimulationPanel(engine); // Crea panel de simulación con el motor
        controlPanel = new ControlPanel(engine, this); // Crea panel de control con motor y referencia a este frame
        chartsPanel = new ChartsPanel(engine); // Crea panel de gráficos con el motor
        statisticsPanel = new StatisticsPanel(engine); // Crea panel de estadísticas con el motor
    }

    private void layoutComponents() { // Método que organiza componentes en el frame
        setLayout(new BorderLayout(5, 5)); // Establece BorderLayout con espaciado de 5 píxeles

        // Create tabbed pane for different views
        JTabbedPane tabbedPane = new JTabbedPane(); // Crea panel de pestañas para diferentes vistas
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 12)); // Establece fuente Segoe UI tamaño 12 para las pestañas

        // Main simulation view
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5)); // Crea panel principal con BorderLayout
        mainPanel.add(simulationPanel, BorderLayout.CENTER); // Agrega panel de simulación al centro
        mainPanel.add(controlPanel, BorderLayout.SOUTH); // Agrega panel de control en la parte inferior

        // FIXED: Remove createIconLabel() calls - just use strings
        tabbedPane.addTab("Simulacion", mainPanel); // Agrega pestaña de simulación con el panel principal
        tabbedPane.addTab("Graficas", chartsPanel); // Agrega pestaña de gráficas con el panel de gráficos
        tabbedPane.addTab("Estadisticas", statisticsPanel); // Agrega pestaña de estadísticas con el panel de estadísticas

        add(tabbedPane, BorderLayout.CENTER); // Agrega panel de pestañas al centro del frame

        // Status bar
        JPanel statusBar = createStatusBar(); // Crea barra de estado
        add(statusBar, BorderLayout.SOUTH); // Agrega barra de estado en la parte inferior del frame
    }

    private JPanel createStatusBar() { // Método que crea la barra de estado
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT)); // Crea panel con FlowLayout alineado a izquierda
        statusBar.setBorder(BorderFactory.createEtchedBorder()); // Establece borde grabado
        statusBar.setPreferredSize(new Dimension(getWidth(), 25)); // Establece altura preferida de 25 píxeles

        JLabel statusLabel = new JLabel("Listo - Simulacion de Cerveza Artesanal v1.0"); // Crea etiqueta con mensaje de estado
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11)); // Establece fuente Segoe UI tamaño 11
        statusBar.add(statusLabel); // Agrega etiqueta a la barra de estado

        // Add memory indicator
        statusBar.add(Box.createHorizontalStrut(20)); // Agrega espaciador horizontal de 20 píxeles
        JLabel memoryLabel = new JLabel(); // Crea etiqueta para mostrar uso de memoria
        statusBar.add(memoryLabel); // Agrega etiqueta de memoria a la barra

        // Update memory usage every second
        Timer memoryTimer = new Timer(1000, e -> { // Crea temporizador que se ejecuta cada 1000ms (1 segundo)
            Runtime runtime = Runtime.getRuntime(); // Obtiene instancia de Runtime para acceder a información de memoria
            long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024); // Calcula memoria usada en MB
            long maxMemory = runtime.maxMemory() / (1024 * 1024); // Calcula memoria máxima disponible en MB
            memoryLabel.setText(String.format("Memoria: %d MB / %d MB", usedMemory, maxMemory)); // Actualiza etiqueta con uso de memoria
        });
        memoryTimer.start(); // Inicia el temporizador de actualización de memoria

        return statusBar; // Retorna la barra de estado configurada
    }

    public void updateAllPanels() { // Método público que actualiza todos los paneles de la interfaz
        try { // Bloque try para capturar excepciones
            simulationPanel.updateDisplay(); // Actualiza panel de simulación
            chartsPanel.updateCharts(); // Actualiza panel de gráficos
            statisticsPanel.updateStatistics(); // Actualiza panel de estadísticas
        } catch (Exception e) { // Captura cualquier excepción durante actualización
            Logger.getInstance().error("Error updating panels", e); // Registra error en log con excepción
        }
    }

    public SimulationPanel getSimulationPanel() { // Método público getter que retorna panel de simulación
        return simulationPanel; // Retorna referencia al panel de simulación
    }

    public ChartsPanel getChartsPanel() { // Método público getter que retorna panel de gráficos
        return chartsPanel; // Retorna referencia al panel de gráficos
    }

    public StatisticsPanel getStatisticsPanel() { // Método público getter que retorna panel de estadísticas
        return statisticsPanel; // Retorna referencia al panel de estadísticas
    }

    public void showConfigurationDialog() { // Método público que muestra el diálogo de configuración
        SimulationConfigDialog dialog = new SimulationConfigDialog(this); // Crea diálogo de configuración pasando este frame como parent
        dialog.setVisible(true); // Hace visible el diálogo (modal)
    }

    public void reloadSimulationEngine() { // Método público que recarga el motor de simulación con nuevos parámetros
        Logger.getInstance().info("Reloading simulation with updated parameters"); // Registra recarga en log
        engine = new SimulationEngine(); // Crea nueva instancia del motor con parámetros actualizados
        simulationPanel.setEngine(engine); // Actualiza panel de simulación con nuevo motor
        controlPanel.setEngine(engine); // Actualiza panel de control con nuevo motor
        chartsPanel.setEngine(engine); // Actualiza panel de gráficos con nuevo motor
        statisticsPanel.setEngine(engine); // Actualiza panel de estadísticas con nuevo motor
        updateAllPanels(); // Actualiza todos los paneles para reflejar cambios
    }
}

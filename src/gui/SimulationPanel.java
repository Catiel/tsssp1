package gui; // Declaración del paquete gui para interfaces gráficas

import core.SimulationEngine; // Importa el motor de simulación
import model.*; // Importa todas las clases del modelo
import statistics.*; // Importa todas las clases de estadísticas
import utils.Localization; // Importa clase de localización de nombres
import javax.swing.*; // Importa componentes Swing
import javax.swing.table.*; // Importa clases de tablas Swing
import java.awt.*; // Importa clases AWT

public class SimulationPanel extends JPanel { // Clase que extiende JPanel para mostrar panel principal de simulación con animación y estadísticas
    private SimulationEngine engine; // Referencia al motor de simulación
    private AnimationPanel animationPanel; // Panel que muestra animación gráfica de la simulación
    private JPanel statsPanel; // Panel que contiene las tablas de estadísticas
    private JTable entityStatsTable; // Tabla que muestra estadísticas de válvulas
    private JTable locationStatsTable; // Tabla que muestra estadísticas de ubicaciones
    private JTable craneStatsTable; // Tabla que muestra estadísticas de la grúa
    private DefaultTableModel entityModel; // Modelo de datos para tabla de válvulas
    private DefaultTableModel locationModel; // Modelo de datos para tabla de ubicaciones
    private DefaultTableModel craneModel; // Modelo de datos para tabla de grúa

    public SimulationPanel(SimulationEngine engine) { // Constructor que inicializa el panel con motor de simulación
        this.engine = engine; // Asigna motor recibido
        setLayout(new BorderLayout(10, 10)); // Establece BorderLayout con espaciado de 10 píxeles

        initializeComponents(); // Inicializa componentes del panel
        layoutComponents(); // Organiza componentes en el panel
    }

    public void setEngine(SimulationEngine engine) { // Método público que cambia el motor de simulación
        this.engine = engine; // Asigna nuevo motor
        animationPanel.setEngine(engine); // Actualiza motor en panel de animación
        updateDisplay(); // Actualiza visualización
    }

    private void initializeComponents() { // Método que inicializa los componentes del panel
        animationPanel = new AnimationPanel(engine); // Crea panel de animación con el motor
        statsPanel = createStatsPanel(); // Crea panel de estadísticas
    }

    private void layoutComponents() { // Método que organiza componentes en el panel
        // Left: Animation
        JPanel animationContainer = new JPanel(new BorderLayout()); // Crea contenedor para animación con BorderLayout
        animationContainer.setBorder(BorderFactory.createTitledBorder("Animacion del Sistema")); // Establece borde con título
        animationContainer.add(animationPanel, BorderLayout.CENTER); // Agrega panel de animación al centro
        animationContainer.add(createLegendPanel(), BorderLayout.SOUTH); // Agrega panel de leyenda en la parte inferior

        // Right: Statistics
        JScrollPane statsScroll = new JScrollPane(statsPanel); // Crea scroll pane para panel de estadísticas
        statsScroll.setBorder(BorderFactory.createTitledBorder("Estadisticas en Tiempo Real")); // Establece borde con título
        statsScroll.setPreferredSize(new Dimension(400, 600)); // Establece tamaño preferido de 400x600 píxeles

        // Layout
        add(animationContainer, BorderLayout.CENTER); // Agrega contenedor de animación al centro del panel principal
        add(statsScroll, BorderLayout.EAST); // Agrega scroll de estadísticas al este (derecha)
    }

    private JPanel createStatsPanel() { // Método que crea el panel de estadísticas con tablas
        JPanel panel = new JPanel(); // Crea panel
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); // Establece BoxLayout vertical
        panel.setBackground(Color.WHITE); // Establece fondo blanco

        // Entity Statistics Table
        String[] entityColumns = {"Valvula", "Llegadas", "Completadas", "En Sistema", "Tasa %"}; // Define columnas de tabla de válvulas
        entityModel = new DefaultTableModel(entityColumns, 0) { // Crea modelo de tabla con columnas y 0 filas iniciales
            @Override // Anotación de sobrescritura
            public boolean isCellEditable(int row, int column) { return false; } // Hace todas las celdas no editables
        };
        entityStatsTable = createStyledTable(entityModel); // Crea tabla con estilo aplicado
        panel.add(createTableSection("Estadisticas de Valvulas", entityStatsTable, 150)); // Agrega sección de tabla con altura 150

        panel.add(Box.createVerticalStrut(10)); // Agrega espaciador vertical de 10 píxeles

        // Location Statistics Table
        String[] locationColumns = {"Ubicacion", "Actual", "Capacidad", "Util %"}; // Define columnas de tabla de ubicaciones
        locationModel = new DefaultTableModel(locationColumns, 0) { // Crea modelo de tabla con columnas
            @Override // Anotación de sobrescritura
            public boolean isCellEditable(int row, int column) { return false; } // Hace todas las celdas no editables
        };
        locationStatsTable = createStyledTable(locationModel); // Crea tabla con estilo aplicado
        panel.add(createTableSection("Estadisticas de Ubicaciones", locationStatsTable, 250)); // Agrega sección de tabla con altura 250

        panel.add(Box.createVerticalStrut(10)); // Agrega espaciador vertical de 10 píxeles

        // Crane Statistics Table
        String[] craneColumns = {"Metrica", "Valor"}; // Define columnas de tabla de grúa
        craneModel = new DefaultTableModel(craneColumns, 0) { // Crea modelo de tabla con columnas
            @Override // Anotación de sobrescritura
            public boolean isCellEditable(int row, int column) { return false; } // Hace todas las celdas no editables
        };
        craneStatsTable = createStyledTable(craneModel); // Crea tabla con estilo aplicado
        panel.add(createTableSection("Estadisticas de la Grua", craneStatsTable, 120)); // Agrega sección de tabla con altura 120

        return panel; // Retorna panel configurado
    }

    private JTable createStyledTable(DefaultTableModel model) { // Método que crea tabla con estilo personalizado
        JTable table = new JTable(model); // Crea tabla con el modelo especificado
        table.setFont(new Font("Segoe UI", Font.PLAIN, 11)); // Establece fuente Segoe UI tamaño 11
        table.setRowHeight(22); // Establece altura de filas en 22 píxeles
        table.setGridColor(new Color(220, 220, 220)); // Establece color gris claro para líneas de cuadrícula
        table.setSelectionBackground(new Color(184, 207, 229)); // Establece color azul claro para selección

        JTableHeader header = table.getTableHeader(); // Obtiene encabezado de la tabla
        header.setFont(new Font("Segoe UI", Font.BOLD, 11)); // Establece fuente negrita para encabezado
        header.setBackground(new Color(70, 130, 180)); // Establece fondo azul para encabezado
        header.setForeground(Color.WHITE); // Establece texto blanco para encabezado

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer(); // Crea renderizador de celdas
        centerRenderer.setHorizontalAlignment(JLabel.CENTER); // Establece alineación centrada
        for (int i = 1; i < table.getColumnCount(); i++) { // Itera sobre columnas (excepto la primera)
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer); // Aplica renderizador centrado a columna i
        }

        return table; // Retorna tabla configurada
    }

    private JPanel createTableSection(String title, JTable table, int height) { // Método que crea sección con título y tabla con altura específica
        JPanel section = new JPanel(new BorderLayout()); // Crea panel con BorderLayout
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, height)); // Establece altura máxima especificada
        section.setBorder(BorderFactory.createCompoundBorder( // Crea borde compuesto
            BorderFactory.createTitledBorder(title), // Borde exterior con título
            BorderFactory.createEmptyBorder(5, 5, 5, 5) // Borde interior vacío de 5 píxeles
        ));

        JScrollPane scrollPane = new JScrollPane(table); // Crea scroll pane para la tabla
        scrollPane.setPreferredSize(new Dimension(380, height - 40)); // Establece tamaño preferido (resta 40 para borde y título)
        section.add(scrollPane, BorderLayout.CENTER); // Agrega scroll pane al centro de la sección

        return section; // Retorna sección configurada
    }

    private JPanel createLegendPanel() { // Método que crea panel de leyenda con colores de válvulas
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5)); // Crea panel con FlowLayout alineado a izquierda, espaciado horizontal 15, vertical 5
        panel.setBackground(new Color(245, 245, 250)); // Establece fondo gris muy claro

        for (Valve.Type type : Valve.Type.values()) { // Itera sobre cada tipo de válvula
            JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0)); // Crea panel para cada ítem de leyenda
            item.setBackground(new Color(245, 245, 250)); // Establece mismo fondo que panel principal

            JLabel colorBox = new JLabel("██"); // Crea etiqueta con dos bloques sólidos (representan color)
            colorBox.setForeground(type.getColor()); // Establece color del tipo de válvula
            colorBox.setFont(new Font("Monospaced", Font.BOLD, 14)); // Establece fuente monoespaciada negrita tamaño 14

            JLabel label = new JLabel(type.getDisplayName()); // Crea etiqueta con nombre descriptivo del tipo
            label.setFont(new Font("Segoe UI", Font.PLAIN, 10)); // Establece fuente Segoe UI tamaño 10

            item.add(colorBox); // Agrega cuadro de color al ítem
            item.add(label); // Agrega etiqueta de texto al ítem
            panel.add(item); // Agrega ítem al panel de leyenda
        }

        return panel; // Retorna panel de leyenda configurado
    }

    public void updateDisplay() { // Método público que actualiza toda la visualización del panel
        // Update animation
        animationPanel.repaint(); // Solicita repintado del panel de animación

        // Update statistics tables
        updateEntityStats(); // Actualiza tabla de estadísticas de válvulas
        updateLocationStats(); // Actualiza tabla de estadísticas de ubicaciones
        updateCraneStats(); // Actualiza tabla de estadísticas de grúa
    }

    private void updateEntityStats() { // Método que actualiza tabla de estadísticas de válvulas
        entityModel.setRowCount(0); // Limpia todas las filas de la tabla
        Statistics stats = engine.getStatistics(); // Obtiene estadísticas del motor

        for (Valve.Type type : Valve.Type.values()) { // Itera sobre cada tipo de válvula
            EntityStats es = stats.getEntityStats(type); // Obtiene estadísticas del tipo
            if (es != null) { // Verifica si existen estadísticas
                entityModel.addRow(new Object[]{ // Agrega nueva fila con datos
                    type.getDisplayName(), // Nombre descriptivo del tipo
                    es.getTotalArrivals(), // Total de arribos
                    es.getTotalCompleted(), // Total de completadas
                    es.getCurrentInSystem(), // Cantidad actual en sistema
                    String.format("%.1f%%", es.getCompletionRate()) // Tasa de completación en porcentaje con 1 decimal
                });
            }
        }
    }

    private void updateLocationStats() { // Método que actualiza tabla de estadísticas de ubicaciones
        locationModel.setRowCount(0); // Limpia todas las filas de la tabla
        // Primero mostrar ubicaciones principales
        String[] mainLocations = {"DOCK", "STOCK", "Almacen_M1", "Almacen_M2", "Almacen_M3"}; // Array con nombres de ubicaciones principales
        for (String name : mainLocations) { // Itera sobre cada ubicación principal
            Location loc = engine.getLocations().get(name); // Obtiene ubicación del motor
            if (loc != null) { // Verifica si existe la ubicación
                String capacity = loc.getCapacity() == Integer.MAX_VALUE ? "∞" : String.valueOf(loc.getCapacity()); // Muestra infinito o número de capacidad
                locationModel.addRow(new Object[]{ // Agrega nueva fila con datos
                    Localization.getLocationDisplayName(loc.getName()), // Nombre localizado de la ubicación
                    loc.getCurrentContents(), // Contenido actual
                    capacity, // Capacidad (∞ o número)
                    String.format("%.1f%%", loc.getUtilization()) // Utilización en porcentaje con 1 decimal
                });
            }
        }

        // Mostrar grupos de máquinas con sus totales (leer cantidades desde config)
        utils.Config config = utils.Config.getInstance(); // Obtiene configuración
        addMachineGroupStats("M1", config.getMachineUnits("m1")); // Agrega estadísticas del grupo M1
        addMachineGroupStats("M2", config.getMachineUnits("m2")); // Agrega estadísticas del grupo M2
        addMachineGroupStats("M3", config.getMachineUnits("m3")); // Agrega estadísticas del grupo M3
    }

    private void addMachineGroupStats(String baseName, int unitCount) { // Método que agrega fila de estadísticas para un grupo de máquinas
        int totalContents = 0; // Inicializa contador de contenido total
        double busySum = 0; // Inicializa suma de tiempo ocupado

        for (int i = 1; i <= unitCount; i++) { // Itera sobre cada unidad de la máquina
            Location unit = engine.getLocations().get(baseName + "." + i); // Obtiene unidad i
            if (unit != null) { // Verifica si existe la unidad
                totalContents += unit.getCurrentContents(); // Acumula contenido actual
                busySum += unit.getTotalBusyTime(); // Acumula tiempo ocupado
            }
        }

        // Calcular utilización usando stats_units (igual que el reporte)
        utils.Config config = utils.Config.getInstance(); // Obtiene configuración
        double statsUnits = config.getMachineStatsUnits(baseName, unitCount); // Obtiene factor de escala de estadísticas
        double scheduledPerUnit = engine.getShiftCalendar().getTotalWorkingHoursPerWeek(); // Obtiene horas laborables por semana
        double currentTime = engine.getCurrentTime(); // Obtiene tiempo actual de simulación
        double weeksSimulated = Math.max(currentTime, 1e-6) / 168.0; // Calcula semanas simuladas
        double totalScheduled = statsUnits * scheduledPerUnit * weeksSimulated; // Calcula total de horas programadas
        double avgUtilization = (totalScheduled > 0.0) ? (busySum / totalScheduled) * 100.0 : 0.0; // Calcula utilización promedio en porcentaje
        // Limitar al 100% máximo
        avgUtilization = Math.min(avgUtilization, 100.0); // Limita al 100% máximo

        locationModel.addRow(new Object[]{ // Agrega nueva fila con datos del grupo
            Localization.getLocationDisplayName(baseName) + " (" + unitCount + " unidades)", // Nombre localizado con número de unidades
            totalContents, // Contenido total del grupo
            unitCount, // Capacidad (número de unidades)
            String.format("%.1f%%", avgUtilization) // Utilización promedio en porcentaje con 1 decimal
        });
    }

    private void updateCraneStats() { // Método que actualiza tabla de estadísticas de la grúa
        craneModel.setRowCount(0); // Limpia todas las filas de la tabla
        Crane crane = engine.getCrane(); // Obtiene grúa del motor

        craneModel.addRow(new Object[]{"Estado", crane.isBusy() ? "OCUPADA" : "LIBRE"}); // Agrega fila con estado actual (ocupada o libre)
        craneModel.addRow(new Object[]{"Viajes Totales", crane.getTotalTrips()}); // Agrega fila con total de viajes realizados
        craneModel.addRow(new Object[]{"Tiempo de Viaje", String.format("%.2f hrs", crane.getTotalTravelTime())}); // Agrega fila con tiempo total de viaje en horas con 2 decimales
            craneModel.addRow(new Object[]{"Utilizacion", String.format("%.1f%%", crane.getUtilization())}); // Agrega fila con utilización en porcentaje con 1 decimal
        craneModel.addRow(new Object[]{"Transportando", crane.getCarryingValve() != null ? // Agrega fila con válvula que está transportando actualmente
            crane.getCarryingValve().toString() : "Ninguno"}); // Muestra toString de válvula o "Ninguno" si no transporta nada
    }
}

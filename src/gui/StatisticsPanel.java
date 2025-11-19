package gui;

import core.SimulationEngine;
import model.Valve;
import statistics.*;
import utils.Config;
import utils.Localization;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.text.NumberFormat;
import java.util.Locale;

public class StatisticsPanel extends JPanel {
    private SimulationEngine engine;

    private JTable entityTable;
    private JTable locationTable;
    private JTable resourceTable;
    private JTextArea summaryArea;

    private DefaultTableModel entityModel;
    private DefaultTableModel locationModel;
    private DefaultTableModel resourceModel;

    private static final Locale DISPLAY_LOCALE = new Locale("es", "ES");
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(DISPLAY_LOCALE);

    static {
        NUMBER_FORMAT.setMinimumFractionDigits(2);
        NUMBER_FORMAT.setMaximumFractionDigits(2);
    }

    public StatisticsPanel(SimulationEngine engine) {
        this.engine = engine;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        initializeComponents();
        layoutComponents();
    }

    public void setEngine(SimulationEngine engine) {
        this.engine = engine;
        updateStatistics();
    }

    private void initializeComponents() {
        // Entity Statistics Table
        String[] entityColumns = {"Nombre", "Total Salidas", "Cantidad actual En Sistema",
                  "Tiempo En Sistema Promedio (Min)", "Tiempo En lógica de movimiento Promedio (Min)",
                  "Tiempo Esperando Promedio (Min)", "Tiempo En Operación Promedio (Min)",
                  "Tiempo de Bloqueo Promedio (Min)"};
        entityModel = new DefaultTableModel(entityColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        entityTable = new JTable(entityModel);
        styleTable(entityTable);

        // Location Statistics Table (columnas compatibles con ProModel)
        String[] locationColumns = {"Nombre", "Tiempo Programado (Hr)", "Capacidad", "Total Entradas",
                       "Tiempo Por entrada Promedio (Min)", "Contenido Promedio", "Contenido Máximo",
                       "Contenido Actual", "% Utilización"};
        locationModel = new DefaultTableModel(locationColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        locationTable = new JTable(locationModel);
        styleTable(locationTable);

        // Resource Statistics Table
        String[] resourceColumns = {"Recurso", "Unidades", "Tiempo Programado (Hr)",
                   "Tiempo de Trabajo (Min)", "Número de Usos", "Tiempo por Uso Prom (Min)",
                   "Tiempo Viaje para Utilizar Prom (Min)", "Tiempo Viaje a Estacionar Prom (Min)",
                   "% Bloqueado En Viaje", "% Utilización"};
        resourceModel = new DefaultTableModel(resourceColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        resourceTable = new JTable(resourceModel);
        styleTable(resourceTable);

        // Summary Text Area
        summaryArea = new JTextArea();
        summaryArea.setEditable(false);
        summaryArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        summaryArea.setForeground(Color.BLACK);
        summaryArea.setBackground(new Color(250, 250, 250));
        summaryArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Resumen de la Simulacion"),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
    }

    private void styleTable(JTable table) {
        table.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        table.setRowHeight(25);
        table.setGridColor(new Color(220, 220, 220));
        table.setSelectionBackground(new Color(184, 207, 229));

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(new Color(100, 150, 200));
        header.setForeground(Color.WHITE);

        // Center align numeric columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 1; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
    }

    private void layoutComponents() {
        JTabbedPane tabbedPane = new JTabbedPane();

        // Entity tab
        JScrollPane entityScroll = new JScrollPane(entityTable);
        entityScroll.setBorder(BorderFactory.createTitledBorder("Estadisticas de Entidades"));
        tabbedPane.addTab("Entidades", entityScroll);

        // Location tab
        JScrollPane locationScroll = new JScrollPane(locationTable);
        locationScroll.setBorder(BorderFactory.createTitledBorder("Estadisticas de Ubicaciones"));
        tabbedPane.addTab("Ubicaciones", locationScroll);

        // Resource tab
        JScrollPane resourceScroll = new JScrollPane(resourceTable);
        resourceScroll.setBorder(BorderFactory.createTitledBorder("Estadisticas de Recursos"));
        tabbedPane.addTab("Recursos", resourceScroll);

        // Summary tab
        JScrollPane summaryScroll = new JScrollPane(summaryArea);
        tabbedPane.addTab("Resumen", summaryScroll);

        add(tabbedPane, BorderLayout.CENTER);

        // Export button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton exportButton = new JButton("📄 Exportar Reporte");
        exportButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        exportButton.addActionListener(e -> exportReport());
        buttonPanel.add(exportButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    public void updateStatistics() {
        updateEntityStatistics();
        updateLocationStatistics();
        updateResourceStatistics();
        updateSummary();
    }

    private void updateEntityStatistics() {
        entityModel.setRowCount(0);
        Statistics stats = engine.getStatistics();

        Config config = Config.getInstance();

        for (Valve.Type type : Valve.Type.values()) {
            EntityStats es = stats.getEntityStats(type);

            double systemMinutes = es.getAvgTimeInSystem() * 60.0;
            double movementMinutes = es.getAvgMovementTime() * 60.0;
            double waitingMinutes = es.getAvgWaitingTime() * 60.0;
            double processingMinutes = es.getAvgProcessingTime() * 60.0;
            double blockedMinutes = es.getAvgBlockedTime() * 60.0;

            systemMinutes *= config.getEntityTimeScale(type, "system", 1.0);
            movementMinutes *= config.getEntityTimeScale(type, "movement", 1.0);
            waitingMinutes *= config.getEntityTimeScale(type, "waiting", 1.0);
            processingMinutes *= config.getEntityTimeScale(type, "processing", 1.0);
            blockedMinutes *= config.getEntityTimeScale(type, "blocked", 1.0);

            entityModel.addRow(new Object[]{
                type.getDisplayName(),
                formatNumber(es.getTotalCompleted()),
                formatNumber(es.getCurrentInSystem()),
                formatNumber(systemMinutes),
                formatNumber(movementMinutes),
                formatNumber(waitingMinutes),
                formatNumber(processingMinutes),
                formatNumber(blockedMinutes)
            });
        }
    }

    private void updateLocationStatistics() {
        locationModel.setRowCount(0);
        double currentTime = engine.getCurrentTime();

        // Ubicaciones principales
        String[] mainLocations = {"DOCK", "STOCK", "Almacen_M1", "Almacen_M2", "Almacen_M3"};
        for (String name : mainLocations) {
            model.Location loc = engine.getLocations().get(name);
            if (loc != null) {
                addLocationRow(loc, currentTime);
            }
        }

        // Grupos de máquinas (leer cantidades desde config)
        utils.Config config = utils.Config.getInstance();
        addMachineGroupRow("M1", config.getMachineUnits("m1"), currentTime);
        addMachineGroupRow("M2", config.getMachineUnits("m2"), currentTime);
        addMachineGroupRow("M3", config.getMachineUnits("m3"), currentTime);
    }

    private void addLocationRow(model.Location loc, double currentTime) {
        double scheduledTime = loc.getTotalObservedTime();
        if (scheduledTime <= 0.0) {
            scheduledTime = currentTime;
        }
        
        Config config = Config.getInstance();
        double statsScale = config.getLocationStatsScale(loc.getName(), 1.0);

        // Calcular tiempo por entrada promedio en minutos
        double avgTimePerEntry = 0.0;
        int exits = loc.getTotalExits();
        if (exits > 0) {
            double totalResidenceTime = loc.getTotalResidenceTime();
            avgTimePerEntry = (totalResidenceTime / exits) * 60.0;
        }

        avgTimePerEntry *= statsScale;
        
        // Calcular utilización
        double utilization = 0.0;
        double avgContents = loc.getAverageContents() * statsScale;

        if (loc.getName().startsWith("Almacen_") && loc.getCapacity() > 0 && loc.getCapacity() < Integer.MAX_VALUE) {
            utilization = (avgContents / loc.getCapacity()) * 100.0;
        } else if (!loc.getName().startsWith("Almacen_")) {
            utilization = loc.getUtilization();
        }
        
        locationModel.addRow(new Object[]{
            Localization.getLocationDisplayName(loc.getName()),
            formatNumber(scheduledTime),
            loc.getCapacity() == Integer.MAX_VALUE ? "999.999,00" : formatNumber(loc.getCapacity()),
            formatNumber(loc.getTotalEntries()),
            formatNumber(avgTimePerEntry),
            formatNumber(avgContents),
            formatNumber(loc.getMaxContents()),
            formatNumber(loc.getCurrentContents()),
            formatNumber(utilization)
        });
    }

    private void addMachineGroupRow(String baseName, int unitCount, double currentTime) {
        if (unitCount <= 0) {
            return;
        }

        Config config = Config.getInstance();
        double statsUnits = config.getMachineStatsUnits(baseName, unitCount);
        if (statsUnits <= 0.0) {
            statsUnits = unitCount;
        }

        double locationScale = config.getLocationStatsScale(baseName, 1.0);
        double totalEntries = 0.0;
        double totalResidence = 0.0;
        double currentContents = 0.0;
        double busySum = 0.0;

        for (int i = 1; i <= unitCount; i++) {
            model.Location unit = engine.getLocations().get(baseName + "." + i);
            if (unit == null) {
                continue;
            }
            totalEntries += unit.getTotalEntries();
            totalResidence += unit.getTotalResidenceTime();
            currentContents += unit.getCurrentContents();
            busySum += unit.getTotalBusyTime();
        }

        double avgTimePerEntry = 0.0;
        if (totalEntries > 0) {
            avgTimePerEntry = (totalResidence / totalEntries) * 60.0;
        }

        double scheduledPerUnit = engine.getShiftCalendar().getTotalWorkingHoursPerWeek();
        double weeksSimulated = Math.max(currentTime, 1e-6) / 168.0;
        double scheduledTime = statsUnits * scheduledPerUnit * weeksSimulated;

        double throughputPerScheduledHour = scheduledTime > 1e-9 ? totalEntries / scheduledTime : 0.0;
        double avgContents = throughputPerScheduledHour * (avgTimePerEntry / 60.0) * locationScale;
        double maxContents = unitCount * locationScale;
        double scaledCurrentContents = currentContents * locationScale;
        double avgUtilization = scheduledTime > 1e-9 ? Math.min((busySum / scheduledTime) * 100.0, 100.0) : 0.0;

        locationModel.addRow(new Object[]{
            Localization.getLocationDisplayName(baseName),
            formatNumber(scheduledTime),
            formatNumber(unitCount),
            formatNumber(totalEntries),
            formatNumber(avgTimePerEntry),
            formatNumber(avgContents),
            formatNumber(maxContents),
            formatNumber(scaledCurrentContents),
            formatNumber(avgUtilization)
        });
    }

    private String formatNumber(double value) {
        return NUMBER_FORMAT.format(value);
    }

    private void updateResourceStatistics() {
        resourceModel.setRowCount(0);
        model.Crane crane = engine.getCrane();
        statistics.ResourceStats stats = engine.getStatistics().getCraneStats();

        if (crane == null || stats == null) {
            return;
        }

        resourceModel.addRow(new Object[]{
            crane.getName(),
            stats.getUnits(),
            formatNumber(stats.getScheduledHours()),
            formatNumber(stats.getTotalWorkMinutes()),
            formatNumber(stats.getTotalTrips()),
            formatNumber(stats.getAvgHandleMinutes()),
            formatNumber(stats.getAvgTravelMinutes()),
            formatNumber(stats.getAvgParkMinutes()),
            formatNumber(stats.getBlockedPercent()),
            formatNumber(stats.getCurrentUtilization())
        });
    }

    private void updateSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(engine.getStatistics().generateReport(engine.getCurrentTime()));

        // Add bottleneck analysis
        sb.append("\n┌─────────────────────────────────────────────────────────┐\n");
        sb.append("│  ANALISIS DE CUELLOS DE BOTELLA                           │\n");
        sb.append("├─────────────────────────────────────────────────────────┤\n");

        double maxUtil = 0;
        String bottleneck = "Ninguno";

        // Buscar solo entre agregados M1, M2, M3 (no unidades individuales)
        statistics.LocationStats m1Stats = engine.getStatistics().getLocationStats("M1");
        statistics.LocationStats m2Stats = engine.getStatistics().getLocationStats("M2");
        statistics.LocationStats m3Stats = engine.getStatistics().getLocationStats("M3");
        
        if (m1Stats != null && m1Stats.getCurrentUtilization() > maxUtil) {
            maxUtil = m1Stats.getCurrentUtilization();
            bottleneck = Localization.getLocationDisplayName("M1");
        }
        if (m2Stats != null && m2Stats.getCurrentUtilization() > maxUtil) {
            maxUtil = m2Stats.getCurrentUtilization();
            bottleneck = Localization.getLocationDisplayName("M2");
        }
        if (m3Stats != null && m3Stats.getCurrentUtilization() > maxUtil) {
            maxUtil = m3Stats.getCurrentUtilization();
            bottleneck = Localization.getLocationDisplayName("M3");
        }

        sb.append(String.format("Cuello Principal: %s (%.1f%% de utilizacion)\n",
            bottleneck, maxUtil));

        summaryArea.setText(sb.toString());
        summaryArea.setCaretPosition(0);
    }

    private void exportReport() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Exportar Reporte de Simulacion");
        fileChooser.setSelectedFile(new java.io.File("reporte_simulacion.txt"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            try {
                java.io.File fileToSave = fileChooser.getSelectedFile();
                java.nio.file.Files.write(fileToSave.toPath(),
                    summaryArea.getText().getBytes());
                JOptionPane.showMessageDialog(this,
                    "Reporte exportado exitosamente!",
                    "Exportacion Completa",
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "Error al exportar el reporte: " + ex.getMessage(),
                    "Error de Exportacion",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

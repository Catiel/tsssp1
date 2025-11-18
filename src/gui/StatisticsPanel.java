package gui;

import core.SimulationEngine;
import model.Valve;
import statistics.*;
import utils.Localization;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;

public class StatisticsPanel extends JPanel {
    private SimulationEngine engine;

    private JTable entityTable;
    private JTable locationTable;
    private JTable resourceTable;
    private JTextArea summaryArea;

    private DefaultTableModel entityModel;
    private DefaultTableModel locationModel;
    private DefaultTableModel resourceModel;

    public StatisticsPanel(SimulationEngine engine) {
        this.engine = engine;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        initializeComponents();
        layoutComponents();
    }

    private void initializeComponents() {
        // Entity Statistics Table
        String[] entityColumns = {"Tipo de Valvula", "Llegadas", "Completadas", "En Sistema",
                      "% Completado", "Tiempo Prom (hrs)", "Proc Prom",
                      "Mov Prom", "Espera Prom"};
        entityModel = new DefaultTableModel(entityColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        entityTable = new JTable(entityModel);
        styleTable(entityTable);

        // Location Statistics Table
        String[] locationColumns = {"Ubicacion", "Cap", "Actual", "Max", "Contenido Prom",
                       "% Util", "Entradas", "Salidas"};
        locationModel = new DefaultTableModel(locationColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        locationTable = new JTable(locationModel);
        styleTable(locationTable);

        // Resource Statistics Table
        String[] resourceColumns = {"Recurso", "Unidades", "Viajes", "Tiempo de Viaje",
                       "% Util Actual", "% Util Prom"};
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

        for (Valve.Type type : Valve.Type.values()) {
            EntityStats es = stats.getEntityStats(type);
            entityModel.addRow(new Object[]{
                type.getDisplayName(),
                es.getTotalArrivals(),
                es.getTotalCompleted(),
                es.getCurrentInSystem(),
                String.format("%.1f%%", es.getCompletionRate()),
                String.format("%.2f", es.getAvgTimeInSystem()),
                String.format("%.2f", es.getAvgProcessingTime()),
                String.format("%.2f", es.getAvgMovementTime()),
                String.format("%.2f", es.getAvgWaitingTime())
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

        // Grupos de máquinas
        addMachineGroupRow("M1", 10, currentTime);
        addMachineGroupRow("M2", 25, currentTime);
        addMachineGroupRow("M3", 17, currentTime);
    }

    private void addLocationRow(model.Location loc, double currentTime) {
        locationModel.addRow(new Object[]{
            Localization.getLocationDisplayName(loc.getName()),
            loc.getCapacity() == Integer.MAX_VALUE ? "∞" : loc.getCapacity(),
            loc.getCurrentContents(),
            (int)loc.getMaxContents(),
            String.format("%.2f", loc.getAverageContents()),
            String.format("%.1f%%", loc.getUtilization(currentTime)),
            loc.getTotalEntries(),
            loc.getTotalExits()
        });
    }

    private void addMachineGroupRow(String baseName, int unitCount, double currentTime) {
        int totalContents = 0;
        int totalEntries = 0;
        int totalExits = 0;
        double maxContents = 0;
        double avgContents = 0;
        double avgUtilization = 0;

        for (int i = 1; i <= unitCount; i++) {
            model.Location unit = engine.getLocations().get(baseName + "." + i);
            if (unit != null) {
                totalContents += unit.getCurrentContents();
                totalEntries += unit.getTotalEntries();
                totalExits += unit.getTotalExits();
                maxContents = Math.max(maxContents, unit.getMaxContents());
                avgContents += unit.getAverageContents();
                avgUtilization += unit.getUtilization(currentTime);
            }
        }
        avgContents /= unitCount;
        avgUtilization /= unitCount;

        locationModel.addRow(new Object[]{
            Localization.getLocationDisplayName(baseName) + " (" + unitCount + " units)",
            unitCount,
            totalContents,
            (int)maxContents,
            String.format("%.2f", avgContents),
            String.format("%.1f%%", avgUtilization),
            totalEntries,
            totalExits
        });
    }

    private void updateResourceStatistics() {
        resourceModel.setRowCount(0);
        model.Crane crane = engine.getCrane();
        double currentTime = engine.getCurrentTime();

        resourceModel.addRow(new Object[]{
            crane.getName(),
            1,
            crane.getTotalTrips(),
            String.format("%.2f hrs", crane.getTotalTravelTime()),
            String.format("%.1f%%", crane.getUtilization(currentTime)),
            String.format("%.1f%%", crane.getUtilization(currentTime))
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
        double currentTime = engine.getCurrentTime();

        for (model.Location loc : engine.getLocations().values()) {
            if (loc.getName().startsWith("M")) {
                double util = loc.getUtilization(currentTime);
                if (util > maxUtil) {
                    maxUtil = util;
                    bottleneck = Localization.getLocationDisplayName(loc.getName());
                }
            }
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

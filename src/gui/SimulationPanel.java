package gui;

import core.SimulationEngine;
import model.*;
import statistics.*;
import utils.Localization;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;

public class SimulationPanel extends JPanel {
    private SimulationEngine engine;
    private AnimationPanel animationPanel;
    private JPanel statsPanel;
    private JTable entityStatsTable;
    private JTable locationStatsTable;
    private JTable craneStatsTable;
    private DefaultTableModel entityModel;
    private DefaultTableModel locationModel;
    private DefaultTableModel craneModel;

    public SimulationPanel(SimulationEngine engine) {
        this.engine = engine;
        setLayout(new BorderLayout(10, 10));

        initializeComponents();
        layoutComponents();
    }

    private void initializeComponents() {
        animationPanel = new AnimationPanel(engine);
        statsPanel = createStatsPanel();
    }

    private void layoutComponents() {
        // Left: Animation
        JPanel animationContainer = new JPanel(new BorderLayout());
        animationContainer.setBorder(BorderFactory.createTitledBorder("Animacion del Sistema"));
        animationContainer.add(animationPanel, BorderLayout.CENTER);
        animationContainer.add(createLegendPanel(), BorderLayout.SOUTH);

        // Right: Statistics
        JScrollPane statsScroll = new JScrollPane(statsPanel);
        statsScroll.setBorder(BorderFactory.createTitledBorder("Estadisticas en Tiempo Real"));
        statsScroll.setPreferredSize(new Dimension(400, 600));

        // Layout
        add(animationContainer, BorderLayout.CENTER);
        add(statsScroll, BorderLayout.EAST);
    }

    private JPanel createStatsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);

        // Entity Statistics Table
        String[] entityColumns = {"Valvula", "Llegadas", "Completadas", "En Sistema", "Tasa %"};
        entityModel = new DefaultTableModel(entityColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        entityStatsTable = createStyledTable(entityModel);
        panel.add(createTableSection("Estadisticas de Valvulas", entityStatsTable, 150));

        panel.add(Box.createVerticalStrut(10));

        // Location Statistics Table
        String[] locationColumns = {"Ubicacion", "Actual", "Capacidad", "Util %"};
        locationModel = new DefaultTableModel(locationColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        locationStatsTable = createStyledTable(locationModel);
        panel.add(createTableSection("Estadisticas de Ubicaciones", locationStatsTable, 250));

        panel.add(Box.createVerticalStrut(10));

        // Crane Statistics Table
        String[] craneColumns = {"Metrica", "Valor"};
        craneModel = new DefaultTableModel(craneColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        craneStatsTable = createStyledTable(craneModel);
        panel.add(createTableSection("Estadisticas de la Grua", craneStatsTable, 120));

        return panel;
    }

    private JTable createStyledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        table.setRowHeight(22);
        table.setGridColor(new Color(220, 220, 220));
        table.setSelectionBackground(new Color(184, 207, 229));

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 11));
        header.setBackground(new Color(70, 130, 180));
        header.setForeground(Color.WHITE);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 1; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        return table;
    }

    private JPanel createTableSection(String title, JTable table, int height) {
        JPanel section = new JPanel(new BorderLayout());
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        section.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(title),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(380, height - 40));
        section.add(scrollPane, BorderLayout.CENTER);

        return section;
    }

    private JPanel createLegendPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        panel.setBackground(new Color(245, 245, 250));

        for (Valve.Type type : Valve.Type.values()) {
            JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            item.setBackground(new Color(245, 245, 250));

            JLabel colorBox = new JLabel("██");
            colorBox.setForeground(type.getColor());
            colorBox.setFont(new Font("Monospaced", Font.BOLD, 14));

            JLabel label = new JLabel(type.getDisplayName());
            label.setFont(new Font("Segoe UI", Font.PLAIN, 10));

            item.add(colorBox);
            item.add(label);
            panel.add(item);
        }

        return panel;
    }

    public void updateDisplay() {
        // Update animation
        animationPanel.repaint();

        // Update statistics tables
        updateEntityStats();
        updateLocationStats();
        updateCraneStats();
    }

    private void updateEntityStats() {
        entityModel.setRowCount(0);
        Statistics stats = engine.getStatistics();

        for (Valve.Type type : Valve.Type.values()) {
            EntityStats es = stats.getEntityStats(type);
            if (es != null) {
                entityModel.addRow(new Object[]{
                    type.getDisplayName(),
                    es.getTotalArrivals(),
                    es.getTotalCompleted(),
                    es.getCurrentInSystem(),
                    String.format("%.1f%%", es.getCompletionRate())
                });
            }
        }
    }

    private void updateLocationStats() {
        locationModel.setRowCount(0);
        // Primero mostrar ubicaciones principales
        String[] mainLocations = {"DOCK", "STOCK", "Almacen_M1", "Almacen_M2", "Almacen_M3"};
        for (String name : mainLocations) {
            Location loc = engine.getLocations().get(name);
            if (loc != null) {
                String capacity = loc.getCapacity() == Integer.MAX_VALUE ? "∞" : String.valueOf(loc.getCapacity());
                locationModel.addRow(new Object[]{
                    Localization.getLocationDisplayName(loc.getName()),
                    loc.getCurrentContents(),
                    capacity,
                    String.format("%.1f%%", loc.getUtilization())
                });
            }
        }

        // Mostrar grupos de máquinas con sus totales (leer cantidades desde config)
        utils.Config config = utils.Config.getInstance();
        addMachineGroupStats("M1", config.getMachineUnits("m1"));
        addMachineGroupStats("M2", config.getMachineUnits("m2"));
        addMachineGroupStats("M3", config.getMachineUnits("m3"));
    }

    private void addMachineGroupStats(String baseName, int unitCount) {
        int totalContents = 0;
        double busySum = 0;

        for (int i = 1; i <= unitCount; i++) {
            Location unit = engine.getLocations().get(baseName + "." + i);
            if (unit != null) {
                totalContents += unit.getCurrentContents();
                busySum += unit.getTotalBusyTime();
            }
        }
        
        // Calcular utilización usando stats_units (igual que el reporte)
        utils.Config config = utils.Config.getInstance();
        double statsUnits = config.getMachineStatsUnits(baseName, unitCount);
        double scheduledPerUnit = engine.getShiftCalendar().getTotalWorkingHoursPerWeek();
        double currentTime = engine.getCurrentTime();
        double weeksSimulated = Math.max(currentTime, 1e-6) / 168.0;
        double totalScheduled = statsUnits * scheduledPerUnit * weeksSimulated;
        double avgUtilization = (totalScheduled > 0.0) ? (busySum / totalScheduled) * 100.0 : 0.0;
        // Limitar al 100% máximo
        avgUtilization = Math.min(avgUtilization, 100.0);

        locationModel.addRow(new Object[]{
            Localization.getLocationDisplayName(baseName) + " (" + unitCount + " unidades)",
            totalContents,
            unitCount,
            String.format("%.1f%%", avgUtilization)
        });
    }

    private void updateCraneStats() {
        craneModel.setRowCount(0);
        Crane crane = engine.getCrane();

        craneModel.addRow(new Object[]{"Estado", crane.isBusy() ? "OCUPADA" : "LIBRE"});
        craneModel.addRow(new Object[]{"Viajes Totales", crane.getTotalTrips()});
        craneModel.addRow(new Object[]{"Tiempo de Viaje", String.format("%.2f hrs", crane.getTotalTravelTime())});
            craneModel.addRow(new Object[]{"Utilizacion", String.format("%.1f%%", crane.getUtilization())});
        craneModel.addRow(new Object[]{"Transportando", crane.getCarryingValve() != null ?
            crane.getCarryingValve().toString() : "Ninguno"});
    }
}

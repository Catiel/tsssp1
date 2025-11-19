package gui;

import model.Valve;
import utils.Config;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class SimulationConfigDialog extends JDialog {
    private final Config config;
    private final MainFrame mainFrame;

    private JSpinner weeksSpinner;
    private final Map<String, JSpinner> arrivalSpinners = new LinkedHashMap<>();
    private final Map<String, JSpinner> machineUnitSpinners = new LinkedHashMap<>();
    private final Map<String, JSpinner> machineMultiplierSpinners = new LinkedHashMap<>();
    private final Map<String, JSpinner> holdTimeSpinners = new LinkedHashMap<>();

    public SimulationConfigDialog(MainFrame owner) {
        super(owner, "Parametros de Simulacion", true);
        this.mainFrame = owner;
        this.config = Config.getInstance();

        setLayout(new BorderLayout(10, 10));
        add(createContentPanel(), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
    }

    private JComponent createContentPanel() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("General", createGeneralTab());
        tabs.addTab("Llegadas", createArrivalsTab());
        tabs.addTab("Maquinas", createMachinesTab());
        tabs.addTab("Ubicaciones", createLocationsTab());
        return tabs;
    }

    private JPanel createGeneralTab() {
        JPanel panel = createFormPanel();
        weeksSpinner = new JSpinner(new SpinnerNumberModel(config.getSimulationWeeks(), 1, 52, 1));
        addFormRow(panel, "Semanas a simular", weeksSpinner, 0);
        return panel;
    }

    private JPanel createArrivalsTab() {
        JPanel panel = createFormPanel();
        Valve.Type[] types = Valve.Type.values();
        for (int i = 0; i < types.length; i++) {
            Valve.Type type = types[i];
            String key = type.name().toLowerCase(Locale.ROOT).replace("_", "");
            int quantity = config.getValveArrivalQuantity(key);
            JSpinner spinner = new JSpinner(new SpinnerNumberModel(quantity, 0, 1000, 1));
            arrivalSpinners.put(key, spinner);
            addFormRow(panel, "Llegadas " + type.getDisplayName(), spinner, i);
        }
        return panel;
    }

    private JPanel createMachinesTab() {
        JPanel panel = createFormPanel();
        String[] machines = {"m1", "m2", "m3"};
        for (int i = 0; i < machines.length; i++) {
            String machine = machines[i];
            int units = config.getMachineUnits(machine);
            double multiplier = config.getMachineTimeMultiplier(machine, 1.0);

            JSpinner unitSpinner = new JSpinner(new SpinnerNumberModel(units, 1, 100, 1));
            JSpinner multiplierSpinner = new JSpinner(new SpinnerNumberModel(multiplier, 0.1, 10.0, 0.01));

            machineUnitSpinners.put(machine, unitSpinner);
            machineMultiplierSpinners.put(machine, multiplierSpinner);

            int rowBase = i * 2;
            addFormRow(panel, "Unidades " + machine.toUpperCase(), unitSpinner, rowBase);
            addFormRow(panel, "Multiplicador tiempo " + machine.toUpperCase(), multiplierSpinner, rowBase + 1);
        }
        return panel;
    }

    private JPanel createLocationsTab() {
        JPanel panel = createFormPanel();
        String[][] settings = {
            {"location.dock.hold_time", "Hold Dock (hrs)"},
            {"location.almacen_m1.hold_time", "Hold Almacen M1 (hrs)"},
            {"location.almacen_m2.hold_time", "Hold Almacen M2 (hrs)"},
            {"location.almacen_m3.hold_time", "Hold Almacen M3 (hrs)"}
        };

        for (int i = 0; i < settings.length; i++) {
            String property = settings[i][0];
            double value = config.getDouble(property, 0.0);
            JSpinner spinner = new JSpinner(new SpinnerNumberModel(value, 0.0, 1000.0, 0.01));
            holdTimeSpinners.put(property, spinner);
            addFormRow(panel, settings[i][1], spinner, i);
        }
        return panel;
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return panel;
    }

    private void addFormRow(JPanel panel, String label, JComponent field, int row) {
        GridBagConstraints gbcLabel = new GridBagConstraints();
        gbcLabel.gridx = 0;
        gbcLabel.gridy = row;
        gbcLabel.insets = new Insets(5, 5, 5, 10);
        gbcLabel.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel(label), gbcLabel);

        GridBagConstraints gbcField = new GridBagConstraints();
        gbcField.gridx = 1;
        gbcField.gridy = row;
        gbcField.weightx = 1.0;
        gbcField.fill = GridBagConstraints.HORIZONTAL;
        gbcField.insets = new Insets(5, 5, 5, 5);
        panel.add(field, gbcField);
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelButton = new JButton("Cancelar");
        cancelButton.addActionListener(e -> dispose());

        JButton saveButton = new JButton("Guardar");
        saveButton.addActionListener(e -> saveConfiguration());

        panel.add(cancelButton);
        panel.add(saveButton);
        return panel;
    }

    private void saveConfiguration() {
        try {
            config.setProperty("simulation.weeks", ((Number) weeksSpinner.getValue()).intValue());

            for (Map.Entry<String, JSpinner> entry : arrivalSpinners.entrySet()) {
                String key = "arrival." + entry.getKey() + ".quantity";
                config.setProperty(key, ((Number) entry.getValue().getValue()).intValue());
            }

            for (Map.Entry<String, JSpinner> entry : machineUnitSpinners.entrySet()) {
                String key = "machine." + entry.getKey() + ".units";
                config.setProperty(key, ((Number) entry.getValue().getValue()).intValue());
            }

            for (Map.Entry<String, JSpinner> entry : machineMultiplierSpinners.entrySet()) {
                String key = "machine." + entry.getKey() + ".time_multiplier";
                config.setProperty(key, ((Number) entry.getValue().getValue()).doubleValue());
            }

            for (Map.Entry<String, JSpinner> entry : holdTimeSpinners.entrySet()) {
                config.setProperty(entry.getKey(), ((Number) entry.getValue().getValue()).doubleValue());
            }

            config.saveConfiguration();
            JOptionPane.showMessageDialog(this,
                "Parametros guardados. La simulacion se reiniciara.",
                "Configuracion",
                JOptionPane.INFORMATION_MESSAGE);
            dispose();
            mainFrame.reloadSimulationEngine();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "No se pudo guardar la configuracion: " + ex.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
}

package gui; // Declaración del paquete gui para interfaces gráficas

import model.Valve; // Importa clase Valve para acceder a tipos de válvulas
import utils.Config; // Importa clase Config para acceder a la configuración

import javax.swing.*; // Importa componentes Swing
import java.awt.*; // Importa clases AWT
import java.util.LinkedHashMap; // Importa LinkedHashMap que mantiene orden de inserción
import java.util.Locale; // Importa Locale para conversiones de texto
import java.util.Map; // Importa interfaz Map

public class SimulationConfigDialog extends JDialog { // Clase que extiende JDialog para mostrar diálogo modal de configuración
    private final Config config; // Referencia final a la configuración singleton
    private final MainFrame mainFrame; // Referencia final al frame principal

    private JSpinner weeksSpinner; // Spinner para configurar número de semanas a simular
    private final Map<String, JSpinner> arrivalSpinners = new LinkedHashMap<>(); // Mapa que asocia tipo de válvula con spinner de arribos (mantiene orden)
    private final Map<String, JSpinner> machineUnitSpinners = new LinkedHashMap<>(); // Mapa que asocia máquina con spinner de unidades
    private final Map<String, JSpinner> machineMultiplierSpinners = new LinkedHashMap<>(); // Mapa que asocia máquina con spinner de multiplicador de tiempo
    private final Map<String, JSpinner> holdTimeSpinners = new LinkedHashMap<>(); // Mapa que asocia locación con spinner de tiempo de retención

    public SimulationConfigDialog(MainFrame owner) { // Constructor que inicializa el diálogo de configuración
        super(owner, "Parametros de Simulacion", true); // Llama al constructor padre con título y modal=true
        this.mainFrame = owner; // Asigna referencia al frame principal
        this.config = Config.getInstance(); // Obtiene instancia singleton de configuración

        setLayout(new BorderLayout(10, 10)); // Establece BorderLayout con espaciado de 10 píxeles
        add(createContentPanel(), BorderLayout.CENTER); // Agrega panel de contenido al centro
        add(createButtonPanel(), BorderLayout.SOUTH); // Agrega panel de botones en la parte inferior

        pack(); // Ajusta tamaño del diálogo al contenido
        setLocationRelativeTo(owner); // Centra el diálogo respecto al frame principal
    }

    private JComponent createContentPanel() { // Método que crea el panel de contenido con pestañas
        JTabbedPane tabs = new JTabbedPane(); // Crea panel de pestañas
        tabs.addTab("General", createGeneralTab()); // Agrega pestaña general
        tabs.addTab("Llegadas", createArrivalsTab()); // Agrega pestaña de configuración de arribos
        tabs.addTab("Maquinas", createMachinesTab()); // Agrega pestaña de configuración de máquinas
        tabs.addTab("Ubicaciones", createLocationsTab()); // Agrega pestaña de configuración de ubicaciones
        return tabs; // Retorna panel de pestañas
    }

    private JPanel createGeneralTab() { // Método que crea la pestaña de configuración general
        JPanel panel = createFormPanel(); // Crea panel con layout de formulario
        weeksSpinner = new JSpinner(new SpinnerNumberModel(config.getSimulationWeeks(), 1, 52, 1)); // Crea spinner con valor actual, mínimo 1, máximo 52, incremento 1
        addFormRow(panel, "Semanas a simular", weeksSpinner, 0); // Agrega fila con etiqueta y spinner en fila 0
        return panel; // Retorna panel configurado
    }

    private JPanel createArrivalsTab() { // Método que crea la pestaña de configuración de arribos
        JPanel panel = createFormPanel(); // Crea panel con layout de formulario
        Valve.Type[] types = Valve.Type.values(); // Obtiene todos los tipos de válvulas del enum
        for (int i = 0; i < types.length; i++) { // Itera sobre cada tipo de válvula
            Valve.Type type = types[i]; // Obtiene tipo actual
            String key = type.name().toLowerCase(Locale.ROOT).replace("_", ""); // Normaliza nombre del tipo a minúsculas sin guiones bajos
            int quantity = config.getValveArrivalQuantity(key); // Obtiene cantidad configurada de arribos para este tipo
            JSpinner spinner = new JSpinner(new SpinnerNumberModel(quantity, 0, 1000, 1)); // Crea spinner con valor actual, mínimo 0, máximo 1000, incremento 1
            arrivalSpinners.put(key, spinner); // Guarda spinner en mapa con la clave normalizada
            addFormRow(panel, "Llegadas " + type.getDisplayName(), spinner, i); // Agrega fila con etiqueta descriptiva y spinner
        }
        return panel; // Retorna panel configurado
    }

    private JPanel createMachinesTab() { // Método que crea la pestaña de configuración de máquinas
        JPanel panel = createFormPanel(); // Crea panel con layout de formulario
        String[] machines = {"m1", "m2", "m3"}; // Array con nombres de las 3 máquinas
        for (int i = 0; i < machines.length; i++) { // Itera sobre cada máquina
            String machine = machines[i]; // Obtiene nombre de máquina actual
            int units = config.getMachineUnits(machine); // Obtiene número de unidades configuradas
            double multiplier = config.getMachineTimeMultiplier(machine, 1.0); // Obtiene multiplicador de tiempo configurado (default 1.0)

            JSpinner unitSpinner = new JSpinner(new SpinnerNumberModel(units, 1, 100, 1)); // Crea spinner para unidades: valor actual, mínimo 1, máximo 100, incremento 1
            JSpinner multiplierSpinner = new JSpinner(new SpinnerNumberModel(multiplier, 0.1, 10.0, 0.01)); // Crea spinner para multiplicador: valor actual, mínimo 0.1, máximo 10.0, incremento 0.01

            machineUnitSpinners.put(machine, unitSpinner); // Guarda spinner de unidades en mapa
            machineMultiplierSpinners.put(machine, multiplierSpinner); // Guarda spinner de multiplicador en mapa

            int rowBase = i * 2; // Calcula fila base para esta máquina (2 filas por máquina)
            addFormRow(panel, "Unidades " + machine.toUpperCase(), unitSpinner, rowBase); // Agrega fila de unidades
            addFormRow(panel, "Multiplicador tiempo " + machine.toUpperCase(), multiplierSpinner, rowBase + 1); // Agrega fila de multiplicador en siguiente fila
        }
        return panel; // Retorna panel configurado
    }

    private JPanel createLocationsTab() { // Método que crea la pestaña de configuración de ubicaciones
        JPanel panel = createFormPanel(); // Crea panel con layout de formulario
        String[][] settings = { // Array bidimensional con pares [clave_propiedad, etiqueta_display]
            {"location.dock.hold_time", "Hold Dock (hrs)"}, // Configuración de tiempo de retención en DOCK
            {"location.almacen_m1.hold_time", "Hold Almacen M1 (hrs)"}, // Configuración de tiempo de retención en Almacen_M1
            {"location.almacen_m2.hold_time", "Hold Almacen M2 (hrs)"}, // Configuración de tiempo de retención en Almacen_M2
            {"location.almacen_m3.hold_time", "Hold Almacen M3 (hrs)"} // Configuración de tiempo de retención en Almacen_M3
        };

        for (int i = 0; i < settings.length; i++) { // Itera sobre cada configuración
            String property = settings[i][0]; // Obtiene clave de propiedad
            double value = config.getDouble(property, 0.0); // Obtiene valor actual de configuración (default 0.0)
            JSpinner spinner = new JSpinner(new SpinnerNumberModel(value, 0.0, 1000.0, 0.01)); // Crea spinner: valor actual, mínimo 0.0, máximo 1000.0, incremento 0.01
            holdTimeSpinners.put(property, spinner); // Guarda spinner en mapa con clave de propiedad
            addFormRow(panel, settings[i][1], spinner, i); // Agrega fila con etiqueta descriptiva y spinner
        }
        return panel; // Retorna panel configurado
    }

    private JPanel createFormPanel() { // Método que crea un panel base con GridBagLayout para formularios
        JPanel panel = new JPanel(new GridBagLayout()); // Crea panel con GridBagLayout (permite posicionamiento flexible)
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Establece borde vacío de 10 píxeles alrededor
        return panel; // Retorna panel configurado
    }

    private void addFormRow(JPanel panel, String label, JComponent field, int row) { // Método que agrega una fila de formulario con etiqueta y campo
        GridBagConstraints gbcLabel = new GridBagConstraints(); // Crea restricciones para la etiqueta
        gbcLabel.gridx = 0; // Columna 0 (izquierda) para etiqueta
        gbcLabel.gridy = row; // Fila especificada por parámetro
        gbcLabel.insets = new Insets(5, 5, 5, 10); // Márgenes: arriba 5, izq 5, abajo 5, derecha 10 (más espacio entre etiqueta y campo)
        gbcLabel.anchor = GridBagConstraints.WEST; // Alinea etiqueta a la izquierda
        panel.add(new JLabel(label), gbcLabel); // Agrega etiqueta al panel con restricciones

        GridBagConstraints gbcField = new GridBagConstraints(); // Crea restricciones para el campo
        gbcField.gridx = 1; // Columna 1 (derecha) para campo
        gbcField.gridy = row; // Misma fila que la etiqueta
        gbcField.weightx = 1.0; // Permite que el campo se expanda horizontalmente
        gbcField.fill = GridBagConstraints.HORIZONTAL; // El campo llena todo el ancho disponible
        gbcField.insets = new Insets(5, 5, 5, 5); // Márgenes uniformes de 5 píxeles
        panel.add(field, gbcField); // Agrega campo al panel con restricciones
    }

    private JPanel createButtonPanel() { // Método que crea el panel de botones en la parte inferior del diálogo
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT)); // Crea panel con FlowLayout alineado a la derecha
        JButton cancelButton = new JButton("Cancelar"); // Crea botón de cancelar
        cancelButton.addActionListener(e -> dispose()); // Asocia acción que cierra el diálogo sin guardar

        JButton saveButton = new JButton("Guardar"); // Crea botón de guardar
        saveButton.addActionListener(e -> saveConfiguration()); // Asocia acción que guarda configuración

        panel.add(cancelButton); // Agrega botón cancelar
        panel.add(saveButton); // Agrega botón guardar
        return panel; // Retorna panel con botones
    }

    private void saveConfiguration() { // Método que guarda la configuración ingresada
        try { // Bloque try para capturar excepciones
            config.setProperty("simulation.weeks", ((Number) weeksSpinner.getValue()).intValue()); // Guarda número de semanas, convierte valor del spinner a int

            for (Map.Entry<String, JSpinner> entry : arrivalSpinners.entrySet()) { // Itera sobre cada spinner de arribos
                String key = "arrival." + entry.getKey() + ".quantity"; // Construye clave completa de propiedad
                config.setProperty(key, ((Number) entry.getValue().getValue()).intValue()); // Guarda cantidad de arribos, convierte a int
            }

            for (Map.Entry<String, JSpinner> entry : machineUnitSpinners.entrySet()) { // Itera sobre cada spinner de unidades de máquina
                String key = "machine." + entry.getKey() + ".units"; // Construye clave completa de propiedad
                config.setProperty(key, ((Number) entry.getValue().getValue()).intValue()); // Guarda número de unidades, convierte a int
            }

            for (Map.Entry<String, JSpinner> entry : machineMultiplierSpinners.entrySet()) { // Itera sobre cada spinner de multiplicador de tiempo
                String key = "machine." + entry.getKey() + ".time_multiplier"; // Construye clave completa de propiedad
                config.setProperty(key, ((Number) entry.getValue().getValue()).doubleValue()); // Guarda multiplicador, convierte a double
            }

            for (Map.Entry<String, JSpinner> entry : holdTimeSpinners.entrySet()) { // Itera sobre cada spinner de tiempo de retención
                config.setProperty(entry.getKey(), ((Number) entry.getValue().getValue()).doubleValue()); // Guarda tiempo de retención usando clave directa, convierte a double
            }

            config.saveConfiguration(); // Persiste toda la configuración al archivo
            JOptionPane.showMessageDialog(this, // Muestra diálogo de información
                "Parametros guardados. La simulacion se reiniciara.", // Mensaje informativo
                "Configuracion", // Título del diálogo
                JOptionPane.INFORMATION_MESSAGE); // Tipo de mensaje
            dispose(); // Cierra el diálogo de configuración
            mainFrame.reloadSimulationEngine(); // Recarga el motor de simulación en el frame principal con los nuevos parámetros
        } catch (Exception ex) { // Captura cualquier excepción durante el guardado
            JOptionPane.showMessageDialog(this, // Muestra diálogo de error
                "No se pudo guardar la configuracion: " + ex.getMessage(), // Mensaje de error con detalles de la excepción
                "Error", // Título del diálogo
                JOptionPane.ERROR_MESSAGE); // Tipo de mensaje
        }
    }
}

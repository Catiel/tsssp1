package gui; // Declaración del paquete gui para interfaces gráficas

import core.SimulationEngine; // Importa el motor de simulación
import model.Valve; // Importa clase Valve
import statistics.*; // Importa todas las clases de estadísticas
import utils.Config; // Importa clase Config para configuración
import utils.Localization; // Importa clase de localización de nombres
import javax.swing.*; // Importa componentes Swing
import javax.swing.table.*; // Importa clases de tablas Swing
import java.awt.*; // Importa clases AWT
import java.text.NumberFormat; // Importa NumberFormat para formateo de números
import java.util.Locale; // Importa Locale para configuración regional

public class StatisticsPanel extends JPanel { // Clase que extiende JPanel para mostrar panel de estadísticas detalladas
    private SimulationEngine engine; // Referencia al motor de simulación

    private JTable entityTable; // Tabla que muestra estadísticas detalladas de entidades (válvulas)
    private JTable locationTable; // Tabla que muestra estadísticas detalladas de ubicaciones
    private JTable resourceTable; // Tabla que muestra estadísticas detalladas de recursos (grúa)
    private JTextArea summaryArea; // Área de texto que muestra resumen general de la simulación

    private DefaultTableModel entityModel; // Modelo de datos para tabla de entidades
    private DefaultTableModel locationModel; // Modelo de datos para tabla de ubicaciones
    private DefaultTableModel resourceModel; // Modelo de datos para tabla de recursos

    private static final Locale DISPLAY_LOCALE = new Locale("es", "ES"); // Constante de locale español de España para formateo
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(DISPLAY_LOCALE); // Formateador de números con configuración española

    static { // Bloque estático de inicialización ejecutado una vez al cargar la clase
        NUMBER_FORMAT.setMinimumFractionDigits(2); // Establece mínimo de 2 decimales
        NUMBER_FORMAT.setMaximumFractionDigits(2); // Establece máximo de 2 decimales
    }

    public StatisticsPanel(SimulationEngine engine) { // Constructor que inicializa el panel con motor de simulación
        this.engine = engine; // Asigna motor recibido
        setLayout(new BorderLayout(10, 10)); // Establece BorderLayout con espaciado de 10 píxeles
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Establece borde vacío de 10 píxeles alrededor

        initializeComponents(); // Inicializa componentes del panel
        layoutComponents(); // Organiza componentes en el panel
    }

    public void setEngine(SimulationEngine engine) { // Método público que cambia el motor de simulación
        this.engine = engine; // Asigna nuevo motor
        updateStatistics(); // Actualiza todas las estadísticas
    }

    private void initializeComponents() { // Método que inicializa todos los componentes del panel
        // Entity Statistics Table
        String[] entityColumns = {"Nombre", "Total Salidas", "Cantidad actual En Sistema", // Define columnas de tabla de entidades (compatible con ProModel)
                  "Tiempo En Sistema Promedio (Min)", "Tiempo En lógica de movimiento Promedio (Min)",
                  "Tiempo Esperando Promedio (Min)", "Tiempo En Operación Promedio (Min)",
                  "Tiempo de Bloqueo Promedio (Min)"};
        entityModel = new DefaultTableModel(entityColumns, 0) { // Crea modelo de tabla con columnas y 0 filas iniciales
            @Override // Anotación de sobrescritura
            public boolean isCellEditable(int row, int column) { // Método que controla editabilidad de celdas
                return false; // Hace todas las celdas no editables
            }
        };
        entityTable = new JTable(entityModel); // Crea tabla con el modelo
        styleTable(entityTable); // Aplica estilo a la tabla

        // Location Statistics Table (columnas compatibles con ProModel)
        String[] locationColumns = {"Nombre", "Tiempo Programado (Hr)", "Capacidad", "Total Entradas", // Define columnas de tabla de ubicaciones
                       "Tiempo Por entrada Promedio (Min)", "Contenido Promedio", "Contenido Máximo",
                       "Contenido Actual", "% Utilización"};
        locationModel = new DefaultTableModel(locationColumns, 0) { // Crea modelo de tabla con columnas
            @Override // Anotación de sobrescritura
            public boolean isCellEditable(int row, int column) { // Método que controla editabilidad
                return false; // Hace todas las celdas no editables
            }
        };
        locationTable = new JTable(locationModel); // Crea tabla con el modelo
        styleTable(locationTable); // Aplica estilo a la tabla

        // Resource Statistics Table
        String[] resourceColumns = {"Recurso", "Unidades", "Tiempo Programado (Hr)", // Define columnas de tabla de recursos
                   "Tiempo de Trabajo (Min)", "Número de Usos", "Tiempo por Uso Prom (Min)",
                   "Tiempo Viaje para Utilizar Prom (Min)", "Tiempo Viaje a Estacionar Prom (Min)",
                   "% Bloqueado En Viaje", "% Utilización"};
        resourceModel = new DefaultTableModel(resourceColumns, 0) { // Crea modelo de tabla con columnas
            @Override // Anotación de sobrescritura
            public boolean isCellEditable(int row, int column) { // Método que controla editabilidad
                return false; // Hace todas las celdas no editables
            }
        };
        resourceTable = new JTable(resourceModel); // Crea tabla con el modelo
        styleTable(resourceTable); // Aplica estilo a la tabla

        // Summary Text Area
        summaryArea = new JTextArea(); // Crea área de texto para resumen
        summaryArea.setEditable(false); // Hace el área de texto no editable
        summaryArea.setFont(new Font("Monospaced", Font.PLAIN, 11)); // Establece fuente monoespaciada tamaño 11
        summaryArea.setForeground(Color.BLACK); // Establece color de texto negro
        summaryArea.setBackground(new Color(250, 250, 250)); // Establece fondo gris muy claro
        summaryArea.setBorder(BorderFactory.createCompoundBorder( // Crea borde compuesto
            BorderFactory.createTitledBorder("Resumen de la Simulacion"), // Borde exterior con título
            BorderFactory.createEmptyBorder(5, 5, 5, 5) // Borde interior vacío de 5 píxeles
        ));
    }

    private void styleTable(JTable table) { // Método que aplica estilo consistente a una tabla
        table.setFont(new Font("Segoe UI", Font.PLAIN, 11)); // Establece fuente Segoe UI tamaño 11
        table.setRowHeight(25); // Establece altura de filas en 25 píxeles
        table.setGridColor(new Color(220, 220, 220)); // Establece color gris claro para líneas de cuadrícula
        table.setSelectionBackground(new Color(184, 207, 229)); // Establece color azul claro para selección

        JTableHeader header = table.getTableHeader(); // Obtiene encabezado de la tabla
        header.setFont(new Font("Segoe UI", Font.BOLD, 12)); // Establece fuente negrita tamaño 12 para encabezado
        header.setBackground(new Color(100, 150, 200)); // Establece fondo azul para encabezado
        header.setForeground(Color.WHITE); // Establece texto blanco para encabezado

        // Center align numeric columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer(); // Crea renderizador de celdas
        centerRenderer.setHorizontalAlignment(JLabel.CENTER); // Establece alineación centrada
        for (int i = 1; i < table.getColumnCount(); i++) { // Itera sobre columnas (excepto la primera que es texto)
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer); // Aplica renderizador centrado a columna i
        }
    }

    private void layoutComponents() { // Método que organiza componentes en el panel
        JTabbedPane tabbedPane = new JTabbedPane(); // Crea panel de pestañas

        // Entity tab
        JScrollPane entityScroll = new JScrollPane(entityTable); // Crea scroll pane para tabla de entidades
        entityScroll.setBorder(BorderFactory.createTitledBorder("Estadisticas de Entidades")); // Establece borde con título
        tabbedPane.addTab("Entidades", entityScroll); // Agrega pestaña de entidades

        // Location tab
        JScrollPane locationScroll = new JScrollPane(locationTable); // Crea scroll pane para tabla de ubicaciones
        locationScroll.setBorder(BorderFactory.createTitledBorder("Estadisticas de Ubicaciones")); // Establece borde con título
        tabbedPane.addTab("Ubicaciones", locationScroll); // Agrega pestaña de ubicaciones

        // Resource tab
        JScrollPane resourceScroll = new JScrollPane(resourceTable); // Crea scroll pane para tabla de recursos
        resourceScroll.setBorder(BorderFactory.createTitledBorder("Estadisticas de Recursos")); // Establece borde con título
        tabbedPane.addTab("Recursos", resourceScroll); // Agrega pestaña de recursos

        // Summary tab
        JScrollPane summaryScroll = new JScrollPane(summaryArea); // Crea scroll pane para área de resumen
        tabbedPane.addTab("Resumen", summaryScroll); // Agrega pestaña de resumen

        add(tabbedPane, BorderLayout.CENTER); // Agrega panel de pestañas al centro del panel principal

        // Export button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT)); // Crea panel de botones alineado a la derecha
        JButton exportButton = new JButton("📄 Exportar Reporte"); // Crea botón de exportar con emoji
        exportButton.setFont(new Font("Segoe UI", Font.BOLD, 12)); // Establece fuente negrita
        exportButton.addActionListener(e -> exportReport()); // Asocia acción de exportar reporte
        buttonPanel.add(exportButton); // Agrega botón al panel

        add(buttonPanel, BorderLayout.SOUTH); // Agrega panel de botones en la parte inferior
    }

    public void updateStatistics() { // Método público que actualiza todas las estadísticas
        updateEntityStatistics(); // Actualiza tabla de entidades
        updateLocationStatistics(); // Actualiza tabla de ubicaciones
        updateResourceStatistics(); // Actualiza tabla de recursos
        updateSummary(); // Actualiza área de resumen
    }

    private void updateEntityStatistics() { // Método que actualiza tabla de estadísticas de entidades
        entityModel.setRowCount(0); // Limpia todas las filas de la tabla
        Statistics stats = engine.getStatistics(); // Obtiene estadísticas del motor

        Config config = Config.getInstance(); // Obtiene configuración

        for (Valve.Type type : Valve.Type.values()) { // Itera sobre cada tipo de válvula
            EntityStats es = stats.getEntityStats(type); // Obtiene estadísticas del tipo

            double systemMinutes = es.getAvgTimeInSystem() * 60.0; // Convierte tiempo en sistema de horas a minutos
            double movementMinutes = es.getAvgMovementTime() * 60.0; // Convierte tiempo de movimiento de horas a minutos
            double waitingMinutes = es.getAvgWaitingTime() * 60.0; // Convierte tiempo de espera de horas a minutos
            double processingMinutes = es.getAvgProcessingTime() * 60.0; // Convierte tiempo de procesamiento de horas a minutos
            double blockedMinutes = es.getAvgBlockedTime() * 60.0; // Convierte tiempo bloqueado de horas a minutos

            systemMinutes *= config.getEntityTimeScale(type, "system", 1.0); // Aplica factor de escala configurado para tiempo en sistema
            movementMinutes *= config.getEntityTimeScale(type, "movement", 1.0); // Aplica factor de escala para movimiento
            waitingMinutes *= config.getEntityTimeScale(type, "waiting", 1.0); // Aplica factor de escala para espera
            processingMinutes *= config.getEntityTimeScale(type, "processing", 1.0); // Aplica factor de escala para procesamiento
            blockedMinutes *= config.getEntityTimeScale(type, "blocked", 1.0); // Aplica factor de escala para bloqueo

            entityModel.addRow(new Object[]{ // Agrega nueva fila con datos formateados
                type.getDisplayName(), // Nombre descriptivo del tipo
                formatNumber(es.getTotalCompleted()), // Total de salidas (completadas)
                formatNumber(es.getCurrentInSystem()), // Cantidad actual en sistema
                formatNumber(systemMinutes), // Tiempo en sistema promedio en minutos
                formatNumber(movementMinutes), // Tiempo en movimiento promedio en minutos
                formatNumber(waitingMinutes), // Tiempo esperando promedio en minutos
                formatNumber(processingMinutes), // Tiempo en operación promedio en minutos
                formatNumber(blockedMinutes) // Tiempo de bloqueo promedio en minutos
            });
        }
    }

    private void updateLocationStatistics() { // Método que actualiza tabla de estadísticas de ubicaciones
        locationModel.setRowCount(0); // Limpia todas las filas de la tabla
        double currentTime = engine.getCurrentTime(); // Obtiene tiempo actual de simulación

        // Ubicaciones principales
        String[] mainLocations = {"DOCK", "STOCK", "Almacen_M1", "Almacen_M2", "Almacen_M3"}; // Array con nombres de ubicaciones principales
        for (String name : mainLocations) { // Itera sobre cada ubicación principal
            model.Location loc = engine.getLocations().get(name); // Obtiene ubicación del motor
            if (loc != null) { // Verifica si existe la ubicación
                addLocationRow(loc, currentTime); // Agrega fila con estadísticas de la ubicación
            }
        }

        // Grupos de máquinas (leer cantidades desde config)
        utils.Config config = utils.Config.getInstance(); // Obtiene configuración
        addMachineGroupRow("M1", config.getMachineUnits("m1"), currentTime); // Agrega fila del grupo M1
        addMachineGroupRow("M2", config.getMachineUnits("m2"), currentTime); // Agrega fila del grupo M2
        addMachineGroupRow("M3", config.getMachineUnits("m3"), currentTime); // Agrega fila del grupo M3
    }

    private void addLocationRow(model.Location loc, double currentTime) { // Método que agrega fila de estadísticas para una ubicación individual
        double scheduledTime = loc.getTotalObservedTime(); // Obtiene tiempo total observado de la ubicación
        if (scheduledTime <= 0.0) { // Si no hay tiempo observado
            scheduledTime = currentTime; // Usa tiempo actual de simulación
        }

        Config config = Config.getInstance(); // Obtiene configuración
        double statsScale = config.getLocationStatsScale(loc.getName(), 1.0); // Obtiene factor de escala de estadísticas (default 1.0)

        // Calcular tiempo por entrada promedio en minutos
        double avgTimePerEntry = 0.0; // Inicializa tiempo promedio por entrada
        int exits = loc.getTotalExits(); // Obtiene total de salidas
        if (exits > 0) { // Si hay salidas
            double totalResidenceTime = loc.getTotalResidenceTime(); // Obtiene tiempo total de residencia acumulado
            avgTimePerEntry = (totalResidenceTime / exits) * 60.0; // Calcula promedio y convierte a minutos
        }

        avgTimePerEntry *= statsScale; // Aplica factor de escala

        // Calcular utilización
        double utilization = 0.0; // Inicializa utilización
        double avgContents = loc.getAverageContents() * statsScale; // Calcula contenido promedio con factor de escala

        if (loc.getName().startsWith("Almacen_") && loc.getCapacity() > 0 && loc.getCapacity() < Integer.MAX_VALUE) { // Si es almacén con capacidad finita
            utilization = (avgContents / loc.getCapacity()) * 100.0; // Calcula utilización como porcentaje de capacidad
        } else if (!loc.getName().startsWith("Almacen_")) { // Si no es almacén (DOCK, STOCK)
            utilization = loc.getUtilization(); // Usa utilización calculada por la ubicación
        }

        locationModel.addRow(new Object[]{ // Agrega nueva fila con datos formateados
            Localization.getLocationDisplayName(loc.getName()), // Nombre localizado de la ubicación
            formatNumber(scheduledTime), // Tiempo programado en horas
            loc.getCapacity() == Integer.MAX_VALUE ? "999.999,00" : formatNumber(loc.getCapacity()), // Capacidad (infinito o número)
            formatNumber(loc.getTotalEntries()), // Total de entradas
            formatNumber(avgTimePerEntry), // Tiempo por entrada promedio en minutos
            formatNumber(avgContents), // Contenido promedio
            formatNumber(loc.getMaxContents()), // Contenido máximo alcanzado
            formatNumber(loc.getCurrentContents()), // Contenido actual
            formatNumber(utilization) // Utilización en porcentaje
        });
    }

    private void addMachineGroupRow(String baseName, int unitCount, double currentTime) { // Método que agrega fila de estadísticas para un grupo de máquinas
        if (unitCount <= 0) { // Si no hay unidades
            return; // Sale sin hacer nada
        }

        Config config = Config.getInstance(); // Obtiene configuración
        double statsUnits = config.getMachineStatsUnits(baseName, unitCount); // Obtiene factor de escala de unidades (para ajustar estadísticas)
        if (statsUnits <= 0.0) { // Si el factor es inválido
            statsUnits = unitCount; // Usa el número real de unidades
        }

        double locationScale = config.getLocationStatsScale(baseName, 1.0); // Obtiene factor de escala adicional
        double totalEntries = 0.0; // Inicializa contador de entradas totales
        double totalResidence = 0.0; // Inicializa acumulador de tiempo de residencia total
        double currentContents = 0.0; // Inicializa contenido actual
        double busySum = 0.0; // Inicializa suma de tiempo ocupado

        for (int i = 1; i <= unitCount; i++) { // Itera sobre cada unidad de la máquina
            model.Location unit = engine.getLocations().get(baseName + "." + i); // Obtiene unidad i
            if (unit == null) { // Si no existe
                continue; // Salta a siguiente
            }
            totalEntries += unit.getTotalEntries(); // Acumula entradas
            totalResidence += unit.getTotalResidenceTime(); // Acumula tiempo de residencia
            currentContents += unit.getCurrentContents(); // Acumula contenido actual
            busySum += unit.getTotalBusyTime(); // Acumula tiempo ocupado
        }

        double avgTimePerEntry = 0.0; // Inicializa tiempo promedio por entrada
        if (totalEntries > 0) { // Si hay entradas
            avgTimePerEntry = (totalResidence / totalEntries) * 60.0; // Calcula promedio y convierte a minutos
        }

        double scheduledPerUnit = engine.getShiftCalendar().getTotalWorkingHoursPerWeek(); // Obtiene horas laborables por semana por unidad
        double weeksSimulated = Math.max(currentTime, 1e-6) / 168.0; // Calcula semanas simuladas
        double scheduledTime = statsUnits * scheduledPerUnit * weeksSimulated; // Calcula tiempo programado total

        double throughputPerScheduledHour = scheduledTime > 1e-9 ? totalEntries / scheduledTime : 0.0; // Calcula throughput (entradas por hora programada)
        double avgContents = throughputPerScheduledHour * (avgTimePerEntry / 60.0) * locationScale; // Calcula contenido promedio usando Ley de Little escalado
        double maxContents = unitCount * locationScale; // Contenido máximo es número de unidades escalado
        double scaledCurrentContents = currentContents * locationScale; // Contenido actual escalado
        double avgUtilization = scheduledTime > 1e-9 ? Math.min((busySum / scheduledTime) * 100.0, 100.0) : 0.0; // Calcula utilización limitada al 100%

        locationModel.addRow(new Object[]{ // Agrega nueva fila con datos formateados
            Localization.getLocationDisplayName(baseName), // Nombre localizado de la máquina
            formatNumber(scheduledTime), // Tiempo programado en horas
            formatNumber(unitCount), // Capacidad (número de unidades)
            formatNumber(totalEntries), // Total de entradas
            formatNumber(avgTimePerEntry), // Tiempo por entrada promedio en minutos
            formatNumber(avgContents), // Contenido promedio
            formatNumber(maxContents), // Contenido máximo
            formatNumber(scaledCurrentContents), // Contenido actual
            formatNumber(avgUtilization) // Utilización promedio en porcentaje
        });
    }

    private String formatNumber(double value) { // Método que formatea número con locale español (2 decimales, separador de miles)
        return NUMBER_FORMAT.format(value); // Usa formateador configurado
    }

    private void updateResourceStatistics() { // Método que actualiza tabla de estadísticas de recursos (grúa)
        resourceModel.setRowCount(0); // Limpia todas las filas de la tabla
        model.Crane crane = engine.getCrane(); // Obtiene grúa del motor
        statistics.ResourceStats stats = engine.getStatistics().getCraneStats(); // Obtiene estadísticas de la grúa

        if (crane == null || stats == null) { // Verifica si grúa o estadísticas existen
            return; // Sale si no existen
        }

        resourceModel.addRow(new Object[]{ // Agrega fila con datos de la grúa formateados
            crane.getName(), // Nombre del recurso (grúa)
            stats.getUnits(), // Número de unidades
            formatNumber(stats.getScheduledHours()), // Tiempo programado en horas
            formatNumber(stats.getTotalWorkMinutes()), // Tiempo de trabajo total en minutos
            formatNumber(stats.getTotalTrips()), // Número de usos (viajes)
            formatNumber(stats.getAvgHandleMinutes()), // Tiempo por uso promedio en minutos
            formatNumber(stats.getAvgTravelMinutes()), // Tiempo de viaje para utilizar promedio en minutos
            formatNumber(stats.getAvgParkMinutes()), // Tiempo de viaje a estacionar promedio en minutos
            formatNumber(stats.getBlockedPercent()), // Porcentaje bloqueado en viaje
            formatNumber(stats.getCurrentUtilization()) // Utilización actual en porcentaje
        });
    }

    private void updateSummary() { // Método que actualiza área de resumen con reporte completo
        StringBuilder sb = new StringBuilder(); // Crea StringBuilder para construir texto
        sb.append(engine.getStatistics().generateReport(engine.getCurrentTime())); // Genera reporte de estadísticas

        // Add bottleneck analysis
        sb.append("\n┌─────────────────────────────────────────────────────────┐\n"); // Agrega borde superior de sección
        sb.append("│  ANALISIS DE CUELLOS DE BOTELLA                           │\n"); // Agrega título de sección
        sb.append("├─────────────────────────────────────────────────────────┤\n"); // Agrega separador

        double maxUtil = 0; // Inicializa utilización máxima
        String bottleneck = "Ninguno"; // Inicializa nombre del cuello de botella

        // Buscar solo entre agregados M1, M2, M3 (no unidades individuales)
        statistics.LocationStats m1Stats = engine.getStatistics().getLocationStats("M1"); // Obtiene estadísticas de M1
        statistics.LocationStats m2Stats = engine.getStatistics().getLocationStats("M2"); // Obtiene estadísticas de M2
        statistics.LocationStats m3Stats = engine.getStatistics().getLocationStats("M3"); // Obtiene estadísticas de M3

        if (m1Stats != null && m1Stats.getCurrentUtilization() > maxUtil) { // Si M1 existe y tiene mayor utilización
            maxUtil = m1Stats.getCurrentUtilization(); // Actualiza utilización máxima
            bottleneck = Localization.getLocationDisplayName("M1"); // Actualiza nombre del cuello de botella
        }
        if (m2Stats != null && m2Stats.getCurrentUtilization() > maxUtil) { // Si M2 existe y tiene mayor utilización
            maxUtil = m2Stats.getCurrentUtilization(); // Actualiza utilización máxima
            bottleneck = Localization.getLocationDisplayName("M2"); // Actualiza nombre del cuello de botella
        }
        if (m3Stats != null && m3Stats.getCurrentUtilization() > maxUtil) { // Si M3 existe y tiene mayor utilización
            maxUtil = m3Stats.getCurrentUtilization(); // Actualiza utilización máxima
            bottleneck = Localization.getLocationDisplayName("M3"); // Actualiza nombre del cuello de botella
        }

        sb.append(String.format("Cuello Principal: %s (%.1f%% de utilizacion)\n", // Formatea y agrega resultado del análisis
            bottleneck, maxUtil)); // Con nombre y porcentaje de utilización

        summaryArea.setText(sb.toString()); // Establece texto completo en área de resumen
        summaryArea.setCaretPosition(0); // Posiciona cursor al inicio del texto
    }

    private void exportReport() { // Método que exporta el reporte a un archivo de texto
        JFileChooser fileChooser = new JFileChooser(); // Crea selector de archivos
        fileChooser.setDialogTitle("Exportar Reporte de Simulacion"); // Establece título del diálogo
        fileChooser.setSelectedFile(new java.io.File("reporte_simulacion.txt")); // Establece nombre de archivo por defecto

        int userSelection = fileChooser.showSaveDialog(this); // Muestra diálogo de guardar y obtiene selección del usuario
        if (userSelection == JFileChooser.APPROVE_OPTION) { // Si el usuario aprobó (clic en Guardar)
            try { // Bloque try para capturar excepciones
                java.io.File fileToSave = fileChooser.getSelectedFile(); // Obtiene archivo seleccionado
                java.nio.file.Files.write(fileToSave.toPath(), // Escribe contenido al archivo
                    summaryArea.getText().getBytes()); // Convierte texto a bytes
                JOptionPane.showMessageDialog(this, // Muestra diálogo de éxito
                    "Reporte exportado exitosamente!", // Mensaje de éxito
                    "Exportacion Completa", // Título del diálogo
                    JOptionPane.INFORMATION_MESSAGE); // Tipo de mensaje
            } catch (Exception ex) { // Captura cualquier excepción durante exportación
                JOptionPane.showMessageDialog(this, // Muestra diálogo de error
                    "Error al exportar el reporte: " + ex.getMessage(), // Mensaje de error con detalles
                    "Error de Exportacion", // Título del diálogo
                    JOptionPane.ERROR_MESSAGE); // Tipo de mensaje
            }
        }
    }
}

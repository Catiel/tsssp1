package gui; // Declaración del paquete gui para interfaces gráficas

import core.SimulationEngine; // Importa el motor de simulación
import model.Valve; // Importa clase Valve
import statistics.*; // Importa todas las clases de estadísticas
import utils.Localization; // Importa clase de localización
import org.jfree.chart.*; // Importa clases de JFreeChart para gráficos
import org.jfree.chart.plot.*; // Importa clases de plots
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer; // Importa renderizador XY
import org.jfree.chart.renderer.category.BarRenderer; // Importa renderizador de barras
import org.jfree.data.xy.*; // Importa datasets XY
import org.jfree.data.category.*; // Importa datasets de categorías
import javax.swing.*; // Importa componentes Swing
import java.awt.*; // Importa clases AWT
import java.util.*; // Importa utilidades de Java

public class ChartsPanel extends JPanel { // Clase que extiende JPanel para mostrar gráficos de estadísticas
    private SimulationEngine engine; // Referencia al motor de simulación

    // Charts
    private ChartPanel entityProgressChart; // Panel de gráfico de progreso de válvulas
    private ChartPanel utilizationChart; // Panel de gráfico de utilización de máquinas
    private ChartPanel wipChart; // Panel de gráfico de trabajo en proceso
    private ChartPanel craneUtilizationChart; // Panel de gráfico de utilización de grúa
    private ChartPanel utilizationBarChart; // Panel de gráfico de barras de utilización

    // Datasets
    private XYSeriesCollection entityDataset; // Dataset para series de progreso de entidades
    private XYSeriesCollection utilizationDataset; // Dataset para series de utilización
    private XYSeriesCollection wipDataset; // Dataset para series de WIP
    private XYSeriesCollection craneDataset; // Dataset para series de grúa
    private DefaultCategoryDataset utilizationBarDataset; // Dataset para gráfico de barras

    public ChartsPanel(SimulationEngine engine) { // Constructor que inicializa el panel con motor de simulación
        this.engine = engine; // Asigna motor recibido
        setLayout(new BorderLayout(10, 10)); // Establece layout BorderLayout con espaciado de 10 píxeles
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Agrega borde vacío de 10 píxeles alrededor

        initializeCharts(); // Inicializa todos los gráficos
        layoutCharts(); // Organiza gráficos en el panel
    }

    public void setEngine(SimulationEngine engine) { // Método público que cambia el motor de simulación
        this.engine = engine; // Asigna nuevo motor
        clearDatasets(); // Limpia todos los datasets
    }

    private void initializeCharts() { // Método que inicializa todos los gráficos
        // Entity Progress Chart
        entityDataset = new XYSeriesCollection(); // Inicializa colección de series XY para entidades
        for (Valve.Type type : Valve.Type.values()) { // Itera sobre cada tipo de válvula
            entityDataset.addSeries(new XYSeries(type.getDisplayName())); // Agrega una serie por cada tipo
        }
        JFreeChart entityChart = ChartFactory.createXYLineChart( // Crea gráfico de líneas XY
            "Progreso de Valvulas Completadas", // Título del gráfico
            "Tiempo (horas)", // Etiqueta del eje X
            "Completadas Acumuladas", // Etiqueta del eje Y
            entityDataset, // Dataset a usar
            PlotOrientation.VERTICAL, // Orientación vertical
            true, true, false // Mostrar leyenda, tooltips, no URLs
        );
        styleChart(entityChart); // Aplica estilo personalizado al gráfico
        entityProgressChart = new ChartPanel(entityChart); // Crea panel de gráfico

        // Machine Utilization Chart
        utilizationDataset = new XYSeriesCollection(); // Inicializa colección de series para utilización
        for (String machine : Arrays.asList("M1", "M2", "M3")) { // Itera sobre las 3 máquinas
            utilizationDataset.addSeries(new XYSeries( // Agrega serie para cada máquina
                Localization.getLocationDisplayName(machine))); // Usa nombre localizado
        }
        JFreeChart utilChart = ChartFactory.createXYLineChart( // Crea gráfico de líneas XY
            "Utilizacion de Maquinas", // Título
            "Tiempo (horas)", // Eje X
            "Utilizacion (%)", // Eje Y
            utilizationDataset, // Dataset
            PlotOrientation.VERTICAL, // Orientación
            true, true, false // Opciones
        );
        styleChart(utilChart); // Aplica estilo
        XYPlot utilizationPlot = utilChart.getXYPlot(); // Obtiene plot del gráfico
        utilizationPlot.getRangeAxis().setRange(0, 100); // Establece rango Y de 0 a 100%
        utilizationChart = new ChartPanel(utilChart); // Crea panel

        // WIP (Work in Process) Chart
        wipDataset = new XYSeriesCollection(); // Inicializa colección para WIP
        for (String location : Arrays.asList("MALTEADO", "COCCION", "FERMENTACION", "EMBOTELLADO", "EMPACADO", "ALMACENAJE")) { // Itera sobre ubicaciones principales
            wipDataset.addSeries(new XYSeries( // Agrega serie para cada almacén
                Localization.getLocationDisplayName(location))); // Nombre localizado
        }
        // Add machine groups
        for (String machine : Arrays.asList("M1", "M2", "M3")) { // Itera sobre máquinas
            wipDataset.addSeries(new XYSeries( // Agrega serie para cada grupo de máquinas
                Localization.getLocationDisplayName(machine))); // Nombre localizado
        }
        JFreeChart wipChartObj = ChartFactory.createXYLineChart( // Crea gráfico de WIP
            "Trabajo en Proceso (WIP)", // Título
            "Tiempo (horas)", // Eje X
            "Numero de Valvulas", // Eje Y
            wipDataset, // Dataset
            PlotOrientation.VERTICAL, // Orientación
            true, true, false // Opciones
        );
        styleChart(wipChartObj); // Aplica estilo
        wipChart = new ChartPanel(wipChartObj); // Crea panel

        // Crane Utilization Chart
        craneDataset = new XYSeriesCollection(); // Inicializa colección para grúa
        craneDataset.addSeries(new XYSeries("Utilizacion de la Grua")); // Agrega serie única para grúa
        JFreeChart craneChart = ChartFactory.createXYLineChart( // Crea gráfico de utilización de grúa
            "Utilizacion de la Grua", // Título
            "Tiempo (horas)", // Eje X
            "Utilizacion (%)", // Eje Y
            craneDataset, // Dataset
            PlotOrientation.VERTICAL, // Orientación
            true, true, false // Opciones
        );
        styleChart(craneChart); // Aplica estilo
        XYPlot cranePlot = craneChart.getXYPlot(); // Obtiene plot
        cranePlot.getRangeAxis().setRange(0, 100); // Establece rango Y de 0 a 100%
        craneUtilizationChart = new ChartPanel(craneChart); // Crea panel

        // Utilization Bar Chart (nuevo)
        utilizationBarDataset = new DefaultCategoryDataset(); // Inicializa dataset de categorías
        JFreeChart barChart = ChartFactory.createBarChart( // Crea gráfico de barras
            "Utilizacion de Ubicaciones", // Título
            "Ubicacion", // Etiqueta del eje X (categorías)
            "Utilizacion (%)", // Etiqueta del eje Y
            utilizationBarDataset, // Dataset
            PlotOrientation.VERTICAL, // Orientación
            true, true, false // Opciones
        );
        styleBarChart(barChart); // Aplica estilo específico para barras
        CategoryPlot barPlot = barChart.getCategoryPlot(); // Obtiene plot de categorías
        barPlot.getRangeAxis().setRange(0, 100); // Establece rango Y de 0 a 100%
        utilizationBarChart = new ChartPanel(barChart); // Crea panel
    }

    private void styleChart(JFreeChart chart) { // Método que aplica estilo a gráficos XY
        chart.setBackgroundPaint(new Color(250, 250, 250)); // Establece fondo gris muy claro
        chart.getTitle().setFont(new Font("Segoe UI", Font.BOLD, 14)); // Establece fuente del título

        XYPlot plot = chart.getXYPlot(); // Obtiene plot del gráfico
        plot.setBackgroundPaint(Color.WHITE); // Fondo blanco para el área de datos
        plot.setDomainGridlinePaint(new Color(200, 200, 200)); // Color gris claro para líneas de cuadrícula del eje X
        plot.setRangeGridlinePaint(new Color(200, 200, 200)); // Color gris claro para líneas de cuadrícula del eje Y

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(); // Crea renderizador de líneas
        renderer.setDefaultShapesVisible(false); // No muestra puntos en las líneas
        renderer.setDefaultStroke(new BasicStroke(2.0f)); // Grosor de línea de 2 píxeles

        // Set colors for series
        Color[] colors = { // Array de colores para las diferentes series
            new Color(255, 23, 68),   // Rojo brillante (Valvula 1)
            new Color(0, 188, 212),   // Cyan/Turquesa (Valvula 2)
            new Color(118, 255, 3),   // Verde lima (Valvula 3)
            new Color(255, 152, 0),   // Naranja (Valvula 4)
            new Color(156, 39, 176),  // Púrpura
            new Color(33, 150, 243)   // Azul
        };

        for (int i = 0; i < colors.length && i < plot.getSeriesCount(); i++) { // Itera sobre series y colores
            renderer.setSeriesPaint(i, colors[i % colors.length]); // Asigna color a cada serie (con módulo para reciclar colores)
        }

        plot.setRenderer(renderer); // Aplica renderizador al plot
    }

    private void styleBarChart(JFreeChart chart) { // Método que aplica estilo a gráficos de barras
        chart.setBackgroundPaint(new Color(250, 250, 250)); // Establece fondo gris muy claro
        chart.getTitle().setFont(new Font("Segoe UI", Font.BOLD, 14)); // Establece fuente del título

        CategoryPlot plot = chart.getCategoryPlot(); // Obtiene plot de categorías
        plot.setBackgroundPaint(Color.WHITE); // Fondo blanco para el área de datos
        plot.setDomainGridlinePaint(new Color(200, 200, 200)); // Color gris para líneas de cuadrícula del eje X
        plot.setRangeGridlinePaint(new Color(200, 200, 200)); // Color gris para líneas de cuadrícula del eje Y

        BarRenderer renderer = (BarRenderer) plot.getRenderer(); // Obtiene renderizador de barras
        renderer.setDrawBarOutline(true); // Activa borde de las barras

        // Color para las barras de utilización
        renderer.setSeriesPaint(0, new Color(33, 150, 243)); // Color azul para la serie de utilización
    }

    private void layoutCharts() { // Método que organiza los gráficos en el panel
        JPanel topPanel = new JPanel(new GridLayout(1, 2, 10, 10)); // Panel superior con 1 fila, 2 columnas, espaciado 10
        topPanel.add(entityProgressChart); // Agrega gráfico de progreso de entidades
        topPanel.add(utilizationChart); // Agrega gráfico de utilización de máquinas

        JPanel middlePanel = new JPanel(new GridLayout(1, 2, 10, 10)); // Panel medio con 1 fila, 2 columnas
        middlePanel.add(wipChart); // Agrega gráfico de WIP
        middlePanel.add(craneUtilizationChart); // Agrega gráfico de utilización de grúa

        JPanel mainPanel = new JPanel(new GridLayout(2, 1, 10, 10)); // Panel principal con 2 filas, 1 columna
        mainPanel.add(topPanel); // Agrega panel superior
        mainPanel.add(middlePanel); // Agrega panel medio

        add(mainPanel, BorderLayout.CENTER); // Agrega panel principal al centro
        add(utilizationBarChart, BorderLayout.SOUTH); // Agrega gráfico de barras en la parte inferior
    }

    public void updateCharts() { // Método público que actualiza todos los gráficos
        updateEntityProgress(); // Actualiza gráfico de progreso de entidades
        updateMachineUtilization(); // Actualiza gráfico de utilización de máquinas
        updateWIP(); // Actualiza gráfico de WIP
        updateCraneUtilization(); // Actualiza gráfico de utilización de grúa
        updateUtilizationBars(); // Actualiza gráfico de barras de utilización
    }

    private void clearDatasets() { // Método que limpia todos los datasets
        for (int i = 0; i < entityDataset.getSeriesCount(); i++) { // Itera sobre series de entidades
            entityDataset.getSeries(i).clear(); // Limpia cada serie
        }
        for (int i = 0; i < utilizationDataset.getSeriesCount(); i++) { // Itera sobre series de utilización
            utilizationDataset.getSeries(i).clear(); // Limpia cada serie
        }
        for (int i = 0; i < wipDataset.getSeriesCount(); i++) { // Itera sobre series de WIP
            wipDataset.getSeries(i).clear(); // Limpia cada serie
        }
        for (int i = 0; i < craneDataset.getSeriesCount(); i++) { // Itera sobre series de grúa
            craneDataset.getSeries(i).clear(); // Limpia cada serie
        }
        utilizationBarDataset.clear(); // Limpia dataset de barras
    }

    private void updateEntityProgress() { // Método que actualiza gráfico de progreso de entidades
        Statistics stats = engine.getStatistics(); // Obtiene estadísticas del motor
        double currentTime = engine.getCurrentTime(); // Obtiene tiempo actual de simulación

        for (Valve.Type type : Valve.Type.values()) { // Itera sobre cada tipo de válvula
            EntityStats entityStats = stats.getEntityStats(type); // Obtiene estadísticas del tipo
            XYSeries series = entityDataset.getSeries(type.getDisplayName()); // Obtiene serie correspondiente

            if (series.getItemCount() == 0 || // Si la serie está vacía O
                series.getX(series.getItemCount() - 1).doubleValue() < currentTime) { // el último punto es anterior al tiempo actual
                series.add(currentTime, entityStats.getTotalCompleted()); // Agrega nuevo punto con tiempo y completadas acumuladas
            }
        }
    }

    private void updateMachineUtilization() { // Método que actualiza gráfico de utilización de máquinas
        double currentTime = engine.getCurrentTime(); // Obtiene tiempo actual

        for (String machineName : Arrays.asList("M1", "M2", "M3")) { // Itera sobre las 3 máquinas
            int unitCount = getMachineUnitCount(machineName); // Obtiene número de unidades de la máquina
            if (unitCount <= 0) { // Si no hay unidades
                continue; // Salta a siguiente máquina
            }
            String displayName = Localization.getLocationDisplayName(machineName); // Obtiene nombre localizado
            XYSeries series = utilizationDataset.getSeries(displayName); // Obtiene serie correspondiente

            double busySum = 0; // Inicializa suma de tiempo ocupado
            int countedUnits = 0; // Inicializa contador de unidades
            for (int i = 1; i <= unitCount; i++) { // Itera sobre cada unidad
                model.Location unit = engine.getLocations().get(machineName + "." + i); // Obtiene unidad i
                if (unit != null) { // Si existe
                    busySum += unit.getTotalBusyTime(); // Acumula tiempo ocupado
                    countedUnits++; // Incrementa contador
                }
            }

            if (countedUnits == 0) { // Si no se contaron unidades
                continue; // Salta a siguiente máquina
            }

            // Calcular utilización usando stats_units (igual que el reporte)
            utils.Config config = utils.Config.getInstance(); // Obtiene configuración
            double statsUnits = config.getMachineStatsUnits(machineName, countedUnits); // Obtiene factor de escala de estadísticas
            double scheduledPerUnit = engine.getShiftCalendar().getTotalWorkingHoursPerWeek(); // Obtiene horas laborables por semana
            double weeksSimulated = Math.max(currentTime, 1e-6) / 168.0; // Calcula semanas simuladas
            double totalScheduled = statsUnits * scheduledPerUnit * weeksSimulated; // Calcula total de horas programadas
            double utilization = (totalScheduled > 0.0) ? (busySum / totalScheduled) * 100.0 : 0.0; // Calcula utilización porcentual
            // Limitar al 100% máximo
            utilization = Math.min(utilization, 100.0); // Limita al 100%

            if (series.getItemCount() == 0 || // Si la serie está vacía O
                series.getX(series.getItemCount() - 1).doubleValue() < currentTime) { // el último punto es anterior
                series.add(currentTime, utilization); // Agrega nuevo punto
            }
        }
    }

    private void updateWIP() { // Método que actualiza gráfico de trabajo en proceso
        double currentTime = engine.getCurrentTime(); // Obtiene tiempo actual

        // Update almacenes
        for (String locationName : Arrays.asList("MALTEADO", "COCCION", "FERMENTACION", "EMBOTELLADO", "EMPACADO", "ALMACENAJE")) { // Itera sobre ubicaciones principales
            model.Location location = engine.getLocations().get(locationName); // Obtiene locación
            String displayName = Localization.getLocationDisplayName(locationName); // Obtiene nombre localizado
            XYSeries series = wipDataset.getSeries(displayName); // Obtiene serie correspondiente

            int contents = location.getCurrentContents(); // Obtiene contenido actual

            if (series.getItemCount() == 0 || // Si la serie está vacía O
                series.getX(series.getItemCount() - 1).doubleValue() < currentTime) { // el último punto es anterior
                series.add(currentTime, contents); // Agrega nuevo punto
            }
        }

        // Update machine groups (sum of all units)
        updateMachineGroupWIP("M1", currentTime); // Actualiza WIP del grupo M1
        updateMachineGroupWIP("M2", currentTime); // Actualiza WIP del grupo M2
        updateMachineGroupWIP("M3", currentTime); // Actualiza WIP del grupo M3
    }

    private void updateMachineGroupWIP(String baseName, double currentTime) { // Método que actualiza WIP de un grupo de máquinas
        int unitCount = getMachineUnitCount(baseName); // Obtiene número de unidades
        if (unitCount <= 0) { // Si no hay unidades
            return; // Sale sin hacer nada
        }
        int totalContents = 0; // Inicializa contador de contenido total
        for (int i = 1; i <= unitCount; i++) { // Itera sobre cada unidad
            model.Location unit = engine.getLocations().get(baseName + "." + i); // Obtiene unidad i
            if (unit != null) { // Si existe
                totalContents += unit.getCurrentContents(); // Acumula contenido
            }
        }

        String displayName = Localization.getLocationDisplayName(baseName); // Obtiene nombre localizado
        XYSeries series = wipDataset.getSeries(displayName); // Obtiene serie correspondiente

        if (series.getItemCount() == 0 || // Si la serie está vacía O
            series.getX(series.getItemCount() - 1).doubleValue() < currentTime) { // el último punto es anterior
            series.add(currentTime, totalContents); // Agrega nuevo punto con contenido total
        }
    }

    private void updateCraneUtilization() { // Método que actualiza gráfico de utilización de grúa
        double currentTime = engine.getCurrentTime(); // Obtiene tiempo actual
        model.Crane crane = engine.getCrane(); // Obtiene grúa
        XYSeries series = craneDataset.getSeries("Utilizacion de la Grua"); // Obtiene serie de grúa

        double utilization = crane.getUtilization(); // Obtiene utilización porcentual de la grúa

        if (series.getItemCount() == 0 || // Si la serie está vacía O
            series.getX(series.getItemCount() - 1).doubleValue() < currentTime) { // el último punto es anterior
            series.add(currentTime, utilization); // Agrega nuevo punto
        }
    }

    private void updateUtilizationBars() { // Método que actualiza gráfico de barras de utilización
        utilizationBarDataset.clear(); // Limpia dataset

        double currentTime = engine.getCurrentTime(); // Obtiene tiempo actual

        // Agregar datos para cada máquina
        for (String machineName : Arrays.asList("M1", "M2", "M3")) { // Itera sobre las 3 máquinas
            int unitCount = getMachineUnitCount(machineName); // Obtiene número de unidades
            if (unitCount <= 0) { // Si no hay unidades
                continue; // Salta a siguiente
            }
            String displayName = Localization.getLocationDisplayName(machineName); // Obtiene nombre localizado

            // Calcular utilización
            double busySum = 0; // Inicializa suma de tiempo ocupado
            int countedUnits = 0; // Inicializa contador de unidades
            for (int i = 1; i <= unitCount; i++) { // Itera sobre cada unidad
                model.Location unit = engine.getLocations().get(machineName + "." + i); // Obtiene unidad i
                if (unit != null) { // Si existe
                    busySum += unit.getTotalBusyTime(); // Acumula tiempo ocupado
                    countedUnits++; // Incrementa contador
                }
            }

            if (countedUnits == 0) continue; // Si no hay unidades contadas, salta a siguiente

            utils.Config config = utils.Config.getInstance(); // Obtiene configuración
            double statsUnits = config.getMachineStatsUnits(machineName, countedUnits); // Obtiene factor de escala
            double scheduledPerUnit = engine.getShiftCalendar().getTotalWorkingHoursPerWeek(); // Obtiene horas por semana
            double weeksSimulated = Math.max(currentTime, 1e-6) / 168.0; // Calcula semanas simuladas
            double totalScheduled = statsUnits * scheduledPerUnit * weeksSimulated; // Calcula total programado
            double utilization = (totalScheduled > 0.0) ? (busySum / totalScheduled) * 100.0 : 0.0; // Calcula utilización
            utilization = Math.min(utilization, 100.0); // Limita al 100%

            // Solo agregar utilización
            utilizationBarDataset.addValue(utilization, "Utilización", displayName); // Agrega valor a dataset (valor, serie, categoría)
        }

        // Agregar almacenes
        for (String almacenName : Arrays.asList("MALTEADO", "COCCION", "FERMENTACION", "EMBOTELLADO", "EMPACADO", "ALMACENAJE")) { // Itera sobre ubicaciones principales
            model.Location location = engine.getLocations().get(almacenName); // Obtiene locación
            String displayName = Localization.getLocationDisplayName(almacenName); // Obtiene nombre localizado

            if (location != null) { // Si existe la locación
                double utilization = location.getUtilization(); // Obtiene utilización
                utilizationBarDataset.addValue(utilization, "Utilización", displayName); // Agrega valor a dataset
            }
        }
    }

    private int getMachineUnitCount(String baseName) { // Método que cuenta unidades de una máquina
        int count = 0; // Inicializa contador
        while (engine.getLocations().containsKey(baseName + "." + (count + 1))) { // Mientras exista siguiente unidad
            count++; // Incrementa contador
        }
        if (count == 0) { // Si no se encontraron unidades
            utils.Config config = utils.Config.getInstance(); // Obtiene configuración
            return config.getMachineUnits(baseName.toLowerCase(Locale.ROOT)); // Retorna valor configurado
        }
        return count; // Retorna número de unidades encontradas
    }
}

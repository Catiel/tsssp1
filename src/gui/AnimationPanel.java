package gui; // Declaración del paquete gui para interfaces gráficas

import core.SimulationEngine; // Importa el motor de simulación
import model.*; // Importa todas las clases del modelo
import utils.Localization; // Importa clase de localización de nombres
import javax.swing.*; // Importa componentes Swing
import javax.swing.Timer; // Importa Timer de Swing para animación
import java.awt.*; // Importa clases AWT para gráficos
import java.awt.geom.AffineTransform; // Importa transformaciones afines para zoom
import java.util.*; // Importa utilidades de Java
import java.util.List; // Importa List explícitamente

public class AnimationPanel extends JPanel { // Clase que extiende JPanel para mostrar animación de simulación
    private SimulationEngine engine; // Referencia al motor de simulación
    private Map<String, Point> locationPositions; // Mapa de posiciones de locaciones por nombre
    private Timer animationTimer; // Temporizador para actualizar frames de animación
    private double zoomFactor = 1.0; // Factor de zoom actual (1.0 = 100%)
    private static final double MIN_ZOOM = 0.5; // Zoom mínimo permitido (50%)
    private static final double MAX_ZOOM = 2.5; // Zoom máximo permitido (250%)
    private static final double ZOOM_STEP = 0.1; // Incremento de zoom por cada paso (10%)

    public AnimationPanel(SimulationEngine engine) { // Constructor que inicializa el panel con motor de simulación
        this.engine = engine; // Asigna motor recibido
        setBackground(new Color(245, 248, 252)); // Establece color de fondo azul claro
        setPreferredSize(new Dimension(1100, 650)); // Establece tamaño preferido del panel
        setFocusable(true); // Permite que el panel reciba foco para eventos

        initializeLayout(); // Inicializa posiciones de locaciones
        startAnimation(); // Inicia temporizador de animación

        addMouseWheelListener(e -> { // Agrega listener para zoom con rueda del mouse
            double rotation = -e.getPreciseWheelRotation(); // Obtiene dirección de rotación (invertida)
            if (Math.abs(rotation) < 1e-6) { // Verifica si hay rotación significativa
                return; // Sale si no hay rotación
            }
            adjustZoom(rotation * ZOOM_STEP); // Ajusta zoom según rotación
        });
    }

    private void initializeLayout() { // Método que inicializa el layout de locaciones
        locationPositions = new HashMap<>(); // Inicializa mapa de posiciones vacío
        refreshLocationPositions(); // Refresca posiciones desde el motor
    }

    private long lastFrameTime = System.currentTimeMillis(); // Variable que almacena tiempo del último frame para cálculo de delta

    private void startAnimation() { // Método que inicia el temporizador de animación
        // 60 FPS animation - usa tiempo REAL para animación suave independiente de la velocidad de simulación
        animationTimer = new Timer(16, e -> { // Crea temporizador que se ejecuta cada 16ms (~60 FPS)
            long currentTime = System.currentTimeMillis(); // Obtiene tiempo actual en milisegundos
            double deltaSeconds = (currentTime - lastFrameTime) / 1000.0; // Calcula tiempo transcurrido desde último frame en segundos
            lastFrameTime = currentTime; // Actualiza tiempo del último frame

            // Actualizar posición visual de todos los operadores basada en TIEMPO REAL
            engine.getOperators().values().forEach(operator -> 
                operator.updateVisualPosition(deltaSeconds)
            );

            repaint(); // Solicita repintado del panel
        });
        animationTimer.start(); // Inicia el temporizador
    }

    private void refreshLocationPositions() { // Método que refresca posiciones de locaciones desde el motor
        Map<String, Location> locs = engine.getLocations(); // Obtiene mapa de locaciones
        for (Map.Entry<String, Location> entry : locs.entrySet()) { // Itera sobre cada locación
            locationPositions.put(entry.getKey(), new Point(entry.getValue().getPosition())); // Guarda copia de posición
        }
    }

    private void adjustZoom(double delta) { // Método que ajusta el factor de zoom
        double newZoom = zoomFactor + delta; // Calcula nuevo zoom
        if (newZoom < MIN_ZOOM) { // Verifica si está por debajo del mínimo
            newZoom = MIN_ZOOM; // Limita al mínimo
        } else if (newZoom > MAX_ZOOM) { // Verifica si está por encima del máximo
            newZoom = MAX_ZOOM; // Limita al máximo
        }

        if (Math.abs(newZoom - zoomFactor) > 1e-6) { // Verifica si hay cambio significativo
            zoomFactor = newZoom; // Actualiza factor de zoom
            repaint(); // Solicita repintado
        }
    }

    public void setEngine(SimulationEngine newEngine) { // Método público que cambia el motor de simulación
        this.engine = newEngine; // Asigna nuevo motor
        refreshLocationPositions(); // Refresca posiciones
    }

    // Método eliminado: getMachineUnitCount (obsoleto en sistema de cervecería)

    @Override // Anotación de sobrescritura
    protected void paintComponent(Graphics g) { // Método que dibuja el componente
        super.paintComponent(g); // Llama al método padre para limpiar fondo
        Graphics2D g2d = (Graphics2D) g; // Castea a Graphics2D para operaciones avanzadas

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // Activa antialiasing para suavizar bordes
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON); // Activa antialiasing de texto

        refreshLocationPositions(); // Refresca posiciones antes de dibujar

        AffineTransform originalTransform = g2d.getTransform(); // Guarda transformación original
        double centerX = getWidth() / 2.0; // Calcula centro X del panel
        double centerY = getHeight() / 2.0; // Calcula centro Y del panel
        g2d.translate(centerX, centerY); // Traslada origen al centro
        g2d.scale(zoomFactor, zoomFactor); // Aplica factor de zoom
        g2d.translate(-centerX, -centerY); // Traslada de vuelta

        // Draw network paths and nodes
        drawNetwork(g2d); // Dibuja red de caminos y nodos

        Set<Valve> valvesInTransit = getValvesCurrentlyInTransit();

        // Draw all brewery locations
        drawLocation(g2d, "SILO_GRANDE", valvesInTransit); // Silo de granos
        drawLocation(g2d, "MALTEADO", valvesInTransit); // Malteado
        drawLocation(g2d, "SECADO", valvesInTransit); // Secado
        drawLocation(g2d, "MOLIENDA", valvesInTransit); // Molienda
        drawLocation(g2d, "MACERADO", valvesInTransit); // Macerado
        drawLocation(g2d, "FILTRADO", valvesInTransit); // Filtrado
        drawLocation(g2d, "COCCION", valvesInTransit); // Cocción
        drawLocation(g2d, "SILO_LUPULO", valvesInTransit); // Silo de lúpulo
        drawLocation(g2d, "ENFRIAMIENTO", valvesInTransit); // Enfriamiento
        drawLocation(g2d, "FERMENTACION", valvesInTransit); // Fermentación
        drawLocation(g2d, "SILO_LEVADURA", valvesInTransit); // Silo de levadura
        drawLocation(g2d, "MADURACION", valvesInTransit); // Maduración
        drawLocation(g2d, "INSPECCION", valvesInTransit); // Inspección
        drawLocation(g2d, "EMBOTELLADO", valvesInTransit); // Embotellado
        drawLocation(g2d, "ETIQUETADO", valvesInTransit); // Etiquetado
        drawLocation(g2d, "ALMACEN_CAJAS", valvesInTransit); // Almacén de cajas
        drawLocation(g2d, "EMPACADO", valvesInTransit); // Empacado
        drawLocation(g2d, "ALMACENAJE", valvesInTransit); // Almacenaje
        drawLocation(g2d, "MERCADO", valvesInTransit); // Mercado (salida)

        // Draw operators (must be AFTER locations so they're on top)
        drawOperators(g2d); // Dibuja todos los operadores encima de todo

        g2d.setTransform(originalTransform); // Restaura transformación original
    }

    private void drawNetwork(Graphics2D g2d) { // Método que dibuja la red de caminos
        PathNetwork network = engine.getPathNetwork(); // Obtiene red de caminos
        if (network == null) { // Verifica si existe
            return; // Sale si no existe
        }

        g2d.setStroke(new BasicStroke(3.5f, // Establece grosor de línea 3.5
            BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, // Bordes redondeados
            new float[]{12, 6}, 0)); // Patrón de línea discontinua (12 píxeles línea, 6 espacio)
        g2d.setColor(new Color(70, 130, 220, 120)); // Color azul semi-transparente

        for (PathNetwork.PathEdge edge : network.getEdges()) { // Itera sobre cada arista
            Point from = network.getNodePosition(edge.getFrom()); // Obtiene posición del nodo origen
            Point to = network.getNodePosition(edge.getTo()); // Obtiene posición del nodo destino
            if (from != null && to != null) { // Verifica que ambos existan
                g2d.drawLine(from.x, from.y, to.x, to.y); // Dibuja línea entre nodos
            }
        }

        // Highlight operator paths
        for (Operator operator : engine.getOperators().values()) {
            List<Point> pathPoints = operator.getCurrentPathPoints();
            if (pathPoints.size() < 2) {
                continue;
            }
            Color pathColor = getOperatorColor(operator.getName());
            g2d.setColor(new Color(pathColor.getRed(), pathColor.getGreen(), pathColor.getBlue(), 190));
            g2d.setStroke(new BasicStroke(operator.isBusy() ? 5.0f : 3.5f,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < pathPoints.size() - 1; i++) {
                Point from = pathPoints.get(i);
                Point to = pathPoints.get(i + 1);
                g2d.drawLine(from.x, from.y, to.x, to.y);
            }
        }

        // Draw nodes
        g2d.setStroke(new BasicStroke(1.5f)); // Establece grosor de línea para nodos
        for (Map.Entry<String, Point> entry : network.getNodePositions().entrySet()) { // Itera sobre cada nodo
            Point node = entry.getValue(); // Obtiene posición del nodo
            g2d.setColor(new Color(35, 73, 147)); // Color azul oscuro para relleno
            g2d.fillOval(node.x - 6, node.y - 6, 12, 12); // Dibuja círculo relleno
            g2d.setColor(Color.WHITE); // Color blanco para borde
            g2d.drawOval(node.x - 6, node.y - 6, 12, 12); // Dibuja borde del círculo
        }
    }

    private void drawLocation(Graphics2D g2d, String name, Set<Valve> valvesInTransit) { // Método que dibuja una locación individual
        Location loc = engine.getLocations().get(name); // Obtiene locación del motor
        if (loc == null) return; // Sale si no existe

        Point pos = locationPositions.get(name); // Obtiene posición guardada
        if (pos == null) return; // Sale si no hay posición

        int w = 140, h = 110; // Dimensiones del rectángulo de locación
        int x = pos.x - w/2; // Calcula X centrado
        int y = pos.y - h/2; // Calcula Y centrado

        // Determine colors
        Color bgColor, borderColor; // Variables para colores
        if (name.equals("DOCK")) { // Si es DOCK
            bgColor = new Color(135, 206, 250, 220); // Azul claro semi-transparente
            borderColor = new Color(70, 130, 200); // Azul medio
        } else if (name.equals("STOCK")) { // Si es STOCK
            bgColor = new Color(144, 238, 144, 220); // Verde claro semi-transparente
            borderColor = new Color(70, 180, 70); // Verde medio
        } else if (name.startsWith("Almacen")) { // Si es almacén
            bgColor = new Color(255, 248, 220, 220); // Amarillo claro semi-transparente
            borderColor = new Color(218, 165, 32); // Dorado
        } else { // Cualquier otra locación
            bgColor = new Color(200, 200, 220, 220); // Gris claro semi-transparente
            borderColor = new Color(100, 100, 130); // Gris medio
        }

        // Shadow
        g2d.setColor(new Color(0, 0, 0, 40)); // Color negro semi-transparente para sombra
        g2d.fillRoundRect(x + 4, y + 4, w, h, 12, 12); // Dibuja sombra desplazada

        // Background
        g2d.setColor(bgColor); // Establece color de fondo
        g2d.fillRoundRect(x, y, w, h, 12, 12); // Dibuja rectángulo con esquinas redondeadas

        // Border
        g2d.setColor(borderColor); // Establece color de borde
        g2d.setStroke(new BasicStroke(3.0f)); // Grosor de borde
        g2d.drawRoundRect(x, y, w, h, 12, 12); // Dibuja borde

        // Draw icon based on type
        if (name.equals("MERCADO")) { // Si es MERCADO (salida)
            drawPalletIcon(g2d, x + w/2, y + 40, true); // Dibuja ícono de pallet
        } else if (name.equals("ALMACENAJE") || name.equals("ALMACEN_CAJAS") || name.startsWith("SILO")) { // Si es almacenamiento
            drawStorageIcon(g2d, x + w/2, y + 40); // Dibuja ícono de almacenamiento
        } else { // Si es proceso (malteado, cocción, fermentación, etc.)
            drawMachineIcon(g2d, x + w/2, y + 45, loc.getProcessingSize() > 0); // Dibuja ícono de máquina
        }

        // Label
        g2d.setColor(Color.BLACK); // Color negro para texto
        g2d.setFont(new Font("Arial", Font.BOLD, 12)); // Fuente Arial negrita tamaño 12
        g2d.drawString(Localization.getLocationDisplayName(name), x + 8, y + 18); // Dibuja nombre localizado

        // Capacity
        int displayedContents = loc.getCurrentContents(); // Obtiene contenido actual
        if (name.equals("MERCADO")) { // Si es MERCADO (salida)
            displayedContents = engine.getCompletedInventoryCount(); // Usa contador de inventario completado
        }

        String cap = String.format("%d/%s", displayedContents, // Formatea capacidad
            loc.getCapacity() == Integer.MAX_VALUE ? "∞" : String.valueOf(loc.getCapacity())); // Usa infinito o número
        g2d.setFont(new Font("Arial", Font.PLAIN, 11)); // Fuente Arial normal tamaño 11
        g2d.drawString(cap, x + 8, y + 33); // Dibuja capacidad

        // Draw valves - excluir la que está siendo transportada si la animación está en progreso
        List<Valve> valves = new ArrayList<>(loc.getAllValves()); // Crea copia de lista de válvulas
        if (name.equals("MERCADO") && !valves.isEmpty()) { // Si es MERCADO y hay válvulas
            valves.clear(); // Limpia lista (MERCADO no muestra válvulas visualmente)
        }

        // Ocultar válvulas que están actualmente en tránsito para evitar duplicados visuales
        if (!valvesInTransit.isEmpty()) {
            valves.removeIf(valvesInTransit::contains);
        }

        if (!valves.isEmpty()) { // Si hay válvulas para mostrar
            int count = Math.min(10, valves.size()); // Muestra máximo 10 válvulas
            for (int i = 0; i < count; i++) { // Itera sobre válvulas a mostrar
                Valve v = valves.get(i); // Obtiene válvula i
                int vx = x + 10 + (i % 5) * 24; // Calcula posición X (5 columnas)
                int vy = y + h - 28 + (i / 5) * 16; // Calcula posición Y (2 filas)
                drawValve(g2d, v, vx, vy); // Dibuja válvula
            }
            if (valves.size() > 10) { // Si hay más de 10 válvulas
                g2d.setColor(Color.BLACK); // Color negro
                g2d.setFont(new Font("Arial", Font.BOLD, 9)); // Fuente pequeña
                g2d.drawString("+" + (valves.size() - 10), x + w - 25, y + h - 10); // Muestra contador de válvulas restantes
            }
        }
    }

    private void drawPalletIcon(Graphics2D g2d, int cx, int cy, boolean isStock) { // Método que dibuja ícono de pallet
        g2d.setColor(new Color(139, 90, 43)); // Color marrón para madera
        if (isStock) { // Si es STOCK
            for (int layer = 0; layer < 4; layer++) { // Dibuja 4 capas apiladas
                int yOff = cy - 5 + layer * 8; // Calcula offset Y para cada capa
                g2d.fillRect(cx - 30, yOff, 60, 6); // Dibuja tabla superior
                g2d.fillRect(cx - 30, yOff + 7, 60, 6); // Dibuja tabla inferior
            }
        } else { // Si es DOCK
            for (int i = 0; i < 5; i++) { // Dibuja 5 tablas
                g2d.fillRect(cx - 30, cy + i * 8, 60, 6); // Dibuja tabla horizontal
            }
        }
    }

    private void drawStorageIcon(Graphics2D g2d, int cx, int cy) { // Método que dibuja ícono de almacenamiento
        g2d.setColor(new Color(218, 165, 32)); // Color dorado
        g2d.setStroke(new BasicStroke(2.5f)); // Grosor de línea
        g2d.drawRect(cx - 25, cy - 20, 50, 40); // Dibuja rectángulo exterior
        g2d.drawLine(cx - 25, cy - 5, cx + 25, cy - 5); // Dibuja línea divisoria superior
        g2d.drawLine(cx - 25, cy + 10, cx + 25, cy + 10); // Dibuja línea divisoria inferior
    }

    private void drawMachineIcon(Graphics2D g2d, int cx, int cy, boolean isActive) { // Método que dibuja ícono de máquina
        g2d.setColor(new Color(160, 160, 180)); // Color gris para cuerpo
        g2d.fillRoundRect(cx - 30, cy - 20, 60, 40, 8, 8); // Dibuja cuerpo de máquina
        g2d.setColor(new Color(90, 90, 110)); // Color gris oscuro
        g2d.fillRect(cx + 10, cy - 15, 15, 15); // Dibuja componente derecho
        g2d.setColor(new Color(50, 50, 70)); // Color gris muy oscuro
        g2d.fillRect(cx - 20, cy - 10, 25, 25); // Dibuja componente izquierdo

        if (isActive) { // Si la máquina está activa
            g2d.setColor(new Color(76, 175, 80)); // Color verde
            g2d.fillOval(cx + 15, cy - 25, 10, 10); // Dibuja luz indicadora verde
        }
    }

    // Método eliminado: drawMachineGroup (obsoleto en sistema de cervecería - todas las ubicaciones son individuales)

    private void drawValve(Graphics2D g2d, Valve v, int x, int y) { // Método que dibuja una válvula individual
        Color c = v.getType().getColor(); // Obtiene color del tipo de válvula
        g2d.setColor(new Color(0, 0, 0, 60)); // Color negro semi-transparente para sombra
        g2d.fillOval(x + 1, y + 1, 14, 14); // Dibuja sombra desplazada
        g2d.setColor(c); // Establece color de la válvula
        g2d.fillOval(x, y, 14, 14); // Dibuja círculo relleno
        g2d.setColor(c.darker()); // Color más oscuro para borde
        g2d.setStroke(new BasicStroke(1.5f)); // Grosor de borde
        g2d.drawOval(x, y, 14, 14); // Dibuja borde
    }

    private void drawOperators(Graphics2D g2d) { // Método que dibuja todos los operadores
        for (Operator operator : engine.getOperators().values()) {
            drawOperator(g2d, operator);
        }
    }

    private void drawOperator(Graphics2D g2d, Operator operator) { // Método que dibuja un operador individual
        Point opPos = operator.getInterpolatedPosition(); // Obtiene posición interpolada suave

        int x = opPos.x; // Coordenada X del operador
        int y = opPos.y - 70; // Coordenada Y elevada para que esté sobre el suelo

        // Determinar color según tipo de operador
        Color bodyColor;
        boolean isTruck = operator.getName().equals("CAMION");
        
        if (isTruck) {
            bodyColor = new Color(50, 150, 200); // Azul para camión
        } else {
            bodyColor = new Color(255, 165, 0); // Naranja para operadores
        }

        // Forklift body
        g2d.setColor(bodyColor); // Color según tipo
        g2d.fillRoundRect(x - 22, y, 44, 35, 8, 8); // Dibuja cuerpo principal

        // Cabin
        g2d.setColor(bodyColor.brighter()); // Color más claro para cabina
        g2d.fillRect(x - 15, y + 5, 30, 22); // Dibuja cabina

        // Window
        g2d.setColor(new Color(135, 206, 250)); // Color azul cielo para ventana
        g2d.fillRect(x - 10, y + 8, 20, 14); // Dibuja ventana

        // Forks
        g2d.setColor(new Color(100, 100, 100)); // Color gris oscuro para horquillas
        g2d.setStroke(new BasicStroke(4.0f)); // Grosor de horquillas
        g2d.drawLine(x - 15, y + 35, x - 15, y + 50); // Dibuja horquilla izquierda exterior
        g2d.drawLine(x - 5, y + 35, x - 5, y + 50); // Dibuja horquilla izquierda interior
        g2d.drawLine(x + 5, y + 35, x + 5, y + 50); // Dibuja horquilla derecha interior
        g2d.drawLine(x + 15, y + 35, x + 15, y + 50); // Dibuja horquilla derecha exterior

        // Wheels
        g2d.setColor(Color.BLACK); // Color negro para ruedas
        g2d.fillOval(x - 20, y + 32, 14, 14); // Dibuja rueda izquierda
        g2d.fillOval(x + 6, y + 32, 14, 14); // Dibuja rueda derecha

        // Status light
        Color status = operator.isBusy() ? new Color(255, 69, 0) : new Color(76, 175, 80); // Rojo si ocupado, verde si libre
        g2d.setColor(status); // Establece color de estado
        g2d.fillOval(x - 6, y - 8, 12, 12); // Dibuja luz de estado

        // Carrying valve
        Valve carrying = operator.getCarryingValve(); // Obtiene válvula transportada
        if (carrying != null) { // Si está transportando algo
            drawValve(g2d, carrying, x - 7, y + 52); // Dibuja válvula en las horquillas
        }

        // Label
        g2d.setColor(Color.BLACK); // Color negro para texto
        g2d.setFont(new Font("Arial", Font.BOLD, 9)); // Fuente negrita pequeña
        String label = operator.getName().replace("OPERADOR_", ""); // Texto con nombre corto
        if (operator.isBusy()) label += " (OCU)";
        FontMetrics fm = g2d.getFontMetrics(); // Obtiene métricas de fuente
        g2d.drawString(label, x - fm.stringWidth(label)/2, y - 12); // Dibuja texto centrado
    }

    private Set<Valve> getValvesCurrentlyInTransit() {
        Set<Valve> inTransit = new HashSet<>();
        for (Operator operator : engine.getOperators().values()) {
            Valve carrying = operator.getCarryingValve();
            if (carrying != null) {
                inTransit.add(carrying);
            }
        }
        return inTransit;
    }

    private Color getOperatorColor(String operatorName) {
        int hash = Math.abs(Objects.hashCode(operatorName));
        float hue = (hash % 360) / 360f;
        return Color.getHSBColor(hue, 0.6f, 0.85f);
    }
}

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

            Crane crane = engine.getCrane(); // Obtiene la grúa del motor

            // Actualizar posición visual basada en TIEMPO REAL, no tiempo de simulación
            // Esto hace que la animación sea suave sin importar qué tan rápido corra la simulación
            crane.updateVisualPosition(deltaSeconds); // Actualiza posición visual de la grúa con delta time

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

    private int getMachineUnitCount(String baseName) { // Método que cuenta unidades de una máquina
        int count = 0; // Inicializa contador
        while (engine.getLocations().containsKey(baseName + "." + (count + 1))) { // Mientras exista siguiente unidad
            count++; // Incrementa contador
        }
        return count; // Retorna total de unidades
    }

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

        // Draw all locations
        drawLocation(g2d, "DOCK"); // Dibuja DOCK
        drawLocation(g2d, "STOCK"); // Dibuja STOCK
        drawLocation(g2d, "Almacen_M1"); // Dibuja Almacen_M1
        drawLocation(g2d, "Almacen_M2"); // Dibuja Almacen_M2
        drawLocation(g2d, "Almacen_M3"); // Dibuja Almacen_M3

        // Draw machine groups with their individual units
        drawMachineGroup(g2d, "M1", getMachineUnitCount("M1")); // Dibuja grupo de M1
        drawMachineGroup(g2d, "M2", getMachineUnitCount("M2")); // Dibuja grupo de M2
        drawMachineGroup(g2d, "M3", getMachineUnitCount("M3")); // Dibuja grupo de M3

        // Draw crane (must be AFTER locations so it's on top)
        drawCrane(g2d); // Dibuja grúa encima de todo

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

        // Highlight current crane path
        List<Point> pathPoints = engine.getCrane().getCurrentPathPoints(); // Obtiene puntos del camino actual de la grúa
        if (pathPoints.size() >= 2) { // Verifica si hay al menos 2 puntos (un camino válido)
            g2d.setColor(new Color(255, 140, 0, 190)); // Color naranja semi-transparente para resaltar
            g2d.setStroke(new BasicStroke(5.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); // Línea más gruesa y redondeada
            for (int i = 0; i < pathPoints.size() - 1; i++) { // Itera sobre segmentos del camino
                Point from = pathPoints.get(i); // Obtiene punto inicial del segmento
                Point to = pathPoints.get(i + 1); // Obtiene punto final del segmento
                g2d.drawLine(from.x, from.y, to.x, to.y); // Dibuja segmento
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

    private void drawLocation(Graphics2D g2d, String name) { // Método que dibuja una locación individual
        Location loc = engine.getLocations().get(name); // Obtiene locación del motor
        if (loc == null) return; // Sale si no existe

        Point pos = locationPositions.get(name); // Obtiene posición guardada
        if (pos == null) return; // Sale si no hay posición

        // Las ubicaciones padre (M1, M2, M3) no se dibujan individualmente
        // Solo se muestran sus unidades mediante drawMachineGroup
        if (name.equals("M1") || name.equals("M2") || name.equals("M3")) { // Verifica si es máquina parent
            return; // Sale sin dibujar
        }

        // Obtener la grúa y verificar si está transportando a esta ubicación
        Crane crane = engine.getCrane(); // Obtiene grúa
        Valve carryingValve = crane.getCarryingValve(); // Obtiene válvula que está transportando

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
        if (name.equals("DOCK") || name.equals("STOCK")) { // Si es DOCK o STOCK
            drawPalletIcon(g2d, x + w/2, y + 40, name.equals("STOCK")); // Dibuja ícono de pallet
        } else if (name.startsWith("Almacen")) { // Si es almacén
            drawStorageIcon(g2d, x + w/2, y + 40); // Dibuja ícono de almacenamiento
        } else { // Si es máquina
            drawMachineIcon(g2d, x + w/2, y + 45, loc.getProcessingSize() > 0); // Dibuja ícono de máquina
        }

        // Label
        g2d.setColor(Color.BLACK); // Color negro para texto
        g2d.setFont(new Font("Arial", Font.BOLD, 12)); // Fuente Arial negrita tamaño 12
        g2d.drawString(Localization.getLocationDisplayName(name), x + 8, y + 18); // Dibuja nombre localizado

        // Capacity
        int displayedContents = loc.getCurrentContents(); // Obtiene contenido actual
        if (name.equals("STOCK")) { // Si es STOCK
            displayedContents = engine.getCompletedInventoryCount(); // Usa contador de inventario completado
        }

        String cap = String.format("%d/%s", displayedContents, // Formatea capacidad
            loc.getCapacity() == Integer.MAX_VALUE ? "∞" : String.valueOf(loc.getCapacity())); // Usa infinito o número
        g2d.setFont(new Font("Arial", Font.PLAIN, 11)); // Fuente Arial normal tamaño 11
        g2d.drawString(cap, x + 8, y + 33); // Dibuja capacidad

        // Draw valves - excluir la que está siendo transportada si la animación está en progreso
        List<Valve> valves = new ArrayList<>(loc.getAllValves()); // Crea copia de lista de válvulas
        if (name.equals("STOCK") && !valves.isEmpty()) { // Si es STOCK y hay válvulas
            valves.clear(); // Limpia lista (STOCK no muestra válvulas visualmente)
        }

        // Si la grúa está en movimiento y transporta una válvula que está en esta ubicación,
        // ocultarla hasta que la animación termine completamente
        if (crane.isMoving() && carryingValve != null && valves.contains(carryingValve)) { // Si grúa se mueve y tiene válvula de esta locación
            valves.remove(carryingValve); // Remueve para no mostrarla duplicada
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

    private void drawMachineGroup(Graphics2D g2d, String baseName, int unitCount) { // Método que dibuja grupo de máquinas
        if (unitCount <= 0) { // Si no hay unidades
            return; // Sale sin dibujar
        }
        // Obtener posición del grupo
        Location parentLoc = engine.getLocations().get(baseName); // Obtiene locación parent
        if (parentLoc == null) return; // Sale si no existe
        Point pos = parentLoc.getPosition(); // Obtiene posición

        Crane crane = engine.getCrane(); // Obtiene grúa
        Valve carryingValve = crane.getCarryingValve(); // Obtiene válvula transportada

        // Contar unidades activas y válvulas
        int activeUnits = 0; // Inicializa contador de unidades activas
        int totalValves = 0; // Inicializa contador total de válvulas
        int queuedValves = 0; // Inicializa contador de válvulas en cola
        int processingValves = 0; // Inicializa contador de válvulas en procesamiento

        for (int i = 1; i <= unitCount; i++) { // Itera sobre cada unidad
            Location unit = engine.getLocations().get(baseName + "." + i); // Obtiene unidad i
            if (unit != null) { // Si existe
                int procSize = unit.getProcessingSize(); // Obtiene tamaño de procesamiento
                int qSize = unit.getQueueSize(); // Obtiene tamaño de cola
                if (procSize > 0) { // Si hay válvulas procesando
                    activeUnits++; // Incrementa unidades activas
                }
                totalValves += unit.getCurrentContents(); // Acumula contenido total
                queuedValves += qSize; // Acumula cola
                processingValves += procSize; // Acumula procesamiento
            }
        }

        int w = 180, h = 140; // Dimensiones del rectángulo del grupo
        int x = pos.x - w/2; // Calcula X centrado
        int y = pos.y - h/2; // Calcula Y centrado

        // Sombra
        g2d.setColor(new Color(0, 0, 0, 40)); // Color negro semi-transparente
        g2d.fillRoundRect(x + 4, y + 4, w, h, 12, 12); // Dibuja sombra desplazada

        // Fondo del grupo
        g2d.setColor(new Color(220, 230, 250, 230)); // Color azul muy claro semi-transparente
        g2d.fillRoundRect(x, y, w, h, 12, 12); // Dibuja fondo

        // Borde
        g2d.setColor(new Color(100, 120, 180)); // Color azul medio
        g2d.setStroke(new BasicStroke(3.0f)); // Grosor de borde
        g2d.drawRoundRect(x, y, w, h, 12, 12); // Dibuja borde

        // Título del grupo
        g2d.setColor(Color.BLACK); // Color negro para texto
        g2d.setFont(new Font("Arial", Font.BOLD, 14)); // Fuente Arial negrita tamaño 14
        String title = Localization.getLocationDisplayName(baseName); // Obtiene nombre localizado
        g2d.drawString(title, x + 10, y + 22); // Dibuja título

        // Badge con número de unidades
        int badgeX = x + w - 60; // Calcula posición X del badge
        int badgeY = y + 8; // Calcula posición Y del badge
        g2d.setColor(new Color(100, 120, 180)); // Color azul medio para fondo
        g2d.fillRoundRect(badgeX, badgeY, 50, 20, 8, 8); // Dibuja fondo del badge
        g2d.setColor(Color.WHITE); // Color blanco para texto
        g2d.setFont(new Font("Arial", Font.BOLD, 11)); // Fuente para badge
        g2d.drawString(unitCount + " units", badgeX + 6, badgeY + 14); // Dibuja texto del badge

        // Ícono de máquina
        drawMachineIcon(g2d, x + w/2, y + 60, activeUnits > 0); // Dibuja ícono centrado

        // Obtener contador de válvulas procesadas
        statistics.LocationStats machineStats = engine.getStatistics().getLocationStats(baseName); // Obtiene estadísticas de la máquina
        int valvesProcessed = machineStats != null ? machineStats.getValvesProcessed() : 0; // Obtiene contador o 0

        // Información de utilización
        g2d.setColor(new Color(50, 50, 70)); // Color gris oscuro para texto
        g2d.setFont(new Font("Arial", Font.BOLD, 11)); // Fuente negrita
        g2d.drawString(String.format("Activas: %d/%d", activeUnits, unitCount), x + 10, y + h - 60); // Dibuja unidades activas

        g2d.setFont(new Font("Arial", Font.PLAIN, 10)); // Fuente normal más pequeña
        g2d.drawString(String.format("Procesando: %d | En cola: %d", processingValves, queuedValves), x + 10, y + h - 45); // Dibuja contadores
        g2d.drawString(String.format("Util: %.1f%%", (activeUnits * 100.0 / unitCount)), x + 10, y + h - 30); // Dibuja utilización

        // Contador de válvulas procesadas
        g2d.setColor(new Color(76, 175, 80)); // Color verde
        g2d.setFont(new Font("Arial", Font.BOLD, 10)); // Fuente negrita
        g2d.drawString(String.format("Procesadas: %d", valvesProcessed), x + 10, y + h - 15); // Dibuja contador total procesadas

        // Dibujar algunas válvulas si hay - excluir la que está siendo transportada
        if (totalValves > 0) { // Si hay válvulas
            List<Valve> allValves = new ArrayList<>(); // Crea lista para todas las válvulas
            for (int i = 1; i <= unitCount; i++) { // Itera sobre unidades
                Location unit = engine.getLocations().get(baseName + "." + i); // Obtiene unidad i
                if (unit != null) { // Si existe
                    allValves.addAll(unit.getAllValves()); // Agrega todas sus válvulas
                }
            }

            // No mostrar la válvula que está siendo transportada hasta que la animación termine
            if (crane.isMoving() && carryingValve != null && allValves.contains(carryingValve)) { // Si grúa se mueve y tiene válvula de este grupo
                allValves.remove(carryingValve); // Remueve para no mostrarla duplicada
            }

            int count = Math.min(8, allValves.size()); // Muestra máximo 8 válvulas
            for (int i = 0; i < count; i++) { // Itera sobre válvulas a mostrar
                Valve v = allValves.get(i); // Obtiene válvula i
                int vx = x + w - 90 + (i % 4) * 18; // Calcula posición X (4 columnas)
                int vy = y + h - 28 + (i / 4) * 16; // Calcula posición Y (2 filas)
                drawValve(g2d, v, vx, vy); // Dibuja válvula
            }
            if (allValves.size() > 8) { // Si hay más de 8 válvulas
                g2d.setColor(Color.BLACK); // Color negro
                g2d.setFont(new Font("Arial", Font.BOLD, 9)); // Fuente pequeña
                g2d.drawString("+" + (allValves.size() - 8), x + w - 25, y + h - 10); // Muestra contador de restantes
            }
        }
    }

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

    private void drawCrane(Graphics2D g2d) { // Método que dibuja la grúa
        Crane crane = engine.getCrane(); // Obtiene grúa
        Point cranePos = crane.getInterpolatedPosition(); // Obtiene posición interpolada suave

        int x = cranePos.x; // Coordenada X de la grúa
        int y = cranePos.y - 70; // Coordenada Y elevada para que esté sobre el suelo

        // Forklift body
        g2d.setColor(new Color(255, 165, 0)); // Color naranja para cuerpo
        g2d.fillRoundRect(x - 22, y, 44, 35, 8, 8); // Dibuja cuerpo principal

        // Cabin
        g2d.setColor(new Color(255, 200, 0)); // Color amarillo para cabina
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
        Color status = crane.isBusy() ? new Color(255, 69, 0) : new Color(76, 175, 80); // Rojo si ocupada, verde si libre
        g2d.setColor(status); // Establece color de estado
        g2d.fillOval(x - 6, y - 8, 12, 12); // Dibuja luz de estado

        // Carrying valve
        Valve carrying = crane.getCarryingValve(); // Obtiene válvula transportada
        if (carrying != null) { // Si está transportando algo
            drawValve(g2d, carrying, x - 7, y + 52); // Dibuja válvula en las horquillas
        }

        // Label
        g2d.setColor(Color.BLACK); // Color negro para texto
        g2d.setFont(new Font("Arial", Font.BOLD, 10)); // Fuente negrita pequeña
        String label = crane.isBusy() ? "OCUPADA" : "LIBRE"; // Texto según estado
        FontMetrics fm = g2d.getFontMetrics(); // Obtiene métricas de fuente
        g2d.drawString(label, x - fm.stringWidth(label)/2, y - 12); // Dibuja texto centrado
    }
}

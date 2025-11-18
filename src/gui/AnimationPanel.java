package gui;

import core.SimulationEngine;
import model.*;
import utils.Localization;
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.util.*;
import java.util.List;

public class AnimationPanel extends JPanel {
    private SimulationEngine engine;
    private Map<String, Point> locationPositions;
    private Timer animationTimer;
    private static final double TIMER_INTERVAL_SECONDS = 0.016; // ~60 FPS

    public AnimationPanel(SimulationEngine engine) {
        this.engine = engine;
        setBackground(new Color(245, 248, 252));
        setPreferredSize(new Dimension(1100, 650));

        initializeLayout();
        startAnimation();
    }

    private void initializeLayout() {
        locationPositions = new HashMap<>();
        refreshLocationPositions();
    }

    private void startAnimation() {
        // 60 FPS animation
        animationTimer = new Timer(16, e -> {
            Crane crane = engine.getCrane();
            // Update crane visual position smoothly
            crane.updateVisualPosition(TIMER_INTERVAL_SECONDS);
            repaint();
        });
        animationTimer.start();
    }

    private void refreshLocationPositions() {
        Map<String, Location> locs = engine.getLocations();
        for (Map.Entry<String, Location> entry : locs.entrySet()) {
            locationPositions.put(entry.getKey(), new Point(entry.getValue().getPosition()));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        refreshLocationPositions();

        // Draw network paths and nodes
        drawNetwork(g2d);

        // Draw all locations
        drawLocation(g2d, "DOCK");
        drawLocation(g2d, "STOCK");
        drawLocation(g2d, "Almacen_M1");
        drawLocation(g2d, "Almacen_M2");
        drawLocation(g2d, "Almacen_M3");
        
        // Draw machine groups with their individual units
        drawMachineGroup(g2d, "M1", 10);
        drawMachineGroup(g2d, "M2", 25);
        drawMachineGroup(g2d, "M3", 17);

        // Draw crane (must be AFTER locations so it's on top)
        drawCrane(g2d);

        // Draw info
        drawInfo(g2d);
    }

    private void drawNetwork(Graphics2D g2d) {
        PathNetwork network = engine.getPathNetwork();
        if (network == null) {
            return;
        }

        g2d.setStroke(new BasicStroke(3.5f,
            BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0,
            new float[]{12, 6}, 0));
        g2d.setColor(new Color(70, 130, 220, 120));

        for (PathNetwork.PathEdge edge : network.getEdges()) {
            Point from = network.getNodePosition(edge.getFrom());
            Point to = network.getNodePosition(edge.getTo());
            if (from != null && to != null) {
                g2d.drawLine(from.x, from.y, to.x, to.y);
            }
        }

        // Highlight current crane path
        List<Point> pathPoints = engine.getCrane().getCurrentPathPoints();
        if (pathPoints.size() >= 2) {
            g2d.setColor(new Color(255, 140, 0, 190));
            g2d.setStroke(new BasicStroke(5.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < pathPoints.size() - 1; i++) {
                Point from = pathPoints.get(i);
                Point to = pathPoints.get(i + 1);
                g2d.drawLine(from.x, from.y, to.x, to.y);
            }
        }

        // Draw nodes
        g2d.setStroke(new BasicStroke(1.5f));
        for (Map.Entry<String, Point> entry : network.getNodePositions().entrySet()) {
            Point node = entry.getValue();
            g2d.setColor(new Color(35, 73, 147));
            g2d.fillOval(node.x - 6, node.y - 6, 12, 12);
            g2d.setColor(Color.WHITE);
            g2d.drawOval(node.x - 6, node.y - 6, 12, 12);
        }
    }

    private void drawLocation(Graphics2D g2d, String name) {
        Location loc = engine.getLocations().get(name);
        if (loc == null) return;

        Point pos = locationPositions.get(name);
        if (pos == null) return;

        // Las ubicaciones padre (M1, M2, M3) no se dibujan individualmente
        // Solo se muestran sus unidades mediante drawMachineGroup
        if (name.equals("M1") || name.equals("M2") || name.equals("M3")) {
            return;
        }

        int w = 140, h = 110;
        int x = pos.x - w/2;
        int y = pos.y - h/2;

        // Determine colors
        Color bgColor, borderColor;
        if (name.equals("DOCK")) {
            bgColor = new Color(135, 206, 250, 220);
            borderColor = new Color(70, 130, 200);
        } else if (name.equals("STOCK")) {
            bgColor = new Color(144, 238, 144, 220);
            borderColor = new Color(70, 180, 70);
        } else if (name.startsWith("Almacen")) {
            bgColor = new Color(255, 248, 220, 220);
            borderColor = new Color(218, 165, 32);
        } else {
            bgColor = new Color(200, 200, 220, 220);
            borderColor = new Color(100, 100, 130);
        }

        // Shadow
        g2d.setColor(new Color(0, 0, 0, 40));
        g2d.fillRoundRect(x + 4, y + 4, w, h, 12, 12);

        // Background
        g2d.setColor(bgColor);
        g2d.fillRoundRect(x, y, w, h, 12, 12);

        // Border
        g2d.setColor(borderColor);
        g2d.setStroke(new BasicStroke(3.0f));
        g2d.drawRoundRect(x, y, w, h, 12, 12);

        // Draw icon based on type
        if (name.equals("DOCK") || name.equals("STOCK")) {
            drawPalletIcon(g2d, x + w/2, y + 40, name.equals("STOCK"));
        } else if (name.startsWith("Almacen")) {
            drawStorageIcon(g2d, x + w/2, y + 40);
        } else {
            drawMachineIcon(g2d, x + w/2, y + 45, loc.getProcessingSize() > 0);
        }

        // Label
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.drawString(Localization.getLocationDisplayName(name), x + 8, y + 18);

        // Capacity
        String cap = String.format("%d/%s", loc.getCurrentContents(),
            loc.getCapacity() == Integer.MAX_VALUE ? "∞" : String.valueOf(loc.getCapacity()));
        g2d.setFont(new Font("Arial", Font.PLAIN, 11));
        g2d.drawString(cap, x + 8, y + 33);

        // Draw valves
        List<Valve> valves = loc.getAllValves();
        if (!valves.isEmpty()) {
            int count = Math.min(10, valves.size());
            for (int i = 0; i < count; i++) {
                Valve v = valves.get(i);
                int vx = x + 10 + (i % 5) * 24;
                int vy = y + h - 28 + (i / 5) * 16;
                drawValve(g2d, v, vx, vy);
            }
            if (valves.size() > 10) {
                g2d.setColor(Color.BLACK);
                g2d.setFont(new Font("Arial", Font.BOLD, 9));
                g2d.drawString("+" + (valves.size() - 10), x + w - 25, y + h - 10);
            }
        }
    }

    private void drawPalletIcon(Graphics2D g2d, int cx, int cy, boolean isStock) {
        g2d.setColor(new Color(139, 90, 43));
        if (isStock) {
            for (int layer = 0; layer < 4; layer++) {
                int yOff = cy - 5 + layer * 8;
                g2d.fillRect(cx - 30, yOff, 60, 6);
                g2d.fillRect(cx - 30, yOff + 7, 60, 6);
            }
        } else {
            for (int i = 0; i < 5; i++) {
                g2d.fillRect(cx - 30, cy + i * 8, 60, 6);
            }
        }
    }

    private void drawStorageIcon(Graphics2D g2d, int cx, int cy) {
        g2d.setColor(new Color(218, 165, 32));
        g2d.setStroke(new BasicStroke(2.5f));
        g2d.drawRect(cx - 25, cy - 20, 50, 40);
        g2d.drawLine(cx - 25, cy - 5, cx + 25, cy - 5);
        g2d.drawLine(cx - 25, cy + 10, cx + 25, cy + 10);
    }

    private void drawMachineIcon(Graphics2D g2d, int cx, int cy, boolean isActive) {
        g2d.setColor(new Color(160, 160, 180));
        g2d.fillRoundRect(cx - 30, cy - 20, 60, 40, 8, 8);
        g2d.setColor(new Color(90, 90, 110));
        g2d.fillRect(cx + 10, cy - 15, 15, 15);
        g2d.setColor(new Color(50, 50, 70));
        g2d.fillRect(cx - 20, cy - 10, 25, 25);

        if (isActive) {
            g2d.setColor(new Color(76, 175, 80));
            g2d.fillOval(cx + 15, cy - 25, 10, 10);
        }
    }

    private void drawMachineGroup(Graphics2D g2d, String baseName, int unitCount) {
        // Obtener posición del grupo
        Location parentLoc = engine.getLocations().get(baseName);
        if (parentLoc == null) return;
        Point pos = parentLoc.getPosition();

        // Contar unidades activas y válvulas
        int activeUnits = 0;
        int totalValves = 0;
        for (int i = 1; i <= unitCount; i++) {
            Location unit = engine.getLocations().get(baseName + "." + i);
            if (unit != null) {
                if (unit.getProcessingSize() > 0) activeUnits++;
                totalValves += unit.getCurrentContents();
            }
        }

        int w = 180, h = 140;
        int x = pos.x - w/2;
        int y = pos.y - h/2;

        // Sombra
        g2d.setColor(new Color(0, 0, 0, 40));
        g2d.fillRoundRect(x + 4, y + 4, w, h, 12, 12);
        
        // Fondo del grupo
        g2d.setColor(new Color(220, 230, 250, 230));
        g2d.fillRoundRect(x, y, w, h, 12, 12);
        
        // Borde
        g2d.setColor(new Color(100, 120, 180));
        g2d.setStroke(new BasicStroke(3.0f));
        g2d.drawRoundRect(x, y, w, h, 12, 12);

        // Título del grupo
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        String title = Localization.getLocationDisplayName(baseName);
        g2d.drawString(title, x + 10, y + 22);

        // Badge con número de unidades
        int badgeX = x + w - 60;
        int badgeY = y + 8;
        g2d.setColor(new Color(100, 120, 180));
        g2d.fillRoundRect(badgeX, badgeY, 50, 20, 8, 8);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 11));
        g2d.drawString(unitCount + " units", badgeX + 6, badgeY + 14);

        // Ícono de máquina
        drawMachineIcon(g2d, x + w/2, y + 60, activeUnits > 0);

        // Información de utilización
        g2d.setColor(new Color(50, 50, 70));
        g2d.setFont(new Font("Arial", Font.BOLD, 11));
        g2d.drawString(String.format("Activas: %d/%d", activeUnits, unitCount), x + 10, y + h - 45);
        
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        g2d.drawString(String.format("Válvulas: %d", totalValves), x + 10, y + h - 30);
        g2d.drawString(String.format("Util: %.1f%%", (activeUnits * 100.0 / unitCount)), x + 10, y + h - 15);

        // Dibujar algunas válvulas si hay
        if (totalValves > 0) {
            List<Valve> allValves = new ArrayList<>();
            for (int i = 1; i <= unitCount; i++) {
                Location unit = engine.getLocations().get(baseName + "." + i);
                if (unit != null) {
                    allValves.addAll(unit.getAllValves());
                }
            }
            int count = Math.min(8, allValves.size());
            for (int i = 0; i < count; i++) {
                Valve v = allValves.get(i);
                int vx = x + w - 90 + (i % 4) * 18;
                int vy = y + h - 28 + (i / 4) * 16;
                drawValve(g2d, v, vx, vy);
            }
            if (allValves.size() > 8) {
                g2d.setColor(Color.BLACK);
                g2d.setFont(new Font("Arial", Font.BOLD, 9));
                g2d.drawString("+" + (allValves.size() - 8), x + w - 25, y + h - 10);
            }
        }
    }

    private void drawValve(Graphics2D g2d, Valve v, int x, int y) {
        Color c = v.getType().getColor();
        g2d.setColor(new Color(0, 0, 0, 60));
        g2d.fillOval(x + 1, y + 1, 14, 14);
        g2d.setColor(c);
        g2d.fillOval(x, y, 14, 14);
        g2d.setColor(c.darker());
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawOval(x, y, 14, 14);
    }

    private void drawCrane(Graphics2D g2d) {
        Crane crane = engine.getCrane();
        Point cranePos = crane.getInterpolatedPosition();

        int x = cranePos.x;
        int y = cranePos.y - 70;

        // Forklift body
        g2d.setColor(new Color(255, 165, 0));
        g2d.fillRoundRect(x - 22, y, 44, 35, 8, 8);

        // Cabin
        g2d.setColor(new Color(255, 200, 0));
        g2d.fillRect(x - 15, y + 5, 30, 22);

        // Window
        g2d.setColor(new Color(135, 206, 250));
        g2d.fillRect(x - 10, y + 8, 20, 14);

        // Forks
        g2d.setColor(new Color(100, 100, 100));
        g2d.setStroke(new BasicStroke(4.0f));
        g2d.drawLine(x - 15, y + 35, x - 15, y + 50);
        g2d.drawLine(x - 5, y + 35, x - 5, y + 50);
        g2d.drawLine(x + 5, y + 35, x + 5, y + 50);
        g2d.drawLine(x + 15, y + 35, x + 15, y + 50);

        // Wheels
        g2d.setColor(Color.BLACK);
        g2d.fillOval(x - 20, y + 32, 14, 14);
        g2d.fillOval(x + 6, y + 32, 14, 14);

        // Status light
        Color status = crane.isBusy() ? new Color(255, 69, 0) : new Color(76, 175, 80);
        g2d.setColor(status);
        g2d.fillOval(x - 6, y - 8, 12, 12);

        // Carrying valve
        Valve carrying = crane.getCarryingValve();
        if (carrying != null) {
            drawValve(g2d, carrying, x - 7, y + 52);
        }

        // Label
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 10));
        String label = crane.isBusy() ? "OCUPADA" : "LIBRE";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(label, x - fm.stringWidth(label)/2, y - 12);
    }

    private void drawInfo(Graphics2D g2d) {
        g2d.setColor(new Color(255, 255, 255, 240));
        g2d.fillRoundRect(20, 20, 240, 140, 12, 12);
        g2d.setColor(new Color(100, 120, 180));
        g2d.setStroke(new BasicStroke(2.5f));
        g2d.drawRoundRect(20, 20, 240, 140, 12, 12);

        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 13));
        g2d.drawString("Estado del Sistema", 35, 42);

        g2d.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g2d.drawString(String.format("Tiempo: %.1f hrs", engine.getCurrentTime()), 35, 62);
        g2d.drawString("En Sistema: " + engine.getTotalValvesInSystem(), 35, 80);
        g2d.drawString("Completadas: " + engine.getCompletedValves().size(), 35, 98);
        g2d.drawString("Viajes Grua: " + engine.getCrane().getTotalTrips(), 35, 116);
        g2d.drawString("Ruta Actual: " + engine.getCrane().getCurrentPathPoints().size(), 35, 134);
    }
}

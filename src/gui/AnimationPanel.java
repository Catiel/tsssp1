package gui;

import core.SimulationEngine;
import model.*;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class AnimationPanel extends JPanel {
    private SimulationEngine engine;

    // Visual constants
    private static final int LOCATION_WIDTH = 120;
    private static final int LOCATION_HEIGHT = 80;
    private static final int VALVE_SIZE = 12;
    private static final int CRANE_WIDTH = 40;
    private static final int CRANE_HEIGHT = 30;

    // Colors
    private static final Color BACKGROUND_COLOR = new Color(245, 245, 250);
    private static final Color GRID_COLOR = new Color(220, 220, 230);
    private static final Color PATH_COLOR = new Color(100, 100, 150);
    private static final Color DOCK_COLOR = new Color(135, 206, 250);
    private static final Color STOCK_COLOR = new Color(144, 238, 144);
    private static final Color ALMACEN_COLOR = new Color(255, 248, 220);
    private static final Color MACHINE_COLOR = new Color(255, 218, 185);
    private static final Color CRANE_COLOR = new Color(70, 70, 70);

    // Animation
    private Map<Location, Point> locationPositions;
    private double animationTime = 0;

    public AnimationPanel(SimulationEngine engine) {
        this.engine = engine;
        setBackground(BACKGROUND_COLOR);
        setPreferredSize(new Dimension(1000, 700));

        initializePositions();
    }

    private void initializePositions() {
        locationPositions = new HashMap<>();

        // Define visual positions (different from logical positions for better layout)
        locationPositions.put(engine.getLocations().get("DOCK"),
            new Point(50, 100));
        locationPositions.put(engine.getLocations().get("STOCK"),
            new Point(50, 400));

        locationPositions.put(engine.getLocations().get("Almacen_M1"),
            new Point(300, 50));
        locationPositions.put(engine.getLocations().get("M1"),
            new Point(300, 150));

        locationPositions.put(engine.getLocations().get("Almacen_M2"),
            new Point(500, 50));
        locationPositions.put(engine.getLocations().get("M2"),
            new Point(500, 150));

        locationPositions.put(engine.getLocations().get("Almacen_M3"),
            new Point(700, 50));
        locationPositions.put(engine.getLocations().get("M3"),
            new Point(700, 150));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Enable anti-aliasing for smooth graphics
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Draw background grid
        drawGrid(g2d);

        // Draw crane path network
        drawPathNetwork(g2d);

        // Draw all locations
        for (Location loc : engine.getLocations().values()) {
            drawLocation(g2d, loc);
        }

        // Draw crane
        drawCrane(g2d, engine.getCrane());

        // Draw floating information
        drawFloatingInfo(g2d);

        animationTime += 0.016; // ~60 FPS
    }

    private void drawGrid(Graphics2D g2d) {
        g2d.setColor(GRID_COLOR);
        g2d.setStroke(new BasicStroke(0.5f));

        int spacing = 50;
        for (int x = 0; x < getWidth(); x += spacing) {
            g2d.drawLine(x, 0, x, getHeight());
        }
        for (int y = 0; y < getHeight(); y += spacing) {
            g2d.drawLine(0, y, getWidth(), y);
        }
    }

    private void drawPathNetwork(Graphics2D g2d) {
        g2d.setColor(PATH_COLOR);
        g2d.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Draw main horizontal rails
        g2d.drawLine(50, 130, 850, 130); // Top rail
        g2d.drawLine(50, 180, 850, 180); // Bottom rail

        // Draw vertical connectors
        int[] xPositions = {50, 300, 500, 700, 850};
        for (int x : xPositions) {
            g2d.drawLine(x, 130, x, 180);
        }

        // Draw decorative elements
        g2d.setColor(new Color(PATH_COLOR.getRed(), PATH_COLOR.getGreen(),
            PATH_COLOR.getBlue(), 50));
        g2d.setStroke(new BasicStroke(10.0f));
        g2d.drawLine(50, 130, 850, 130);
    }

    private void drawLocation(Graphics2D g2d, Location loc) {
        Point pos = locationPositions.get(loc);
        if (pos == null) return;

        int x = pos.x;
        int y = pos.y;

        // Choose color based on location type
        Color bgColor = getLocationColor(loc.getName());
        Color borderColor = bgColor.darker();

        // Draw shadow
        g2d.setColor(new Color(0, 0, 0, 30));
        g2d.fillRoundRect(x + 5, y + 5, LOCATION_WIDTH, LOCATION_HEIGHT, 15, 15);

        // Draw location box with gradient
        GradientPaint gradient = new GradientPaint(
            x, y, bgColor.brighter(),
            x, y + LOCATION_HEIGHT, bgColor
        );
        g2d.setPaint(gradient);
        g2d.fillRoundRect(x, y, LOCATION_WIDTH, LOCATION_HEIGHT, 15, 15);

        // Draw border
        g2d.setColor(borderColor);
        g2d.setStroke(new BasicStroke(2.5f));
        g2d.drawRoundRect(x, y, LOCATION_WIDTH, LOCATION_HEIGHT, 15, 15);

        // Draw location name
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 12));
        FontMetrics fm = g2d.getFontMetrics();
        String name = loc.getName();
        int nameWidth = fm.stringWidth(name);
        g2d.drawString(name, x + (LOCATION_WIDTH - nameWidth) / 2, y + 20);

        // Draw capacity info
        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        String capacityStr = String.format("Cap: %d/%s",
            loc.getCurrentContents(),
            loc.getCapacity() == Integer.MAX_VALUE ? "∞" : loc.getCapacity());
        int capWidth = g2d.getFontMetrics().stringWidth(capacityStr);
        g2d.drawString(capacityStr, x + (LOCATION_WIDTH - capWidth) / 2, y + 35);

        // Draw capacity bar
        if (loc.getCapacity() != Integer.MAX_VALUE) {
            int barWidth = LOCATION_WIDTH - 20;
            int barHeight = 8;
            int barX = x + 10;
            int barY = y + 40;

            double fillPercent = (double) loc.getCurrentContents() / loc.getCapacity();
            int fillWidth = (int) (barWidth * fillPercent);

            // Bar background
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.fillRoundRect(barX, barY, barWidth, barHeight, 4, 4);

            // Bar fill with color based on utilization
            Color barColor = getUtilizationColor(fillPercent);
            g2d.setColor(barColor);
            g2d.fillRoundRect(barX, barY, fillWidth, barHeight, 4, 4);

            // Bar border
            g2d.setColor(Color.DARK_GRAY);
            g2d.setStroke(new BasicStroke(1.0f));
            g2d.drawRoundRect(barX, barY, barWidth, barHeight, 4, 4);
        }

        // Draw valves in location
        drawValvesInLocation(g2d, loc, x, y);

        // Draw processing indicator for machines
        if (loc.getName().startsWith("M") && loc.getProcessingSize() > 0) {
            drawProcessingIndicator(g2d, x, y);
        }
    }

    private Color getLocationColor(String name) {
        if (name.equals("DOCK")) return DOCK_COLOR;
        if (name.equals("STOCK")) return STOCK_COLOR;
        if (name.startsWith("Almacen")) return ALMACEN_COLOR;
        if (name.startsWith("M")) return MACHINE_COLOR;
        return Color.WHITE;
    }

    private Color getUtilizationColor(double utilization) {
        if (utilization < 0.5) return new Color(76, 175, 80);  // Green
        if (utilization < 0.75) return new Color(255, 193, 7); // Yellow
        if (utilization < 0.9) return new Color(255, 152, 0);  // Orange
        return new Color(244, 67, 54); // Red
    }

    private void drawValvesInLocation(Graphics2D g2d, Location loc, int locX, int locY) {
        List<Valve> valves = loc.getAllValves();
        int maxDisplay = 8;
        int displayCount = Math.min(valves.size(), maxDisplay);

        int startX = locX + 10;
        int startY = locY + 55;
        int spacing = (LOCATION_WIDTH - 20) / Math.max(displayCount, 1);

        for (int i = 0; i < displayCount; i++) {
            Valve valve = valves.get(i);
            int vx = startX + i * spacing;
            int vy = startY;

            drawValve(g2d, valve, vx, vy);
        }

        // Draw "+N more" if there are more valves
        if (valves.size() > maxDisplay) {
            g2d.setColor(Color.DARK_GRAY);
            g2d.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            String moreText = "+" + (valves.size() - maxDisplay) + " more";
            g2d.drawString(moreText, locX + 10, locY + LOCATION_HEIGHT - 5);
        }
    }

    private void drawValve(Graphics2D g2d, Valve valve, int x, int y) {
        Color color = valve.getType().getColor();

        // Draw shadow
        g2d.setColor(new Color(0, 0, 0, 50));
        g2d.fillOval(x + 2, y + 2, VALVE_SIZE, VALVE_SIZE);

        // Draw valve with gradient
        GradientPaint gradient = new GradientPaint(
            x, y, color.brighter(),
            x, y + VALVE_SIZE, color
        );
        g2d.setPaint(gradient);
        g2d.fillOval(x, y, VALVE_SIZE, VALVE_SIZE);

        // Draw border
        g2d.setColor(color.darker());
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawOval(x, y, VALVE_SIZE, VALVE_SIZE);

        // Draw state indicator
        drawStateIndicator(g2d, valve.getState(), x + VALVE_SIZE / 2, y - 2);
    }

    private void drawStateIndicator(Graphics2D g2d, Valve.State state, int x, int y) {
        int size = 4;
        Color color;

        switch (state) {
            case PROCESSING:
                color = new Color(76, 175, 80); // Green
                break;
            case IN_TRANSIT:
                color = new Color(33, 150, 243); // Blue
                break;
            case WAITING_CRANE:
                color = new Color(255, 193, 7); // Yellow
                break;
            case BLOCKED:
                color = new Color(244, 67, 54); // Red
                break;
            default:
                return;
        }

        g2d.setColor(color);
        g2d.fillOval(x - size/2, y - size/2, size, size);
    }

    private void drawProcessingIndicator(Graphics2D g2d, int x, int y) {
        // Animated gear/processing icon
        double angle = animationTime * 2;
        int centerX = x + LOCATION_WIDTH - 20;
        int centerY = y + 10;
        int radius = 8;

        g2d.setColor(new Color(76, 175, 80));
        for (int i = 0; i < 8; i++) {
            double a = angle + (i * Math.PI / 4);
            int x1 = centerX + (int)(Math.cos(a) * radius);
            int y1 = centerY + (int)(Math.sin(a) * radius);
            int x2 = centerX + (int)(Math.cos(a) * (radius + 3));
            int y2 = centerY + (int)(Math.sin(a) * (radius + 3));
            g2d.setStroke(new BasicStroke(2.0f));
            g2d.drawLine(x1, y1, x2, y2);
        }

        g2d.fillOval(centerX - 3, centerY - 3, 6, 6);
    }

    private void drawCrane(Graphics2D g2d, Crane crane) {
        Point pos = crane.getInterpolatedPosition();

        // Scale position to match our visual layout
        int x = pos.x;
        int y = 155; // Fixed y position on the rail

        // Draw crane shadow
        g2d.setColor(new Color(0, 0, 0, 30));
        g2d.fillRoundRect(x - CRANE_WIDTH/2 + 3, y - CRANE_HEIGHT + 3,
            CRANE_WIDTH, CRANE_HEIGHT, 5, 5);

        // Draw crane body
        GradientPaint gradient = new GradientPaint(
            x, y - CRANE_HEIGHT, CRANE_COLOR.brighter(),
            x, y, CRANE_COLOR
        );
        g2d.setPaint(gradient);
        g2d.fillRoundRect(x - CRANE_WIDTH/2, y - CRANE_HEIGHT,
            CRANE_WIDTH, CRANE_HEIGHT, 5, 5);

        // Draw crane border
        g2d.setColor(CRANE_COLOR.darker());
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.drawRoundRect(x - CRANE_WIDTH/2, y - CRANE_HEIGHT,
            CRANE_WIDTH, CRANE_HEIGHT, 5, 5);

        // Draw crane hook/cable
        g2d.setColor(Color.DARK_GRAY);
        g2d.setStroke(new BasicStroke(2.0f));
        int hookY = y + 5;
        g2d.drawLine(x, y, x, hookY);

        // Draw hook
        g2d.fillOval(x - 3, hookY - 3, 6, 6);

        // Draw valve being carried
        Valve carrying = crane.getCarryingValve();
        if (carrying != null) {
            drawValve(g2d, carrying, x - VALVE_SIZE/2, hookY + 5);

            // Draw connection line
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_BEVEL, 0, new float[]{2}, 0));
            g2d.drawLine(x, hookY, x, hookY + 5);
        }

        // Draw crane label
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 10));
        String label = crane.isBusy() ? "BUSY" : "IDLE";
        FontMetrics fm = g2d.getFontMetrics();
        int labelWidth = fm.stringWidth(label);
        g2d.drawString(label, x - labelWidth/2, y - CRANE_HEIGHT/2 + 5);

        // Draw status indicator
        Color statusColor = crane.isBusy() ?
            new Color(255, 152, 0) : new Color(76, 175, 80);
        g2d.setColor(statusColor);
        g2d.fillOval(x - CRANE_WIDTH/2 + 5, y - CRANE_HEIGHT + 5, 8, 8);
    }

    private void drawFloatingInfo(Graphics2D g2d) {
        int x = getWidth() - 220;
        int y = 20;

        // Draw semi-transparent background
        g2d.setColor(new Color(255, 255, 255, 230));
        g2d.fillRoundRect(x, y, 200, 200, 10, 10);
        g2d.setColor(Color.DARK_GRAY);
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.drawRoundRect(x, y, 200, 200, 10, 10);

        // Draw system info
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 11));
        g2d.drawString("System Status", x + 10, y + 20);

        g2d.setFont(new Font("Monospaced", Font.PLAIN, 10));
        int lineY = y + 40;
        int lineHeight = 18;

        // Total valves in system
        int totalInSystem = engine.getTotalValvesInSystem();
        g2d.drawString(String.format("In System: %d", totalInSystem),
            x + 10, lineY);
        lineY += lineHeight;

        // Completed valves
        int completed = engine.getCompletedValves().size();
        g2d.drawString(String.format("Completed: %d", completed),
            x + 10, lineY);
        lineY += lineHeight;

        // Completion rate
        int total = engine.getAllValves().size();
        double compRate = total > 0 ? (completed * 100.0 / total) : 0;
        g2d.drawString(String.format("Rate: %.1f%%", compRate),
            x + 10, lineY);
        lineY += lineHeight;

        lineY += 10;

        // Crane info
        Crane crane = engine.getCrane();
        g2d.drawString(String.format("Crane Trips: %d", crane.getTotalTrips()),
            x + 10, lineY);
        lineY += lineHeight;

        double craneUtil = crane.getUtilization(engine.getCurrentTime());
        g2d.drawString(String.format("Crane Util: %.1f%%", craneUtil),
            x + 10, lineY);
        lineY += lineHeight;

        lineY += 10;

        // Bottleneck indicator
        String bottleneck = findBottleneck();
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 10));
        g2d.setColor(new Color(244, 67, 54));
        g2d.drawString("Bottleneck:", x + 10, lineY);
        g2d.setFont(new Font("Monospaced", Font.PLAIN, 10));
        g2d.drawString(bottleneck, x + 10, lineY + lineHeight);
    }

    private String findBottleneck() {
        double maxUtil = 0;
        String bottleneck = "None";
        double currentTime = engine.getCurrentTime();

        for (Location loc : engine.getLocations().values()) {
            if (loc.getName().startsWith("M")) {
                double util = loc.getUtilization(currentTime);
                if (util > maxUtil) {
                    maxUtil = util;
                    bottleneck = loc.getName();
                }
            }
        }

        return String.format("%s (%.0f%%)", bottleneck, maxUtil);
    }
}

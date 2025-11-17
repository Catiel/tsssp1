package gui;

import core.SimulationEngine;
import javax.swing.*;
import java.awt.*;

public class SimulationPanel extends JPanel {
    private SimulationEngine engine;
    private AnimationPanel animationPanel;
    private JPanel infoPanel;

    public SimulationPanel(SimulationEngine engine) {
        this.engine = engine;
        setLayout(new BorderLayout(5, 5));

        initializeComponents();
        layoutComponents();
    }

    private void initializeComponents() {
        animationPanel = new AnimationPanel(engine);
        infoPanel = createInfoPanel();
    }

    private void layoutComponents() {
        add(animationPanel, BorderLayout.CENTER);
        add(infoPanel, BorderLayout.EAST);
    }

    private JPanel createInfoPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setPreferredSize(new Dimension(250, 0));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("System Status"),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        // Add legend and info components
        panel.add(createLegendPanel());
        panel.add(Box.createVerticalStrut(10));
        panel.add(createQuickStatsPanel());

        return panel;
    }

    private JPanel createLegendPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(0, 1, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Legend"));

        for (model.Valve.Type type : model.Valve.Type.values()) {
            JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JLabel colorBox = new JLabel("  ██  ");
            colorBox.setForeground(type.getColor());
            colorBox.setFont(new Font("Monospaced", Font.BOLD, 12));
            item.add(colorBox);
            item.add(new JLabel(type.name()));
            panel.add(item);
        }

        return panel;
    }

    private JPanel createQuickStatsPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Quick Stats"));

        // These will be updated dynamically
        return panel;
    }

    public void updateDisplay() {
        animationPanel.repaint();
        infoPanel.repaint();
    }
}

import gui.MainFrame;
import utils.Config;
import utils.Logger;
import javax.swing.*;
import com.formdev.flatlaf.FlatDarkLaf;

public class Main {
    public static void main(String[] args) {
        // Initialize Config first
        Config config = Config.getInstance();

        // Then configure Logger
        Logger logger = Logger.getInstance();
        logger.configure(config);

        logger.info("Starting Valve Manufacturing Simulation...");

        // Set FlatLaf Look and Feel for modern UI
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
            logger.info("FlatLaf Dark theme loaded");
        } catch (Exception e) {
            logger.warn("Failed to initialize FlatLaf: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
            logger.info("Main frame displayed");
        });
    }
}

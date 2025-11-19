import gui.MainFrame; // Importa clase MainFrame que es la ventana principal de la aplicación
import utils.Config; // Importa clase Config para gestión de configuración
import utils.Logger; // Importa clase Logger para registro de eventos y errores
import javax.swing.*; // Importa componentes Swing para interfaz gráfica
import com.formdev.flatlaf.FlatDarkLaf; // Importa tema oscuro de FlatLaf para Look and Feel moderno

public class Main { // Clase principal de la aplicación
    public static void main(String[] args) { // Método principal que es el punto de entrada de la aplicación
        // Initialize Config first
        Config config = Config.getInstance(); // Obtiene instancia singleton de configuración (inicializa antes de todo)

        // Then configure Logger
        Logger logger = Logger.getInstance(); // Obtiene instancia singleton del logger
        logger.configure(config); // Configura logger con parámetros de config (nivel, archivo, etc.)

        logger.info("Starting Valve Manufacturing Simulation..."); // Registra mensaje de inicio de aplicación en log

        // Set FlatLaf Look and Feel for modern UI
        try { // Bloque try para capturar excepciones al configurar Look and Feel
            UIManager.setLookAndFeel(new FlatDarkLaf()); // Establece tema oscuro de FlatLaf como Look and Feel de Swing
            logger.info("FlatLaf Dark theme loaded"); // Registra carga exitosa del tema en log
        } catch (Exception e) { // Captura cualquier excepción durante configuración de tema
            logger.warn("Failed to initialize FlatLaf: " + e.getMessage()); // Registra advertencia con mensaje de error en log
        }

        SwingUtilities.invokeLater(() -> { // Ejecuta código en el Event Dispatch Thread (EDT) de Swing para thread-safety
            MainFrame frame = new MainFrame(); // Crea instancia del frame principal de la aplicación
            frame.setVisible(true); // Hace visible la ventana principal
            logger.info("Main frame displayed"); // Registra que el frame principal fue mostrado en log
        });
    }
}

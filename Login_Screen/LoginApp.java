import javax.swing.*;
import java.awt.*;

/**
 * LoginApp  –  Application entry point.
 *
 * Execution order:
 *   1. Apply system Look-and-Feel (falls back gracefully).
 *   2. Initialise / verify the login.db SQLite database.
 *   3. Show LoginFrame on the Event Dispatch Thread.
 *
 * Compile & run (from the login_screen/ folder):
 *
 *   javac -cp ".;sqlite-jdbc-<version>.jar" *.java
 *   java  -cp ".;sqlite-jdbc-<version>.jar" LoginApp
 *
 *  
 */
public class LoginApp {

    public static void main(String[] args) {

        // Look-and-Feel
        applyLookAndFeel();

        // Database initialisation
        AuthService authService = new AuthService();
        boolean dbReady = authService.initializeDatabase();

        if (!dbReady) {
            JOptionPane.showMessageDialog(null,
                    "Could not initialise login.db.\n\n"
                    + "Make sure:\n"
                    + "  • The application has write permission in its folder.\n"
                    + "  • sqlite-jdbc-<version>.jar is on the classpath.\n\n"
                    + "The application will now exit.",
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        // Launch login window on the EDT 
        SwingUtilities.invokeLater(() -> {
            LoginFrame frame = new LoginFrame(authService);
            frame.setVisible(true);
        });
    }

    private static void applyLookAndFeel() {
        // Prefer the native L&F, if unavailable fall back to cross-platform
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            try {
                UIManager.setLookAndFeel(
                        UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception e) {
                System.err.println("[LoginApp] Could not set L&F: " + e.getMessage());
            }
        }

        // Global font defaults so text is crisp on all platforms
        Font baseFont = new Font("Segoe UI", Font.PLAIN, 13);
        java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key  = keys.nextElement();
            Object val  = UIManager.get(key);
            if (val instanceof Font) UIManager.put(key, baseFont);
        }

        // Rendering hints for crisper text
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
    }
}

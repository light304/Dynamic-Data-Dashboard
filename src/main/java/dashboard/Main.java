package dashboard;

import dashboard.auth.AuthService;
import dashboard.auth.LoginFrame;
import dashboard.database.SchemaIntrospector;

import javax.swing.*;
import java.awt.Font;
import java.util.Enumeration;

/**
 * Application entry point.
 *
 * Order of events:
 *   1. Apply the look and feel and base font.
 *   2. Confirm the backend service is reachable.
 *   3. Open and, if needed, create the login database.
 *   4. Show the login window. The dashboard opens only after
 *      a successful sign-in.
 */
public class Main {

    public static void main(String[] args) {

        applyLookAndFeel();
        requireBackend();

        AuthService authService = new AuthService();

        if (!authService.initializeDatabase()) {
            JOptionPane.showMessageDialog(null,
                    "Could not open the login database.\n\n"
                    + "The application needs permission to create login.db in:\n"
                    + System.getProperty("user.dir"),
                    "Login Database Error",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        SwingUtilities.invokeLater(() ->
                new LoginFrame(authService).setVisible(true));
    }

    /**
     * Checks the backend before the login window appears, so the user finds
     * out immediately rather than after entering their credentials.
     */
    private static void requireBackend() {
        try {
            SchemaIntrospector.introspect();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null,
                    "Cannot reach the backend service at http://localhost:3000\n\n"
                    + "Start it first, in a separate terminal:\n\n"
                    + "    cd src\\main\\java\\dashboard\\database\n"
                    + "    npm start\n\n"
                    + "Then run this application again.",
                    "Backend Not Running",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private static void applyLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception e) {
                System.err.println("[Main] Could not set look and feel: " + e.getMessage());
            }
        }

        // Base font for anything that doesn't set its own — dialogs, file
        // choosers, tables. Raising this size is the single cheapest
        // accessibility improvement available.
        Font baseFont = new Font("Segoe UI", Font.PLAIN, 13);
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            if (UIManager.get(key) instanceof Font) {
                UIManager.put(key, baseFont);
            }
        }

        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
    }
}
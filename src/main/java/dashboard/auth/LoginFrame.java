package dashboard.auth;
import dashboard.gui.DashboardFrame;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/**
 * LoginFrame  –  Swing login window for the Dynamic Data Dashboard.
 *
 * Design language: dark navy theme matching the main dashboard.
 * Features
 *   • SHA-256 password validation via AuthService
 *   • Login-audit logging (built into AuthService)
 *   • Brute-force lockout (5 attempts)
 *   • Frame shake animation on bad credentials
 *   • Background authentication (SwingWorker – UI never freezes)
 *   • Enter key triggers Sign-In button
 */
public class LoginFrame extends JFrame {

    // Palette 
    private static final Color BG          = new Color(0x0D1B2A);
    private static final Color CARD_BG     = new Color(0x1B2A3B);
    private static final Color CARD_BORDER = new Color(0x1E3A5F);
    private static final Color FIELD_BG    = new Color(0x0A1628);
    private static final Color FIELD_IDLE  = new Color(0x263A4E);
    private static final Color FIELD_FOCUS = new Color(0x4FC3F7);
    private static final Color BTN_START   = new Color(0x0D47A1);
    private static final Color BTN_END     = new Color(0x4FC3F7);
    private static final Color TEXT_WHITE  = new Color(0xF0F4F8);
    private static final Color TEXT_MUTED  = new Color(0x8BAAB9);
    private static final Color ACCENT      = new Color(0x4FC3F7);
    private static final Color ERROR       = new Color(0xFF5252);
    private static final Color SUCCESS     = new Color(0x69F0AE);

    // Component references 
    private JTextField     usernameField;
    private JPasswordField passwordField;
    private JButton        signInBtn;
    private JTextArea      errorArea;
    private JLabel         statusLabel;

    private final AuthService authService;

    // Constructor 
    public LoginFrame(AuthService authService) {
        this.authService = authService;
        setupFrame();
        buildUI();
        getRootPane().setDefaultButton(signInBtn);   // Enter key triggers login
    }

    // Frame setup 
    private void setupFrame() {
        setTitle("Dynamic Data Dashboard - Sign In");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(480, 700);
        setLocationRelativeTo(null);
        setResizable(false);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());
    }

    // UI construction 
    private void buildUI() {
        // Centre the card vertically and horizontally
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(BG);
        wrapper.add(buildCard());
        add(wrapper, BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    private JPanel buildCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(CARD_BORDER, 1, true),
                new EmptyBorder(44, 52, 44, 52)));

        card.add(buildHeader());
        card.add(vGap(28));

        card.add(fieldLabel("USERNAME"));
        card.add(vGap(6));
        usernameField = buildTextField();
        card.add(usernameField);
        card.add(vGap(18));

        card.add(fieldLabel("PASSWORD"));
        card.add(vGap(6));
        passwordField = buildPasswordField();
        card.add(passwordField);
        card.add(vGap(12));

        // Error area — wraps natively, fixed height so nothing shifts
        errorArea = new JTextArea();
        errorArea.setLineWrap(true);
        errorArea.setWrapStyleWord(true);
        errorArea.setEditable(false);
        errorArea.setFocusable(false);
        errorArea.setOpaque(false);
        errorArea.setBorder(null);
        errorArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        errorArea.setForeground(ERROR);
        errorArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        errorArea.setMinimumSize(new Dimension(320, 54));
        errorArea.setPreferredSize(new Dimension(320, 54));
        errorArea.setMaximumSize(new Dimension(320, 54));
        card.add(errorArea);
        card.add(vGap(26));

        signInBtn = buildSignInButton();
        card.add(signInBtn);
        card.add(vGap(14));

        // Status line (shows "Welcome, ..." on success)
        statusLabel = styledLabel(" ", 13, TEXT_MUTED);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setMinimumSize(new Dimension(320, 22));
        statusLabel.setPreferredSize(new Dimension(320, 22));
        statusLabel.setMaximumSize(new Dimension(320, 22));
        card.add(statusLabel);

        return card;
    }

    // Header (icon + title) 
    private JPanel buildHeader() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(CARD_BG);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Hex-diamond icon rendered via Unicode
        JLabel icon = new JLabel("⬡", SwingConstants.CENTER);
        icon.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 42));
        icon.setForeground(ACCENT);
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(icon);

        p.add(vGap(8));

        JLabel title = new JLabel("Dynamic Data Dashboard", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(TEXT_WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(title);

        p.add(vGap(4));

        JLabel sub = new JLabel("Online Retail  ·  Business Intelligence", SwingConstants.CENTER);
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(TEXT_MUTED);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(sub);

        p.add(vGap(16));

        // Separator
        JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
        sep.setForeground(CARD_BORDER);
        sep.setMaximumSize(new Dimension(Short.MAX_VALUE, 1));
        p.add(sep);

        return p;
    }

    // Field helpers 
    private JLabel fieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setForeground(TEXT_MUTED);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JTextField buildTextField() {
        JTextField f = new JTextField();
        styleField(f);
        return f;
    }

    private JPasswordField buildPasswordField() {
        JPasswordField f = new JPasswordField();
        styleField(f);
        f.setEchoChar('•');
        return f;
    }

    private void styleField(JTextField f) {
        f.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        f.setForeground(TEXT_WHITE);
        f.setBackground(FIELD_BG);
        f.setCaretColor(ACCENT);
        f.setSelectionColor(new Color(0x1E3A5F));
        f.setBorder(fieldBorder(FIELD_IDLE));
        f.setMaximumSize(new Dimension(Short.MAX_VALUE, 46));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);

        f.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { f.setBorder(fieldBorder(FIELD_FOCUS)); }
            @Override public void focusLost (FocusEvent e) { f.setBorder(fieldBorder(FIELD_IDLE));  }
        });
    }

    private Border fieldBorder(Color color) {
        return BorderFactory.createCompoundBorder(
                new LineBorder(color, 1, true),
                new EmptyBorder(10, 14, 10, 14));
    }

    // Sign-In button (gradient paint) 
    private JButton buildSignInButton() {
        JButton btn = new JButton("SIGN  IN") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = isEnabled()
                        ? new GradientPaint(0, 0, BTN_START, getWidth(), 0, BTN_END)
                        : new GradientPaint(0, 0, new Color(0x37474F),
                                            getWidth(), 0, new Color(0x546E7A));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setForeground(TEXT_WHITE);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Short.MAX_VALUE, 48));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setOpaque(false);

        btn.addActionListener(e -> performLogin());
        return btn;
    }

    // Footer 
    private JPanel buildFooter() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(0, 0, 10, 0));
        JLabel lbl = new JLabel(
                "© 2026 AUT Dynamic Data Dashboard  ·  v1.0 - PROTOTYPE ·  Offline Localhost App");
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setForeground(new Color(0x3A556A));
        p.add(lbl);
        return p;
    }

    // Authentication flow 
    private void performLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password.");
            return;
        }

        // Lock UI while worker runs
        setFormEnabled(false);
        signInBtn.setText("Verifying…");
        clearError();

        SwingWorker<AuthService.AuthResult, Void> worker =
                new SwingWorker<>() {
            @Override
            protected AuthService.AuthResult doInBackground() {
                return authService.authenticate(username, password);
            }

            @Override
            protected void done() {
                try {
                    AuthService.AuthResult result = get();
                    if (result.success) {
                        statusLabel.setForeground(SUCCESS);
                        statusLabel.setText("Success! Welcome, "
                                + result.session.getFullName() + "!");
                        // Brief pause so the user sees the welcome message
                        Timer t = new Timer(900, ev -> openDashboard(result.session));
                        t.setRepeats(false);
                        t.start();
                    } else {
                        showError(result.errorMessage);
                        signInBtn.setText("SIGN  IN");
                        setFormEnabled(true);
                        statusLabel.setText(" ");
                        passwordField.setText("");
                        passwordField.requestFocus();
                    }
                } catch (Exception ex) {
                    showError("Unexpected error: " + ex.getMessage());
                    signInBtn.setText("SIGN  IN");
                    setFormEnabled(true);
                }
            }
        };
        worker.execute();
    }

    //  Post-login 
    private void openDashboard(UserSession session) {
        dispose();
        SwingUtilities.invokeLater(() -> new DashboardFrame().setVisible(true));
    }

    // UI helpers 
    private void showError(String message) {
        errorArea.setText(message);
        shakeWindow();
    }

    private void clearError() {
        errorArea.setText("");
    }

    private void setFormEnabled(boolean enabled) {
        usernameField.setEnabled(enabled);
        passwordField.setEnabled(enabled);
        signInBtn.setEnabled(enabled);
    }

    /** Quick horizontal shake animation to signal a failed login. */
    private void shakeWindow() {
        final Point origin = getLocation();
        final int[] step   = {0};
        Timer t = new Timer(25, null);
        t.addActionListener(e -> {
            step[0]++;
            int dx = (step[0] % 2 == 0) ? 8 : -8;
            setLocation(origin.x + dx, origin.y);
            if (step[0] >= 10) {
                ((Timer) e.getSource()).stop();
                setLocation(origin);
            }
        });
        t.start();
    }

    private JLabel styledLabel(String text, int size, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, size));
        l.setForeground(color);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private Component vGap(int height) {
        return Box.createRigidArea(new Dimension(0, height));
    }
}

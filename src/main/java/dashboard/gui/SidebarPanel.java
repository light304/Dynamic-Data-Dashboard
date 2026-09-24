package dashboard.gui;
import dashboard.auth.UserSession;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.border.Border;

public class SidebarPanel extends JPanel {
    private static final Color SIDEBAR = Theme.SIDEBAR;
    private static final Color ACTIVE = Theme.ACCENT;
    private static final Color DIVIDER = new Color(30, 41, 59);
    private final Consumer<String> pageChangeHandler;
    private final Map<String,JButton> buttons = new LinkedHashMap<>();

    public SidebarPanel(Consumer<String> pageChangeHandler, Runnable onUpload, Runnable onLogout) {
        this.pageChangeHandler = pageChangeHandler;
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(210,0));
        setBackground(SIDEBAR);
        setBorder(new EmptyBorder(0,18,25,18));

        // Nav Bar (Pages)
        JPanel nav = new JPanel();
        nav.setLayout(new BoxLayout(nav,BoxLayout.Y_AXIS));
        nav.setBackground(SIDEBAR);

        List<String> pages = new ArrayList<>(List.of(
                "Overview", "Sales", "Inventory", "Products",
                "Marketing", "Customers"));

        if (UserSession.getInstance().isManager()) {
            pages.add("Alerts");
        }

        for (String page : pages) {
            addButton(nav, page, "Overview".equals(page));
        }

        /*
         * Upload sits with the navigation rather than beside Log Out.
         * Loading data is a task; signing out ends the session. Keeping
         * them apart makes the destructive one harder to hit by accident.
         */
        JButton upload = new JButton("Upload Data (CSV)");
        upload.setFocusPainted(false);
        upload.setBorderPainted(false);
        upload.setOpaque(true);
        upload.setBackground(Theme.SIDEBAR_BUTTON);
        upload.setForeground(Color.WHITE);
        upload.setFont(Theme.BODY);
        upload.setPreferredSize(new Dimension(174, 38));
        upload.setMaximumSize(new Dimension(174, 38));
        upload.setAlignmentX(Component.LEFT_ALIGNMENT);
        upload.setToolTipText("Load products, customers, marketing, inventory or sales data from a CSV file");
        upload.addActionListener(e -> onUpload.run());

        JSeparator rule = new JSeparator(SwingConstants.HORIZONTAL);
        rule.setForeground(DIVIDER);
        rule.setBackground(SIDEBAR);
        rule.setMaximumSize(new Dimension(174, 1));
        rule.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.setBackground(SIDEBAR);
        north.add(buildBranding());
        north.add(nav);
        north.add(Box.createVerticalStrut(6));
        north.add(rule);
        north.add(Box.createVerticalStrut(14));
        north.add(upload);

        add(north, BorderLayout.NORTH);

        // Log Out stays alone at the bottom, beneath the signed-in details.
        JButton logout = new JButton("Log Out");
        logout.setFocusPainted(false);
        logout.setBorderPainted(false);
        logout.setOpaque(true);
        logout.setBackground(Theme.SIDEBAR_BUTTON);
        logout.setForeground(Color.WHITE);
        logout.setFont(Theme.BODY);
        logout.setPreferredSize(new Dimension(174, 34));
        logout.setMaximumSize(new Dimension(174, 34));
        logout.setAlignmentX(Component.LEFT_ALIGNMENT);
        logout.setToolTipText("Sign out and return to the login screen");
        logout.addActionListener(e -> onLogout.run());

        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setBackground(SIDEBAR);
        bottom.add(buildSignedIn());
        bottom.add(Box.createVerticalStrut(10));
        bottom.add(logout);

        add(bottom, BorderLayout.SOUTH);
    }

    /**
     * Signed-in details.
     *
     * A JTextArea rather than a JLabel: a plain JLabel will not wrap, and
     * the HTML-in-JLabel alternative is unreliable inside a BoxLayout, so a
     * long name would be truncated rather than run onto a second line.
     */
    private JTextArea buildSignedIn() {

        UserSession session = UserSession.getInstance();

        String text = session.getFullName() == null
                ? " "
                : "Signed in as: " + session.getFullName() 
                + "\nAccess Level: " + session.getRoleName();

        JTextArea who = new JTextArea(text);
        who.setFont(Theme.SMALL);
        who.setForeground(new Color(148, 163, 184));
        who.setBackground(SIDEBAR);
        who.setLineWrap(true);
        who.setWrapStyleWord(true);
        who.setEditable(false);
        who.setFocusable(false);
        who.setOpaque(true);
        who.setBorder(null);
        who.setAlignmentX(Component.LEFT_ALIGNMENT);
        who.setMaximumSize(new Dimension(174, 80));

        return who;
    }

    /**
     * Application branding, moved here from DashboardFrame's top strip.
     */
    private JPanel buildBranding() {

        JPanel branding = new JPanel();
        branding.setLayout(new BoxLayout(branding, BoxLayout.Y_AXIS));
        branding.setBackground(SIDEBAR);
        branding.setBorder(new EmptyBorder(24, 0, 22, 0));
        branding.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel line1 = new JLabel("Dynamic Retail");
        line1.setForeground(Color.WHITE);
        line1.setFont(Theme.BRAND);
        line1.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel line2 = new JLabel("Dashboard");
        line2.setForeground(Color.WHITE);
        line2.setFont(Theme.BRAND);
        line2.setAlignmentX(Component.LEFT_ALIGNMENT);

        String role = UserSession.getInstance().getRoleName();

        // JLabel sub = new JLabel(
        //         role == null ? "Dashboard" : role + " Access");
        // sub.setForeground(new Color(170, 180, 195));
        // sub.setFont(Theme.SMALL);
        // sub.setAlignmentX(Component.LEFT_ALIGNMENT);

        branding.add(line1);
        branding.add(line2);
        branding.add(Box.createVerticalStrut(3));
        // branding.add(sub);

        return branding;
    }

    private void addButton(JPanel nav,String page,boolean active) {
        JButton button = new JButton(page);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setBackground(active?ACTIVE:SIDEBAR);
        button.setForeground(Color.WHITE);
        button.setFont(active ? Theme.BODY_STRONG : Theme.BODY);
        button.setPreferredSize(new Dimension(174,40));
        button.setMaximumSize(new Dimension(174,40));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBorder(navBorder(active));
        button.addActionListener(e->{ setActive(page); pageChangeHandler.accept(page); });
        buttons.put(page,button);
        nav.add(button);
        nav.add(Box.createVerticalStrut(8));
    }

    private void setActive(String selected) {
        buttons.forEach((name, button) -> {
            boolean on = name.equals(selected);
            button.setBackground(on ? ACTIVE : SIDEBAR);
            button.setFont(on ? Theme.BODY_STRONG : Theme.BODY);
            button.setBorder(navBorder(on));
        });
    }

    private static Border navBorder(boolean active) {
        return active
                ? BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 3, 0, 0, Theme.HIGHLIGHT),
                        new EmptyBorder(0, 12, 0, 15))
                : new EmptyBorder(0, 15, 0, 15);
    }
}

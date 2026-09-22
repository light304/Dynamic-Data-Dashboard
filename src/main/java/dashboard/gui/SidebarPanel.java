package dashboard.gui;
import dashboard.auth.UserSession;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class SidebarPanel extends JPanel {
    private static final Color SIDEBAR = new Color(17,24,39);
    private static final Color ACTIVE = new Color(0,212,255);
    private final Consumer<String> pageChangeHandler;
    private final Map<String,JButton> buttons = new LinkedHashMap<>();

        public SidebarPanel(Consumer<String> pageChangeHandler, Runnable onUpload, Runnable onLogout) {
        this.pageChangeHandler = pageChangeHandler;
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(210,0));
        setBackground(SIDEBAR);
        setBorder(new EmptyBorder(25,18,25,18));

        // Nav Bar (Pages)
        JPanel nav = new JPanel();
        nav.setLayout(new BoxLayout(nav,BoxLayout.Y_AXIS));
        nav.setBackground(SIDEBAR);

        // Pages (all viewable, except Alerts is only for Managers)
        List<String> pages = new ArrayList<>(List.of(
                "Overview", "Sales", "Inventory", "Products",
                "Marketing", "Customers", "Reports"));

        if (UserSession.getInstance().isManager()) {
            pages.add("Alerts");
        }

        for (String page : pages) {
            addButton(nav, page, "Overview".equals(page));
        }
        
        add(nav,BorderLayout.NORTH);

        // Upload CSV Button
        JButton upload = new JButton("Upload Data (CSV)");
        upload.setFocusPainted(false);
        upload.setBorderPainted(false);
        upload.setOpaque(true);
        upload.setBackground(new Color(30, 41, 59));
        upload.setForeground(Color.WHITE);
        upload.setFont(new Font("SansSerif", Font.PLAIN, 13));
        upload.setPreferredSize(new Dimension(174, 38));
        upload.setToolTipText("Load products, customers, marketing, inventory or sales data from a CSV file");
        upload.addActionListener(e -> onUpload.run());

        // Logout Button
        JButton logout = new JButton("Log Out");
        logout.setFocusPainted(false);
        logout.setBorderPainted(false);
        logout.setOpaque(true);
        logout.setBackground(new Color(55, 65, 81));
        logout.setForeground(Color.WHITE);
        logout.setFont(new Font("SansSerif", Font.PLAIN, 13));
        logout.setPreferredSize(new Dimension(174, 34));
        logout.setToolTipText("Sign out and return to the login screen");
        logout.addActionListener(e -> onLogout.run());

        UserSession session = UserSession.getInstance();
        JLabel who = new JLabel(session.getFullName() == null
                ? " "
                : session.getFullName() + "  ·  " + session.getRoleName());
        who.setForeground(new Color(148, 163, 184));
        who.setFont(new Font("SansSerif", Font.PLAIN, 11));
        who.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setBackground(SIDEBAR);
        bottom.add(who);
        bottom.add(Box.createVerticalStrut(8));
        bottom.add(upload);
        bottom.add(Box.createVerticalStrut(6));
        bottom.add(logout);

        add(bottom, BorderLayout.SOUTH);

    }

    private void addButton(JPanel nav,String page,boolean active) {
        JButton button = new JButton(page);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setBackground(active?ACTIVE:SIDEBAR);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("SansSerif",Font.PLAIN,14));
        button.setPreferredSize(new Dimension(174,40));
        button.setMaximumSize(new Dimension(174,40));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBorder(new EmptyBorder(0,15,0,15));
        button.addActionListener(e->{ setActive(page); pageChangeHandler.accept(page); });
        buttons.put(page,button);
        nav.add(button);
        nav.add(Box.createVerticalStrut(8));
    }

    private void setActive(String selected) {
        buttons.forEach((name,button)->button.setBackground(name.equals(selected)?ACTIVE:SIDEBAR));
    }
}

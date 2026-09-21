package dashboard.gui;

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

    public SidebarPanel(Consumer<String> pageChangeHandler, Runnable onUpload) {
        this.pageChangeHandler = pageChangeHandler;
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(210,0));
        setBackground(SIDEBAR);
        setBorder(new EmptyBorder(25,18,25,18));

        // Nav Bar (Pages)
        JPanel nav = new JPanel();
        nav.setLayout(new BoxLayout(nav,BoxLayout.Y_AXIS));
        nav.setBackground(SIDEBAR);

        for (String page : new String[]{"Overview","Sales","Inventory","Products","Marketing","Customers","Reports","Alerts"}) {
            addButton(nav,page,"Overview".equals(page));
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

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(SIDEBAR);
        bottom.add(upload, BorderLayout.SOUTH);
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

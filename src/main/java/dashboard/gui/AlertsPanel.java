package dashboard.gui;

import dashboard.database.AnalyticsApi;
import dashboard.database.AnalyticsApi.LowStockAlert;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Displays inventory alerts returned by the backend.
 *
 * Currently this page focuses on low-stock products and allows
 * the user to refresh the alerts without restarting the dashboard.
 */
public class AlertsPanel extends JPanel {

    private static final Color BACKGROUND =
            new Color(245, 247, 250);

    private static final Color PRIMARY =
            new Color(31, 41, 55);

    private static final Color SECONDARY =
            new Color(100, 116, 139);

    private static final Color BORDER =
            new Color(226, 232, 240);

    private static final Color ACCENT =
            new Color(0, 188, 225);

    private static final Color WARNING =
            new Color(220, 38, 38);

    private final JLabel alertCount =
            new JLabel("0");

    private final JLabel statusLabel =
            new JLabel("Loading alerts...");

    private final JButton refreshButton =
            new JButton("Refresh");

    private final DefaultTableModel tableModel;

    public AlertsPanel() {

        setLayout(
                new BorderLayout(
                        0,
                        18
                )
        );

        setBackground(BACKGROUND);

        setBorder(
                new EmptyBorder(
                        20,
                        25,
                        25,
                        25
                )
        );


        // =====================================================
        // PAGE HEADER
        // =====================================================

        JPanel header =
                new JPanel(
                        new BorderLayout()
                );

        header.setOpaque(false);


        JPanel titleArea =
                new JPanel();

        titleArea.setOpaque(false);

        titleArea.setLayout(
                new BoxLayout(
                        titleArea,
                        BoxLayout.Y_AXIS
                )
        );


        JLabel title =
                new JLabel("Alerts");

        title.setFont(
                Theme.PAGE_TITLE
        );

        title.setForeground(PRIMARY);


        JLabel subtitle =
                new JLabel(
                        "Inventory warnings and products requiring attention"
                );

        subtitle.setFont(
                Theme.BODY
        );

        subtitle.setForeground(SECONDARY);


        titleArea.add(title);

        titleArea.add(
                Box.createVerticalStrut(4)
        );

        titleArea.add(subtitle);


        // Refreshes the alerts without reloading the application.
        styleRefreshButton();

        refreshButton.addActionListener(
                e -> refreshData()
        );


        JPanel buttonArea =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.RIGHT,
                                0,
                                0
                        )
                );

        buttonArea.setOpaque(false);

        buttonArea.add(refreshButton);


        header.add(
                titleArea,
                BorderLayout.WEST
        );

        header.add(
                buttonArea,
                BorderLayout.EAST
        );


        add(
                header,
                BorderLayout.NORTH
        );


        // =====================================================
        // MAIN CONTENT
        // =====================================================

        JPanel content =
                new JPanel(
                        new BorderLayout(
                                0,
                                16
                        )
                );

        content.setOpaque(false);


        content.add(
                createSummaryCard(),
                BorderLayout.NORTH
        );


        // =====================================================
        // ALERT TABLE
        // =====================================================

        tableModel =
                new DefaultTableModel(
                        new Object[]{
                                "Product ID",
                                "Category",
                                "Warehouse",
                                "Stock Level",
                                "Status"
                        },
                        0
                ) {

                    @Override
                    public boolean isCellEditable(
                            int row,
                            int column
                    ) {

                        return false;
                    }
                };


        JTable table =
                new JTable(
                        tableModel
                );

        /*
         * 34 rather than 30: 14pt rows need a little more height
         * before the text starts to feel cramped.
         */
        table.setRowHeight(34);

        table.setFont(
                Theme.BODY
        );

        table.setForeground(PRIMARY);

        table.setBackground(Color.WHITE);

        table.setSelectionBackground(
                new Color(
                        224,
                        247,
                        250
                )
        );

        table.setFillsViewportHeight(true);

        table.setAutoCreateRowSorter(true);

        table.getTableHeader()
                .setReorderingAllowed(false);

        table.getTableHeader()
                .setFont(
                        Theme.BODY_STRONG
                );


        JScrollPane scrollPane =
                new JScrollPane(table);

        scrollPane.setBorder(
                BorderFactory.createLineBorder(
                        BORDER
                )
        );

        scrollPane.getViewport()
                .setBackground(
                        Color.WHITE
                );


        content.add(
                scrollPane,
                BorderLayout.CENTER
        );


        add(
                content,
                BorderLayout.CENTER
        );


        // Load the alerts when the page is first created.
        refreshData();
    }


    // =========================================================
    // SUMMARY CARD
    // =========================================================

    private JPanel createSummaryCard() {

        JPanel card =
                new JPanel(
                        new BorderLayout()
                );

        card.setBackground(Color.WHITE);

        card.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                BORDER
                        ),
                        new EmptyBorder(
                                18,
                                20,
                                18,
                                20
                        )
                )
        );


        JPanel text =
                new JPanel();

        text.setOpaque(false);

        text.setLayout(
                new BoxLayout(
                        text,
                        BoxLayout.Y_AXIS
                )
        );


        JLabel heading =
                new JLabel(
                        "Low Stock Alerts"
                );

        heading.setFont(
                Theme.CHART_TITLE
        );

        heading.setForeground(PRIMARY);


        statusLabel.setFont(
                Theme.BODY
        );

        statusLabel.setForeground(SECONDARY);


        text.add(heading);

        text.add(
                Box.createVerticalStrut(4)
        );

        text.add(statusLabel);


        // Large count showing the number of active alerts.
        alertCount.setFont(
                Theme.PAGE_TITLE
        );

        alertCount.setForeground(WARNING);


        card.add(
                text,
                BorderLayout.WEST
        );

        card.add(
                alertCount,
                BorderLayout.EAST
        );


        return card;
    }


    // =========================================================
    // LOAD ALERTS
    // =========================================================

    /**
     * Fetches the latest low-stock information in the background
     * so the Swing interface remains responsive.
     */
    public void refreshData() {

        refreshButton.setEnabled(false);

        refreshButton.setText(
                "Loading..."
        );

        statusLabel.setText(
                "Checking inventory..."
        );


        new SwingWorker<List<LowStockAlert>, Void>() {

            @Override
            protected List<LowStockAlert> doInBackground()
                    throws Exception {

                return AnalyticsApi.lowStockAlerts();
            }


            @Override
            protected void done() {

                try {

                    List<LowStockAlert> alerts =
                            get();

                    updateTable(alerts);

                } catch (Exception ex) {

                    statusLabel.setText(
                            "Unable to load alerts"
                    );

                    JOptionPane.showMessageDialog(
                            AlertsPanel.this,
                            ex.getMessage(),
                            "Alerts Error",
                            JOptionPane.ERROR_MESSAGE
                    );

                } finally {

                    refreshButton.setEnabled(
                            true
                    );

                    refreshButton.setText(
                            "Refresh"
                    );
                }
            }
        }.execute();
    }


    // =========================================================
    // UPDATE TABLE
    // =========================================================

    /**
     * Replaces the current table data with the latest backend
     * alert results.
     */
    private void updateTable(
            List<LowStockAlert> alerts
    ) {

        tableModel.setRowCount(0);


        for (LowStockAlert alert : alerts) {

            tableModel.addRow(
                    new Object[]{
                            alert.productId(),
                            alert.category(),
                            alert.warehouse(),
                            alert.stockLevel(),
                            "Low Stock"
                    }
            );
        }


        int count =
                alerts.size();

        alertCount.setText(
                String.valueOf(count)
        );


        if (count == 0) {

            statusLabel.setText(
                    "No low-stock products currently require attention."
            );

        } else if (count == 1) {

            statusLabel.setText(
                    "1 product currently requires attention."
            );

        } else {

            statusLabel.setText(
                    count
                            + " products currently require attention."
            );
        }
    }


    // =========================================================
    // REFRESH BUTTON STYLE
    // =========================================================

    private void styleRefreshButton() {

        refreshButton.setFont(
                Theme.BODY_STRONG
        );

        refreshButton.setForeground(
                Color.WHITE
        );

        refreshButton.setBackground(
                ACCENT
        );

        refreshButton.setOpaque(true);

        refreshButton.setContentAreaFilled(
                true
        );

        refreshButton.setBorderPainted(
                false
        );

        refreshButton.setFocusPainted(
                false
        );

        refreshButton.setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR
                )
        );

        refreshButton.setPreferredSize(
                new Dimension(
                        115,
                        38
                )
        );
    }
}
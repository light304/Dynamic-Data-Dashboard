package dashboard.gui;

import dashboard.database.AnalyticsApi;
import dashboard.database.AnalyticsApi.ActiveAlert;
import dashboard.database.AnalyticsApi.AlertThreshold;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class AlertsPanel extends JPanel {

    private static final Color BACKGROUND = Theme.PAGE_BG;
    private static final Color PRIMARY    = Theme.TEXT;
    private static final Color SECONDARY  = Theme.TEXT_MUTED;
    private static final Color BORDER     = Theme.BORDER;
    private static final Color ACCENT     = Theme.ACCENT;
    private static final Color WARNING    = Theme.CRITICAL;


    // =========================================================
    // SUMMARY
    // =========================================================

    private final JLabel alertCount =
            new JLabel("0");

    private final JLabel statusLabel =
            new JLabel("Loading alerts...");


    // =========================================================
    // BUTTONS
    // =========================================================

    private final JButton refreshButton =
            new JButton("Refresh");

    private final JButton saveButton =
            new JButton("Save Thresholds");


    // =========================================================
    // THRESHOLD CONTROLS
    // =========================================================

    private final JSpinner lowStockSpinner =
            createPercentSpinner();

    private final JSpinner revenueDropSpinner =
            createPercentSpinner();

    private final JSpinner profitDropSpinner =
            createPercentSpinner();


    // =========================================================
    // TABLE
    // =========================================================

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
                        "Monitor active warnings and configure alert thresholds"
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


        styleButton(
                refreshButton,
                ACCENT
        );

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
                new JPanel();

        content.setOpaque(false);

        content.setLayout(
                new BoxLayout(
                        content,
                        BoxLayout.Y_AXIS
                )
        );


        JPanel summaryCard =
                createSummaryCard();

        summaryCard.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );


        JPanel thresholdCard =
                createThresholdCard();

        thresholdCard.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );


        content.add(summaryCard);

        content.add(
                Box.createVerticalStrut(15)
        );

        content.add(thresholdCard);

        content.add(
                Box.createVerticalStrut(15)
        );


        // =====================================================
        // ACTIVE ALERTS HEADING
        // =====================================================

        JPanel tableHeading =
                new JPanel(
                        new BorderLayout()
                );

        tableHeading.setOpaque(false);

        tableHeading.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        tableHeading.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        32
                )
        );


        JLabel activeHeading =
                new JLabel(
                        "Active Alerts"
                );

        activeHeading.setFont(
                Theme.CHART_TITLE
        );

        activeHeading.setForeground(PRIMARY);


        JLabel activeDescription =
                new JLabel(
                        "Alerts currently requiring attention"
                );

        activeDescription.setFont(
                Theme.BODY
        );

        activeDescription.setForeground(SECONDARY);


        tableHeading.add(
                activeHeading,
                BorderLayout.WEST
        );

        tableHeading.add(
                activeDescription,
                BorderLayout.EAST
        );


        content.add(tableHeading);

        content.add(
                Box.createVerticalStrut(8)
        );


        // =====================================================
        // ALERT TABLE
        // =====================================================

        tableModel =
                new DefaultTableModel(
                        new Object[]{
                                "Type",
                                "Title",
                                "Message",
                                "Current",
                                "Threshold",
                                "Severity",
                                "Created"
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

        table.setSelectionForeground(PRIMARY);

        table.setFillsViewportHeight(true);

        table.setAutoCreateRowSorter(true);

        table.setAutoResizeMode(
                JTable.AUTO_RESIZE_LAST_COLUMN
        );


        table.getTableHeader()
                .setReorderingAllowed(false);

        table.getTableHeader()
                .setFont(
                        Theme.BODY_STRONG
                );


        // Make the message column wider.
        table.getColumnModel()
                .getColumn(2)
                .setPreferredWidth(330);

        table.getColumnModel()
                .getColumn(1)
                .setPreferredWidth(170);

        table.getColumnModel()
                .getColumn(5)
                .setPreferredWidth(90);


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

        scrollPane.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        scrollPane.setPreferredSize(
                new Dimension(
                        1000,
                        300
                )
        );

        scrollPane.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        Integer.MAX_VALUE
                )
        );


        content.add(scrollPane);


        add(
                content,
                BorderLayout.CENTER
        );


        // =====================================================
        // INITIAL LOAD
        // =====================================================

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

        card.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        90
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
                        "Active Alerts"
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
    // THRESHOLD CARD
    // =========================================================

    private JPanel createThresholdCard() {

        JPanel card =
                new JPanel(
                        new BorderLayout(
                                15,
                                12
                        )
                );

        card.setBackground(Color.WHITE);

        card.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                BORDER
                        ),
                        new EmptyBorder(
                                16,
                                20,
                                16,
                                20
                        )
                )
        );

        card.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        155
                )
        );


        // -----------------------------------------------------
        // Heading
        // -----------------------------------------------------

        JPanel headingArea =
                new JPanel();

        headingArea.setOpaque(false);

        headingArea.setLayout(
                new BoxLayout(
                        headingArea,
                        BoxLayout.Y_AXIS
                )
        );


        JLabel heading =
                new JLabel(
                        "Alert Thresholds"
                );

        heading.setFont(
                Theme.CHART_TITLE
        );

        heading.setForeground(PRIMARY);


        JLabel description =
                new JLabel(
                        "Adjust when the dashboard should generate alerts."
                );

        description.setFont(
                Theme.BODY
        );

        description.setForeground(SECONDARY);


        headingArea.add(heading);

        headingArea.add(
                Box.createVerticalStrut(3)
        );

        headingArea.add(description);


        card.add(
                headingArea,
                BorderLayout.NORTH
        );


        // -----------------------------------------------------
        // Threshold fields
        // -----------------------------------------------------

        JPanel controls =
                new JPanel(
                        new GridLayout(
                                1,
                                3,
                                18,
                                0
                        )
                );

        controls.setOpaque(false);


        controls.add(
                createThresholdField(
                        "Low Stock",
                        "Percentage of average sales",
                        lowStockSpinner
                )
        );


        controls.add(
                createThresholdField(
                        "Revenue Drop",
                        "Month-to-month decrease",
                        revenueDropSpinner
                )
        );


        controls.add(
                createThresholdField(
                        "Profit Drop",
                        "Month-to-month decrease",
                        profitDropSpinner
                )
        );


        card.add(
                controls,
                BorderLayout.CENTER
        );


        // -----------------------------------------------------
        // Save button
        // -----------------------------------------------------

        styleButton(
                saveButton,
                PRIMARY
        );


        saveButton.setPreferredSize(
                new Dimension(
                        150,
                        38
                )
        );


        saveButton.addActionListener(
                e -> saveThresholds()
        );


        JPanel saveArea =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.RIGHT,
                                0,
                                0
                        )
                );

        saveArea.setOpaque(false);

        saveArea.add(saveButton);


        card.add(
                saveArea,
                BorderLayout.EAST
        );


        return card;
    }


    // =========================================================
    // THRESHOLD FIELD
    // =========================================================

    private JPanel createThresholdField(
            String title,
            String description,
            JSpinner spinner
    ) {

        JPanel panel =
                new JPanel();

        panel.setOpaque(false);

        panel.setLayout(
                new BoxLayout(
                        panel,
                        BoxLayout.Y_AXIS
                )
        );


        JLabel label =
                new JLabel(title);

        label.setFont(
                Theme.BODY_STRONG
        );

        label.setForeground(PRIMARY);


        JLabel descriptionLabel =
                new JLabel(description);

        descriptionLabel.setFont(
                Theme.SMALL
        );

        descriptionLabel.setForeground(SECONDARY);


        JPanel spinnerRow =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT,
                                0,
                                0
                        )
                );

        spinnerRow.setOpaque(false);


        spinner.setPreferredSize(
                new Dimension(
                        85,
                        30
                )
        );


        JLabel percent =
                new JLabel("  %");

        percent.setFont(
                Theme.BODY_STRONG
        );

        percent.setForeground(SECONDARY);


        spinnerRow.add(spinner);

        spinnerRow.add(percent);


        panel.add(label);

        panel.add(
                Box.createVerticalStrut(2)
        );

        panel.add(descriptionLabel);

        panel.add(
                Box.createVerticalStrut(6)
        );

        panel.add(spinnerRow);


        return panel;
    }


    // =========================================================
    // SPINNER
    // =========================================================

    private static JSpinner createPercentSpinner() {

        JSpinner spinner =
                new JSpinner(
                        new SpinnerNumberModel(
                                10.0,
                                0.0,
                                100.0,
                                1.0
                        )
                );


        JSpinner.NumberEditor editor =
                new JSpinner.NumberEditor(
                        spinner,
                        "0.0"
                );

        spinner.setEditor(editor);


        return spinner;
    }


    // =========================================================
    // LOAD DATA
    // =========================================================

    public void refreshData() {

        refreshButton.setEnabled(false);

        refreshButton.setText(
                "Loading..."
        );

        statusLabel.setText(
                "Checking alert conditions..."
        );


        new SwingWorker<AlertPageData, Void>() {

            @Override
            protected AlertPageData doInBackground()
                    throws Exception {

                List<AlertThreshold> thresholds =
                        AnalyticsApi.alertThresholds();


                List<ActiveAlert> alerts =
                        AnalyticsApi.activeAlerts();


                return new AlertPageData(
                        thresholds,
                        alerts
                );
            }


            @Override
            protected void done() {

                try {

                    AlertPageData data =
                            get();


                    updateThresholdControls(
                            data.thresholds()
                    );


                    updateTable(
                            data.alerts()
                    );


                } catch (Exception ex) {

                    Throwable cause =
                            ex.getCause() != null
                                    ? ex.getCause()
                                    : ex;


                    statusLabel.setText(
                            "Unable to load alerts"
                    );


                    JOptionPane.showMessageDialog(
                            AlertsPanel.this,
                            cause.getMessage(),
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
    // UPDATE THRESHOLDS
    // =========================================================

    private void updateThresholdControls(
            List<AlertThreshold> thresholds
    ) {

        for (
                AlertThreshold threshold :
                thresholds
        ) {

            switch (
                    threshold.key()
            ) {

                case "LOW_STOCK_PERCENT" ->
                        lowStockSpinner.setValue(
                                threshold.value()
                        );


                case "REVENUE_DROP_PERCENT" ->
                        revenueDropSpinner.setValue(
                                threshold.value()
                        );


                case "PROFIT_DROP_PERCENT" ->
                        profitDropSpinner.setValue(
                                threshold.value()
                        );


                default -> {
                    // Ignore unknown future thresholds.
                }
            }
        }
    }


    // =========================================================
    // SAVE THRESHOLDS
    // =========================================================

    private void saveThresholds() {

        saveButton.setEnabled(false);

        saveButton.setText(
                "Saving..."
        );

        statusLabel.setText(
                "Saving thresholds and checking alerts..."
        );


        double lowStock =
                ((Number)
                        lowStockSpinner.getValue()
                ).doubleValue();


        double revenueDrop =
                ((Number)
                        revenueDropSpinner.getValue()
                ).doubleValue();


        double profitDrop =
                ((Number)
                        profitDropSpinner.getValue()
                ).doubleValue();


        new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground()
                    throws Exception {

                AnalyticsApi.saveAlertThreshold(
                        "LOW_STOCK_PERCENT",
                        lowStock
                );


                AnalyticsApi.saveAlertThreshold(
                        "REVENUE_DROP_PERCENT",
                        revenueDrop
                );


                AnalyticsApi.saveAlertThreshold(
                        "PROFIT_DROP_PERCENT",
                        profitDrop
                );


                AnalyticsApi.detectAlerts();


                return null;
            }


            @Override
            protected void done() {

                try {

                    get();


                    JOptionPane.showMessageDialog(
                            AlertsPanel.this,
                            "Alert thresholds saved successfully.",
                            "Thresholds Saved",
                            JOptionPane.INFORMATION_MESSAGE
                    );


                    refreshData();


                } catch (Exception ex) {

                    Throwable cause =
                            ex.getCause() != null
                                    ? ex.getCause()
                                    : ex;


                    statusLabel.setText(
                            "Unable to save thresholds"
                    );


                    JOptionPane.showMessageDialog(
                            AlertsPanel.this,
                            cause.getMessage(),
                            "Save Error",
                            JOptionPane.ERROR_MESSAGE
                    );


                } finally {

                    saveButton.setEnabled(
                            true
                    );

                    saveButton.setText(
                            "Save Thresholds"
                    );
                }
            }

        }.execute();
    }


    // =========================================================
    // UPDATE ACTIVE ALERT TABLE
    // =========================================================

    private void updateTable(
            List<ActiveAlert> alerts
    ) {

        tableModel.setRowCount(0);


        for (
                ActiveAlert alert :
                alerts
        ) {

            tableModel.addRow(
                    new Object[]{

                            formatAlertType(
                                    alert.alertType()
                            ),

                            alert.title(),

                            alert.message(),

                            formatNumber(
                                    alert.currentValue()
                            ),

                            formatNumber(
                                    alert.thresholdValue()
                            ),

                            alert.severity(),

                            alert.createdAt()
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
                    "No active alerts. All monitored values are within their thresholds."
            );

            alertCount.setForeground(
                    Theme.GOOD
            );

        } else if (count == 1) {

            statusLabel.setText(
                    "1 active alert currently requires attention."
            );

            alertCount.setForeground(
                    WARNING
            );

        } else {

            statusLabel.setText(
                    count
                            + " active alerts currently require attention."
            );

            alertCount.setForeground(
                    WARNING
            );
        }
    }


    // =========================================================
    // FORMAT ALERT TYPE
    // =========================================================

    private String formatAlertType(
            String type
    ) {

        if (type == null) {
            return "";
        }


        return switch (type) {

            case "LOW_STOCK" ->
                    "Low Stock";

            case "REVENUE_DROP" ->
                    "Revenue Drop";

            case "PROFIT_DROP" ->
                    "Profit Drop";

            default ->
                    type.replace(
                            "_",
                            " "
                    );
        };
    }


    // =========================================================
    // FORMAT NUMBERS
    // =========================================================

    private String formatNumber(
            double value
    ) {

        if (
                value
                        ==
                Math.rint(value)
        ) {

            return String.valueOf(
                    (long) value
            );
        }


        return String.format(
                "%.2f",
                value
        );
    }


    // =========================================================
    // BUTTON STYLE
    // =========================================================

    private void styleButton(
            JButton button,
            Color background
    ) {

        button.setFont(
                Theme.BODY_STRONG
        );

        button.setForeground(
                Color.WHITE
        );

        button.setBackground(
                background
        );

        button.setOpaque(true);

        button.setContentAreaFilled(
                true
        );

        button.setBorderPainted(
                false
        );

        button.setFocusPainted(
                false
        );

        button.setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR
                )
        );

        button.setPreferredSize(
                new Dimension(
                        115,
                        38
                )
        );
    }


    // =========================================================
    // PAGE DATA HOLDER
    // =========================================================

    private record AlertPageData(
            List<AlertThreshold> thresholds,
            List<ActiveAlert> alerts
    ) {}
}
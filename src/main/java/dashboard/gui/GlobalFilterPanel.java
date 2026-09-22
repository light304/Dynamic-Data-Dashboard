package dashboard.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * Shared filter bar used across the dashboard.
 *
 * The selected filters are passed back to DashboardFrame so the same
 * filter can be applied when moving between the different dashboard pages.
 */
public class GlobalFilterPanel extends JPanel {

    // Main colours used by the filter bar.
    private static final Color ACTIVE = new Color(0, 190, 225);
    private static final Color ACTIVE_HOVER = new Color(0, 168, 204);
    private static final Color TEXT = new Color(31, 41, 55);
    private static final Color MUTED = new Color(100, 116, 139);
    private static final Color BORDER = new Color(226, 232, 240);
    private static final Color SUCCESS = new Color(22, 163, 74);

    // Dashboard filter controls.
    private final JComboBox<Integer> yearFilter =
            new JComboBox<>(new Integer[]{2023, 2024});

    private final JComboBox<String> scopeFilter =
            new JComboBox<>(new String[]{
                    "Yearly",
                    "Quarterly",
                    "Monthly",
                    "Weekly"
            });

    private final JComboBox<String> monthFilter =
            new JComboBox<>(months());

    private final JComboBox<String> periodFilter =
            new JComboBox<>();

    private final JComboBox<String> regionFilter =
            new JComboBox<>(new String[]{
                    "All Regions",
                    "Auckland",
                    "Christchurch",
                    "Melbourne",
                    "Sydney",
                    "Wellington"
            });

    private final JLabel monthLabel = label("Month");

    // Gives the user temporary feedback after applying or resetting filters.
    private final JLabel statusLabel = new JLabel(" ");

    // Sends the selected filter back to DashboardFrame.
    private final Consumer<DashboardFilter> listener;

    // Timer removes the success message after a short delay.
    private final Timer statusTimer;

    public GlobalFilterPanel(Consumer<DashboardFilter> listener) {

        this.listener = listener;

        /*
         * The timer is restarted whenever a new status message is shown.
         * This prevents "Filters applied" from staying on screen permanently.
         */
        statusTimer = new Timer(
                2500,
                e -> statusLabel.setText(" ")
        );

        statusTimer.setRepeats(false);

        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(
                                0,
                                0,
                                1,
                                0,
                                BORDER
                        ),
                        new EmptyBorder(
                                12,
                                22,
                                12,
                                22
                        )
                )
        );

        // Container holding all filters and action buttons.
        JPanel filters =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT,
                                14,
                                0
                        )
                );

        filters.setOpaque(false);

        filters.add(
                group(
                        "Year",
                        yearFilter
                )
        );

        filters.add(
                group(
                        "Scope",
                        scopeFilter
                )
        );

        /*
         * Month is separate because it is only displayed when
         * the user selects Weekly scope.
         */
        JPanel monthGroup = new JPanel();

        monthGroup.setOpaque(false);

        monthGroup.setLayout(
                new BoxLayout(
                        monthGroup,
                        BoxLayout.Y_AXIS
                )
        );

        monthLabel.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        monthFilter.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        monthGroup.add(monthLabel);

        monthGroup.add(
                Box.createVerticalStrut(4)
        );

        monthGroup.add(monthFilter);

        filters.add(monthGroup);

        filters.add(
                group(
                        "Period",
                        periodFilter
                )
        );

        filters.add(
                group(
                        "Region",
                        regionFilter
                )
        );


        // =====================================================
        // APPLY FILTERS BUTTON
        // =====================================================

        RoundedButton apply =
                new RoundedButton(
                        "Apply Filters",
                        ACTIVE,
                        ACTIVE_HOVER
                );

        apply.setPreferredSize(
                new Dimension(
                        140,
                        38
                )
        );

        apply.setFont(
                Theme.SMALL_BOLD
        );


        // =====================================================
        // RESET BUTTON
        // =====================================================

        JButton reset =
                new JButton("Reset");

        reset.setPreferredSize(
                new Dimension(
                        72,
                        38
                )
        );

        reset.setFont(
                Theme.SMALL
        );

        reset.setForeground(MUTED);
        reset.setBackground(Color.WHITE);
        reset.setFocusPainted(false);

        reset.setBorder(
                BorderFactory.createLineBorder(
                        BORDER
                )
        );


        // =====================================================
        // FILTER STATUS MESSAGE
        // =====================================================

        statusLabel.setFont(
                Theme.SMALL_BOLD
        );

        statusLabel.setForeground(SUCCESS);

        /*
         * Reserve some space for the message so the filter bar
         * does not move when the text appears or disappears.
         *
         * 125 rather than 105: "Filters applied" is wider at 13pt
         * than it was at 11pt and was being clipped.
         */
        statusLabel.setPreferredSize(
                new Dimension(
                        125,
                        38
                )
        );


        JPanel actions =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.LEFT,
                                8,
                                18
                        )
                );

        actions.setOpaque(false);

        actions.add(apply);
        actions.add(reset);
        actions.add(statusLabel);

        filters.add(actions);

        add(
                filters,
                BorderLayout.CENTER
        );


        // Apply consistent styling to every combo box.
        styleCombo(yearFilter);
        styleCombo(scopeFilter);
        styleCombo(monthFilter);
        styleCombo(periodFilter);
        styleCombo(regionFilter);


        // =====================================================
        // FILTER CHANGE LISTENERS
        // =====================================================

        /*
         * If the user changes a filter after applying one,
         * remove the previous success message because the
         * displayed data no longer represents the new selection.
         */
        scopeFilter.addActionListener(
                e -> {
                    updatePeriodOptions();
                    clearStatusMessage();
                }
        );

        yearFilter.addActionListener(
                e -> clearStatusMessage()
        );

        monthFilter.addActionListener(
                e -> clearStatusMessage()
        );

        periodFilter.addActionListener(
                e -> clearStatusMessage()
        );

        regionFilter.addActionListener(
                e -> clearStatusMessage()
        );


        // Apply or reset the dashboard-wide filter.
        apply.addActionListener(
                e -> publishFilter()
        );

        reset.addActionListener(
                e -> resetFilters()
        );

        // Set the correct Period options when the dashboard first opens.
        updatePeriodOptions();
    }


    // =========================================================
    // FILTER GROUP CREATION
    // =========================================================

    /**
     * Creates a labelled filter control used in the filter bar.
     */
    private JPanel group(
            String title,
            JComboBox<?> combo
    ) {

        JPanel group = new JPanel();

        group.setOpaque(false);

        group.setLayout(
                new BoxLayout(
                        group,
                        BoxLayout.Y_AXIS
                )
        );

        JLabel groupLabel =
                label(title);

        groupLabel.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        combo.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        group.add(groupLabel);

        group.add(
                Box.createVerticalStrut(4)
        );

        group.add(combo);

        return group;
    }


    /**
     * Creates the small uppercase labels shown above each filter.
     */
    private static JLabel label(String text) {

        JLabel label =
                new JLabel(
                        text.toUpperCase()
                );

        label.setFont(
                Theme.SMALL_BOLD
        );

        label.setForeground(MUTED);

        return label;
    }


    /**
     * Keeps all dashboard filter combo boxes visually consistent.
     */
    private void styleCombo(
            JComboBox<?> combo
    ) {

        combo.setPreferredSize(
                new Dimension(
                        125,
                        36
                )
        );

        combo.setMaximumSize(
                new Dimension(
                        145,
                        36
                )
        );

        combo.setBackground(Color.WHITE);
        combo.setForeground(TEXT);

        combo.setFont(
                Theme.SMALL
        );
    }


    // =========================================================
    // PERIOD OPTIONS
    // =========================================================

    /**
     * Updates the Period dropdown depending on the selected scope.
     *
     * Yearly     -> Full Year
     * Quarterly  -> Q1 - Q4
     * Monthly    -> January - December
     * Weekly     -> Week 1 - Week 5
     */
    private void updatePeriodOptions() {

        String scope =
                String.valueOf(
                        scopeFilter.getSelectedItem()
                );

        periodFilter.removeAllItems();

        boolean weekly =
                "Weekly".equals(scope);

        /*
         * Weekly filtering needs both a month and week.
         * Other scopes do not require the separate Month control.
         */
        monthLabel.setVisible(weekly);
        monthFilter.setVisible(weekly);

        switch (scope) {

            case "Quarterly" -> {

                periodFilter.addItem("Q1");
                periodFilter.addItem("Q2");
                periodFilter.addItem("Q3");
                periodFilter.addItem("Q4");
            }

            case "Monthly" -> {

                for (String month : months()) {
                    periodFilter.addItem(month);
                }
            }

            case "Weekly" -> {

                for (int week = 1; week <= 5; week++) {
                    periodFilter.addItem(
                            "Week " + week
                    );
                }
            }

            default ->
                    periodFilter.addItem(
                            "Full Year"
                    );
        }

        revalidate();
        repaint();
    }


    // =========================================================
    // APPLY FILTER
    // =========================================================

    /**
     * Packages the current UI selections into a DashboardFilter
     * and sends it to DashboardFrame.
     */
    private void publishFilter() {

        String scope =
                String.valueOf(
                        scopeFilter.getSelectedItem()
                );

        String period =
                String.valueOf(
                        periodFilter.getSelectedItem()
                );

        /*
         * Weekly scope uses the separate Month dropdown.
         * Monthly scope uses the selected Period as its month.
         */
        String month =
                "Weekly".equals(scope)
                        ? String.valueOf(
                                monthFilter.getSelectedItem()
                        )
                        : (
                                "Monthly".equals(scope)
                                        ? period
                                        : "January"
                        );


        DashboardFilter filter =
                new DashboardFilter(
                        (Integer) yearFilter.getSelectedItem(),
                        scope,
                        month,
                        period,
                        String.valueOf(
                                regionFilter.getSelectedItem()
                        )
                );


        // Send the selected filter to the dashboard.
        if (listener != null) {
            listener.accept(filter);
        }


        // Give the user clear temporary confirmation.
        showStatusMessage(
                "✓ Filters applied"
        );
    }


    // =========================================================
    // RESET FILTERS
    // =========================================================

    /**
     * Returns every filter to its default value and immediately
     * applies those defaults to the dashboard.
     */
    private void resetFilters() {

        /*
         * Stop the existing status timer first so an older
         * message cannot disappear while the reset is occurring.
         */
        statusTimer.stop();

        yearFilter.setSelectedItem(2023);
        scopeFilter.setSelectedItem("Yearly");
        monthFilter.setSelectedItem("January");
        regionFilter.setSelectedItem("All Regions");

        updatePeriodOptions();


        DashboardFilter defaultFilter =
                new DashboardFilter(
                        2023,
                        "Yearly",
                        "January",
                        "Full Year",
                        "All Regions"
                );


        if (listener != null) {
            listener.accept(
                    defaultFilter
            );
        }


        // Reset gets its own confirmation rather than saying "Applied".
        showStatusMessage(
                "✓ Filters reset"
        );
    }


    // =========================================================
    // STATUS FEEDBACK
    // =========================================================

    /**
     * Displays a temporary confirmation message beside the
     * Apply and Reset buttons.
     */
    private void showStatusMessage(
            String message
    ) {

        statusTimer.stop();

        statusLabel.setText(message);

        statusTimer.restart();
    }


    /**
     * Removes the previous confirmation when a user changes
     * one of the filter selections.
     */
    private void clearStatusMessage() {

        statusTimer.stop();

        statusLabel.setText(" ");
    }


    // =========================================================
    // MONTH DATA
    // =========================================================

    private static String[] months() {

        return new String[]{
                "January",
                "February",
                "March",
                "April",
                "May",
                "June",
                "July",
                "August",
                "September",
                "October",
                "November",
                "December"
        };
    }


    // =========================================================
    // ROUNDED APPLY BUTTON
    // =========================================================

    /**
     * Small custom Swing button used for the primary filter action.
     * The button changes colour when the mouse moves over it.
     */
    private static class RoundedButton
            extends JButton {

        private Color fill;

        private final Color normal;
        private final Color hover;

        RoundedButton(
                String text,
                Color normal,
                Color hover
        ) {

            super(text);

            this.normal = normal;
            this.hover = hover;
            this.fill = normal;

            setForeground(Color.WHITE);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);

            setCursor(
                    Cursor.getPredefinedCursor(
                            Cursor.HAND_CURSOR
                    )
            );


            // Change button colour while hovering.
            addMouseListener(
                    new MouseAdapter() {

                        @Override
                        public void mouseEntered(
                                MouseEvent e
                        ) {

                            fill =
                                    RoundedButton.this.hover;

                            repaint();
                        }


                        @Override
                        public void mouseExited(
                                MouseEvent e
                        ) {

                            fill =
                                    RoundedButton.this.normal;

                            repaint();
                        }
                    }
            );
        }


        /**
         * Paints the cyan rounded background before Swing
         * draws the normal button text.
         */
        @Override
        protected void paintComponent(
                Graphics g
        ) {

            Graphics2D g2 =
                    (Graphics2D) g.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            g2.setColor(fill);

            g2.fillRoundRect(
                    0,
                    0,
                    getWidth(),
                    getHeight(),
                    14,
                    14
            );

            g2.dispose();

            super.paintComponent(g);
        }
    }
}
package dashboard.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.concurrent.Callable;

public abstract class BaseAnalyticsPage extends JPanel implements FilterableDashboardPage {

    protected static final Color BACKGROUND = new Color(245, 247, 250);
    protected static final Color PRIMARY = new Color(31, 41, 55);
    protected static final Color SECONDARY = new Color(100, 116, 139);
    protected static final Color ACCENT = new Color(0, 188, 225);

    protected DashboardFilter filter = DashboardFilter.defaults();

    /*
     * Shared chart area for all analytics pages.
     *
     * Two columns on normal dashboard sizes.
     * Vertical scrolling only.
     */
    protected final JPanel charts = new ResponsiveGridPanel();

    private final JButton refreshButton = new JButton("Refresh");

    protected BaseAnalyticsPage(String title, String subtitle) {

        setLayout(new BorderLayout(0, 18));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(20, 25, 25, 25));

        /*
         * HEADER
         */
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 5, 0));

        JPanel titleArea = new JPanel();
        titleArea.setOpaque(false);
        titleArea.setLayout(new BoxLayout(titleArea, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(
                new Font("SansSerif", Font.BOLD, 28)
        );
        titleLabel.setForeground(PRIMARY);

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(
                new Font("SansSerif", Font.PLAIN, 13)
        );
        subtitleLabel.setForeground(SECONDARY);

        titleArea.add(titleLabel);
        titleArea.add(Box.createVerticalStrut(4));
        titleArea.add(subtitleLabel);

        /*
         * REFRESH BUTTON
         */
        refreshButton.setFont(
                new Font("SansSerif", Font.BOLD, 13)
        );

        refreshButton.setForeground(Color.WHITE);
        refreshButton.setBackground(ACCENT);

        refreshButton.setOpaque(true);
        refreshButton.setContentAreaFilled(true);
        refreshButton.setBorderPainted(false);
        refreshButton.setFocusPainted(false);

        refreshButton.setCursor(
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );

        refreshButton.setPreferredSize(
                new Dimension(115, 38)
        );

        refreshButton.addActionListener(e -> refreshData());

        JPanel refreshArea =
                new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));

        refreshArea.setOpaque(false);
        refreshArea.add(refreshButton);

        header.add(titleArea, BorderLayout.WEST);
        header.add(refreshArea, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        /*
         * CHART AREA
         */
        charts.setBackground(BACKGROUND);

        /*
         * 2-column dashboard structure:
         *
         * [ Chart 1 ] [ Chart 2 ]
         *
         * [ Chart 3 ] [ Chart 4 ]
         */
        charts.setLayout(
                new GridLayout(
                        0,
                        2,
                        18,
                        18
                )
        );

        /*
         * Adds breathing room beneath the final row.
         */
        JPanel chartContainer = new JPanel(new BorderLayout());

        chartContainer.setBackground(BACKGROUND);
        chartContainer.setBorder(
                new EmptyBorder(0, 0, 20, 0)
        );

        chartContainer.add(
                charts,
                BorderLayout.NORTH
        );

        JScrollPane scroll =
                new JScrollPane(chartContainer);

        scroll.setBorder(null);

        scroll.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );

        scroll.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        );

        scroll.getVerticalScrollBar()
                .setUnitIncrement(18);

        scroll.getViewport()
                .setBackground(BACKGROUND);

        add(scroll, BorderLayout.CENTER);
    }

    @Override
    public void applyFilter(DashboardFilter filter) {

        this.filter =
                filter == null
                        ? DashboardFilter.defaults()
                        : filter;

        refreshData();
    }

    /*
     * Clears the current cards before an asynchronous reload.
     */
    protected final void startRefresh() {

        refreshButton.setEnabled(false);
        refreshButton.setText("Loading...");

        charts.removeAll();

        charts.revalidate();
        charts.repaint();
    }

    /*
     * Called when loading is complete.
     */
    protected final void finishRefresh() {

        /*
         * Give every chart a consistent dashboard height.
         *
         * Width is handled automatically by GridLayout.
         */
        for (Component component : charts.getComponents()) {

            if (component instanceof JComponent card) {

                card.setPreferredSize(
                        new Dimension(450, 310)
                );

                card.setMinimumSize(
                        new Dimension(300, 280)
                );
            }
        }

        refreshButton.setEnabled(true);
        refreshButton.setText("Refresh");

        charts.revalidate();
        charts.repaint();
    }

    protected final void showError(Exception ex) {

        charts.add(
                AnalyticsCharts.messageCard(
                        "Unable to load data",
                        ex.getMessage()
                )
        );
    }

    protected abstract void refreshData();

    /*
     * Used by pages that retrieve lists of chart cards
     * asynchronously.
     */
    protected final void loadAsync(
            Callable<List<JPanel>> loader
    ) {

        startRefresh();

        charts.add(
                AnalyticsCharts.messageCard(
                        "Loading",
                        "Fetching data..."
                )
        );

        charts.revalidate();
        charts.repaint();

        new SwingWorker<List<JPanel>, Void>() {

            @Override
            protected List<JPanel> doInBackground()
                    throws Exception {

                return loader.call();
            }

            @Override
            protected void done() {

                charts.removeAll();

                try {

                    List<JPanel> loadedCards = get();

                    for (JPanel card : loadedCards) {
                        charts.add(card);
                    }

                } catch (Exception ex) {

                    showError(ex);
                }

                finishRefresh();
            }

        }.execute();
    }

    /*
     * Makes the chart area follow the width of the viewport.
     *
     * This is important because normal JPanel behaviour can cause
     * charts to retain a very large preferred width and get cut off.
     */
    private static class ResponsiveGridPanel
            extends JPanel
            implements Scrollable {

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(
                Rectangle visibleRect,
                int orientation,
                int direction
        ) {
            return 18;
        }

        @Override
        public int getScrollableBlockIncrement(
                Rectangle visibleRect,
                int orientation,
                int direction
        ) {
            return 100;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
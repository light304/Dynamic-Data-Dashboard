package dashboard.gui;

import dashboard.database.AnalyticsApi;
import dashboard.database.SchemaIntrospector.TableMeta;

import dashboard.report.ChartCatalogue;
import dashboard.report.ChartSpec;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OverviewPanel extends JPanel
        implements FilterableDashboardPage {

    private static final Color BACKGROUND = Theme.PAGE_BG;
    private static final Color PRIMARY    = Theme.TEXT;
    private static final Color SECONDARY  = Theme.TEXT_MUTED;
    private static final Color ACCENT     = Theme.ACCENT;

    private final KpiPanel kpiPanel =
            new KpiPanel();

    private final JPanel chartsGrid =
            new JPanel();

    private final JButton refreshButton =
            new JButton("Refresh");

    private DashboardFilter currentFilter =
            DashboardFilter.defaults();


    public OverviewPanel(
            Map<String, TableMeta> schema
    ) {

        setLayout(
                new BorderLayout(
                        0,
                        14
                )
        );

        setBackground(BACKGROUND);

        setBorder(
                new EmptyBorder(
                        18,
                        25,
                        20,
                        25
                )
        );

        createLayout();
    }


    // =========================================================
    // MAIN LAYOUT
    // =========================================================

    private void createLayout() {

        add(
                createHeader(),
                BorderLayout.NORTH
        );


        JPanel content =
                new JPanel();

        content.setLayout(
                new BoxLayout(
                        content,
                        BoxLayout.Y_AXIS
                )
        );

        content.setBackground(BACKGROUND);


        // =====================================================
        // KPI CARDS
        // =====================================================

        kpiPanel.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        content.add(kpiPanel);

        content.add(
                Box.createVerticalStrut(14)
        );


        // =====================================================
        // ANALYTICS HEADER
        // =====================================================

        JPanel analyticsHeader =
                new JPanel(
                        new BorderLayout()
                );

        analyticsHeader.setOpaque(false);

        analyticsHeader.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        /*
         * 52 rather than 42: the heading and its subtitle both grew
         * with the larger fonts and no longer fit in 42px.
         */
        analyticsHeader.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        52
                )
        );


        JPanel textArea =
                new JPanel();

        textArea.setOpaque(false);

        textArea.setLayout(
                new BoxLayout(
                        textArea,
                        BoxLayout.Y_AXIS
                )
        );


        JLabel analyticsTitle =
                new JLabel(
                        "Dashboard Analytics"
                );

        analyticsTitle.setFont(
                Theme.SECTION
        );

        analyticsTitle.setForeground(PRIMARY);


        JLabel analyticsSubtitle =
                new JLabel(
                        "Key customer, sales, inventory, products and marketing information graphed and displayed for the selected period"
                );

        analyticsSubtitle.setFont(
                Theme.SMALL
        );

        analyticsSubtitle.setForeground(
                SECONDARY
        );


        textArea.add(analyticsTitle);

        textArea.add(
                Box.createVerticalStrut(2)
        );

        textArea.add(analyticsSubtitle);


        analyticsHeader.add(
                textArea,
                BorderLayout.WEST
        );


        content.add(analyticsHeader);

        content.add(
                Box.createVerticalStrut(8)
        );


        // =====================================================
        // CHART GRID
        // =====================================================

        chartsGrid.setLayout(
                new GridLayout(
                        0,
                        2,
                        16,
                        16
                )
        );

        chartsGrid.setBackground(
                BACKGROUND
        );

        chartsGrid.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        chartsGrid.add(
                AnalyticsCharts.messageCard(
                        "Loading",
                        "Waiting for dashboard data..."
                )
        );


        content.add(chartsGrid);

        content.add(
                Box.createVerticalStrut(10)
        );


        // =====================================================
        // SCROLLING
        // =====================================================

        JPanel scrollContainer =
                new JPanel(
                        new BorderLayout()
                );

        scrollContainer.setBackground(
                BACKGROUND
        );

        scrollContainer.add(
                content,
                BorderLayout.NORTH
        );


        JScrollPane scrollPane =
                new JScrollPane(
                        scrollContainer
                );

        scrollPane.setBorder(null);

        scrollPane.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );

        scrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        );

        scrollPane
                .getVerticalScrollBar()
                .setUnitIncrement(18);

        scrollPane
                .getViewport()
                .setBackground(BACKGROUND);


        add(
                scrollPane,
                BorderLayout.CENTER
        );
    }


    // =========================================================
    // HEADER
    // =========================================================

    private JPanel createHeader() {

        JPanel header =
                new JPanel(
                        new BorderLayout()
                );

        header.setOpaque(false);

        header.setBorder(
                new EmptyBorder(
                        0,
                        0,
                        4,
                        0
                )
        );


        JPanel heading =
                new JPanel();

        heading.setOpaque(false);

        heading.setLayout(
                new BoxLayout(
                        heading,
                        BoxLayout.Y_AXIS
                )
        );


        JLabel title =
                new JLabel(
                        "Overview"
                );

        title.setFont(
                Theme.PAGE_TITLE
        );

        title.setForeground(PRIMARY);


        JLabel subtitle =
                new JLabel(
                        "A business insight overview of retail operations across customers, sales, products, inventory and marketing for the selected period"
                );

        subtitle.setFont(
                Theme.BODY
        );

        subtitle.setForeground(
                SECONDARY
        );


        heading.add(title);

        heading.add(
                Box.createVerticalStrut(3)
        );

        heading.add(subtitle);


        // =====================================================
        // REFRESH BUTTON
        // =====================================================

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

        refreshButton.addActionListener(
                e -> refreshEverything()
        );


        JPanel buttonArea =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.RIGHT,
                                10,
                                0
                        )
                );

        buttonArea.setOpaque(false);

        buttonArea.add(
                ExportButton.create(
                        this,
                        ChartSpec.Page.OVERVIEW,
                        () -> currentFilter
                )
        );

        buttonArea.add(
                refreshButton
        );


        header.add(
                heading,
                BorderLayout.WEST
        );

        header.add(
                buttonArea,
                BorderLayout.EAST
        );


        return header;
    }


    // =========================================================
    // FILTER
    // =========================================================

    @Override
    public void applyFilter(
            DashboardFilter filter
    ) {

        currentFilter =
                filter == null
                        ? DashboardFilter.defaults()
                        : filter;

        refreshEverything();
    }


    // =========================================================
    // REFRESH
    // =========================================================

    private void refreshEverything() {

        refreshKpis();
        refreshOverviewCharts();
    }


    // =========================================================
    // KPI DATA
    // =========================================================

    private void refreshKpis() {

        new SwingWorker<
                AnalyticsApi.Kpis,
                Void
                >() {

            @Override
            protected AnalyticsApi.Kpis doInBackground()
                    throws Exception {

                return AnalyticsApi.overview(
                        currentFilter.toParams()
                );
            }


            @Override
            protected void done() {

                try {

                    kpiPanel.update(
                            get()
                    );

                } catch (Exception ex) {

                    ex.printStackTrace();

                    kpiPanel.showError();
                }
            }

        }.execute();
    }


    // =========================================================
    // CHART DATA
    // =========================================================

    private void refreshOverviewCharts() {

        refreshButton.setEnabled(false);

        refreshButton.setText(
                "Loading..."
        );


        chartsGrid.removeAll();

        chartsGrid.add(
                AnalyticsCharts.messageCard(
                        "Loading",
                        "Fetching dashboard analytics..."
                )
        );


        chartsGrid.revalidate();
        chartsGrid.repaint();


        new SwingWorker<
                List<JPanel>,
                Void
                >() {

            @Override
            protected List<JPanel> doInBackground()
                    throws Exception {

                /*
                 * The four Overview charts are declared in ChartCatalogue,
                 * three of them owned by other pages. Titles, descriptions,
                 * axis labels, endpoints and drill-downs all come from the
                 * spec, so the Overview copy of a chart cannot drift from
                 * the page that owns it.
                 */
                List<JPanel> loaded =
                        new ArrayList<>();


                for (
                        ChartSpec spec :
                        ChartCatalogue.overviewCharts()
                ) {

                    loaded.add(
                            CatalogueRenderer.card(
                                    spec,
                                    currentFilter,
                                    OverviewPanel.this
                            )
                    );
                }


                return loaded;
            }


            @Override
            protected void done() {

                chartsGrid.removeAll();


                try {

                    List<JPanel> loaded =
                            get();


                    for (
                            JPanel chart :
                            loaded
                    ) {

                        /*
                         * Compact Overview charts.
                         *
                         * Full-size view is available
                         * by double-clicking.
                         *
                         * These must match the sizes set in
                         * RevenueChartPanel.configurePanel(), which
                         * shares this grid.
                         *
                         * Taller than before: catalogue cards carry a
                         * title and description above the plot, and at
                         * 260px the plot area was squeezed to nothing.
                         */
                        chart.setPreferredSize(
                                new Dimension(
                                        420,
                                        380
                                )
                        );

                        chart.setMinimumSize(
                                new Dimension(
                                        280,
                                        340
                                )
                        );

                        chartsGrid.add(chart);
                    }

                } catch (Exception ex) {

                    ex.printStackTrace();

                    chartsGrid.add(
                            AnalyticsCharts.messageCard(
                                    "Unable to load charts",
                                    ex.getMessage() == null
                                            ? "Dashboard chart request failed."
                                            : ex.getMessage()
                            )
                    );
                }


                refreshButton.setEnabled(true);

                refreshButton.setText(
                        "Refresh"
                );


                chartsGrid.revalidate();
                chartsGrid.repaint();
            }

        }.execute();
    }
}
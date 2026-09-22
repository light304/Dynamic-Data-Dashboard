package dashboard.gui;

import dashboard.database.ApiClient;
import dashboard.database.SchemaIntrospector;
import dashboard.database.SchemaIntrospector.ComparisonRow;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

import java.time.LocalDate;
import java.time.format.TextStyle;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RevenueOverTimeChart extends JPanel {

    private static final Color ACTIVE_COLOR   = Theme.SERIES_1;
    private static final Color PRIMARY_TEXT   = Theme.TEXT;
    private static final Color SECONDARY_TEXT = Theme.TEXT_MUTED;
    private static final Color BORDER_COLOR   = Theme.BORDER;

    private DefaultCategoryDataset dataset;

    private JLabel statusLabel;

    public RevenueOverTimeChart() {

        configurePanel();
        createChart();
    }

    private void configurePanel() {

        setLayout(
                new BorderLayout(
                        0,
                        8
                )
        );

        setBackground(
                Color.WHITE
        );

        setBorder(
                BorderFactory.createCompoundBorder(

                        BorderFactory.createLineBorder(
                                BORDER_COLOR
                        ),

                        new EmptyBorder(
                                15,
                                16,
                                12,
                                16
                        )
                )
        );
    }

    private void createChart() {

        JPanel titlePanel =
                new JPanel();

        titlePanel.setLayout(
                new BoxLayout(
                        titlePanel,
                        BoxLayout.Y_AXIS
                )
        );

        titlePanel.setBackground(
                Color.WHITE
        );

        JLabel title =
                new JLabel(
                        "Revenue Over Time"
                );

        title.setFont(
                Theme.CHART_TITLE
        );

        title.setForeground(
                PRIMARY_TEXT
        );

        title.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        JLabel description =
                new JLabel(
                        "Revenue grouped by time from the sales table"
                );

        description.setFont(
                Theme.SMALL
        );

        description.setForeground(
                SECONDARY_TEXT
        );

        description.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        titlePanel.add(
                title
        );

        titlePanel.add(
                Box.createVerticalStrut(
                        2
                )
        );

        titlePanel.add(
                description
        );

        add(
                titlePanel,
                BorderLayout.NORTH
        );

        dataset =
                new DefaultCategoryDataset();

        JFreeChart chart =
                ChartFactory.createLineChart(
                        null,
                        "Period",
                        "Revenue ($)",
                        dataset
                );

        chart.setBackgroundPaint(
                Color.WHITE
        );

        CategoryPlot plot =
                chart.getCategoryPlot();

        plot.setBackgroundPaint(
                Color.WHITE
        );

        plot.setOutlineVisible(
                false
        );

        plot.setRangeGridlinePaint(
                BORDER_COLOR
        );

        LineAndShapeRenderer renderer =
                new LineAndShapeRenderer(
                        true,
                        true
                );

        renderer.setSeriesPaint(
                0,
                ACTIVE_COLOR
        );

        renderer.setSeriesStroke(
                0,
                new BasicStroke(
                        2.0f
                )
        );

        plot.setRenderer(
                renderer
        );

        CategoryAxis axis =
                plot.getDomainAxis();

        axis.setMaximumCategoryLabelLines(
                1
        );

        ChartPanel chartPanel =
                new ChartPanel(
                        chart
                );

        chartPanel.setMouseWheelEnabled(
                false
        );

        chartPanel.setBackground(
                Color.WHITE
        );

        add(
                chartPanel,
                BorderLayout.CENTER
        );

        statusLabel =
                new JLabel(
                        "Waiting for sales data..."
                );

        statusLabel.setFont(
                Theme.SMALL
        );

        statusLabel.setForeground(
                SECONDARY_TEXT
        );

        add(
                statusLabel,
                BorderLayout.SOUTH
        );
    }

    /*
     * Applies:
     *
     * Year
     * Scope
     * Month (only used for Weekly)
     * Period
     */
    public void applyFilters(
            int selectedYear,
            String scope,
            String selectedMonth,
            String period
    ) {

        try {

            String json =
                    ApiClient.getData(
                            "api/query/compare",
                            Map.of(
                                    "table",
                                    "sales",

                                    "measureColumn",
                                    "revenue",

                                    "groupColumn",
                                    "order_date",

                                    "aggFn",
                                    "SUM"
                            )
                    );

            List<ComparisonRow> rows =
                    SchemaIntrospector
                            .parseCompareRows(
                                    json
                            );

            dataset.clear();

            Map<String, Double> totals =
                    new LinkedHashMap<>();

            for (ComparisonRow row : rows) {

                LocalDate date;

                try {

                    date =
                            LocalDate.parse(
                                    row.label
                            );

                } catch (Exception e) {

                    continue;
                }

                /*
                 * YEAR FILTER
                 */
                if (
                        date.getYear()
                                != selectedYear
                ) {
                    continue;
                }

                /*
                 * ==========================
                 * YEARLY
                 * ==========================
                 */
                if (
                        "Yearly".equals(scope)
                ) {

                    String label =
                            date.getMonth()
                                    .getDisplayName(
                                            TextStyle.SHORT,
                                            Locale.ENGLISH
                                    );

                    totals.put(
                            label,
                            totals.getOrDefault(
                                    label,
                                    0.0
                            ) + row.value
                    );
                }

                /*
                 * ==========================
                 * QUARTERLY
                 * ==========================
                 */
                else if (
                        "Quarterly".equals(scope)
                ) {

                    int selectedQuarter =
                            Integer.parseInt(
                                    period.substring(
                                            1
                                    )
                            );

                    int dateQuarter =
                            (
                                    (
                                            date.getMonthValue()
                                                    - 1
                                    )
                                            / 3
                            )
                                    + 1;

                    if (
                            selectedQuarter
                                    != dateQuarter
                    ) {
                        continue;
                    }

                    String label =
                            date.getMonth()
                                    .getDisplayName(
                                            TextStyle.SHORT,
                                            Locale.ENGLISH
                                    );

                    totals.put(
                            label,
                            totals.getOrDefault(
                                    label,
                                    0.0
                            ) + row.value
                    );
                }

                /*
                 * ==========================
                 * MONTHLY
                 * ==========================
                 */
                else if (
                        "Monthly".equals(scope)
                ) {

                    String currentMonth =
                            date.getMonth()
                                    .getDisplayName(
                                            TextStyle.FULL,
                                            Locale.ENGLISH
                                    );

                    if (
                            !currentMonth.equals(
                                    period
                            )
                    ) {
                        continue;
                    }

                    /*
                     * Show each day of selected month.
                     */
                    String label =
                            String.valueOf(
                                    date.getDayOfMonth()
                            );

                    totals.put(
                            label,
                            totals.getOrDefault(
                                    label,
                                    0.0
                            ) + row.value
                    );
                }

                /*
                 * ==========================
                 * WEEKLY
                 * ==========================
                 */
                else if (
                        "Weekly".equals(scope)
                ) {

                    if (selectedMonth == null) {
                        continue;
                    }

                    String currentMonth =
                            date.getMonth()
                                    .getDisplayName(
                                            TextStyle.FULL,
                                            Locale.ENGLISH
                                    );

                    /*
                     * Date must be inside the
                     * month selected by user.
                     */
                    if (
                            !currentMonth.equals(
                                    selectedMonth
                            )
                    ) {
                        continue;
                    }

                    int selectedWeek =
                            Integer.parseInt(
                                    period.replace(
                                            "Week ",
                                            ""
                                    )
                            );

                    int day =
                            date.getDayOfMonth();

                    /*
                     * Week within selected month.
                     *
                     * 1-7   = Week 1
                     * 8-14  = Week 2
                     * 15-21 = Week 3
                     * 22-28 = Week 4
                     * 29+   = Week 5
                     */
                    int dateWeek =
                            (
                                    (day - 1)
                                            / 7
                            )
                                    + 1;

                    if (
                            dateWeek
                                    != selectedWeek
                    ) {
                        continue;
                    }

                    /*
                     * Show actual weekday + date.
                     *
                     * Mon 8
                     * Tue 9
                     * Wed 10
                     */
                    String label =
                            date.getDayOfWeek()
                                    .getDisplayName(
                                            TextStyle.SHORT,
                                            Locale.ENGLISH
                                    )
                                    + " "
                                    + date.getDayOfMonth();

                    totals.put(
                            label,
                            totals.getOrDefault(
                                    label,
                                    0.0
                            ) + row.value
                    );
                }
            }

            /*
             * ADD REAL FILTERED DATA
             * TO JFREECHART
             */
            for (
                    Map.Entry<String, Double> entry
                    : totals.entrySet()
            ) {

                dataset.addValue(
                        entry.getValue(),
                        "Revenue",
                        entry.getKey()
                );
            }

            /*
             * STATUS MESSAGE
             */
            if (totals.isEmpty()) {

                statusLabel.setText(
                        "No sales data found for selected filter"
                );

            } else if (
                    "Weekly".equals(scope)
            ) {

                statusLabel.setText(
                        selectedYear
                                + " • "
                                + selectedMonth
                                + " • "
                                + period
                                + " • Live sales data"
                );

            } else {

                statusLabel.setText(
                        selectedYear
                                + " • "
                                + scope
                                + " • "
                                + period
                                + " • Live sales data"
                );
            }

        } catch (Exception e) {

            e.printStackTrace();

            dataset.clear();

            statusLabel.setText(
                    "Unable to load sales revenue"
            );
        }
    }
    /** Receives the dashboard-wide filter and reuses the existing chart filter logic. */
    public void applyFilter(DashboardFilter filter) {
        if (filter == null) filter = DashboardFilter.defaults();
        String selectedMonth = "Weekly".equals(filter.scope()) ? filter.month() : null;
        applyFilters(filter.year(), filter.scope(), selectedMonth, filter.period());
    }

}
package dashboard.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import dashboard.database.ApiClient;
import dashboard.database.SchemaIntrospector;
import dashboard.database.SchemaIntrospector.ComparisonRow;

/*
 * This class creates the Revenue Trend graph.
 *
 * IMPORTANT:
 * There is NO sample data in this class.
 *
 * Revenue values are loaded from the real backend using:
 *
 * /api/query/compare
 *
 * The backend returns SUM(sales.revenue) grouped by sales.order_date.
 *
 * The Java GUI then groups those real database rows into:
 * - Monthly
 * - Quarterly
 * - Yearly
 */
public class RevenueChartPanel extends JPanel {

    private static final Color ACTIVE_COLOR =
            new Color(0, 212, 255);

    private static final Color PRIMARY_TEXT =
            new Color(31, 41, 55);

    private static final Color SECONDARY_TEXT =
            new Color(100, 116, 139);

    private static final Color BORDER_COLOR =
            new Color(226, 232, 240);

    /*
     * JFreeChart dataset.
     */
    private DefaultCategoryDataset revenueDataset;

    /*
     * Status text underneath the graph.
     */
    private JLabel statusLabel;

    /*
     * Current values received from the global Overview filter.
     */
    private Integer selectedYear = 2023;
    private String selectedScope = "Monthly";
    private String selectedPeriod = "January";
    private String selectedRegion = "All Regions";

    /*
     * Creates the chart.
     */
    public RevenueChartPanel() {

        configurePanel();
        createChartLayout();
    }

    /*
     * Configures the outer chart card.
     */
    private void configurePanel() {

        setLayout(
                new BorderLayout(
                        0,
                        10
                )
        );

        setBackground(
                Color.WHITE
        );

        setPreferredSize(
                new Dimension(
                        600,
                        350
                )
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

    /*
     * Creates the chart and its title.
     *
     * The separate graph filters have been removed because the Overview
     * page now has one global filter controlling everything.
     */
    private void createChartLayout() {

        add(
                createHeader(),
                BorderLayout.NORTH
        );

        revenueDataset =
                new DefaultCategoryDataset();

        JFreeChart revenueChart =
                ChartFactory.createBarChart(
                        null,
                        "Period",
                        "Revenue ($)",
                        revenueDataset
                );

        styleChart(
                revenueChart
        );

        ChartPanel chartPanel =
                new ChartPanel(
                        revenueChart
                );

        chartPanel.setBackground(
                Color.WHITE
        );

        chartPanel.setBorder(
                null
        );

        chartPanel.setMouseWheelEnabled(
                false
        );

        add(
                chartPanel,
                BorderLayout.CENTER
        );

        statusLabel =
                new JLabel(
                        "Waiting for dashboard filter..."
                );

        statusLabel.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        12
                )
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
     * Creates the chart title.
     *
     * No chart-specific dropdowns are needed anymore.
     */
    private JPanel createHeader() {

        JPanel header =
                new JPanel();

        header.setLayout(
                new BoxLayout(
                        header,
                        BoxLayout.Y_AXIS
                )
        );

        header.setBackground(
                Color.WHITE
        );

        JLabel chartTitle =
                new JLabel(
                        "Revenue Trend"
                );

        chartTitle.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        17
                )
        );

        chartTitle.setForeground(
                PRIMARY_TEXT
        );

        chartTitle.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        JLabel description =
                new JLabel(
                        "Revenue from the live sales database"
                );

        description.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        12
                )
        );

        description.setForeground(
                SECONDARY_TEXT
        );

        description.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        header.add(
                chartTitle
        );

        header.add(
                Box.createVerticalStrut(
                        2
                )
        );

        header.add(
                description
        );

        return header;
    }

    /*
     * Called by OverviewPanel whenever Apply Filters is clicked.
     */
    public void applyFilters(
            Integer year,
            String scope,
            String period,
            String region
    ) {

        if (year == null
                || scope == null
                || period == null
                || region == null) {

            return;
        }

        selectedYear =
                year;

        selectedScope =
                scope;

        selectedPeriod =
                period;

        selectedRegion =
                region;

        /*
         * Now query the REAL database.
         */
        refreshChart();
    }

    /*
     * Reloads real revenue information from the backend.
     */
    public void refreshChart() {
        
        if (revenueDataset == null) return;
        try {
            statusLabel.setText("Loading revenue data...");

            /*
             * Ask the real backend to calculate:
             *
             * SUM(revenue)
             * GROUP BY order_date
             *
             * from the SALES table.
             */
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
            System.out.println("Revenue API response:");
            System.out.println(json.replace("{\"label\"", "\n  {\"label\""));

            /*
             * SchemaIntrospector already knows how to parse
             * /api/query/compare results.
             */
            List<ComparisonRow> rows =
                    SchemaIntrospector.parseCompareRows(
                            json
                    );

            /*
             * Build the JFreeChart using only real rows.
             */
            loadDatabaseRevenue(
                    rows
            );

        } catch (Exception e) {

            e.printStackTrace();

            revenueDataset.clear();

            statusLabel.setText(
                    "Unable to load revenue data."
            );
        }
    }

    /*
     * Converts real daily database revenue rows into the selected
     * dashboard time scope.
     */
    private void loadDatabaseRevenue(
            List<ComparisonRow> rows
    ) {

        revenueDataset.clear();

        /*
         * This keeps the chart buckets in the correct order.
         */
        Map<String, Double> totals =
                new LinkedHashMap<>();

        /*
         * Create the required buckets before reading the data.
         */
        createEmptyBuckets(
                totals
        );

        DateTimeFormatter databaseDateFormat =
                DateTimeFormatter.ofPattern(
                        "yyyy-MM-dd"
                );

        /*
         * Read every real revenue row returned by SQLite.
         */
        for (ComparisonRow row : rows) {

            try {

                LocalDate orderDate =
                        LocalDate.parse(
                                row.label,
                                databaseDateFormat
                        );

                /*
                 * Only use rows from the selected year.
                 */
                if (orderDate.getYear()
                        != selectedYear) {

                    continue;
                }

                /*
                 * Work out which chart bucket this database row belongs to.
                 */
                String bucket =
                        getBucketForDate(
                                orderDate
                        );

                if (bucket == null) {
                    continue;
                }

                totals.put(
                        bucket,
                        totals.getOrDefault(
                                bucket,
                                0.0
                        )
                                + row.value
                );

            } catch (Exception dateException) {

                /*
                 * Ignore rows whose date is invalid.
                 */
                System.err.println(
                        "Unable to parse order date: "
                                + row.label
                );
            }
        }

        /*
         * Put the real aggregated values into JFreeChart.
         */
        for (
                Map.Entry<String, Double> entry
                : totals.entrySet()
        ) {

            revenueDataset.addValue(
                    entry.getValue(),
                    "Revenue",
                    entry.getKey()
            );
        }

        updateStatus(
                totals
        );
    }

    /*
     * Creates empty chart buckets depending on the selected scope.
     */
    private void createEmptyBuckets(
            Map<String, Double> totals
    ) {

        switch (selectedScope) {

            case "Monthly" -> {

                /*
                 * The global Period filter selects a specific month.
                 *
                 * Show the weeks inside that month.
                 */
                totals.put(
                        "Week 1",
                        0.0
                );

                totals.put(
                        "Week 2",
                        0.0
                );

                totals.put(
                        "Week 3",
                        0.0
                );

                totals.put(
                        "Week 4",
                        0.0
                );

                totals.put(
                        "Week 5",
                        0.0
                );
            }

            case "Quarterly" -> {

                /*
                 * The selected quarter is split into its three months.
                 */
                int quarter =
                        quarterNumber(
                                selectedPeriod
                        );

                int firstMonth =
                        ((quarter - 1) * 3) + 1;

                for (
                        int month = firstMonth;
                        month < firstMonth + 3;
                        month++
                ) {

                    totals.put(
                            monthName(
                                    month
                            ),
                            0.0
                    );
                }
            }

            case "Yearly" -> {

                /*
                 * Full year shows every month.
                 */
                for (
                        int month = 1;
                        month <= 12;
                        month++
                ) {

                    totals.put(
                            monthName(
                                    month
                            ),
                            0.0
                    );
                }
            }

            default -> throw new IllegalArgumentException(
                    "Unknown scope: "
                            + selectedScope
            );
        }
    }

    /*
     * Works out which bucket a database date belongs in.
     */
    private String getBucketForDate(
            LocalDate date
    ) {

        switch (selectedScope) {

            case "Monthly" -> {

                int selectedMonth =
                        monthNumber(
                                selectedPeriod
                        );

                if (date.getMonthValue()
                        != selectedMonth) {

                    return null;
                }

                int day =
                        date.getDayOfMonth();

                int week =
                        ((day - 1) / 7) + 1;

                /*
                 * Prevent anything beyond Week 5.
                 */
                week =
                        Math.min(
                                week,
                                5
                        );

                return "Week "
                        + week;
            }

            case "Quarterly" -> {

                int selectedQuarter =
                        quarterNumber(
                                selectedPeriod
                        );

                int dateQuarter =
                        ((date.getMonthValue() - 1)
                                / 3)
                                + 1;

                if (dateQuarter
                        != selectedQuarter) {

                    return null;
                }

                return monthName(
                        date.getMonthValue()
                );
            }

            case "Yearly" -> {

                return monthName(
                        date.getMonthValue()
                );
            }

            default -> {

                return null;
            }
        }
    }

    /*
     * Converts January -> 1, February -> 2, etc.
     */
    private int monthNumber(
            String month
    ) {

        return switch (month) {

            case "January" -> 1;
            case "February" -> 2;
            case "March" -> 3;
            case "April" -> 4;
            case "May" -> 5;
            case "June" -> 6;
            case "July" -> 7;
            case "August" -> 8;
            case "September" -> 9;
            case "October" -> 10;
            case "November" -> 11;
            case "December" -> 12;

            default -> 1;
        };
    }

    /*
     * Converts month number into short month label.
     */
    private String monthName(
            int month
    ) {

        return YearMonth.of(
                        selectedYear,
                        month
                )
                .getMonth()
                .toString()
                .substring(
                        0,
                        3
                );
    }

    /*
     * Converts Q1/Q2/Q3/Q4 into an integer.
     */
    private int quarterNumber(
            String quarter
    ) {

        return switch (quarter) {

            case "Q1" -> 1;
            case "Q2" -> 2;
            case "Q3" -> 3;
            case "Q4" -> 4;

            default -> 1;
        };
    }

    /*
     * Updates the message under the chart.
     */
    private void updateStatus(
            Map<String, Double> totals
    ) {

        double total =
                0.0;

        for (
                double value
                : totals.values()
        ) {

            total += value;
        }

        String filterDescription;

        switch (selectedScope) {

            case "Monthly" ->
                    filterDescription =
                            selectedPeriod
                                    + " "
                                    + selectedYear;

            case "Quarterly" ->
                    filterDescription =
                            selectedPeriod
                                    + " "
                                    + selectedYear;

            case "Yearly" ->
                    filterDescription =
                            "Full Year "
                                    + selectedYear;

            default ->
                    filterDescription =
                            String.valueOf(
                                    selectedYear
                            );
        }

        /*
         * Region is currently shown in the filter description,
         * but the existing compare API does not support combining
         * order_date + region in one request yet.
         */
        if (!"All Regions".equals(
                selectedRegion
        )) {

            statusLabel.setText(
                    filterDescription
                            + " | Region filtering requires backend support"
            );

        } else {

            statusLabel.setText(
                    String.format(
                            "%s | Total Revenue: $%,.2f",
                            filterDescription,
                            total
                    )
            );
        }
    }

    /*
     * Styling for the JFreeChart.
     */
    private void styleChart(
            JFreeChart chart
    ) {

        chart.setBackgroundPaint(
                Color.WHITE
        );

        chart.setBorderVisible(
                false
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

        plot.setDomainGridlinesVisible(
                false
        );

        BarRenderer renderer =
                (BarRenderer)
                        plot.getRenderer();

        renderer.setSeriesPaint(
                0,
                ACTIVE_COLOR
        );

        renderer.setMaximumBarWidth(
                0.09
        );

        renderer.setShadowVisible(
                false
        );
    }
}
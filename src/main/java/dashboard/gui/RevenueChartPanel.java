//The drill-down feature allows users to click directly on a chart element, such as a revenue bar, region, or product category, 
// and view the detailed records behind that summary. Instead of only seeing aggregated values, 
// users can inspect the individual sales transactions that contributed to the chart. 
// The existing dashboard filters, such as year, period, 
// and region, are carried into the drill-down so the displayed data remains relevant to the current view. 
// Double-clicking is kept separate and is used to expand the chart, while a single click opens the detailed data table.

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
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.entity.CategoryItemEntity;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import dashboard.database.ApiClient;
import dashboard.database.SchemaIntrospector;
import dashboard.database.SchemaIntrospector.ComparisonRow;

public class RevenueChartPanel extends JPanel {

    private static final Color ACTIVE_COLOR =
            new Color(0, 212, 255);

    private static final Color PRIMARY_TEXT =
            new Color(31, 41, 55);

    private static final Color SECONDARY_TEXT =
            new Color(100, 116, 139);

    private static final Color BORDER_COLOR =
            new Color(226, 232, 240);

    private DefaultCategoryDataset revenueDataset;

    private JLabel statusLabel;

    private Integer selectedYear = 2023;

    private String selectedScope =
            "Monthly";

    private String selectedPeriod =
            "January";

    private String selectedRegion =
            "All Regions";

    private JFreeChart revenueChart;

    public RevenueChartPanel() {

        configurePanel();

        createChartLayout();
    }

    private void configurePanel() {

        setLayout(
                new BorderLayout(
                        0,
                        6
                )
        );

        setBackground(Color.WHITE);

        setPreferredSize(
                new Dimension(
                        420,
                        220
                )
        );

        setMinimumSize(
                new Dimension(
                        280,
                        200
                )
        );

        setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                BORDER_COLOR
                        ),
                        new EmptyBorder(
                                10,
                                12,
                                8,
                                12
                        )
                )
        );
    }

    private void createChartLayout() {

        add(
                createHeader(),
                BorderLayout.NORTH
        );

        revenueDataset =
                new DefaultCategoryDataset();

        revenueChart =
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
                createChartPanel(
                        revenueChart
                );

        chartPanel.addChartMouseListener(
                new ChartMouseListener() {

                    @Override
                    public void chartMouseMoved(
                            ChartMouseEvent event
                    ) {
                    }

                    @Override
                    public void chartMouseClicked(
                            ChartMouseEvent event
                    ) {

                        /*
                         * DOUBLE CLICK
                         * Open expanded chart.
                         */
                        if (
                                event.getTrigger() != null
                                        && event
                                        .getTrigger()
                                        .getClickCount() >= 2
                        ) {

                            showExpandedChart();

                            return;
                        }

                        /*
                         * SINGLE CLICK
                         * Drill down into selected bar.
                         */
                        if (
                                event.getEntity()
                                        instanceof CategoryItemEntity entity
                        ) {

                            String clickedPeriod =
                                    entity
                                            .getColumnKey()
                                            .toString();

                            openDrillDown(
                                    clickedPeriod
                            );
                        }
                    }
                }
        );

        add(
                chartPanel,
                BorderLayout.CENTER
        );

        JPanel bottom =
                new JPanel(
                        new BorderLayout()
                );

        bottom.setOpaque(false);

        statusLabel =
                new JLabel(
                        "Waiting for dashboard filter..."
                );

        statusLabel.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        10
                )
        );

        statusLabel.setForeground(
                SECONDARY_TEXT
        );

        JLabel hint =
                new JLabel(
                        "Click bar for details • Double-click to expand",
                        SwingConstants.RIGHT
                );

        hint.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        9
                )
        );

        hint.setForeground(
                SECONDARY_TEXT
        );

        bottom.add(
                statusLabel,
                BorderLayout.WEST
        );

        bottom.add(
                hint,
                BorderLayout.EAST
        );

        add(
                bottom,
                BorderLayout.SOUTH
        );
    }

    // =========================================================
    // DRILL DOWN
    // =========================================================
    private void openDrillDown(
        String clickedPeriod
) {

    String month = null;
    String week = null;


    /*
     * YEARLY
     *
     * Chart bars are:
     * JAN, FEB, MAR, etc.
     *
     * Clicking a month opens that month's sales.
     */
    if (
            "Yearly".equals(
                    selectedScope
            )
    ) {

        int monthNumber =
                monthNumberFromShortName(
                        clickedPeriod
                );

        month =
                String.format(
                        "%04d-%02d",
                        selectedYear,
                        monthNumber
                );
    }


    /*
     * QUARTERLY
     *
     * Chart bars are the three months
     * inside the selected quarter.
     *
     * Example:
     * Q1 -> JAN, FEB, MAR
     */
    else if (
            "Quarterly".equals(
                    selectedScope
            )
    ) {

        int monthNumber =
                monthNumberFromShortName(
                        clickedPeriod
                );

        month =
                String.format(
                        "%04d-%02d",
                        selectedYear,
                        monthNumber
                );
    }


    /*
     * MONTHLY
     *
     * Chart bars are:
     * Week 1
     * Week 2
     * Week 3
     * Week 4
     * Week 5
     *
     * We send BOTH:
     *
     * month = 2023-01
     * week  = Week 2
     *
     * so the backend returns only the
     * transactions from that exact week.
     */
    else if (
            "Monthly".equals(
                    selectedScope
            )
    ) {

        int monthNumber =
                monthNumber(
                        selectedPeriod
                );

        month =
                String.format(
                        "%04d-%02d",
                        selectedYear,
                        monthNumber
                );

        week =
                clickedPeriod;
    }


    /*
     * Open the sales transaction table.
     *
     * New signature:
     *
     * parent
     * month
     * week
     * category
     * region
     */
    DrilldownDialog.showSales(
            this,
            month,
            week,
            null,
            selectedRegion
    );
}

    private int monthNumberFromShortName(
            String month
    ) {

        return switch (
                month.toUpperCase()
        ) {

            case "JAN" -> 1;
            case "FEB" -> 2;
            case "MAR" -> 3;
            case "APR" -> 4;
            case "MAY" -> 5;
            case "JUN" -> 6;
            case "JUL" -> 7;
            case "AUG" -> 8;
            case "SEP" -> 9;
            case "OCT" -> 10;
            case "NOV" -> 11;
            case "DEC" -> 12;

            default -> 1;
        };
    }

    private ChartPanel createChartPanel(
            JFreeChart chart
    ) {

        ChartPanel panel =
                new ChartPanel(chart);

        panel.setBackground(
                Color.WHITE
        );

        panel.setBorder(null);

        panel.setMouseWheelEnabled(
                false
        );

        panel.setMinimumDrawWidth(0);
        panel.setMinimumDrawHeight(0);

        panel.setMaximumDrawWidth(
                Integer.MAX_VALUE
        );

        panel.setMaximumDrawHeight(
                Integer.MAX_VALUE
        );

        return panel;
    }

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
                        15
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
                        10
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
                Box.createVerticalStrut(1)
        );

        header.add(
                description
        );

        return header;
    }

    // =========================================================
    // EXPANDED CHART
    // =========================================================

    private void showExpandedChart() {

        if (revenueChart == null) {
            return;
        }

        JDialog dialog =
                new JDialog();

        dialog.setTitle(
                "Revenue Trend"
        );

        dialog.setModal(false);

        dialog.setDefaultCloseOperation(
                WindowConstants.DISPOSE_ON_CLOSE
        );

        dialog.setLayout(
                new BorderLayout()
        );

        JPanel header =
                new JPanel(
                        new BorderLayout()
                );

        header.setBackground(
                Color.WHITE
        );

        header.setBorder(
                new EmptyBorder(
                        14,
                        20,
                        14,
                        20
                )
        );

        JLabel title =
                new JLabel(
                        "Revenue Trend"
                );

        title.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        20
                )
        );

        title.setForeground(
                PRIMARY_TEXT
        );

        JButton close =
                new JButton(
                        "Close"
                );

        close.setForeground(
                Color.WHITE
        );

        close.setBackground(
                ACTIVE_COLOR
        );

        close.setFocusPainted(
                false
        );

        close.setBorderPainted(
                false
        );

        close.addActionListener(
                e -> dialog.dispose()
        );

        header.add(
                title,
                BorderLayout.WEST
        );

        header.add(
                close,
                BorderLayout.EAST
        );

        ChartPanel expanded =
                createChartPanel(
                        revenueChart
                );

        expanded.setMouseWheelEnabled(
                true
        );

        JPanel wrapper =
                new JPanel(
                        new BorderLayout()
                );

        wrapper.setBackground(
                new Color(
                        245,
                        247,
                        250
                )
        );

        wrapper.setBorder(
                new EmptyBorder(
                        18,
                        18,
                        18,
                        18
                )
        );

        wrapper.add(
                expanded,
                BorderLayout.CENTER
        );

        dialog.add(
                header,
                BorderLayout.NORTH
        );

        dialog.add(
                wrapper,
                BorderLayout.CENTER
        );

        Dimension screen =
                Toolkit
                        .getDefaultToolkit()
                        .getScreenSize();

        dialog.setSize(
                Math.min(
                        1250,
                        screen.width - 80
                ),
                Math.min(
                        850,
                        screen.height - 80
                )
        );

        dialog.setLocationRelativeTo(
                this
        );

        dialog.setVisible(true);
    }

    // =========================================================
    // FILTERS
    // =========================================================

    public void applyFilters(
            Integer year,
            String scope,
            String period,
            String region
    ) {

        if (
                year == null
                        || scope == null
                        || period == null
                        || region == null
        ) {

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

        refreshChart();
    }

    // =========================================================
    // DATA
    // =========================================================

    public void refreshChart() {
        
        if (revenueDataset == null) return;
        try {
            statusLabel.setText("Loading revenue data...");

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

    private void loadDatabaseRevenue(
            List<ComparisonRow> rows
    ) {

        revenueDataset.clear();

        Map<String, Double> totals =
                new LinkedHashMap<>();

        createEmptyBuckets(
                totals
        );

        DateTimeFormatter format =
                DateTimeFormatter.ofPattern(
                        "yyyy-MM-dd"
                );

        for (
                ComparisonRow row
                : rows
        ) {

            try {

                LocalDate orderDate =
                        LocalDate.parse(
                                row.label,
                                format
                        );

                if (
                        orderDate.getYear()
                                != selectedYear
                ) {

                    continue;
                }

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

            } catch (Exception ex) {

                System.err.println(
                        "Unable to parse order date: "
                                + row.label
                );
            }
        }

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

    private void createEmptyBuckets(
            Map<String, Double> totals
    ) {

        switch (selectedScope) {

            case "Monthly" -> {

                totals.put("Week 1", 0.0);
                totals.put("Week 2", 0.0);
                totals.put("Week 3", 0.0);
                totals.put("Week 4", 0.0);
                totals.put("Week 5", 0.0);
            }

            case "Quarterly" -> {

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
                            monthName(month),
                            0.0
                    );
                }
            }

            case "Yearly" -> {

                for (
                        int month = 1;
                        month <= 12;
                        month++
                ) {

                    totals.put(
                            monthName(month),
                            0.0
                    );
                }
            }

            default ->
                    throw new IllegalArgumentException(
                            "Unknown scope: "
                                    + selectedScope
                    );
        }
    }

    private String getBucketForDate(
            LocalDate date
    ) {

        switch (selectedScope) {

            case "Monthly" -> {

                int selectedMonthNumber =
                        monthNumber(
                                selectedPeriod
                        );

                if (
                        date.getMonthValue()
                                != selectedMonthNumber
                ) {

                    return null;
                }

                int week =
                        ((date.getDayOfMonth() - 1)
                                / 7)
                                + 1;

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

                if (
                        dateQuarter
                                != selectedQuarter
                ) {

                    return null;
                }

                return monthName(
                        date.getMonthValue()
                );
            }

            case "Yearly" ->
                    {

                        return monthName(
                                date.getMonthValue()
                        );
                    }

            default ->
                    {

                        return null;
                    }
        }
    }

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

    private void updateStatus(
            Map<String, Double> totals
    ) {

        double total = 0.0;

        for (
                double value
                : totals.values()
        ) {

            total += value;
        }

        String description;

        switch (selectedScope) {

            case "Monthly" ->
                    description =
                            selectedPeriod
                                    + " "
                                    + selectedYear;

            case "Quarterly" ->
                    description =
                            selectedPeriod
                                    + " "
                                    + selectedYear;

            case "Yearly" ->
                    description =
                            "Full Year "
                                    + selectedYear;

            default ->
                    description =
                            String.valueOf(
                                    selectedYear
                            );
        }

        statusLabel.setText(
                String.format(
                        "%s | $%,.0f",
                        description,
                        total
                )
        );
    }

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

        plot.getDomainAxis()
                .setTickLabelFont(
                        new Font(
                                "SansSerif",
                                Font.PLAIN,
                                9
                        )
                );

        plot.getRangeAxis()
                .setTickLabelFont(
                        new Font(
                                "SansSerif",
                                Font.PLAIN,
                                9
                        )
                );

        BarRenderer renderer =
                (BarRenderer)
                        plot.getRenderer();

        renderer.setSeriesPaint(
                0,
                ACTIVE_COLOR
        );

        renderer.setMaximumBarWidth(
                0.08
        );

        renderer.setShadowVisible(
                false
        );
    }
}
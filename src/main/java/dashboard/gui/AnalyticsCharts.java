package dashboard.gui;

import dashboard.database.AnalyticsApi.Point;
import dashboard.database.AnalyticsApi.SeriesPoint;
import dashboard.database.AnalyticsApi.XYPoint;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.entity.CategoryItemEntity;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.chart.entity.PieSectionEntity;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class AnalyticsCharts {

    private static final Color BORDER = new Color(226, 232, 240);
    private static final Color GRID = new Color(226, 232, 240);
    private static final Color TEXT = new Color(31, 41, 55);
    private static final Color SECONDARY_TEXT = new Color(100, 116, 139);
    private static final Color ACCENT = new Color(0, 188, 225);

    private AnalyticsCharts() {
    }

    // =========================================================
    // LINE CHART
    // =========================================================

    public static JPanel line(
            String title,
            String xLabel,
            String yLabel,
            List<Point> values,
            String series,
            Consumer<String> categoryClick
    ) {

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for (Point p : values) {
            dataset.addValue(
                    p.value(),
                    series,
                    p.label()
            );
        }

        JFreeChart chart = ChartFactory.createLineChart(
                title,
                xLabel,
                yLabel,
                dataset,
                PlotOrientation.VERTICAL,
                false,
                true,
                false
        );

        styleCategory(chart, true);

        return wrap(chart, categoryClick);
    }

    // =========================================================
    // BAR CHART
    // =========================================================

    public static JPanel bar(
            String title,
            String xLabel,
            String yLabel,
            List<Point> values,
            String series,
            boolean horizontal,
            Consumer<String> categoryClick
    ) {

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for (Point p : values) {
            dataset.addValue(
                    p.value(),
                    series,
                    p.label()
            );
        }

        JFreeChart chart = ChartFactory.createBarChart(
                title,
                xLabel,
                yLabel,
                dataset,
                horizontal
                        ? PlotOrientation.HORIZONTAL
                        : PlotOrientation.VERTICAL,
                false,
                true,
                false
        );

        styleCategory(chart, false);

        return wrap(chart, categoryClick);
    }

    // =========================================================
    // GROUPED BAR CHART
    // =========================================================

    public static JPanel groupedBar(
            String title,
            String xLabel,
            String yLabel,
            List<SeriesPoint> values,
            Consumer<String> categoryClick
    ) {

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for (SeriesPoint p : values) {
            dataset.addValue(
                    p.value(),
                    p.series(),
                    p.label()
            );
        }

        JFreeChart chart = ChartFactory.createBarChart(
                title,
                xLabel,
                yLabel,
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        styleCategory(chart, false);
        styleLegend(chart);

        return wrap(chart, categoryClick);
    }

    // =========================================================
    // MULTI LINE CHART
    // =========================================================

    public static JPanel multiLine(
            String title,
            String xLabel,
            String yLabel,
            List<SeriesPoint> values,
            Consumer<String> categoryClick
    ) {

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for (SeriesPoint p : values) {
            dataset.addValue(
                    p.value(),
                    p.series(),
                    p.label()
            );
        }

        JFreeChart chart = ChartFactory.createLineChart(
                title,
                xLabel,
                yLabel,
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        styleCategory(chart, true);
        styleLegend(chart);

        return wrap(chart, categoryClick);
    }

    // =========================================================
    // PIE CHART
    // =========================================================

    public static JPanel pie(
        String title,
        List<Point> values
) {

    return pie(
            title,
            values,
            null
    );
}


public static JPanel pie(
        String title,
        List<Point> values,
        Consumer<String> sectionClick
) {

    DefaultPieDataset<String> dataset =
            new DefaultPieDataset<>();

    for (Point p : values) {

        dataset.setValue(
                p.label(),
                p.value()
        );
    }


    JFreeChart chart =
            ChartFactory.createPieChart(
                    title,
                    dataset,
                    true,
                    true,
                    false
            );


    styleChartTitle(chart);

    chart.setBackgroundPaint(
            Color.WHITE
    );


    PiePlot<?> plot =
            (PiePlot<?>) chart.getPlot();

    plot.setBackgroundPaint(
            Color.WHITE
    );

    plot.setOutlineVisible(
            false
    );

    plot.setLabelFont(
            new Font(
                    "SansSerif",
                    Font.PLAIN,
                    11
            )
    );

    plot.setLabelPaint(TEXT);

    plot.setLabelBackgroundPaint(
            new Color(
                    248,
                    250,
                    252
            )
    );

    plot.setLabelOutlinePaint(
            BORDER
    );

    plot.setLabelShadowPaint(
            null
    );

    styleLegend(chart);


    JPanel card =
            baseCard();

    card.setLayout(
            new BorderLayout()
    );


    ChartPanel chartPanel =
            createChartPanel(chart);


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
                     * Double-click = expand.
                     */
                    if (
                            event.getTrigger() != null
                            && event.getTrigger()
                                    .getClickCount() >= 2
                    ) {

                        showExpandedChart(chart);

                        return;
                    }


                    /*
                     * Single-click = pie drill-down.
                     */
                    if (
                            sectionClick != null
                            && event.getEntity()
                                    instanceof PieSectionEntity entity
                    ) {

                        sectionClick.accept(
                                entity.getSectionKey()
                                        .toString()
                        );
                    }
                }
            }
    );


    JLabel hint =
            new JLabel(
                    sectionClick == null
                            ? "Double-click to expand"
                            : "Click section for details • Double-click to expand",
                    SwingConstants.RIGHT
            );

    hint.setFont(
            new Font(
                    "SansSerif",
                    Font.PLAIN,
                    10
            )
    );

    hint.setForeground(
            SECONDARY_TEXT
    );

    hint.setBorder(
            new EmptyBorder(
                    4,
                    0,
                    0,
                    2
            )
    );


    card.add(
            chartPanel,
            BorderLayout.CENTER
    );

    card.add(
            hint,
            BorderLayout.SOUTH
    );


    return card;
}

    // =========================================================
    // SCATTER CHART
    // =========================================================

    public static JPanel scatter(
            String title,
            String xLabel,
            String yLabel,
            List<XYPoint> values
    ) {

        Map<String, XYSeries> byCategory = new LinkedHashMap<>();

        Map<String, List<String>> productIdsByCategory = new LinkedHashMap<>();

        for (XYPoint p : values) {

            byCategory
                    .computeIfAbsent(
                            p.category(),
                            key -> new XYSeries(key, false)
                    )
                    .add(
                            p.x(),
                            p.y()
                    );

            productIdsByCategory
                    .computeIfAbsent(
                            p.category(),
                            key -> new ArrayList<>()
                    )
                    .add(p.label());
        }

        XYSeriesCollection dataset = new XYSeriesCollection();

        for (XYSeries series : byCategory.values()) {
            dataset.addSeries(series);
        }

        List<List<String>> productIdsBySeriesIndex =
                new ArrayList<>(productIdsByCategory.values());

        JFreeChart chart = ChartFactory.createScatterPlot(
                title,
                xLabel,
                yLabel,
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        styleChartTitle(chart);

        chart.setBackgroundPaint(Color.WHITE);

        XYPlot plot = chart.getXYPlot();

        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(GRID);
        plot.setRangeGridlinePaint(GRID);
        plot.setOutlineVisible(false);

        // Shows category, Product ID, and both plotted values on hover -
        // replaces the default "Category: (x, y)" tooltip.
        XYToolTipGenerator tooltipGenerator =
                (XYDataset ds, int series, int item) -> {

                    String category =
                            String.valueOf(ds.getSeriesKey(series));

                    String productId =
                            (series < productIdsBySeriesIndex.size()
                                    && item < productIdsBySeriesIndex.get(series).size())
                                    ? productIdsBySeriesIndex.get(series).get(item)
                                    : "?";

                    double x = ds.getXValue(series, item);
                    double y = ds.getYValue(series, item);

                    return "<html>"
                            + category + " - Product Id: " + productId + "<br>"
                            + axisLabel(xLabel) + ": " + axisValue(xLabel, x) + "<br>"
                            + axisLabel(yLabel) + ": " + axisValue(yLabel, y)
                            + "</html>";
                };

        if (plot.getRenderer() != null) {
            plot.getRenderer().setDefaultToolTipGenerator(tooltipGenerator);
        }

        if (plot.getDomainAxis() != null) {

            plot.getDomainAxis().setLabelFont(
                    new Font(
                            "SansSerif",
                            Font.BOLD,
                            11
                    )
            );

            plot.getDomainAxis().setTickLabelFont(
                    new Font(
                            "SansSerif",
                            Font.PLAIN,
                            10
                    )
            );
        }

        if (plot.getRangeAxis() != null) {

            plot.getRangeAxis().setLabelFont(
                    new Font(
                            "SansSerif",
                            Font.BOLD,
                            11
                    )
            );

            plot.getRangeAxis().setTickLabelFont(
                    new Font(
                            "SansSerif",
                            Font.PLAIN,
                            10
                    )
            );
        }

        styleLegend(chart);

        return wrap(chart, null);
    }

    // Axis label with any trailing unit annotation (e.g. " ($)") stripped, for tooltip text.
    private static String axisLabel(String label) {
        int unitStart = label.indexOf(" (");
        return unitStart >= 0 ? label.substring(0, unitStart) : label;
    }

    // Formats a tooltip value as currency if its axis label carries a "$" unit, plain otherwise.
    private static String axisValue(String label, double value) {
        if (label.contains("$")) {
            return String.format("$%,.2f", value);
        }
        return value == Math.rint(value)
                ? String.format("%,.0f", value)
                : String.format("%,.2f", value);
    }

    // =========================================================
    // MESSAGE CARD
    // =========================================================

    public static JPanel messageCard(
            String title,
            String message
    ) {

        JPanel panel = baseCard();

        panel.setLayout(
                new BoxLayout(
                        panel,
                        BoxLayout.Y_AXIS
                )
        );

        JLabel heading = new JLabel(title);

        heading.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        17
                )
        );

        heading.setForeground(TEXT);

        JLabel body = new JLabel(
                "<html>"
                        + message
                        + "</html>"
        );

        body.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        13
                )
        );

        body.setForeground(SECONDARY_TEXT);

        panel.add(heading);

        panel.add(
                Box.createVerticalStrut(10)
        );

        panel.add(body);

        return panel;
    }

    // =========================================================
    // WRAP CHART
    // =========================================================

    private static JPanel wrap(
            JFreeChart chart,
            Consumer<String> categoryClick
    ) {

        JPanel card = baseCard();

        card.setLayout(
                new BorderLayout()
        );

        ChartPanel chartPanel =
                createChartPanel(chart);

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
                         * Expands chart into large window.
                         */
                        if (
                                event.getTrigger() != null
                                        && event.getTrigger()
                                        .getClickCount() >= 2
                        ) {

                            showExpandedChart(chart);

                            return;
                        }

                        /*
                         * SINGLE CLICK
                         * Keeps existing drill-down behaviour.
                         */
                        if (
                                categoryClick != null
                                        && event.getEntity()
                                        instanceof CategoryItemEntity entity
                        ) {

                            categoryClick.accept(
                                    entity
                                            .getColumnKey()
                                            .toString()
                            );
                        }
                    }
                }
        );

        JLabel expandHint =
                new JLabel(
                        "Double-click to expand",
                        SwingConstants.RIGHT
                );

        expandHint.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        10
                )
        );

        expandHint.setForeground(
                SECONDARY_TEXT
        );

        expandHint.setBorder(
                new EmptyBorder(
                        4,
                        0,
                        0,
                        2
                )
        );

        card.add(
                chartPanel,
                BorderLayout.CENTER
        );

        card.add(
                expandHint,
                BorderLayout.SOUTH
        );

        return card;
    }

    // =========================================================
    // CREATE CHART PANEL
    // =========================================================
    private static ChartPanel createChartPanel(
        JFreeChart chart
) {

    ChartPanel chartPanel =
            new ChartPanel(chart);

    chartPanel.setBorder(null);

    chartPanel.setBackground(
            Color.WHITE
    );

    // Allow charts to resize smoothly with the dashboard.
    chartPanel.setMinimumDrawWidth(0);
    chartPanel.setMinimumDrawHeight(0);

    chartPanel.setMaximumDrawWidth(
            Integer.MAX_VALUE
    );

    chartPanel.setMaximumDrawHeight(
            Integer.MAX_VALUE
    );

    /*
     * Disable JFreeChart's default drag-to-zoom behaviour.
     *
     * Users can still single-click charts for drill-downs
     * and double-click them to open the expanded view.
     */
    chartPanel.setDomainZoomable(false);
    chartPanel.setRangeZoomable(false);

    // Mouse wheel zoom is disabled on normal dashboard cards.
    chartPanel.setMouseWheelEnabled(false);

    return chartPanel;
}

    // =========================================================
    // EXPANDED CHART WINDOW
    // =========================================================

    private static void showExpandedChart(
            JFreeChart chart
    ) {

        String title =
                chart.getTitle() != null
                        ? chart.getTitle().getText()
                        : "Chart";

        showExpandedChart(chart, title);
    }

    static void showExpandedChart(
            JFreeChart chart,
            String title
    ) {
        JFrame dialog = new JFrame(title);

        dialog.setDefaultCloseOperation(
                WindowConstants.DISPOSE_ON_CLOSE
        );

        dialog.setLayout(
                new BorderLayout()
        );

        dialog.getContentPane().setBackground(
                new Color(
                        245,
                        247,
                        250
                )
        );

        // =====================================================
        // HEADER
        // =====================================================

        JPanel header =
                new JPanel(
                        new BorderLayout()
                );

        header.setBackground(
                Color.WHITE
        );

        header.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(
                                0,
                                0,
                                1,
                                0,
                                BORDER
                        ),
                        new EmptyBorder(
                                14,
                                20,
                                14,
                                20
                        )
                )
        );

        JPanel titlePanel = new JPanel();

        titlePanel.setOpaque(false);

        titlePanel.setLayout(
                new BoxLayout(
                        titlePanel,
                        BoxLayout.Y_AXIS
                )
        );

        JLabel heading =
                new JLabel(title);

        heading.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        20
                )
        );

        heading.setForeground(TEXT);

        JLabel subtitle =
                new JLabel(
                        "Expanded chart view"
                );

        subtitle.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        12
                )
        );

        subtitle.setForeground(
                SECONDARY_TEXT
        );

        titlePanel.add(heading);

        titlePanel.add(
                Box.createVerticalStrut(3)
        );

        titlePanel.add(subtitle);

        // =====================================================
        // CLOSE BUTTON
        // =====================================================

        JButton closeButton =
                new JButton("Close");

        closeButton.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        12
                )
        );

        closeButton.setForeground(
                Color.WHITE
        );

        closeButton.setBackground(
                ACCENT
        );

        closeButton.setOpaque(true);

        closeButton.setContentAreaFilled(
                true
        );

        closeButton.setBorderPainted(false);

        closeButton.setFocusPainted(false);

        closeButton.setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR
                )
        );

        closeButton.setPreferredSize(
                new Dimension(
                        90,
                        36
                )
        );

        closeButton.addActionListener(
                e -> dialog.dispose()
        );

        JPanel buttonPanel =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.RIGHT,
                                0,
                                0
                        )
                );

        buttonPanel.setOpaque(false);

        buttonPanel.add(closeButton);

        header.add(
                titlePanel,
                BorderLayout.WEST
        );

        header.add(
                buttonPanel,
                BorderLayout.EAST
        );

        // =====================================================
        // LARGE CHART
        // =====================================================

        ChartPanel expandedChart =
                createChartPanel(chart);

        expandedChart.setMouseWheelEnabled(
                true
        );

        expandedChart.setPreferredSize(
                new Dimension(
                        1100,
                        680
                )
        );

        JPanel chartWrapper =
                new JPanel(
                        new BorderLayout()
                );

        chartWrapper.setBackground(
                new Color(
                        245,
                        247,
                        250
                )
        );

        chartWrapper.setBorder(
                new EmptyBorder(
                        18,
                        18,
                        18,
                        18
                )
        );

        JPanel chartCard =
                new JPanel(
                        new BorderLayout()
                );

        chartCard.setBackground(
                Color.WHITE
        );

        chartCard.setBorder(
                BorderFactory.createLineBorder(
                        BORDER
                )
        );

        chartCard.add(
                expandedChart,
                BorderLayout.CENTER
        );

        chartWrapper.add(
                chartCard,
                BorderLayout.CENTER
        );

        // =====================================================
        // ADD EVERYTHING
        // =====================================================

        dialog.add(
                header,
                BorderLayout.NORTH
        );

        dialog.add(
                chartWrapper,
                BorderLayout.CENTER
        );

        dialog.pack();

        // =====================================================
        // SCREEN SIZE
        // =====================================================

        Dimension screen =
                Toolkit
                        .getDefaultToolkit()
                        .getScreenSize();

        int width =
                Math.min(
                        1250,
                        screen.width - 80
                );

        int height =
                Math.min(
                        850,
                        screen.height - 80
                );

        dialog.setSize(
                width,
                height
        );

        dialog.setLocationRelativeTo(null);

        dialog.setVisible(true);
    }

    // =========================================================
    // BASE CARD
    // =========================================================

    private static JPanel baseCard() {

        JPanel card = new JPanel();

        card.setOpaque(true);

        card.setBackground(
                Color.WHITE
        );

        card.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                BORDER
                        ),
                        new EmptyBorder(
                                10,
                                10,
                                8,
                                10
                        )
                )
        );

        return card;
    }

    // =========================================================
    // CATEGORY CHART STYLE
    // =========================================================

    private static void styleCategory(
            JFreeChart chart,
            boolean lineChart
    ) {

        styleChartTitle(chart);

        chart.setBackgroundPaint(
                Color.WHITE
        );

        CategoryPlot plot =
                chart.getCategoryPlot();

        plot.setBackgroundPaint(
                Color.WHITE
        );

        plot.setRangeGridlinePaint(
                GRID
        );

        plot.setDomainGridlinePaint(
                GRID
        );

        plot.setOutlineVisible(false);

        CategoryAxis domainAxis =
                plot.getDomainAxis();

        if (domainAxis != null) {

            domainAxis.setLabelFont(
                    new Font(
                            "SansSerif",
                            Font.BOLD,
                            11
                    )
            );

            domainAxis.setLabelPaint(
                    TEXT
            );

            domainAxis.setTickLabelFont(
                    new Font(
                            "SansSerif",
                            Font.PLAIN,
                            9
                    )
            );

            domainAxis.setTickLabelPaint(
                    SECONDARY_TEXT
            );

            /*
             * Fixes month labels becoming "2..."
             */
            if (
                    plot.getDataset() != null
                            && plot
                            .getDataset()
                            .getColumnCount() > 6
            ) {

                domainAxis.setCategoryLabelPositions(
                        CategoryLabelPositions.UP_45
                );

            } else {

                domainAxis.setCategoryLabelPositions(
                        CategoryLabelPositions.STANDARD
                );
            }

            domainAxis.setMaximumCategoryLabelWidthRatio(
                    1.0f
            );
        }

        if (
                plot.getRangeAxis()
                        instanceof NumberAxis rangeAxis
        ) {

            rangeAxis.setLabelFont(
                    new Font(
                            "SansSerif",
                            Font.BOLD,
                            11
                    )
            );

            rangeAxis.setLabelPaint(
                    TEXT
            );

            rangeAxis.setTickLabelFont(
                    new Font(
                            "SansSerif",
                            Font.PLAIN,
                            9
                    )
            );

            rangeAxis.setTickLabelPaint(
                    SECONDARY_TEXT
            );

            /*
             * Line charts do not need to start
             * at zero.
             *
             * This fixes Cost per Conversion
             * looking almost completely flat.
             */
            rangeAxis.setAutoRangeIncludesZero(
                    !lineChart
            );
        }

        /*
         * Make line charts easier to inspect.
         */
        if (
                lineChart
                        && plot.getRenderer()
                        instanceof LineAndShapeRenderer renderer
        ) {

            renderer.setDefaultStroke(
                    new BasicStroke(
                            2.0f
                    )
            );

            renderer.setDefaultShapesVisible(
                    true
            );

            renderer.setDefaultShapesFilled(
                    true
            );
        }
    }

    // =========================================================
    // CHART TITLE
    // =========================================================

    private static void styleChartTitle(
            JFreeChart chart
    ) {

        if (chart.getTitle() != null) {

            chart
                    .getTitle()
                    .setFont(
                            new Font(
                                    "SansSerif",
                                    Font.BOLD,
                                    17
                            )
                    );

            chart
                    .getTitle()
                    .setPaint(
                            TEXT
                    );
        }
    }

    // =========================================================
    // LEGEND
    // =========================================================

    private static void styleLegend(
            JFreeChart chart
    ) {

        if (chart.getLegend() != null) {

            chart
                    .getLegend()
                    .setItemFont(
                            new Font(
                                    "SansSerif",
                                    Font.PLAIN,
                                    10
                            )
                    );

            chart
                    .getLegend()
                    .setItemPaint(
                            SECONDARY_TEXT
                    );

            chart
                    .getLegend()
                    .setBackgroundPaint(
                            Color.WHITE
                    );
        }
    }
}
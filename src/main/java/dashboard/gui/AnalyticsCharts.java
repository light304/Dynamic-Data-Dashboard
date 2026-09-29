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
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.entity.CategoryItemEntity;
import org.jfree.chart.entity.PieSectionEntity;
import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.labels.StandardPieToolTipGenerator;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.RingPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.AreaRenderer;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Builds the chart cards: line, area, bar, grouped bar, combo (columns plus
 * a line on a second axis), ring/pie and scatter.
 *
 * Every chart gets the same treatment:
 *   - axis numbers in short form (1.2M, 48K, $3.5K, 62%)
 *   - axis ranges that start and end on round numbers; bars and areas start
 *     at zero (their length is the value), lines and scatters are zoomed to
 *     the data
 *   - one colour per series, from Theme
 *   - exact values in the hover tooltip
 *
 * Titles are not drawn by JFreeChart: CatalogueRenderer puts a title and an
 * (i) info button above the plot so the description can sit in a popup.
 */
public final class AnalyticsCharts {

    /** Card client property holding String[]{title, subtitle} for the expanded view. */
    static final String EXPAND_INFO = "expandInfo";

    private static final Color BORDER = Theme.BORDER;
    private static final Color GRID = Theme.GRID;
    private static final Color TEXT = Theme.TEXT;
    private static final Color SECONDARY_TEXT = Theme.TEXT_MUTED;
    private static final Color ACCENT = Theme.ACCENT;

    private static final Color[] PALETTE = {
            Theme.SERIES_1, Theme.SERIES_2, Theme.SERIES_3,
            Theme.SERIES_4, Theme.SERIES_5, Theme.SERIES_6
    };

    private static final Shape DOT = new Ellipse2D.Double(-3, -3, 6, 6);

    private enum Look { LINE, BAR, AREA }

    private AnalyticsCharts() {
    }

    private static Color colour(int index) {
        return PALETTE[index % PALETTE.length];
    }

    // =========================================================
    // LINE
    // =========================================================

    public static JPanel line(String title, String xLabel, String yLabel,
                              List<Point> values, String series,
                              Consumer<String> categoryClick) {

        DefaultCategoryDataset dataset = singleSeries(values, series);

        JFreeChart chart = ChartFactory.createLineChart(
                title, xLabel, yLabel, dataset,
                PlotOrientation.VERTICAL, false, true, false);

        styleCategory(chart, Look.LINE, yLabel, false);

        return wrap(chart, categoryClick);
    }

    // =========================================================
    // AREA
    // =========================================================

    public static JPanel area(String title, String xLabel, String yLabel,
                              List<Point> values, String series,
                              Consumer<String> categoryClick) {

        DefaultCategoryDataset dataset = singleSeries(values, series);

        JFreeChart chart = ChartFactory.createAreaChart(
                title, xLabel, yLabel, dataset,
                PlotOrientation.VERTICAL, false, true, false);

        styleCategory(chart, Look.AREA, yLabel, false);

        return wrap(chart, categoryClick);
    }

    // =========================================================
    // BAR
    // =========================================================

    public static JPanel bar(String title, String xLabel, String yLabel,
                             List<Point> values, String series,
                             boolean horizontal,
                             Consumer<String> categoryClick) {
        return bar(title, xLabel, yLabel, values, series, horizontal,
                categoryClick, null);
    }

    /**
     * As above, but when categoryColour is given each bar is painted with
     * the colour it returns for that bar's category name (the regions use
     * this) instead of every bar sharing the series colour.
     */
    public static JPanel bar(String title, String xLabel, String yLabel,
                             List<Point> values, String series,
                             boolean horizontal,
                             Consumer<String> categoryClick,
                             java.util.function.Function<String, Color> categoryColour) {

        DefaultCategoryDataset dataset = singleSeries(values, series);

        JFreeChart chart = ChartFactory.createBarChart(
                title, xLabel, yLabel, dataset,
                horizontal ? PlotOrientation.HORIZONTAL : PlotOrientation.VERTICAL,
                false, true, false);

        // Swapped in before styling so the renderer gets the same look
        // (bar width, tooltips, painter) as the one it replaces.
        if (categoryColour != null) {
            chart.getCategoryPlot().setRenderer(
                    new CategoryColourRenderer(categoryColour));
        }

        styleCategory(chart, Look.BAR, yLabel, horizontal);

        return wrap(chart, categoryClick);
    }

    // =========================================================
    // GROUPED BAR
    // =========================================================

    public static JPanel groupedBar(String title, String xLabel, String yLabel,
                                    List<SeriesPoint> values,
                                    Consumer<String> categoryClick) {

        JFreeChart chart = ChartFactory.createBarChart(
                title, xLabel, yLabel, seriesData(values),
                PlotOrientation.VERTICAL, true, true, false);

        styleCategory(chart, Look.BAR, yLabel, false);
        styleLegend(chart);

        return wrap(chart, categoryClick);
    }

    // =========================================================
    // MULTI LINE
    // =========================================================

    public static JPanel multiLine(String title, String xLabel, String yLabel,
                                   List<SeriesPoint> values,
                                   Consumer<String> categoryClick) {

        JFreeChart chart = ChartFactory.createLineChart(
                title, xLabel, yLabel, seriesData(values),
                PlotOrientation.VERTICAL, true, true, false);

        styleCategory(chart, Look.LINE, yLabel, false);
        styleLegend(chart);

        return wrap(chart, categoryClick);
    }

    // =========================================================
    // COMBO: columns on the left axis, a line on the right axis
    // =========================================================

    /**
     * Two measures on different scales, e.g. spend in thousands beside
     * revenue in millions. On one axis the smaller would be a flat line at
     * the bottom.
     *
     * @param axisLabels    "left label|right label"
     * @param columnSeries  the series drawn as columns; every other series is
     *                      drawn as the line. Null means the first series.
     */
    public static JPanel combo(String title, String xLabel, String axisLabels,
                               List<SeriesPoint> values, String columnSeries,
                               Consumer<String> categoryClick) {

        String[] axes = axisLabels == null ? new String[0] : axisLabels.split("\\|", 2);
        String leftLabel = axes.length > 0 ? axes[0].trim() : "";
        String rightLabel = axes.length > 1 ? axes[1].trim() : "";

        String columns = columnSeries;
        boolean found = false;
        for (SeriesPoint p : values) {
            if (p.series().equals(columnSeries)) { found = true; break; }
        }
        if (!found) columns = values.isEmpty() ? "" : values.get(0).series();

        // Both datasets must share one category order, or the line drifts
        // out of step with the columns when a month is missing from one.
        Set<String> labels = new LinkedHashSet<>();
        Set<String> columnKeys = new LinkedHashSet<>();
        Set<String> lineKeys = new LinkedHashSet<>();
        for (SeriesPoint p : values) {
            labels.add(p.label());
            (p.series().equals(columns) ? columnKeys : lineKeys).add(p.series());
        }

        DefaultCategoryDataset barData = new DefaultCategoryDataset();
        DefaultCategoryDataset lineData = new DefaultCategoryDataset();

        for (String key : columnKeys) {
            for (String label : labels) barData.addValue((Number) null, key, label);
        }
        for (String key : lineKeys) {
            for (String label : labels) lineData.addValue((Number) null, key, label);
        }
        for (SeriesPoint p : values) {
            (p.series().equals(columns) ? barData : lineData)
                    .setValue(p.value(), p.series(), p.label());
        }

        JFreeChart chart = ChartFactory.createBarChart(
                title, xLabel, leftLabel, barData,
                PlotOrientation.VERTICAL, true, true, false);

        styleCategory(chart, Look.BAR, leftLabel, false);

        CategoryPlot plot = chart.getCategoryPlot();

        // Left axis and columns take the first colour, right axis and
        // line the second, so each axis reads as belonging to its series.
        Color columnColour = colour(0);
        Color lineColour = colour(2);

        tintAxis(plot.getRangeAxis(0), columnColour);

        if (plot.getRenderer() instanceof BarRenderer bars) {
            for (int i = 0; i < barData.getRowCount(); i++) {
                bars.setSeriesPaint(i, columnColour);
            }
        }

        NumberAxis right = new NumberAxis(rightLabel);
        styleNumberAxis(right, rangeOf(lineData), false, rightLabel);
        tintAxis(right, lineColour);

        plot.setRangeAxis(1, right);
        plot.setDataset(1, lineData);
        plot.mapDatasetToRangeAxis(1, 1);

        LineAndShapeRenderer lines = new LineAndShapeRenderer(true, true);
        for (int i = 0; i < lineData.getRowCount(); i++) {
            lines.setSeriesPaint(i, lineColour);
            lines.setSeriesStroke(i, new BasicStroke(2.2f));
            lines.setSeriesShape(i, DOT);
        }
        lines.setDefaultToolTipGenerator(
                new StandardCategoryToolTipGenerator("{0} - {1}: {2}",
                        AxisScale.exact(rightLabel)));

        plot.setRenderer(1, lines);

        // Draw the line over the columns, not behind them.
        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

        styleLegend(chart);

        return wrap(chart, categoryClick);
    }

    // =========================================================
    // PIE / RING
    // =========================================================

    public static JPanel pie(String title, List<Point> values) {
        return ring(title, values, null, false);
    }

    public static JPanel pie(String title, List<Point> values,
                             Consumer<String> sectionClick) {
        return ring(title, values, sectionClick, false);
    }

    /** A donut: parts of a whole. Labels show each slice's percentage. */
    public static JPanel ring(String title, List<Point> values,
                              Consumer<String> sectionClick) {
        return ring(title, values, sectionClick, true);
    }

    private static JPanel ring(String title, List<Point> values,
                               Consumer<String> sectionClick, boolean donut) {

        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        for (Point p : values) {
            dataset.setValue(p.label(), p.value());
        }

        JFreeChart chart = donut
                ? ChartFactory.createRingChart(title, dataset, true, true, false)
                : ChartFactory.createPieChart(title, dataset, true, true, false);

        chart.setBackgroundPaint(Color.WHITE);

        @SuppressWarnings("unchecked")
        PiePlot<String> plot = (PiePlot<String>) chart.getPlot();

        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        plot.setShadowPaint(null);

        int i = 0;
        for (String key : dataset.getKeys()) {
            plot.setSectionPaint(key, colour(i++));
        }

        if (chart.getPlot() instanceof RingPlot ringPlot) {
            ringPlot.setSectionDepth(0.42);
            ringPlot.setSeparatorsVisible(false);
        }

        // Percentage on the slice, category names in the legend. Names on
        // the slices collide once the card is a quarter of the page.
        NumberFormat percent = new DecimalFormat("0%");

        plot.setLabelGenerator(new StandardPieSectionLabelGenerator(
                "{2}", new DecimalFormat("#,##0"), percent));

        plot.setLabelFont(Theme.SMALL_BOLD);
        plot.setLabelPaint(TEXT);
        plot.setLabelBackgroundPaint(null);
        plot.setLabelOutlinePaint(null);
        plot.setLabelShadowPaint(null);

        plot.setToolTipGenerator(new StandardPieToolTipGenerator(
                "{0}: {1} ({2})", new DecimalFormat("#,##0.##"), percent));

        styleLegend(chart);

        JPanel card = baseCard();
        card.setLayout(new BorderLayout());

        ChartPanel chartPanel = createChartPanel(chart);

        chartPanel.addChartMouseListener(new ChartMouseListener() {

            @Override
            public void chartMouseMoved(ChartMouseEvent event) {
            }

            @Override
            public void chartMouseClicked(ChartMouseEvent event) {

                if (isDoubleClick(event)) {
                    showExpanded(card, chart);
                    return;
                }

                if (sectionClick != null
                        && event.getEntity() instanceof PieSectionEntity entity) {
                    sectionClick.accept(entity.getSectionKey().toString());
                }
            }
        });

        card.add(chartPanel, BorderLayout.CENTER);
        card.add(expandHint(), BorderLayout.SOUTH);
        card.putClientProperty(EXPAND_INFO, new String[]{"Chart", ""});

        return card;
    }

    // =========================================================
    // SCATTER
    // =========================================================

    public static JPanel scatter(String title, String xLabel, String yLabel,
                                 List<XYPoint> values) {

        Map<String, XYSeries> byCategory = new LinkedHashMap<>();
        Map<String, List<String>> productIdsByCategory = new LinkedHashMap<>();

        for (XYPoint p : values) {

            byCategory
                    .computeIfAbsent(p.category(), key -> new XYSeries(key, false))
                    .add(p.x(), p.y());

            productIdsByCategory
                    .computeIfAbsent(p.category(), key -> new ArrayList<>())
                    .add(p.label());
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        for (XYSeries series : byCategory.values()) {
            dataset.addSeries(series);
        }

        List<List<String>> productIdsBySeriesIndex =
                new ArrayList<>(productIdsByCategory.values());

        JFreeChart chart = ChartFactory.createScatterPlot(
                title, xLabel, yLabel, dataset,
                PlotOrientation.VERTICAL, true, true, false);

        chart.setBackgroundPaint(Color.WHITE);

        XYPlot plot = chart.getXYPlot();

        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(GRID);
        plot.setRangeGridlinePaint(GRID);
        plot.setOutlineVisible(false);

        // Hover text: category, product ID and both plotted values.
        XYToolTipGenerator tooltipGenerator =
                (XYDataset ds, int series, int item) -> {

                    String category = String.valueOf(ds.getSeriesKey(series));

                    String productId =
                            (series < productIdsBySeriesIndex.size()
                                    && item < productIdsBySeriesIndex.get(series).size())
                                    ? productIdsBySeriesIndex.get(series).get(item)
                                    : "?";

                    return "<html>"
                            + category + " - Product Id: " + productId + "<br>"
                            + axisLabel(xLabel) + ": "
                            + AxisScale.exact(xLabel).format(ds.getXValue(series, item)) + "<br>"
                            + axisLabel(yLabel) + ": "
                            + AxisScale.exact(yLabel).format(ds.getYValue(series, item))
                            + "</html>";
                };

        XYItemRenderer renderer = plot.getRenderer();

        if (renderer != null) {
            renderer.setDefaultToolTipGenerator(tooltipGenerator);

            for (int i = 0; i < dataset.getSeriesCount(); i++) {
                renderer.setSeriesPaint(i, colour(i));
                renderer.setSeriesShape(i, DOT);
            }
        }

        if (plot.getDomainAxis() instanceof NumberAxis domain) {
            styleNumberAxis(domain, domainRangeOf(dataset), false, xLabel);
        }

        if (plot.getRangeAxis() instanceof NumberAxis range) {
            styleNumberAxis(range, rangeRangeOf(dataset), false, yLabel);
        }

        styleLegend(chart);

        return wrap(chart, null);
    }

    // Axis label with any trailing unit annotation (e.g. " ($)") stripped.
    private static String axisLabel(String label) {
        int unitStart = label.indexOf(" (");
        return unitStart >= 0 ? label.substring(0, unitStart) : label;
    }

    // =========================================================
    // MESSAGE CARD
    // =========================================================

    public static JPanel messageCard(String title, String message) {

        JPanel panel = baseCard();

        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel heading = new JLabel(title);
        heading.setFont(Theme.CARD_TITLE);
        heading.setForeground(TEXT);

        JLabel body = new JLabel("<html>" + message + "</html>");
        body.setFont(Theme.BODY);
        body.setForeground(SECONDARY_TEXT);

        panel.add(heading);
        panel.add(Box.createVerticalStrut(10));
        panel.add(body);

        return panel;
    }

    // =========================================================
    // DATASETS
    // =========================================================

    private static DefaultCategoryDataset singleSeries(List<Point> values, String series) {

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for (Point p : values) {
            dataset.addValue(p.value(), series, p.label());
        }

        return dataset;
    }

    private static DefaultCategoryDataset seriesData(List<SeriesPoint> values) {

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for (SeriesPoint p : values) {
            dataset.addValue(p.value(), p.series(), p.label());
        }

        return dataset;
    }

    // =========================================================
    // RANGES
    // =========================================================

    private static double[] rangeOf(CategoryDataset dataset) {

        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;

        for (int row = 0; row < dataset.getRowCount(); row++) {
            for (int col = 0; col < dataset.getColumnCount(); col++) {
                Number n = dataset.getValue(row, col);
                if (n == null) continue;
                min = Math.min(min, n.doubleValue());
                max = Math.max(max, n.doubleValue());
            }
        }

        return min > max ? new double[]{0, 1} : new double[]{min, max};
    }

    private static double[] domainRangeOf(XYDataset dataset) {
        return xyRange(dataset, true);
    }

    private static double[] rangeRangeOf(XYDataset dataset) {
        return xyRange(dataset, false);
    }

    private static double[] xyRange(XYDataset dataset, boolean domain) {

        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;

        for (int s = 0; s < dataset.getSeriesCount(); s++) {
            for (int i = 0; i < dataset.getItemCount(s); i++) {
                double v = domain ? dataset.getXValue(s, i) : dataset.getYValue(s, i);
                min = Math.min(min, v);
                max = Math.max(max, v);
            }
        }

        return min > max ? new double[]{0, 1} : new double[]{min, max};
    }

    /**
     * Fixes the axis to round start and end values, with short tick labels.
     * Bars and areas must include zero; lines and scatters need not.
     */
    private static void styleNumberAxis(NumberAxis axis, double[] dataRange,
                                        boolean includeZero, String label) {

        AxisScale.Bounds bounds =
                AxisScale.bounds(dataRange[0], dataRange[1], includeZero, 5);

        axis.setRange(bounds.min(), bounds.max());
        axis.setAutoTickUnitSelection(false);
        axis.setTickUnit(new NumberTickUnit(bounds.step(), AxisScale.formatFor(label)));

        axis.setLabelFont(Theme.AXIS_LABEL);
        axis.setLabelPaint(TEXT);
        axis.setTickLabelFont(Theme.AXIS_TICK);
        axis.setTickLabelPaint(SECONDARY_TEXT);
    }

    private static void tintAxis(org.jfree.chart.axis.ValueAxis axis, Color colour) {
        axis.setLabelPaint(colour);
        axis.setTickLabelPaint(colour);
    }

    // =========================================================
    // CATEGORY CHART STYLE
    // =========================================================

    private static void styleCategory(JFreeChart chart, Look look,
                                      String yLabel, boolean horizontal) {

        chart.setBackgroundPaint(Color.WHITE);

        CategoryPlot plot = chart.getCategoryPlot();

        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(GRID);
        plot.setDomainGridlinesVisible(false);
        plot.setOutlineVisible(false);

        CategoryDataset dataset = plot.getDataset();

        // ----- category axis -----
        CategoryAxis domain = plot.getDomainAxis();

        domain.setLabelFont(Theme.AXIS_LABEL);
        domain.setLabelPaint(TEXT);
        domain.setTickLabelFont(Theme.AXIS_TICK);
        domain.setTickLabelPaint(SECONDARY_TEXT);
        domain.setMaximumCategoryLabelWidthRatio(1.0f);

        int columns = dataset.getColumnCount();
        int longest = 0;
        for (int c = 0; c < columns; c++) {
            longest = Math.max(longest, String.valueOf(dataset.getColumnKey(c)).length());
        }

        // Short labels (Jan, 17, WH_A) stay flat. Only long ones tilt.
        domain.setCategoryLabelPositions(
                (!horizontal && columns > 6 && longest > 4)
                        ? CategoryLabelPositions.UP_45
                        : CategoryLabelPositions.STANDARD);

        // An area should run edge to edge, not float inside margins.
        if (look == Look.AREA) {
            domain.setLowerMargin(0.0);
            domain.setUpperMargin(0.0);
            domain.setCategoryMargin(0.0);
        }

        // ----- value axis -----
        if (plot.getRangeAxis() instanceof NumberAxis range) {
            styleNumberAxis(range, rangeOf(dataset), look != Look.LINE, yLabel);
        }

        // ----- series -----
        CategoryItemRenderer renderer = plot.getRenderer();

        int rows = dataset.getRowCount();

        for (int i = 0; i < rows; i++) {
            renderer.setSeriesPaint(i, colour(i));
        }

        renderer.setDefaultToolTipGenerator(
                new StandardCategoryToolTipGenerator("{0} - {1}: {2}",
                        AxisScale.exact(yLabel)));

        if (renderer instanceof BarRenderer bars) {
            bars.setShadowVisible(false);
            bars.setBarPainter(new StandardBarPainter());
            bars.setMaximumBarWidth(0.14);
            bars.setItemMargin(0.06);
        }

        if (renderer instanceof LineAndShapeRenderer lines) {
            for (int i = 0; i < rows; i++) {
                lines.setSeriesStroke(i, new BasicStroke(2.2f));
                lines.setSeriesShape(i, DOT);
                lines.setSeriesShapesVisible(i, true);
            }
        }

        if (renderer instanceof AreaRenderer) {
            for (int i = 0; i < rows; i++) {
                Color c = colour(i);
                renderer.setSeriesPaint(i, new Color(c.getRed(), c.getGreen(), c.getBlue(), 150));
            }
        }
    }

    // =========================================================
    // LEGEND
    // =========================================================

    private static void styleLegend(JFreeChart chart) {

        if (chart.getLegend() != null) {
            chart.getLegend().setItemFont(Theme.LEGEND);
            chart.getLegend().setItemPaint(SECONDARY_TEXT);
            chart.getLegend().setBackgroundPaint(Color.WHITE);
        }
    }

    // =========================================================
    // CARD
    // =========================================================

    private static boolean isDoubleClick(ChartMouseEvent event) {
        return event.getTrigger() != null && event.getTrigger().getClickCount() >= 2;
    }

    private static JLabel expandHint() {

        JLabel hint = new JLabel("Double-click to expand", SwingConstants.RIGHT);

        hint.setFont(Theme.SMALL.deriveFont(11f));
        hint.setForeground(SECONDARY_TEXT);
        hint.setBorder(new EmptyBorder(2, 0, 0, 2));

        return hint;
    }

    private static JPanel wrap(JFreeChart chart, Consumer<String> categoryClick) {

        JPanel card = baseCard();

        card.setLayout(new BorderLayout());

        ChartPanel chartPanel = createChartPanel(chart);

        chartPanel.addChartMouseListener(new ChartMouseListener() {

            @Override
            public void chartMouseMoved(ChartMouseEvent event) {
            }

            @Override
            public void chartMouseClicked(ChartMouseEvent event) {

                // Double-click opens the large window.
                if (isDoubleClick(event)) {
                    showExpanded(card, chart);
                    return;
                }

                // Single click keeps the drill-down.
                if (categoryClick != null
                        && event.getEntity() instanceof CategoryItemEntity entity) {

                    categoryClick.accept(entity.getColumnKey().toString());
                }
            }
        });

        card.add(chartPanel, BorderLayout.CENTER);
        card.add(expandHint(), BorderLayout.SOUTH);

        // CatalogueRenderer overwrites this with the spec's title and text.
        card.putClientProperty(EXPAND_INFO, new String[]{"Chart", ""});

        return card;
    }

    private static void showExpanded(JPanel card, JFreeChart chart) {

        Object info = card.getClientProperty(EXPAND_INFO);

        String title = "Chart";
        String subtitle = "";

        if (info instanceof String[] parts && parts.length >= 2) {
            title = parts[0] == null ? title : parts[0];
            subtitle = parts[1] == null ? "" : parts[1];
        }

        showExpandedChart(chart, title, subtitle);
    }

    private static ChartPanel createChartPanel(JFreeChart chart) {

        ChartPanel chartPanel = new ChartPanel(chart);

        chartPanel.setBorder(null);
        chartPanel.setBackground(Color.WHITE);

        // Charts follow the card size rather than scaling a fixed bitmap.
        chartPanel.setMinimumDrawWidth(0);
        chartPanel.setMinimumDrawHeight(0);
        chartPanel.setMaximumDrawWidth(Integer.MAX_VALUE);
        chartPanel.setMaximumDrawHeight(Integer.MAX_VALUE);

        // Drag-to-zoom off: single-click is drill-down, double-click expands.
        chartPanel.setDomainZoomable(false);
        chartPanel.setRangeZoomable(false);
        chartPanel.setMouseWheelEnabled(false);

        // The plot is sized by the card; the default 680x420 would make
        // the four-per-page grid demand more room than the window has.
        chartPanel.setPreferredSize(new Dimension(200, 120));

        return chartPanel;
    }

    // =========================================================
    // EXPANDED CHART WINDOW
    // =========================================================

    static void showExpandedChart(JFreeChart chart, String title, String subtitle) {

        JFrame dialog = new JFrame(title);

        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(Theme.PAGE_BG);

        // ----- header -----
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                new EmptyBorder(14, 20, 14, 20)));

        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));

        JLabel heading = new JLabel(title);
        heading.setFont(Theme.SECTION);
        heading.setForeground(TEXT);
        titlePanel.add(heading);

        String sub = (subtitle == null || subtitle.isBlank())
                ? "Expanded chart view"
                : subtitle;

        JLabel subLabel = new JLabel("<html><div style='width:820px'>" + sub + "</div></html>");
        subLabel.setFont(Theme.SMALL);
        subLabel.setForeground(SECONDARY_TEXT);

        titlePanel.add(Box.createVerticalStrut(3));
        titlePanel.add(subLabel);

        JButton closeButton = new JButton("Close");
        closeButton.setFont(Theme.SMALL_BOLD);
        closeButton.setForeground(Color.WHITE);
        closeButton.setBackground(ACCENT);
        closeButton.setOpaque(true);
        closeButton.setContentAreaFilled(true);
        closeButton.setBorderPainted(false);
        closeButton.setFocusPainted(false);
        closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeButton.setPreferredSize(new Dimension(90, 36));
        closeButton.addActionListener(e -> dialog.dispose());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(closeButton);

        header.add(titlePanel, BorderLayout.WEST);
        header.add(buttonPanel, BorderLayout.EAST);

        // ----- large chart -----
        ChartPanel expanded = createChartPanel(chart);
        expanded.setMouseWheelEnabled(true);
        expanded.setPreferredSize(new Dimension(1100, 640));

        JPanel chartCard = new JPanel(new BorderLayout());
        chartCard.setBackground(Color.WHITE);
        chartCard.setBorder(BorderFactory.createLineBorder(BORDER));
        chartCard.add(expanded, BorderLayout.CENTER);

        JPanel chartWrapper = new JPanel(new BorderLayout());
        chartWrapper.setBackground(Theme.PAGE_BG);
        chartWrapper.setBorder(new EmptyBorder(18, 18, 18, 18));
        chartWrapper.add(chartCard, BorderLayout.CENTER);

        dialog.add(header, BorderLayout.NORTH);
        dialog.add(chartWrapper, BorderLayout.CENTER);

        dialog.pack();

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

        dialog.setSize(Math.min(1250, screen.width - 80),
                       Math.min(820, screen.height - 80));

        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    // =========================================================
    // BASE CARD
    // =========================================================

    private static JPanel baseCard() {

        JPanel card = new JPanel();

        card.setOpaque(true);
        card.setBackground(Color.WHITE);

        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(8, 10, 6, 10)));

        return card;
    }
}
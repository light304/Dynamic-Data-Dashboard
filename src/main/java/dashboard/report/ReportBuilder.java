package dashboard.report;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import dashboard.database.AnalyticsApi;
import dashboard.database.AnalyticsApi.Kpis;
import dashboard.database.AnalyticsApi.Point;
import dashboard.database.AnalyticsApi.SeriesPoint;
import dashboard.database.AnalyticsApi.XYPoint;

import dashboard.gui.CategoryColourRenderer;
import dashboard.gui.Theme;
import dashboard.report.ChartSpec.Page;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes one page's PDF.
 *
 * Every chart it draws comes from ChartCatalogue, so the PDF cannot
 * disagree with the screen about a title, an axis label or a caveat.
 */
public final class ReportBuilder {

    private static final Font H1 =
            new Font(Font.HELVETICA, 20, Font.BOLD, Theme.TEXT);
    private static final Font H2 =
            new Font(Font.HELVETICA, 13, Font.BOLD, Theme.TEXT);
    private static final Font BODY =
            new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(55, 65, 81));
    private static final Font MUTED =
            new Font(Font.HELVETICA, 9, Font.ITALIC, Theme.TEXT_MUTED);

    /** Same six series colours the dashboard draws with. */
    private static final Color[] SERIES = {
            Theme.SERIES_1, Theme.SERIES_2, Theme.SERIES_3,
            Theme.SERIES_4, Theme.SERIES_5, Theme.SERIES_6
    };

    private ReportBuilder() {}

    // =====================================================
    // Entry points
    // =====================================================

    /** The export button on an analytics page calls this. */
    public static void writePdf(File target,
                                Page page,
                                Map<String, String> filters,
                                String periodLabel) throws Exception {

        writePdf(target,
                 heading(page),
                 ChartCatalogue.reportFor(page),
                 filters,
                 periodLabel);
    }

    /** Takes an explicit list, so Alerts can reuse the same header later. */
    public static void writePdf(File target,
                                String heading,
                                List<ChartSpec> specs,
                                Map<String, String> filters,
                                String periodLabel) throws Exception {

        Document doc = new Document(PageSize.A4, 45, 45, 50, 50);
        PdfWriter.getInstance(doc, new FileOutputStream(target));
        doc.open();

        doc.add(new Paragraph("Dynamic Retail Dashboard", H1));
        doc.add(new Paragraph(heading, H2));
        doc.add(Chunk.NEWLINE);

        doc.add(new Paragraph("Period: " + periodLabel, BODY));
        doc.add(new Paragraph(
                "Region: " + filters.getOrDefault("region", "All Regions"), BODY));
        doc.add(new Paragraph("Generated: "
                + LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm")), MUTED));
        doc.add(Chunk.NEWLINE);

        for (ChartSpec spec : specs) {

            if (spec.kind() == ChartSpec.Kind.KPI_TABLE) {
                addKpiBlock(doc, spec, filters);
                continue;
            }

            if (spec.endpoint() == null) continue;

            addChartBlock(doc, spec, filters);
        }

        doc.close();
    }

    private static String heading(Page page) {
        return switch (page) {
            case OVERVIEW  -> "Business Overview Report";
            case SALES     -> "Sales Report";
            case PRODUCTS  -> "Products Report";
            case INVENTORY -> "Inventory Report";
            case MARKETING -> "Marketing Report";
            case CUSTOMERS -> "Customers Report";
        };
    }

    // =====================================================
    // Blocks
    // =====================================================

    private static void addKpiBlock(Document doc,
                                    ChartSpec spec,
                                    Map<String, String> filters) throws Exception {

        doc.add(new Paragraph(spec.title(), H2));
        doc.add(new Paragraph(spec.description(), BODY));
        doc.add(Chunk.NEWLINE);

        Kpis kpis = AnalyticsApi.overview(filters);
        doc.add(kpiTable(kpis));

        if (!kpis.profitIncludesMarketing() || kpis.turnoverRegionIgnored()) {
            doc.add(Chunk.NEWLINE);
            doc.add(new Paragraph(
                    "Note: a region filter is applied. Profit is shown gross, because "
                    + "marketing spend has no region. Inventory turnover and customer "
                    + "retention are whole-business figures, because inventory and "
                    + "customers have no region either.", MUTED));
        }
        doc.add(Chunk.NEWLINE);
    }

    private static void addChartBlock(Document doc,
                                      ChartSpec catalogueSpec,
                                      Map<String, String> filters) throws Exception {

        // "Months" or "Days", to match what the filter makes the chart plot.
        ChartSpec spec = catalogueSpec.forScope(filters.get("scope"));

        JFreeChart chart = buildChart(spec, filters);

        if (chart == null) {
            doc.add(new Paragraph(spec.title(), H2));
            doc.add(new Paragraph(spec.fullDescription(), BODY));
            doc.add(Chunk.NEWLINE);
            doc.add(new Paragraph("No data for this period.", MUTED));
            doc.add(Chunk.NEWLINE);
            return;
        }

        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ChartUtils.writeChartAsPNG(png, chart, 700, 360);

        Image image = Image.getInstance(png.toByteArray());
        image.scaleToFit(500, 260);

        Paragraph block = new Paragraph();
        block.setKeepTogether(true);
        block.add(new Paragraph(spec.title(), H2));
        block.add(new Paragraph(spec.fullDescription(), BODY));
        block.add(Chunk.NEWLINE);
        block.add(new Chunk(image, 0, 0));

        if (spec.showsDataTable()) {
            block.add(Chunk.NEWLINE);
            block.add(dataTable(
                    AnalyticsApi.points(spec.endpoint(), filters), spec));
        }

        doc.add(block);
        doc.add(Chunk.NEWLINE);
    }

    // =====================================================
    // Tables
    // =====================================================

    private static PdfPTable kpiTable(Kpis k) {
        PdfPTable table = new PdfPTable(new float[]{3f, 2f});
        table.setWidthPercentage(70);

        header(table, "Indicator");
        header(table, "Value");

        row(table, "Total Revenue",       String.format("$%,.2f", k.revenue()));
        row(table, "Revenue Growth", k.growth() == null
                ? "N/A"
                : String.format("%.2f%%", k.growth()));
        row(table, "Profit (net)",        String.format("$%,.2f", k.profit()));
        row(table, "Gross Profit Margin", String.format("%.2f%%", k.margin()));
        row(table, "Inventory Turnover",  String.format("%.3f", k.turnover()));
        row(table, "Customer Retention",  String.format("%.2f%%", k.retention()));
        row(table, "Cost per Conversion", String.format("$%,.2f", k.costPerConversion()));

        return table;
    }

    private static PdfPTable dataTable(List<Point> points, ChartSpec spec) {
        PdfPTable table = new PdfPTable(new float[]{3f, 2f});
        table.setWidthPercentage(60);
        header(table, spec.xLabel());
        header(table, spec.yLabel());
        for (Point p : points) {
            row(table, p.label(), String.format("%,.2f", p.value()));
        }
        return table;
    }

    private static void header(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, H2));
        cell.setBackgroundColor(new Color(242, 244, 247));
        cell.setPadding(6);
        table.addCell(cell);
    }

    private static void row(PdfPTable table, String name, String value) {
        PdfPCell a = new PdfPCell(new Phrase(name, BODY));
        PdfPCell b = new PdfPCell(new Phrase(value, BODY));
        a.setPadding(6);
        b.setPadding(6);
        table.addCell(a);
        table.addCell(b);
    }

    // =====================================================
    // Charts
    // =====================================================

    private static JFreeChart buildChart(ChartSpec spec,
                                         Map<String, String> filters) throws Exception {

        JFreeChart chart = switch (spec.kind()) {
            case BAR, LINE                -> singleSeries(spec, filters);
            case AREA                     -> area(spec, filters);
            case GROUPED_BAR, MULTI_LINE  -> multiSeries(spec, filters);
            case COMBO                    -> combo(spec, filters);
            case PIE                      -> pie(spec, filters, false);
            case RING                     -> pie(spec, filters, true);
            case SCATTER                  -> scatter(spec, filters);
            case KPI_TABLE                -> null;
        };

        if (chart != null) style(chart, spec);
        return chart;
    }

    private static JFreeChart singleSeries(ChartSpec spec,
                                           Map<String, String> filters) throws Exception {

        List<Point> points = AnalyticsApi.points(spec.endpoint(), filters);
        if (points.isEmpty()) return null;

        String key = spec.seriesName() == null ? spec.yLabel() : spec.seriesName();

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (Point p : points) {
            dataset.addValue(p.value(), key, p.label());
        }

        PlotOrientation orientation = spec.horizontal()
                ? PlotOrientation.HORIZONTAL
                : PlotOrientation.VERTICAL;

        return spec.kind() == ChartSpec.Kind.LINE
                ? ChartFactory.createLineChart(
                        null, spec.xLabel(), spec.yLabel(),
                        dataset, orientation, false, false, false)
                : ChartFactory.createBarChart(
                        null, spec.xLabel(), spec.yLabel(),
                        dataset, orientation, false, false, false);
    }

    private static JFreeChart area(ChartSpec spec,
                                   Map<String, String> filters) throws Exception {

        List<Point> points = AnalyticsApi.points(spec.endpoint(), filters);
        if (points.isEmpty()) return null;

        String key = spec.seriesName() == null ? spec.yLabel() : spec.seriesName();

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (Point p : points) {
            dataset.addValue(p.value(), key, p.label());
        }

        return ChartFactory.createAreaChart(
                null, spec.xLabel(), spec.yLabel(),
                dataset, PlotOrientation.VERTICAL, false, false, false);
    }

    /**
     * GROUPED_BAR and MULTI_LINE data, merged from a second endpoint via
     * ChartData when the spec declares one (see ChartSpec.second()) -
     * the same merge the screen performs, so the PDF cannot show a
     * different picture than the card it was exported from.
     */
    private static JFreeChart multiSeries(ChartSpec spec,
                                          Map<String, String> filters) throws Exception {

        List<SeriesPoint> points = ChartData.series(spec, filters);
        if (points.isEmpty()) return null;

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (SeriesPoint p : points) {
            dataset.addValue(p.value(), p.series(), p.label());
        }

        PlotOrientation orientation = spec.horizontal()
                ? PlotOrientation.HORIZONTAL
                : PlotOrientation.VERTICAL;

        return spec.kind() == ChartSpec.Kind.MULTI_LINE
                ? ChartFactory.createLineChart(
                        null, spec.xLabel(), spec.yLabel(),
                        dataset, orientation, true, false, false)
                : ChartFactory.createBarChart(
                        null, spec.xLabel(), spec.yLabel(),
                        dataset, orientation, true, false, false);
    }

    /**
     * Columns on the left axis, a line on the right axis - the PDF
     * equivalent of AnalyticsCharts.combo(). Kept as its own method rather
     * than shared with the screen version because ChartPanel/Swing types
     * are not available on this side of the split.
     */
    private static JFreeChart combo(ChartSpec spec,
        Map<String, String> filters) throws Exception {

        List<SeriesPoint> points = ChartData.series(spec, filters);
        if (points.isEmpty()) return null;

        String[] axes = spec.yLabel() == null ? new String[0] : spec.yLabel().split("\\|", 2);
        String leftLabel = axes.length > 0 ? axes[0].trim() : "";
        String rightLabel = axes.length > 1 ? axes[1].trim() : "";

        String columnSeries = spec.seriesName();
        boolean found = false;
        for (SeriesPoint p : points) {
            if (p.series().equals(columnSeries)) { found = true; break; }
        }
        if (!found) columnSeries = points.get(0).series();

        java.util.Set<String> labels = new java.util.LinkedHashSet<>();
        java.util.Set<String> columnKeys = new java.util.LinkedHashSet<>();
        java.util.Set<String> lineKeys = new java.util.LinkedHashSet<>();

        for (SeriesPoint p : points) {
            labels.add(p.label());
            (p.series().equals(columnSeries) ? columnKeys : lineKeys).add(p.series());
        }

        DefaultCategoryDataset barData = new DefaultCategoryDataset();
        DefaultCategoryDataset lineData = new DefaultCategoryDataset();

        for (String key : columnKeys) {
            for (String label : labels) barData.addValue((Number) null, key, label);
        }
        for (String key : lineKeys) {
            for (String label : labels) lineData.addValue((Number) null, key, label);
        }
        for (SeriesPoint p : points) {
            (p.series().equals(columnSeries) ? barData : lineData)
                    .setValue(p.value(), p.series(), p.label());
        }

        JFreeChart chart = ChartFactory.createBarChart(
                null, spec.xLabel(), leftLabel, barData,
                PlotOrientation.VERTICAL, true, false, false);

        CategoryPlot plot = chart.getCategoryPlot();

        Color columnColour = SERIES[0];
        Color lineColour = SERIES[2];

        // Left axis (the columns). styleCategory() skips number-axis
        // formatting entirely for COMBO charts (it has no way to tell
        // which of the two axes it would be touching), so this is the
        // only place the left axis gets its round-number range and
        // compact tick labels.
        if (plot.getRangeAxis(0) instanceof org.jfree.chart.axis.NumberAxis left) {
            applyNumberAxis(left, rangeOf(barData), true, leftLabel);
        }

        org.jfree.chart.axis.NumberAxis right =
                new org.jfree.chart.axis.NumberAxis(rightLabel);
        applyNumberAxis(right, rangeOf(lineData), false, rightLabel);
        right.setLabelPaint(lineColour);
        right.setTickLabelPaint(lineColour);

        plot.setRangeAxis(1, right);
        plot.setDataset(1, lineData);
        plot.mapDatasetToRangeAxis(1, 1);

        org.jfree.chart.renderer.category.LineAndShapeRenderer lines =
                new org.jfree.chart.renderer.category.LineAndShapeRenderer(true, true);

        for (int i = 0; i < lineData.getRowCount(); i++) {
            lines.setSeriesPaint(i, lineColour);
            lines.setSeriesStroke(i, new BasicStroke(2.2f));
        }

        plot.setRenderer(1, lines);
        plot.setDatasetRenderingOrder(org.jfree.chart.plot.DatasetRenderingOrder.FORWARD);

        // style(chart, spec) still runs styleCategory() on dataset 0 (the
        // columns) below; tint the columns and the left axis to match here,
        // since styleCategory has no knowledge of the second series.
        if (plot.getRenderer(0) instanceof BarRenderer bars) {
            for (int i = 0; i < barData.getRowCount(); i++) {
                bars.setSeriesPaint(i, columnColour);
            }
        }
        plot.getRangeAxis(0).setLabelPaint(columnColour);
        plot.getRangeAxis(0).setTickLabelPaint(columnColour);

        return chart;
    }

    private static JFreeChart pie(ChartSpec spec, Map<String, String> filters,
                                  boolean donut) throws Exception {

        List<Point> points = AnalyticsApi.points(spec.endpoint(), filters);
        if (points.isEmpty()) return null;

        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        for (Point p : points) {
            dataset.setValue(p.label(), p.value());
        }

        return donut
                ? ChartFactory.createRingChart(null, dataset, true, false, false)
                : ChartFactory.createPieChart(null, dataset, true, false, false);
    }

    private static JFreeChart scatter(ChartSpec spec,
                                      Map<String, String> filters) throws Exception {

        List<XYPoint> points = AnalyticsApi.xyPoints(spec.endpoint(), filters);
        if (points.isEmpty()) return null;

        // One series per category, so the legend names the categories.
        Map<String, XYSeries> byCategory = new LinkedHashMap<>();
        for (XYPoint p : points) {
            String category = p.category() == null ? "All" : p.category();
            byCategory.computeIfAbsent(category, XYSeries::new)
                      .add(p.x(), p.y());
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        byCategory.values().forEach(dataset::addSeries);

        return ChartFactory.createScatterPlot(
                null, spec.xLabel(), spec.yLabel(),
                dataset, PlotOrientation.VERTICAL, true, false, false);
    }

    // =====================================================
    // Styling
    //
    // Dispatches on the plot, because only a bar or line chart has a
    // CategoryPlot. The old code called getCategoryPlot() unconditionally,
    // which would have thrown on the first pie.
    //
    // Axis numbers are shortened the same way the screen shortens them
    // (AxisScale), so a chart does not read differently in the PDF than
    // it did on the page it was exported from.
    // =====================================================

    private static void style(JFreeChart chart, ChartSpec spec) {

        chart.setBackgroundPaint(Color.WHITE);

        if (chart.getPlot() instanceof CategoryPlot plot) {
            styleCategory(plot, spec);
        } else if (chart.getPlot() instanceof PiePlot) {
            stylePie(chart);
        } else if (chart.getPlot() instanceof XYPlot plot) {
            styleXy(plot, spec);
        }
    }

    private static void styleCategory(CategoryPlot plot, ChartSpec spec) {

        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Theme.GRID);
        plot.setOutlineVisible(false);

        int rows = plot.getDataset() == null ? 0 : plot.getDataset().getRowCount();

        boolean isCombo = spec.kind() == ChartSpec.Kind.COMBO;

        // Region bars each take their region's colour, as on screen.
        if (spec.categoriesAreRegions()
                && plot.getRenderer() instanceof BarRenderer) {
            plot.setRenderer(new CategoryColourRenderer(Theme::regionColour));
        }

        if (plot.getRenderer() instanceof BarRenderer bar) {
            bar.setShadowVisible(false);
            bar.setBarPainter(new StandardBarPainter());
            // COMBO already painted its column series in combo(); a second
            // pass here would flatten it back to the default palette.
            if (!isCombo) {
                for (int i = 0; i < rows; i++) {
                    bar.setSeriesPaint(i, SERIES[i % SERIES.length]);
                }
            }
        } else {
            for (int i = 0; i < rows; i++) {
                plot.getRenderer().setSeriesPaint(i, SERIES[i % SERIES.length]);
                plot.getRenderer().setSeriesStroke(i, new BasicStroke(2.0f));
            }
        }

        int columns = plot.getDataset() == null ? 0 : plot.getDataset().getColumnCount();

        // Horizontal bars read their labels down the side, so they never collide.
        plot.getDomainAxis().setCategoryLabelPositions(
                (!spec.horizontal() && columns > 6)
                        ? CategoryLabelPositions.UP_45
                        : CategoryLabelPositions.STANDARD);

        plot.getDomainAxis().setMaximumCategoryLabelWidthRatio(1.0f);

        // COMBO built and styled both range axes itself, since the second
        // (right-hand) axis and its own tick formatting exist outside what
        // this method can see from a single CategoryPlot pass.
        if (isCombo) return;

        boolean includeZero = spec.kind() != ChartSpec.Kind.LINE
                && spec.kind() != ChartSpec.Kind.MULTI_LINE;

        if (plot.getRangeAxis() instanceof org.jfree.chart.axis.NumberAxis range) {
            applyNumberAxis(range, rangeOf(plot.getDataset()), includeZero, spec.yLabel());
        }
    }

    private static void stylePie(JFreeChart chart) {

        @SuppressWarnings("unchecked")
        PiePlot<String> plot = (PiePlot<String>) chart.getPlot();

        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        plot.setShadowPaint(null);
        plot.setLabelBackgroundPaint(Color.WHITE);
        plot.setLabelOutlinePaint(null);
        plot.setLabelShadowPaint(null);

        int i = 0;
        for (String key : plot.getDataset().getKeys()) {
            plot.setSectionPaint(key, SERIES[i++ % SERIES.length]);
        }

        if (chart.getPlot() instanceof org.jfree.chart.plot.RingPlot ringPlot) {
            ringPlot.setSectionDepth(0.42);
            ringPlot.setSeparatorsVisible(false);
        }

        plot.setLabelGenerator(new org.jfree.chart.labels.StandardPieSectionLabelGenerator(
                "{0}: {2}",
                new java.text.DecimalFormat("#,##0"),
                new java.text.DecimalFormat("0%")));
    }

    private static void styleXy(XYPlot plot, ChartSpec spec) {

        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Theme.GRID);
        plot.setRangeGridlinePaint(Theme.GRID);
        plot.setOutlineVisible(false);

        if (plot.getRenderer() instanceof XYLineAndShapeRenderer renderer) {
            for (int i = 0; i < plot.getDataset().getSeriesCount(); i++) {
                renderer.setSeriesPaint(i, SERIES[i % SERIES.length]);
            }
        }

        if (plot.getDomainAxis() instanceof org.jfree.chart.axis.NumberAxis domain) {
            applyNumberAxis(domain, xyRange(plot.getDataset(), true), false, spec.xLabel());
        }

        if (plot.getRangeAxis() instanceof org.jfree.chart.axis.NumberAxis range) {
            applyNumberAxis(range, xyRange(plot.getDataset(), false), false, spec.yLabel());
        }
    }

    // =====================================================
    // Axis ranges and compact tick formatting - see dashboard.gui.AxisScale
    // for the maths. Duplicated here in miniature (rangeOf/xyRange) rather
    // than sharing AnalyticsCharts' private helpers, since those operate on
    // Swing's ChartPanel-bound charts and this side never touches Swing.
    // =====================================================

    private static void applyNumberAxis(org.jfree.chart.axis.NumberAxis axis,
                                        double[] dataRange, boolean includeZero,
                                        String label) {

        dashboard.gui.AxisScale.Bounds bounds =
                dashboard.gui.AxisScale.bounds(dataRange[0], dataRange[1], includeZero, 5);

        axis.setRange(bounds.min(), bounds.max());
        axis.setAutoTickUnitSelection(false);
        axis.setTickUnit(new org.jfree.chart.axis.NumberTickUnit(
                bounds.step(), dashboard.gui.AxisScale.formatFor(label)));
    }

    private static double[] rangeOf(org.jfree.data.category.CategoryDataset dataset) {

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

    private static double[] xyRange(org.jfree.data.xy.XYDataset dataset, boolean domain) {

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
}
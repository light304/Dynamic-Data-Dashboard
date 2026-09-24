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

    /** Same five series colours the dashboard draws with. */
    private static final Color[] SERIES = {
            Theme.SERIES_1, Theme.SERIES_2, Theme.SERIES_3,
            Theme.SERIES_4, Theme.SERIES_5
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
                                      ChartSpec spec,
                                      Map<String, String> filters) throws Exception {

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
            case GROUPED_BAR, MULTI_LINE  -> multiSeries(spec, filters);
            case PIE                      -> pie(spec, filters);
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

    private static JFreeChart multiSeries(ChartSpec spec,
                                          Map<String, String> filters) throws Exception {

        List<SeriesPoint> points = AnalyticsApi.seriesPoints(spec.endpoint(), filters);
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

    private static JFreeChart pie(ChartSpec spec,
                                  Map<String, String> filters) throws Exception {

        List<Point> points = AnalyticsApi.points(spec.endpoint(), filters);
        if (points.isEmpty()) return null;

        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        for (Point p : points) {
            dataset.setValue(p.label(), p.value());
        }

        return ChartFactory.createPieChart(null, dataset, true, false, false);
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
    // =====================================================

    private static void style(JFreeChart chart, ChartSpec spec) {

        chart.setBackgroundPaint(Color.WHITE);

        if (chart.getPlot() instanceof CategoryPlot plot) {
            styleCategory(plot, spec);
        } else if (chart.getPlot() instanceof PiePlot) {
            stylePie(chart);
        } else if (chart.getPlot() instanceof XYPlot plot) {
            styleXy(plot);
        }
    }

    private static void styleCategory(CategoryPlot plot, ChartSpec spec) {

        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Theme.GRID);
        plot.setOutlineVisible(false);

        int rows = plot.getDataset() == null ? 0 : plot.getDataset().getRowCount();

        if (plot.getRenderer() instanceof BarRenderer bar) {
            bar.setShadowVisible(false);
            bar.setBarPainter(new StandardBarPainter());
            for (int i = 0; i < rows; i++) {
                bar.setSeriesPaint(i, SERIES[i % SERIES.length]);
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
    }

    private static void styleXy(XYPlot plot) {

        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Theme.GRID);
        plot.setRangeGridlinePaint(Theme.GRID);
        plot.setOutlineVisible(false);

        if (plot.getRenderer() instanceof XYLineAndShapeRenderer renderer) {
            for (int i = 0; i < plot.getDataset().getSeriesCount(); i++) {
                renderer.setSeriesPaint(i, SERIES[i % SERIES.length]);
            }
        }
    }
}
package dashboard.report;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import dashboard.database.AnalyticsApi;
import dashboard.database.AnalyticsApi.Kpis;
import dashboard.database.AnalyticsApi.Point;
import dashboard.database.AnalyticsApi.SeriesPoint;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ReportBuilder {

    private static final Font H1 =
            new Font(Font.HELVETICA, 20, Font.BOLD, new Color(31, 41, 55));
    private static final Font H2 =
            new Font(Font.HELVETICA, 13, Font.BOLD, new Color(31, 41, 55));
    private static final Font BODY =
            new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(55, 65, 81));
    private static final Font MUTED =
            new Font(Font.HELVETICA, 9, Font.ITALIC, new Color(120, 130, 145));

    private ReportBuilder() {}

    // =====================================================
    // PDF
    // =====================================================

    public static void writePdf(File target,
                                Map<String, String> filters,
                                String periodLabel,
                                Set<ReportSection> sections) throws Exception {

        Document doc = new Document(PageSize.A4, 45, 45, 50, 50);
        PdfWriter.getInstance(doc, new FileOutputStream(target));
        doc.open();

        // ---- header ----
        doc.add(new Paragraph("Dynamic Retail Dashboard", H1));
        doc.add(new Paragraph("Business Overview Report", H2));
        doc.add(Chunk.NEWLINE);

        doc.add(new Paragraph("Period: " + periodLabel, BODY));
        doc.add(new Paragraph("Region: " + filters.getOrDefault("region", "All Regions"), BODY));
        doc.add(new Paragraph("Generated: "
                + LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm")), MUTED));
        doc.add(Chunk.NEWLINE);

        // ---- KPI table ----
        if (sections.contains(ReportSection.KPI_TABLE)) {
            doc.add(new Paragraph("Key Performance Indicators", H2));
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

        // ---- charts ----
                for (ReportSection section : ReportSection.values()) {

            if (section.endpoint() == null) continue;
            if (!sections.contains(section)) continue;

            JFreeChart chart = buildChart(section, filters);

            if (chart == null) {
                doc.add(new Paragraph(section.label(), H2));
                doc.add(Chunk.NEWLINE);
                doc.add(new Paragraph("No data for this period.", MUTED));
                doc.add(Chunk.NEWLINE);
                continue;
            }

            ByteArrayOutputStream png = new ByteArrayOutputStream();
            ChartUtils.writeChartAsPNG(png, chart, 700, 360);

            Image image = Image.getInstance(png.toByteArray());
            image.scaleToFit(500, 260);

            Paragraph block = new Paragraph();
            block.setKeepTogether(true);
            block.add(new Paragraph(section.label(), H2));
            block.add(new Paragraph(section.description(), BODY));
            block.add(Chunk.NEWLINE);
            block.add(new Chunk(image, 0, 0));

            if (section.showsDataTable()) {
                block.add(Chunk.NEWLINE);
                block.add(dataTable(
                        AnalyticsApi.points(section.endpoint(), filters), section));
            }

            doc.add(block);
            doc.add(Chunk.NEWLINE);
        }

        doc.close();
    }

    private static PdfPTable kpiTable(Kpis k) {
        PdfPTable table = new PdfPTable(new float[]{3f, 2f});
        table.setWidthPercentage(70);

        header(table, "Indicator");
        header(table, "Value");

        row(table, "Total Revenue",        String.format("$%,.2f", k.revenue()));
        row(table, "Revenue Growth", k.growth() == null
            ? "N/A"
            : String.format("%.2f%%", k.growth()));
        row(table, "Profit (net)",         String.format("$%,.2f", k.profit()));
        row(table, "Gross Profit Margin",  String.format("%.2f%%", k.margin()));
        row(table, "Inventory Turnover",   String.format("%.3f", k.turnover()));
        row(table, "Customer Retention",   String.format("%.2f%%", k.retention()));
        row(table, "Cost per Conversion",  String.format("$%,.2f", k.costPerConversion()));

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

    private static PdfPTable dataTable(List<Point> points, ReportSection section) {
        PdfPTable table = new PdfPTable(new float[]{3f, 2f});
        table.setWidthPercentage(60);
        header(table, section.xLabel());
        header(table, section.yLabel());
        for (Point p : points) {
            row(table, p.label(), String.format("%,.2f", p.value()));
        }
        return table;
    }

    // =====================================================
    // Charts
    // =====================================================

    private static JFreeChart buildChart(ReportSection section,
                                         Map<String, String> filters) throws Exception {

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        if (section.isSeries()) {
            List<SeriesPoint> points =
                    AnalyticsApi.seriesPoints(section.endpoint(), filters);
            if (points.isEmpty()) return null;
            for (SeriesPoint p : points) {
                dataset.addValue(p.value(), p.series(), p.label());
            }
        } else {
            List<Point> points = AnalyticsApi.points(section.endpoint(), filters);
            if (points.isEmpty()) return null;
            for (Point p : points) {
                dataset.addValue(p.value(), section.yLabel(), p.label());
            }
        }

        boolean overTime = section.xLabel().equals("Month");

        JFreeChart chart = overTime
                ? ChartFactory.createLineChart(
                        null, section.xLabel(), section.yLabel(),
                        dataset, PlotOrientation.VERTICAL, section.isSeries(), false, false)
                : ChartFactory.createBarChart(
                        null, section.xLabel(), section.yLabel(),
                        dataset, PlotOrientation.VERTICAL, section.isSeries(), false, false);

        style(chart);
        return chart;
    }

    private static void style(JFreeChart chart) {
        chart.setBackgroundPaint(Color.WHITE);

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(new Color(220, 224, 230));
        plot.setOutlinePaint(new Color(220, 224, 230));

        plot.getDomainAxis().setCategoryLabelPositions(
                CategoryLabelPositions.UP_45);
        plot.getDomainAxis().setMaximumCategoryLabelWidthRatio(5.0f);
    }

    // =====================================================
    // CSV
    // =====================================================

    public static void writeCsv(File target,
                                Map<String, String> filters,
                                String periodLabel,
                                Set<ReportSection> sections) throws Exception {

        try (PrintWriter out = new PrintWriter(target, "UTF-8")) {

            out.println("Dynamic Retail Dashboard - Business Overview Report");
            out.println("Period," + csv(periodLabel));
            out.println("Region," + csv(filters.getOrDefault("region", "All Regions")));
            out.println("Generated," + csv(LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
            out.println();

            if (sections.contains(ReportSection.KPI_TABLE)) {
                Kpis k = AnalyticsApi.overview(filters);
                out.println("Key Performance Indicators");
                out.println("Indicator,Value");
                out.println("Total Revenue," + k.revenue());
                out.println("Revenue Growth %," + (k.growth() == null 
                    ? "N/A" 
                    : k.growth()));
                out.println("Profit (net)," + k.profit());
                out.println("Gross Profit Margin %," + k.margin());
                out.println("Inventory Turnover," + k.turnover());
                out.println("Customer Retention %," + k.retention());
                out.println("Cost per Conversion," + k.costPerConversion());

                if (!k.profitIncludesMarketing() || k.turnoverRegionIgnored()) {
                    out.println();
                    out.println(csv("Note: region filter applied. Profit is gross; "
                            + "turnover and retention are whole-business."));
                }

                out.println();
            }

            for (ReportSection section : ReportSection.values()) {

                if (section.endpoint() == null) continue;
                if (!sections.contains(section)) continue;

                out.println(csv(section.label()));

                if (section.isSeries()) {
                    List<SeriesPoint> points =
                            AnalyticsApi.seriesPoints(section.endpoint(), filters);
                    if (points.isEmpty()) {
                        out.println("No data for this period");
                    } else {
                        out.println(csv(section.xLabel()) + ",Series," + csv(section.yLabel()));
                        for (SeriesPoint p : points) {
                            out.println(csv(p.label()) + "," + csv(p.series()) + "," + p.value());
                        }
                    }
                } else {
                    List<Point> points = AnalyticsApi.points(section.endpoint(), filters);
                    if (points.isEmpty()) {
                        out.println("No data for this period");
                    } else {
                        out.println(csv(section.xLabel()) + "," + csv(section.yLabel()));
                        for (Point p : points) {
                            out.println(csv(p.label()) + "," + p.value());
                        }
                    }
                }

                out.println();
            }
        }
    }

    /** Quotes a value if it contains a comma or quote. */
    private static String csv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
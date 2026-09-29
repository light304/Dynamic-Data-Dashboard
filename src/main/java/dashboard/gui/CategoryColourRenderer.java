package dashboard.gui;

import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BarRenderer;

import java.awt.Color;
import java.awt.Paint;
import java.util.function.Function;

/**
 * A bar renderer that colours each bar by its category name rather than
 * by its series.
 *
 * A single-series bar chart normally paints every bar the same colour.
 * Where the categories are things with an identity of their own - the
 * regions - each bar takes that identity's colour from Theme, so the same
 * region is the same colour wherever it appears.
 *
 * Shared by the on-screen charts (AnalyticsCharts) and the PDF
 * (ReportBuilder), which is what keeps the two matching.
 */
public final class CategoryColourRenderer extends BarRenderer {

    private final Function<String, Color> colourOf;

    public CategoryColourRenderer(Function<String, Color> colourOf) {
        this.colourOf = colourOf;
    }

    @Override
    public Paint getItemPaint(int row, int column) {

        CategoryPlot plot = getPlot();

        if (colourOf != null && plot != null && plot.getDataset() != null) {
            Color c = colourOf.apply(String.valueOf(plot.getDataset().getColumnKey(column)));
            if (c != null) return c;
        }

        return super.getItemPaint(row, column);
    }
}

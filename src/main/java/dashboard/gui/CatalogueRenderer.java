package dashboard.gui;

import dashboard.database.AnalyticsApi;
import dashboard.report.ChartCatalogue;
import dashboard.report.ChartSpec;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Draws a ChartSpec as a live Swing card.
 *
 * The twin of ReportBuilder.buildChart: both read the same spec, so a
 * chart cannot be titled one way on screen and another in the PDF.
 *
 * The chart title is drawn as a Swing label rather than handed to
 * JFreeChart, so the description can sit beneath it - the same header
 * the hand-written chart classes use.
 */
public final class CatalogueRenderer {

    private CatalogueRenderer() {}

    /** Every catalogue-drawn chart for a page, in catalogue order. */
    public static List<JPanel> cardsFor(ChartSpec.Page page,
                                        DashboardFilter filter,
                                        Component parent) throws Exception {

        List<JPanel> cards = new ArrayList<>();

        for (ChartSpec spec : ChartCatalogue.drawnOn(page)) {
            cards.add(card(spec, filter, parent));
        }

        return cards;
    }

    /** One card. Public so a page can place a single chart by id. */
    public static JPanel card(ChartSpec spec,
                              DashboardFilter filter,
                              Component parent) throws Exception {

        Map<String, String> params = filter.toParams();
        Consumer<String> click = drilldown(spec, filter, parent);
        String endpoint = spec.endpoint();

        JPanel card = switch (spec.kind()) {

            case BAR -> AnalyticsCharts.bar(
                    null, spec.xLabel(), spec.yLabel(),
                    relabel(AnalyticsApi.points(endpoint, params), filter.scope()),
                    seriesKey(spec), spec.horizontal(), click);

            case LINE -> AnalyticsCharts.line(
                    null, spec.xLabel(), spec.yLabel(),
                    relabel(AnalyticsApi.points(endpoint, params), filter.scope()),
                    seriesKey(spec), click);

            case GROUPED_BAR -> AnalyticsCharts.groupedBar(
                    null, spec.xLabel(), spec.yLabel(),
                    relabelSeries(AnalyticsApi.seriesPoints(endpoint, params), filter.scope()),
                    click);

            case MULTI_LINE -> AnalyticsCharts.multiLine(
                    null, spec.xLabel(), spec.yLabel(),
                    relabelSeries(AnalyticsApi.seriesPoints(endpoint, params), filter.scope()),
                    click);

            case PIE -> AnalyticsCharts.pie(
                    null,
                    AnalyticsApi.points(endpoint, params), click);

            case SCATTER -> AnalyticsCharts.scatter(
                    null, spec.xLabel(), spec.yLabel(),
                    AnalyticsApi.xyPoints(endpoint, params));

            // Never reached: the KPI table is REPORT_ONLY, so drawnOn()
            // filters it out. Present so the switch stays exhaustive.
            case KPI_TABLE -> AnalyticsCharts.messageCard(
                    spec.title(),
                    "The KPI summary is shown on the Overview page and in the PDF.");
        };

        addHeader(card, spec);
        addFooter(card, spec, filter);
        return card;
    }

    /**
     * Title and description, inside the white card, above the plot.
     *
     * AnalyticsCharts.wrap builds the card with BorderLayout and leaves
     * NORTH free - the chart sits at CENTER and the expand hint at SOUTH.
     */
    private static void addHeader(JPanel card, ChartSpec spec) {

        if (!(card.getLayout() instanceof BorderLayout)) return;

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(new EmptyBorder(2, 2, 8, 2));

        JLabel title = new JLabel(spec.title());
        title.setFont(Theme.CHART_TITLE);
        title.setForeground(Theme.TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(title);

        String description = spec.fullDescription();

        if (description != null && !description.isBlank()) {

            // JLabel will not wrap plain text, so the width is set in HTML.
            JLabel subtitle = new JLabel(
                    "<html><div style='width:430px'>" + description + "</div></html>");

            subtitle.setFont(Theme.SMALL);
            subtitle.setForeground(Theme.TEXT_MUTED);
            subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

            header.add(Box.createVerticalStrut(3));
            header.add(subtitle);
        }

        card.add(header, BorderLayout.NORTH);
    }

    private static String seriesKey(ChartSpec spec) {
        return spec.seriesName() == null || spec.seriesName().isBlank()
                ? spec.yLabel()
                : spec.seriesName();
    }

    /**
     * Turns the spec's Drilldown into the dialog call the old pages
     * wrote out by hand. Returning null means the chart is not clickable.
     */
    private static Consumer<String> drilldown(ChartSpec spec,
                                              DashboardFilter filter,
                                              Component parent) {

        Map<String, String> params = filter.toParams();

        return switch (spec.drilldown()) {

            case NONE -> null;

            case BY_MONTH -> label ->
                    DrilldownDialog.showSales(parent, label, null, filter.region());

            case BY_CATEGORY -> label ->
                    DrilldownDialog.showSales(parent, null, label, filter.region());

            case BY_REGION -> label ->
                    DrilldownDialog.showSales(parent, null, null, label);

            case BY_WAREHOUSE -> label ->
                    DrilldownDialog.showInventory(parent, label, params);

            case BY_CHANNEL -> label ->
                    DrilldownDialog.showMarketing(parent, label, params);
        };
    }

    /**
     * The filter breadcrumb, plus whatever note the spec adds.
     *
     * Was written out by hand in each of the four Sales chart classes.
     * Every card carries it now, so a reader always knows which slice of
     * data they are looking at.
     */
    private static void addFooter(JPanel card, ChartSpec spec, DashboardFilter filter) {

        if (!(card.getLayout() instanceof BorderLayout layout)) return;

        JLabel status = new JLabel(statusText(spec, filter));
        status.setFont(Theme.SMALL);
        status.setForeground(Theme.TEXT_MUTED);

        // AnalyticsCharts.wrap already put the "Double-click to expand"
        // hint at SOUTH, so it moves across rather than being replaced.
        Component hint = layout.getLayoutComponent(BorderLayout.SOUTH);

        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(status, BorderLayout.WEST);

        if (hint != null) {
            card.remove(hint);
            south.add(hint, BorderLayout.EAST);
        }

        card.add(south, BorderLayout.SOUTH);
    }

    private static String statusText(ChartSpec spec, DashboardFilter filter) {

        // Weekly names the month, because "Week 2" alone says nothing.
        String breadcrumb = "Weekly".equals(filter.scope())
                ? filter.year() + " • " + filter.month() + " • " + filter.period()
                : filter.year() + " • " + filter.scope() + " • " + filter.period();

        return spec.status() == null || spec.status().isBlank()
                ? breadcrumb
                : breadcrumb + " • " + spec.status();
    }

    private static List<AnalyticsApi.Point> relabel(
            List<AnalyticsApi.Point> points, String scope) {

        List<AnalyticsApi.Point> out = new ArrayList<>(points.size());
        for (AnalyticsApi.Point p : points) {
            out.add(new AnalyticsApi.Point(timeLabel(p.label(), scope), p.value()));
        }
        return out;
    }

    private static List<AnalyticsApi.SeriesPoint> relabelSeries(
            List<AnalyticsApi.SeriesPoint> points, String scope) {

        List<AnalyticsApi.SeriesPoint> out = new ArrayList<>(points.size());
        for (AnalyticsApi.SeriesPoint p : points) {
            out.add(new AnalyticsApi.SeriesPoint(
                    timeLabel(p.label(), scope), p.series(), p.value()));
        }
        return out;
    }

    /**
     * Turns a SQL date label into something readable on an axis.
     *
     *   2023-04     -> Apr
     *   2023-04-17  -> 17          (Monthly)
     *   2023-04-17  -> Mon 17      (Weekly)
     *
     * Anything that is not a date - a category, a region, a country -
     * falls through unchanged, so this is safe to apply everywhere.
     */
    private static String timeLabel(String raw, String scope) {

        if (raw == null) return "";

        try {
            if (raw.length() == 7) {
                return YearMonth.parse(raw).getMonth()
                        .getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            }

            LocalDate date = LocalDate.parse(raw);

            if ("Weekly".equals(scope)) {
                return date.getDayOfWeek()
                        .getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                        + " " + date.getDayOfMonth();
            }

            return String.valueOf(date.getDayOfMonth());

        } catch (Exception ex) {
            return raw;
        }
    }
}
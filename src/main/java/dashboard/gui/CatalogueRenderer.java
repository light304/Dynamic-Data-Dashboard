package dashboard.gui;

import dashboard.database.AnalyticsApi;
import dashboard.report.ChartCatalogue;
import dashboard.report.ChartData;
import dashboard.report.ChartSpec;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.awt.geom.Ellipse2D;
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
    public static JPanel card(ChartSpec catalogueSpec,
                              DashboardFilter filter,
                              Component parent) throws Exception {

        // "Months" or "Days", to match what the filter makes the chart plot.
        ChartSpec spec = catalogueSpec.forScope(filter.scope());

        Map<String, String> params = filter.toParams();
        Consumer<String> click = drilldown(spec, filter, parent);
        String endpoint = spec.endpoint();

        JPanel card = switch (spec.kind()) {

            case BAR -> AnalyticsCharts.bar(
                    null, spec.xLabel(), spec.yLabel(),
                    relabel(AnalyticsApi.points(endpoint, params), filter.scope()),
                    seriesKey(spec), spec.horizontal(), click,
                    spec.categoriesAreRegions() ? Theme::regionColour : null);

            case LINE -> AnalyticsCharts.line(
                    null, spec.xLabel(), spec.yLabel(),
                    relabel(AnalyticsApi.points(endpoint, params), filter.scope()),
                    seriesKey(spec), click);

            case AREA -> AnalyticsCharts.area(
                    null, spec.xLabel(), spec.yLabel(),
                    relabel(AnalyticsApi.points(endpoint, params), filter.scope()),
                    seriesKey(spec), click);

            case GROUPED_BAR -> AnalyticsCharts.groupedBar(
                    null, spec.xLabel(), spec.yLabel(),
                    relabelSeries(ChartData.series(spec, params), filter.scope()),
                    click);

            case MULTI_LINE -> AnalyticsCharts.multiLine(
                    null, spec.xLabel(), spec.yLabel(),
                    relabelSeries(ChartData.series(spec, params), filter.scope()),
                    click);

            case COMBO -> AnalyticsCharts.combo(
                    null, spec.xLabel(), spec.yLabel(),
                    relabelSeries(ChartData.series(spec, params), filter.scope()),
                    spec.seriesName(), click);

            case PIE -> AnalyticsCharts.pie(
                    null,
                    AnalyticsApi.points(endpoint, params), click);

            case RING -> AnalyticsCharts.ring(
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

        // The expanded window is opened from inside AnalyticsCharts, which
        // has no spec, so it reads its title and text from the card. Full
        // description here (what it shows + how to read it + scope), since
        // the expanded view has room and no (i) button of its own.
        card.putClientProperty(AnalyticsCharts.EXPAND_INFO,
                new String[]{spec.title(), spec.fullDescription()});

        addHeader(card, spec);
        addFooter(card, spec, filter);
        return card;
    }

    /**
     * Title and (i) button, inside the white card, above the plot.
     *
     * The description used to sit under the title and cost every card
     * about 60px of height. It now lives in the popup the (i) button opens,
     * which is what lets four charts share one screen.
     */
    private static void addHeader(JPanel card, ChartSpec spec) {

        if (!(card.getLayout() instanceof BorderLayout)) return;

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 2, 4, 2));

        JLabel title = new JLabel(spec.title());
        title.setFont(Theme.CARD_TITLE);
        title.setForeground(Theme.TEXT);

        header.add(title);
        header.add(infoButton(spec));

        card.add(header, BorderLayout.NORTH);
    }

    // ---------------------------------------------------------------
    // (i) info button and popup
    // ---------------------------------------------------------------

    private static JButton infoButton(ChartSpec spec) {

        JButton button = new JButton() {

            @Override
            protected void paintComponent(Graphics g) {

                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                boolean hot = getModel().isRollover() || getModel().isPressed();
                g2.setColor(hot ? Theme.ACCENT : Theme.TEXT_MUTED);

                int d = Math.min(getWidth(), getHeight()) - 3;
                g2.fill(new Ellipse2D.Double(1, 1, d, d));

                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 12));

                FontMetrics fm = g2.getFontMetrics();
                String letter = "i";
                int x = 1 + (d - fm.stringWidth(letter)) / 2;
                int y = 1 + (d - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(letter, x, y);

                g2.dispose();
            }
        };

        button.setPreferredSize(new Dimension(20, 20));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setRolloverEnabled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setToolTipText("What this chart shows and how to read it");
        button.getAccessibleContext().setAccessibleName("About " + spec.title());
        button.addActionListener(e -> showInfo(button, spec));

        return button;
    }

    private static void showInfo(Component anchor, ChartSpec spec) {

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.WHITE);
        body.setBorder(new EmptyBorder(12, 14, 12, 14));

        JLabel heading = new JLabel(spec.title());
        heading.setFont(Theme.CARD_TITLE);
        heading.setForeground(Theme.TEXT);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(heading);

        addInfoSection(body, "What it shows", spec.description());
        addInfoSection(body, "How to read it", spec.reading());
        addInfoSection(body, "Scope", spec.caveat());

        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        popup.add(body);
        popup.show(anchor, 0, anchor.getHeight() + 4);
    }

    private static void addInfoSection(JPanel body, String label, String text) {

        if (text == null || text.isBlank()) return;

        JLabel name = new JLabel(label.toUpperCase());
        name.setFont(Theme.SMALL_BOLD.deriveFont(11f));
        name.setForeground(Theme.TEXT_MUTED);
        name.setAlignmentX(Component.LEFT_ALIGNMENT);

        // JLabel will not wrap plain text, so the width is set in HTML.
        JLabel value = new JLabel(
                "<html><div style='width:340px'>" + text + "</div></html>");
        value.setFont(Theme.SMALL);
        value.setForeground(Theme.TEXT);
        value.setAlignmentX(Component.LEFT_ALIGNMENT);

        body.add(Box.createVerticalStrut(10));
        body.add(name);
        body.add(Box.createVerticalStrut(2));
        body.add(value);
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

        JLabel status = new JLabel(statusText(filter));
        status.setFont(Theme.SMALL.deriveFont(11f));
        status.setForeground(Theme.TEXT_MUTED);
        status.setBorder(new EmptyBorder(2, 2, 0, 0));

        // AnalyticsCharts already put the "Double-click to expand" hint at
        // SOUTH, so it moves across rather than being replaced.
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

    private static String statusText(DashboardFilter filter) {

        // Weekly names the month, because "Week 2" alone says nothing.
        return "Weekly".equals(filter.scope())
                ? filter.year() + " \u2022 " + filter.month() + " \u2022 " + filter.period()
                : filter.year() + " \u2022 " + filter.scope() + " \u2022 " + filter.period();
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
package dashboard.report;

/**
 * The single declaration of one dashboard chart.
 *
 * Read by three consumers:
 *   - the analytics pages, to draw the live Swing card
 *   - ReportBuilder, to draw the PDF version
 *   - ChartCatalogue, to group charts by page
 *
 * Because all three read this one object, a chart cannot be renamed
 * on the screen and stay wrongly named in the PDF.
 */
public record ChartSpec(
        String id,
        Page page,
        Kind kind,
        Source source,
        String title,
        String xLabel,
        String yLabel,
        String seriesName,
        String endpoint,
        String description,
        String caveat,
        Drilldown drilldown,
        boolean horizontal,
        String status
) {

    /** Which dashboard page the chart belongs to. */
    public enum Page {
        OVERVIEW,
        SALES,
        PRODUCTS,
        INVENTORY,
        MARKETING,
        CUSTOMERS
    }

    /**
     * How the chart is drawn.
     *
     * Replaces ReportBuilder's old guess, which decided between a line
     * and a bar by testing whether the x-axis label was the string "Month".
     */
    public enum Kind {
        BAR,
        LINE,
        GROUPED_BAR,
        MULTI_LINE,
        PIE,
        SCATTER,
        KPI_TABLE
    }

    /** Where the chart appears. */
    public enum Source {
        /** The catalogue draws it on the page and in the PDF. */
        CATALOGUE,
        /** Has a hand-written chart class on the page; the catalogue drives the PDF only. */
        PAGE_CUSTOM,
        /** Appears in the PDF only, not on any page. */
        REPORT_ONLY
    }

    /** What clicking a bar, point or slice opens. */
    public enum Drilldown {
        NONE,
        BY_MONTH,
        BY_CATEGORY,
        BY_REGION,
        BY_WAREHOUSE,
        BY_CHANNEL
    }

    /** Grouped and multi-series charts need seriesPoints() rather than points(). */
    public boolean isSeries() {
        return kind == Kind.GROUPED_BAR || kind == Kind.MULTI_LINE;
    }

    /** Scatters need xyPoints(). */
    public boolean isXy() {
        return kind == Kind.SCATTER;
    }

    /** True where the catalogue is responsible for drawing the live card. */
    public boolean drawnFromCatalogue() {
        return source == Source.CATALOGUE;
    }

    /**
     * A data table under the chart in the PDF suits a short category
     * list, not twelve months of readings and not a scatter of every
     * product.
     */
    public boolean showsDataTable() {
        if (kind != Kind.BAR && kind != Kind.PIE) return false;
        return !"Month".equals(xLabel);
    }

    /** Description plus any region caveat, for the PDF body text. */
    public String fullDescription() {
        return caveat == null || caveat.isBlank()
                ? description
                : description + " " + caveat;
    }

    // ---------------------------------------------------------------
    // Builder
    //
    // A record with thirteen fields is painful to construct positionally,
    // and adding a fourteenth later would break every call site. The
    // builder keeps the entries readable and additions cheap.
    // ---------------------------------------------------------------

    public static Builder of(String id, Page page, Kind kind) {
        return new Builder(id, page, kind);
    }

    public static final class Builder {

        private final String id;
        private final Page page;
        private final Kind kind;

        private Source source = Source.CATALOGUE;
        private String title = "";
        private String xLabel = "";
        private String yLabel = "";
        private String seriesName = null;
        private String endpoint = null;
        private String description = "";
        private String caveat = null;
        private Drilldown drilldown = Drilldown.NONE;
        private boolean horizontal = false;
        private String status = null;

        private Builder(String id, Page page, Kind kind) {
            this.id = id;
            this.page = page;
            this.kind = kind;
        }

        public Builder title(String value)        { this.title = value; return this; }
        public Builder endpoint(String value)     { this.endpoint = value; return this; }
        public Builder series(String value)       { this.seriesName = value; return this; }
        public Builder describe(String value)     { this.description = value; return this; }
        public Builder caveat(String value)       { this.caveat = value; return this; }
        public Builder source(Source value)       { this.source = value; return this; }
        public Builder drilldown(Drilldown value) { this.drilldown = value; return this; }
        public Builder status(String value)       { this.status = value; return this; }

        public Builder axes(String x, String y) {
            this.xLabel = x;
            this.yLabel = y;
            return this;
        }

        public Builder horizontal() {
            this.horizontal = true;
            return this;
        }

        public ChartSpec build() {
            return new ChartSpec(
                    id, page, kind, source, title, xLabel, yLabel,
                    seriesName, endpoint, description, caveat,
                    drilldown, horizontal, status
            );
        }
    }
}
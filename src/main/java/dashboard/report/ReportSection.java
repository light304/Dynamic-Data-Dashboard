package dashboard.report;

/**
 * One optional block in a generated report.
 *
 * `endpoint` is null for the KPI table, which comes from the overview
 * endpoint rather than a chart endpoint.
 */
public enum ReportSection {

        KPI_TABLE("KPI summary table",
            "All seven key performance indicators for the selected period, with a "
            + "note on any figure that could not be filtered by region.",
            null, null, null, true),

    REVENUE_TREND("Revenue over Time",
            "Total sales revenue by month across the selected period. The series "
            + "shows the direction of trading performance and any seasonal pattern; "
            + "a sustained break from that pattern is worth investigating. "
            + "Reflects the selected region.",
            "api/sales/revenue-trend", "Month", "Revenue ($)", true),

    REVENUE_REGION("Revenue by Region",
            "Revenue split across trading regions. The spread shows how concentrated "
            + "the revenue base is; a large gap between comparable regions may reflect "
            + "differences in market maturity or local performance. "
            + "Always shows all regions.",
            "api/sales/revenue-region", "Region", "Revenue ($)", true),

    REVENUE_CATEGORY("Revenue by Category",
            "Revenue contribution of each product category. The ranking shows where "
            + "trading volume sits. Revenue is not the same as profit, so this should "
            + "be read alongside realised margin. Reflects the selected region.",
            "api/products/revenue-category", "Category", "Revenue ($)", true),

    STOCK_COVER("Stock Cover by Category (Weeks)",
            "Weeks of stock held relative to the current rate of sale, based on "
            + "average stock across the period. High cover means capital tied up in "
            + "slow-moving inventory; low cover means exposure to stockouts. "
            + "Whole business only, as inventory is not held by region.",
            "api/inventory/stock-cover-weeks", "Category", "Weeks of Cover", true),

    PROFIT_MARGIN("Profit Margin over Time",
            "Gross profit as a percentage of revenue, before marketing. A declining "
            + "series means cost of goods is rising faster than price, regardless of "
            + "sales volume. Reflects the selected region.",
            "api/sales/profit-margin", "Month", "Margin %", false),

    COST_PER_CONVERSION("Cost per Conversion by Channel",
            "Marketing spend per recorded conversion, by channel. Lower means more "
            + "efficient acquisition. The source data does not link revenue to "
            + "campaigns, so this measures media efficiency rather than return. "
            + "Whole business only, as marketing is not recorded by region.",
            "api/marketing/cost-conversion", "Channel", "Cost / Conversion ($)", false),

    NEW_SIGNUPS("New Customers by Signup Month",
            "Customer registrations by month. Read against retention, this separates "
            + "genuine growth from replacement of lapsed customers. Whole business "
            + "only, as customer records do not carry a region.",
            "api/customers/new-signups", "Month", "Customers", false);

    private final String label;
    private final String description;
    private final String endpoint;
    private final String xLabel;
    private final String yLabel;
    private final boolean onByDefault;

    ReportSection(String label, String description, String endpoint,
                  String xLabel, String yLabel, boolean onByDefault) {
        this.label = label;
        this.description = description;
        this.endpoint = endpoint;
        this.xLabel = xLabel;
        this.yLabel = yLabel;
        this.onByDefault = onByDefault;
    }

    public String label()      { return label; }
    public String description() { return description; }
    public String endpoint()   { return endpoint; }
    public String xLabel()     { return xLabel; }
    public String yLabel()     { return yLabel; }
    public boolean onByDefault() { return onByDefault; }

    /** Stock cover returns label/series/value rather than label/value. */
    public boolean isSeries() { return false; }

    /** Tables suit short category lists, not twelve months of data. */
    public boolean showsDataTable() {
        return endpoint != null && !isSeries() && !"Month".equals(xLabel);
    }
}
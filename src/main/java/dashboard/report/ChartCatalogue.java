package dashboard.report;

import dashboard.report.ChartSpec.Drilldown;
import dashboard.report.ChartSpec.Kind;
import dashboard.report.ChartSpec.Page;
import dashboard.report.ChartSpec.Source;

import java.util.List;
import java.util.Optional;

/**
 * Every chart in the dashboard, declared once.
 *
 * To add a chart: add one entry here. It appears on its page and in
 * that page's PDF, with no other file touched.
 *
 * To retire a chart: delete its entry.
 *
 * Titles here are the single source. They previously carried a suffix
 * naming the tables each query joins - "(products + sales)" - which is
 * useful information but belongs in the description, not the heading.
 */
public final class ChartCatalogue {

    private ChartCatalogue() {}

    /** The KPI summary block. Not a chart, so it carries no endpoint. */
    public static final ChartSpec KPI_TABLE =
            ChartSpec.of("overview.kpi-table", Page.OVERVIEW, Kind.KPI_TABLE)
                    .source(Source.REPORT_ONLY)
                    .title("Key Performance Indicators")
                    .describe("All seven headline indicators for the selected period, "
                            + "with a note on any figure that could not be filtered "
                            + "by region.")
                    .build();

    private static final List<ChartSpec> ALL = List.of(

            KPI_TABLE,

            // =========================================================
            // SALES
            //
            // Drawn by CatalogueRenderer like every other page. The four
            // hand-written chart classes these replaced carried their own
            // titles and status text, which the catalogue now owns.
            // =========================================================

            ChartSpec.of("sales.revenue-trend", Page.SALES, Kind.LINE)
                    .title("Revenue Over Time")
                    // "Period", not "Month": at Monthly or Weekly scope the
                    // endpoint groups by day and the axis shows day numbers.
                    .axes("Period", "Revenue ($)")
                    .series("Revenue")
                    .endpoint("api/sales/revenue-trend")
                    .describe("Total sales revenue by month across the selected period. "
                            + "The series shows the direction of trading performance and "
                            + "any seasonal pattern; a sustained break from that pattern "
                            + "is worth investigating.")
                    .caveat("Reflects the selected region.")
                    .drilldown(Drilldown.BY_MONTH)
                    .status("Live sales data")
                    .build(),

            ChartSpec.of("sales.revenue-region", Page.SALES, Kind.BAR)
                    .title("Revenue by Region")
                    .axes("Region", "Revenue ($)")
                    .series("Revenue")
                    .endpoint("api/sales/revenue-region")
                    .describe("Revenue split across trading regions. The spread shows how "
                            + "concentrated the revenue base is; a large gap between "
                            + "comparable regions may reflect differences in market "
                            + "maturity or local performance.")
                    .caveat("Always shows every region, regardless of the region filter.")
                    .drilldown(Drilldown.BY_REGION)
                    .horizontal()
                    .status("Live filtered regional revenue")
                    .build(),

            ChartSpec.of("sales.profit-margin", Page.SALES, Kind.LINE)
                    .title("Profit Margin Over Time")
                    .axes("Period", "Margin %")
                    .series("Margin %")
                    .endpoint("api/sales/profit-margin")
                    .describe("Gross profit as a percentage of revenue, before marketing. "
                            + "Combines sales with product costs. A declining series means "
                            + "cost of goods is rising faster than price, regardless of "
                            + "sales volume.")
                    .caveat("Reflects the selected region.")
                    .drilldown(Drilldown.BY_MONTH)
                    .status("cross-table KPI")
                    .build(),

            ChartSpec.of("sales.quantity-revenue", Page.SALES, Kind.SCATTER)
                    .title("Quantity vs Revenue by Product")
                    .axes("Units Sold", "Revenue ($)")
                    .endpoint("api/sales/quantity-revenue")
                    .describe("Each point is one product, plotting units sold against the "
                            + "revenue it earned over the period, coloured by category. A "
                            + "tight diagonal band means consistent pricing; points well "
                            + "below it are high-volume low-value lines, and a wide "
                            + "vertical spread at one quantity indicates price variation "
                            + "for the same order size, which is usually discounting.")
                    .caveat("Reflects the selected region.")
                    .status("one point per product")
                    .build(),

            // =========================================================
            // PRODUCTS
            // =========================================================

            ChartSpec.of("products.price-cost", Page.PRODUCTS, Kind.SCATTER)
                    .title("Price vs Cost")
                    .axes("Price ($)", "Cost ($)")
                    .endpoint("api/products/price-cost")
                    .describe("Each point is one product, plotting its list price against "
                            + "its unit cost. The vertical distance above the diagonal is "
                            + "the catalogue margin on that item. Points close to the "
                            + "diagonal earn almost nothing per unit and only make sense "
                            + "at volume. This is a view of the price list, not of what "
                            + "sold.")
                    .caveat("Whole catalogue; products are not held by region or period.")
                    .build(),

            ChartSpec.of("products.catalogue-margin", Page.PRODUCTS, Kind.BAR)
                    .title("Catalogue Margin by Category")
                    .axes("Category", "Margin %")
                    .series("Catalogue Margin %")
                    .endpoint("api/products/catalogue-margin")
                    .describe("The margin each category would earn at list price, from the "
                            + "product table alone. This is the intended margin rather "
                            + "than the achieved one, so it is the baseline to read "
                            + "realised margin against.")
                    .caveat("Whole catalogue; products are not held by region or period.")
                    .build(),

            ChartSpec.of("products.realised-margin", Page.PRODUCTS, Kind.BAR)
                    .title("Realised Margin by Category")
                    .axes("Category", "Margin %")
                    .series("Realised Margin %")
                    .endpoint("api/products/realised-margin")
                    .describe("The margin each category actually achieved on what sold, "
                            + "combining product costs with sales. Read directly against "
                            + "catalogue margin: a realised figure below the catalogue "
                            + "figure means discounting, or that the lower-margin items in "
                            + "the category are the ones selling.")
                    .caveat("Reflects the selected region.")
                    .drilldown(Drilldown.BY_CATEGORY)
                    .build(),

            ChartSpec.of("products.revenue-category", Page.PRODUCTS, Kind.BAR)
                    .title("Revenue by Category")
                    .axes("Category", "Revenue ($)")
                    .series("Revenue")
                    .endpoint("api/products/revenue-category")
                    .describe("Revenue contribution of each product category, combining "
                            + "products with sales. The ranking shows where trading volume "
                            + "sits. Revenue is not the same as profit, so read it "
                            + "alongside realised margin.")
                    .caveat("Reflects the selected region.")
                    .drilldown(Drilldown.BY_CATEGORY)
                    .horizontal()
                    .build(),

            // =========================================================
            // INVENTORY
            // =========================================================

            ChartSpec.of("inventory.stock-cover-weeks", Page.INVENTORY, Kind.BAR)
                    .title("Weeks of Stock Cover by Category")
                    .axes("Category", "Weeks of Coverage")
                    .series("Weeks of Cover")
                    .endpoint("api/inventory/stock-cover-weeks")
                    .describe("How many weeks the current stock would last at the present "
                            + "rate of sale, based on average stock across the period and "
                            + "combining inventory, products and sales. High cover means "
                            + "capital tied up in slow-moving stock; low cover means "
                            + "exposure to stockouts. What counts as healthy differs by "
                            + "category, so compare each against its own history.")
                    .caveat("Whole business only, as inventory is not held by region.")
                    .drilldown(Drilldown.BY_CATEGORY)
                    .build(),

            ChartSpec.of("inventory.stock-trend", Page.INVENTORY, Kind.LINE)
                    .title("Stock Level Over Time")
                    .axes("Month", "Average Stock Level")
                    .series("Average Stock")
                    .endpoint("api/inventory/stock-trend")
                    .describe("Average units held by month. Read against the revenue "
                            + "trend: stock rising while revenue is flat means working "
                            + "capital is accumulating, and stock falling while revenue "
                            + "holds means the buffer is being consumed rather than "
                            + "replaced.")
                    .caveat("Whole business only, as inventory is not held by region.")
                    .build(),

            ChartSpec.of("inventory.turnover", Page.INVENTORY, Kind.LINE)
                    .title("Inventory Turnover Over Time")
                    .axes("Month", "Turnover")
                    .series("Inventory Turnover")
                    .endpoint("api/inventory/turnover")
                    .describe("How many times the average stock holding was sold through "
                            + "in each month, combining inventory, products and sales. "
                            + "Rising turnover means stock is working harder for the same "
                            + "capital. A falling series with steady revenue points to "
                            + "overbuying rather than to a demand problem.")
                    .caveat("Whole business only, as inventory is not held by region.")
                    .drilldown(Drilldown.BY_MONTH)
                    .build(),

            ChartSpec.of("inventory.stock-cover", Page.INVENTORY, Kind.GROUPED_BAR)
                    .title("Stock Held vs Sold by Category")
                    .axes("Category", "Units")
                    .endpoint("api/inventory/stock-cover")
                    .describe("Units held set beside units sold for each category, so "
                            + "cover can be read in absolute terms rather than as a ratio. "
                            + "A tall stock bar beside a short sales bar is the clearest "
                            + "picture of overstock; the reverse is a category being run "
                            + "close to empty.")
                    .caveat("Whole business only, as inventory is not held by region.")
                    .drilldown(Drilldown.BY_CATEGORY)
                    .build(),

            // =========================================================
            // MARKETING
            // =========================================================

            ChartSpec.of("marketing.spend-channel", Page.MARKETING, Kind.PIE)
                    .title("Marketing Spend by Channel")
                    .axes("Channel", "Spend ($)")
                    .endpoint("api/marketing/spend-channel")
                    .describe("How the marketing budget was divided between channels. This "
                            + "shows commitment, not return: read it against cost per "
                            + "conversion by channel, because the largest slice is only "
                            + "justified if its acquisition cost holds up.")
                    .caveat("Whole business only, as marketing is not recorded by region.")
                    .drilldown(Drilldown.BY_CHANNEL)
                    .build(),

            ChartSpec.of("marketing.cost-conversion", Page.MARKETING, Kind.BAR)
                    .source(Source.REPORT_ONLY)
                    .title("Cost per Conversion by Channel")
                    .axes("Channel", "Cost / Conversion ($)")
                    .series("Cost / Conversion")
                    .endpoint("api/marketing/cost-conversion")
                    .describe("Marketing spend per recorded conversion, by channel. Lower "
                            + "means more efficient acquisition. The source data does not "
                            + "link revenue to campaigns, so this measures media "
                            + "efficiency rather than return.")
                    .caveat("Whole business only, as marketing is not recorded by region.")
                    .build(),

            ChartSpec.of("marketing.cost-per-conversion-trend", Page.MARKETING, Kind.LINE)
                    .title("Cost per Conversion Over Time")
                    .axes("Month", "Cost per Conversion ($)")
                    .series("Cost per Conversion")
                    .endpoint("api/marketing/cost-per-conversion-trend")
                    .describe("Acquisition cost by month across all channels. This is the "
                            + "one headline measure where a rising line is bad news. A "
                            + "steady climb usually means the cheap audience is exhausted "
                            + "and spend is reaching people less likely to convert.")
                    .caveat("Whole business only, as marketing is not recorded by region.")
                    .build(),

            ChartSpec.of("marketing.spend-revenue", Page.MARKETING, Kind.MULTI_LINE)
                    .title("Marketing Spend vs Revenue")
                    .axes("Month", "Value ($)")
                    .endpoint("api/marketing/spend-revenue")
                    .describe("Marketing spend and total revenue on the same monthly axis. "
                            + "The two series move together when spend is working, but the "
                            + "source data does not link a campaign to an order, so this "
                            + "shows coincidence in time and not attribution. Treat a gap "
                            + "that opens between the lines as a question, not a finding.")
                    .caveat("Whole business only, as marketing is not recorded by region.")
                    .drilldown(Drilldown.BY_MONTH)
                    .build(),

            // =========================================================
            // CUSTOMERS
            // =========================================================

            ChartSpec.of("customers.new-signups", Page.CUSTOMERS, Kind.BAR)
                    .title("New Customers by Month")
                    .axes("Month", "Customers")
                    .series("New Customers")
                    .endpoint("api/customers/new-signups")
                    .describe("Customer registrations by month. Read against retention, "
                            + "this separates genuine growth from replacement of lapsed "
                            + "customers.")
                    .caveat("Whole business only, as customer records do not carry a region.")
                    .build(),

            ChartSpec.of("customers.country-breakdown", Page.CUSTOMERS, Kind.BAR)
                    .title("Customers by Country")
                    .axes("Country", "Customers")
                    .series("Customers")
                    .endpoint("api/customers/country-breakdown")
                    .describe("Where the customer base is registered. This counts people, "
                            + "not money, so a country with many customers and little "
                            + "revenue is a different situation from one with few "
                            + "customers and a lot - compare it against revenue by "
                            + "country before drawing a conclusion.")
                    .caveat("Whole business only, as customer records do not carry a region.")
                    .build(),

            ChartSpec.of("customers.retention", Page.CUSTOMERS, Kind.LINE)
                    .title("Customer Retention Over Time")
                    .axes("Month", "Retention %")
                    .series("Retention %")
                    .endpoint("api/customers/retention")
                    .describe("The share of the previous month's active customers who "
                            + "bought again. Retention compounds, so a few points of "
                            + "sustained decline costs more revenue over a year than a "
                            + "single bad month of acquisition. Note this is month on "
                            + "month, and so is not the same measure as the Customer "
                            + "Retention indicator on the Overview page.")
                    .caveat("Reflects the selected region.")
                    .drilldown(Drilldown.BY_MONTH)
                    .build(),

            ChartSpec.of("customers.revenue-segment", Page.CUSTOMERS, Kind.BAR)
                    .title("Revenue by Customer Country")
                    .axes("Country", "Revenue ($)")
                    .series("Revenue")
                    .endpoint("api/customers/revenue-segment")
                    .describe("Revenue traced back to the country each customer is "
                            + "registered in. Set beside the customer count by country, "
                            + "this gives revenue per customer by market and shows which "
                            + "markets are large because they are populous and which are "
                            + "large because they spend.")
                    .caveat("Segments customers by their own country, which is not the "
                            + "same as the sales region filter.")
                    .build(),

            // =========================================================
            // OVERVIEW
            //
            // Overview's other three charts are the same specs that
            // Sales, Products and Marketing own - see forOverview().
            // Only this one is unique to the page.
            // =========================================================

            ChartSpec.of("overview.stock-warehouse", Page.OVERVIEW, Kind.BAR)
                    .title("Stock by Warehouse")
                    .axes("Warehouse", "Units in Stock")
                    .series("Stock")
                    .endpoint("api/inventory/stock-warehouse")
                    .describe("Units held at each warehouse. Read against stock cover: a "
                            + "warehouse holding a large share of total stock concentrates "
                            + "both the capital and the fulfilment risk in one place.")
                    .caveat("Whole business only, as inventory is not held by region.")
                    .drilldown(Drilldown.BY_WAREHOUSE)
                    .build()
    );

    // =========================================================
    // LOOKUPS
    // =========================================================

    public static List<ChartSpec> all() {
        return ALL;
    }

    /** Charts the catalogue is responsible for drawing on the given page. */
    public static List<ChartSpec> drawnOn(Page page) {
        return ALL.stream()
                .filter(spec -> spec.page() == page)
                .filter(ChartSpec::drawnFromCatalogue)
                .toList();
    }

    /**
     * Everything that belongs in the given page's PDF, including charts
     * the page draws by hand and charts that appear in reports only.
     */
    public static List<ChartSpec> reportFor(Page page) {
        if (page == Page.OVERVIEW) return forOverview();

        return ALL.stream()
                .filter(spec -> spec.page() == page)
                .filter(spec -> spec.endpoint() != null)
                .toList();
    }

    /**
     * The Overview report: the KPI table, then the four charts the page
     * shows.
     *
     * Three of those four belong to other pages, so they are named here
     * by id rather than duplicated - one chart, one spec, wherever it
     * happens to be displayed.
     */
    public static List<ChartSpec> forOverview() {
        return List.of(
                KPI_TABLE,
                require("sales.revenue-trend"),
                require("sales.revenue-region"),
                require("products.revenue-category"),
                require("overview.stock-warehouse"),
                require("marketing.spend-channel")
        );
    }

    public static Optional<ChartSpec> byId(String id) {
        return ALL.stream().filter(spec -> spec.id().equals(id)).findFirst();
    }

    private static ChartSpec require(String id) {
        return byId(id).orElseThrow(
                () -> new IllegalStateException("Unknown chart id: " + id));
    }

    /** The Overview page's four charts, without the KPI table. */
    public static List<ChartSpec> overviewCharts() {
        return forOverview().stream()
                .filter(spec -> spec.kind() != Kind.KPI_TABLE)
                .toList();
    }
}
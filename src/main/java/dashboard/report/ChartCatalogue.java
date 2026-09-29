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
            // SALES  (area, ranked bars, line, scatter)
            // =========================================================

            ChartSpec.of("sales.revenue-trend", Page.SALES, Kind.AREA)
                    .title("Revenue Over Time")
                    // TIME_AXIS becomes "Months" or "Days" to match the filter
                    // (see ChartSpec.forScope): at Monthly or Weekly scope the
                    // endpoint groups by day.
                    .axes(ChartSpec.TIME_AXIS, "Revenue ($)")
                    .series("Revenue")
                    .endpoint("api/sales/revenue-trend")
                    .describe("Total sales revenue in each month (or day, at Monthly "
                            + "and Weekly scope) of the selected period.")
                    .reading("Height is the amount earned. Look for the overall direction "
                            + "and any repeating seasonal shape; a dip or spike that breaks "
                            + "the pattern is worth investigating. Click a point to see "
                            + "the orders behind it.")
                    .caveat("Reflects the selected region.")
                    .drilldown(Drilldown.BY_MONTH)
                    .status("Live sales data")
                    .build(),

            ChartSpec.of("sales.revenue-region", Page.SALES, Kind.BAR)
                    .title("Revenue by Region")
                    .axes("Region", "Revenue ($)")
                    .series("Revenue")
                    .endpoint("api/sales/revenue-region")
                    .describe("Revenue earned in each trading region over the selected "
                            + "period.")
                    .reading("Longer bar means more revenue. The gaps show how concentrated "
                            + "the business is: a large gap between similar-sized regions "
                            + "points to a difference in local performance. Click a bar "
                            + "to see that region's orders.")
                    .caveat("Always shows every region, regardless of the region filter.")
                    .drilldown(Drilldown.BY_REGION)
                    .horizontal()
                    .status("Live filtered regional revenue")
                    .build(),

            ChartSpec.of("sales.profit-margin", Page.SALES, Kind.LINE)
                    .title("Gross Profit Margin")
                    .axes(ChartSpec.TIME_AXIS, "Margin %")
                    .series("Margin %")
                    .endpoint("api/sales/profit-margin")
                    .describe("Gross profit as a percentage of revenue (before marketing) "
                            + "in each month or day, from sales combined with product "
                            + "costs.")
                    .reading("A rising line means each sale keeps more of its price. A "
                            + "falling line means the cost of goods is growing faster than "
                            + "prices, whatever the sales volume. The axis is zoomed to the "
                            + "data, so small moves look bigger than they are - read the "
                            + "numbers on the left.")
                    .caveat("Reflects the selected region.")
                    .drilldown(Drilldown.BY_MONTH)
                    .status("cross-table KPI")
                    .build(),

            ChartSpec.of("sales.quantity-revenue", Page.SALES, Kind.SCATTER)
                    .title("Units Sold vs Revenue by Product")
                    .axes("Units Sold", "Revenue ($)")
                    .endpoint("api/sales/quantity-revenue")
                    .describe("Each dot is one product: how many units it sold against "
                            + "the revenue it earned, coloured by category.")
                    .reading("Dots toward the top-right are the best sellers on both "
                            + "counts. A dot high above its neighbours at the same units "
                            + "is a premium-priced product; one well below them is high "
                            + "volume but low value. Hover a dot for its product ID.")
                    .caveat("Reflects the selected region.")
                    .status("one point per product")
                    .build(),

            // =========================================================
            // PRODUCTS  (scatter, grouped bars, ring)
            //
            // Catalogue Margin and Realised Margin were two separate bar
            // charts that only make sense read against each other, so they
            // are now one grouped chart.
            // =========================================================

            ChartSpec.of("products.price-cost", Page.PRODUCTS, Kind.SCATTER)
                    .title("Price vs Cost")
                    .axes("Price ($)", "Cost ($)")
                    .endpoint("api/products/price-cost")
                    .describe("Each dot is one product, plotting its list price against "
                            + "its unit cost.")
                    .reading("The gap between a dot and the price axis is its cost; the "
                            + "space above it is the profit per unit. Dots hugging the "
                            + "diagonal earn almost nothing per unit and only make sense "
                            + "at volume. This is the price list, not what sold.")
                    .caveat("Whole catalogue; products are not held by region or period.")
                    .build(),

            ChartSpec.of("products.margin-compare", Page.PRODUCTS, Kind.GROUPED_BAR)
                    .title("Catalogue vs Realised Margin")
                    .axes("Category", "Margin %")
                    .series("Catalogue margin")
                    .endpoint("api/products/catalogue-margin")
                    .second("api/products/realised-margin", "Realised margin")
                    .describe("For each category, the margin it should earn at list price "
                            + "(catalogue) beside the margin it actually earned on what "
                            + "sold (realised).")
                    .reading("If the realised bar is shorter than the catalogue bar, "
                            + "discounting is eating margin, or the lower-margin items in "
                            + "the category are the ones selling. Click a category to see "
                            + "its orders.")
                    .caveat("Realised margin reflects the selected region; catalogue "
                            + "margin is the whole catalogue.")
                    .drilldown(Drilldown.BY_CATEGORY)
                    .build(),

            ChartSpec.of("products.revenue-category", Page.PRODUCTS, Kind.RING)
                    .title("Revenue Share by Category")
                    .axes("Category", "Revenue ($)")
                    .series("Revenue")
                    .endpoint("api/products/revenue-category")
                    .describe("How total revenue divides between the product categories.")
                    .reading("A bigger slice means a bigger share of income. Revenue is "
                            + "not profit, so read it beside the realised margin chart: "
                            + "a large slice with a thin margin earns less than it looks. "
                            + "Click a slice to see that category's orders.")
                    .caveat("Reflects the selected region.")
                    .drilldown(Drilldown.BY_CATEGORY)
                    .build(),

            // =========================================================
            // INVENTORY  (combo, bar, horizontal bar)
            //
            // Stock Level and Turnover are now one dual-axis chart. Stock
            // Held vs Sold was retired: it showed the same held-against-sold
            // comparison as Weeks of Stock Cover, only as raw units.
            // =========================================================

            ChartSpec.of("inventory.stock-turnover", Page.INVENTORY, Kind.COMBO)
                    .title("Stock Level and Turnover")
                    .axes("Month", "Avg Stock (units)|Turnover (x)")
                    .series("Average stock")
                    .endpoint("api/inventory/stock-trend")
                    .second("api/inventory/turnover", "Turnover")
                    .describe("Columns (left axis) are the average units held each month. "
                            + "The line (right axis) is turnover: how many times that "
                            + "stock was sold through in the month.")
                    .reading("Stock rising while turnover falls means stock is piling up "
                            + "faster than it sells - a sign of overbuying rather than "
                            + "weak demand. Both rising together means the business is "
                            + "growing into its stock.")
                    .caveat("Whole business only, as inventory is not held by region.")
                    .drilldown(Drilldown.BY_MONTH)
                    .build(),

            ChartSpec.of("inventory.stock-cover-weeks", Page.INVENTORY, Kind.BAR)
                    .title("Weeks of Stock Cover")
                    .axes("Category", "Weeks of Coverage")
                    .series("Weeks of Cover")
                    .endpoint("api/inventory/stock-cover-weeks")
                    .describe("How many weeks the average stock in each category would "
                            + "last at its current rate of sale.")
                    .reading("A tall bar means capital tied up in slow-moving stock; a "
                            + "short bar means stockouts are a risk. What counts as "
                            + "healthy differs by category, so compare each with its own "
                            + "history. Click a bar for the inventory records.")
                    .caveat("Whole business only, as inventory is not held by region.")
                    .drilldown(Drilldown.BY_CATEGORY)
                    .build(),

            ChartSpec.of("inventory.stock-warehouse", Page.INVENTORY, Kind.BAR)
                    .title("Average Stock by Warehouse")
                    .axes("Warehouse", "Avg Stock (units)")
                    .series("Average stock")
                    .endpoint("api/inventory/stock-warehouse")
                    .describe("Average units held per stock record at each warehouse "
                            + "over the selected period.")
                    .reading("Similar bars mean stock is spread evenly. A warehouse "
                            + "holding noticeably more concentrates both the capital "
                            + "and the fulfilment risk in one place. Click a bar for "
                            + "that warehouse's records.")
                    .caveat("Whole business only, as inventory is not held by region.")
                    .drilldown(Drilldown.BY_WAREHOUSE)
                    .horizontal()
                    .build(),

            // =========================================================
            // MARKETING  (ring, bar, line, combo)
            // =========================================================

            ChartSpec.of("marketing.spend-channel", Page.MARKETING, Kind.RING)
                    .title("Marketing Spend by Channel")
                    .axes("Channel", "Spend ($)")
                    .endpoint("api/marketing/spend-channel")
                    .describe("How the marketing budget was divided between channels "
                            + "over the selected period.")
                    .reading("A bigger slice is a bigger commitment, not a better return. "
                            + "Compare it with cost per conversion: the largest slice is "
                            + "only justified if its acquisition cost holds up. Click a "
                            + "slice for that channel's campaigns.")
                    .caveat("Whole business only, as marketing is not recorded by region.")
                    .drilldown(Drilldown.BY_CHANNEL)
                    .build(),

            // Was PDF-only; promoted to the page because the ring above
            // says where the money went and this says what it bought.
            ChartSpec.of("marketing.cost-conversion", Page.MARKETING, Kind.BAR)
                    .title("Cost per Conversion by Channel")
                    .axes("Channel", "Cost / Conversion ($)")
                    .series("Cost / Conversion")
                    .endpoint("api/marketing/cost-conversion")
                    .describe("Marketing spend divided by conversions for each channel.")
                    .reading("Lower is better: a shorter bar acquires customers more "
                            + "cheaply. The data does not link revenue to campaigns, so "
                            + "this measures media efficiency, not profit return.")
                    .caveat("Whole business only, as marketing is not recorded by region.")
                    .drilldown(Drilldown.BY_CHANNEL)
                    .build(),

            ChartSpec.of("marketing.cost-per-conversion-trend", Page.MARKETING, Kind.LINE)
                    .title("Cost per Conversion Over Time")
                    .axes("Month", "Cost per Conversion ($)")
                    .series("Cost per Conversion")
                    .endpoint("api/marketing/cost-per-conversion-trend")
                    .describe("Average marketing spend needed to win one conversion, by "
                            + "month, across all channels.")
                    .reading("This is the one headline line where rising is bad news. A "
                            + "steady climb usually means the cheap audience is used up "
                            + "and spend is reaching people less likely to convert. The "
                            + "axis is zoomed to the data - read the numbers on the left.")
                    .caveat("Whole business only, as marketing is not recorded by region.")
                    .build(),

            // Two lines on one axis put spend (thousands) flat against
            // revenue (millions). Two axes let both be read.
            ChartSpec.of("marketing.spend-revenue", Page.MARKETING, Kind.COMBO)
                    .title("Marketing Spend vs Revenue")
                    .axes("Month", "Spend ($)|Revenue ($)")
                    .series("Marketing Spend")
                    .endpoint("api/marketing/spend-revenue")
                    .describe("Columns (left axis) are marketing spend each month. The "
                            + "line (right axis) is total revenue for the same month. "
                            + "The axes differ in scale so both can be read.")
                    .reading("If spend is working, the line should rise after the "
                            + "columns do. The data does not link a campaign to an "
                            + "order, so this shows coincidence in time, not cause. "
                            + "Treat a gap that opens up as a question, not a finding.")
                    .caveat("Whole business only, as marketing is not recorded by region.")
                    .drilldown(Drilldown.BY_MONTH)
                    .build(),

            // =========================================================
            // CUSTOMERS  (bar, ring, line, horizontal bar)
            // =========================================================

            ChartSpec.of("customers.new-signups", Page.CUSTOMERS, Kind.BAR)
                    .title("New Customers by Month")
                    .axes("Month", "Customers")
                    .series("New Customers")
                    .endpoint("api/customers/new-signups")
                    .describe("How many customers registered in each month.")
                    .reading("Read against retention: strong sign-ups with falling "
                            + "retention means growth is only replacing lapsed customers.")
                    .caveat("Whole business only, as customer records do not carry a region.")
                    .build(),

            ChartSpec.of("customers.country-breakdown", Page.CUSTOMERS, Kind.RING)
                    .title("Customer Base by Country")
                    .axes("Country", "Customers")
                    .series("Customers")
                    .endpoint("api/customers/country-breakdown")
                    .describe("Each country's share of all registered customers.")
                    .reading("This counts people, not money. Compare it with revenue by "
                            + "country: a country with many customers but little revenue "
                            + "is a different situation from one with few customers who "
                            + "spend a lot.")
                    .caveat("Whole business only, as customer records do not carry a region.")
                    .build(),

            ChartSpec.of("customers.retention", Page.CUSTOMERS, Kind.LINE)
                    .title("Customer Retention Over Time")
                    .axes("Month", "Retention %")
                    .series("Retention %")
                    .endpoint("api/customers/retention")
                    .describe("The share of the previous month's active customers who "
                            + "bought again this month.")
                    .reading("Retention compounds, so a few points of sustained decline "
                            + "costs more over a year than one weak month of sign-ups. "
                            + "Month on month, so not the same measure as the Customer "
                            + "Retention figure on the Overview. The axis is zoomed to "
                            + "the data - read the numbers on the left.")
                    .caveat("Reflects the selected region.")
                    .drilldown(Drilldown.BY_MONTH)
                    .build(),

            ChartSpec.of("customers.revenue-segment", Page.CUSTOMERS, Kind.BAR)
                    .title("Revenue by Customer Country")
                    .axes("Country", "Revenue ($)")
                    .series("Revenue")
                    .endpoint("api/customers/revenue-segment")
                    .describe("Revenue traced back to the country each customer is "
                            + "registered in.")
                    .reading("Set beside the customer share ring, this gives revenue per "
                            + "customer by market: which markets are large because they "
                            + "are populous and which because they spend.")
                    .caveat("Uses the customer's own country, which is not the same as "
                            + "the sales region filter.")
                    .horizontal()
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
     * shows - one each from Sales, Products, Inventory and Customers.
     *
     * All four belong to other pages, so they are named here
     * by id rather than duplicated - one chart, one spec, wherever it
     * happens to be displayed.
     */
    public static List<ChartSpec> forOverview() {
        return List.of(
                KPI_TABLE,
                require("sales.revenue-trend"),
                require("products.revenue-category"),
                require("inventory.stock-warehouse"),
                require("customers.country-breakdown")
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
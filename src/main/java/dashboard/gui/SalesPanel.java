package dashboard.gui;


/**
 * Sales analytics page.
 *
 * Keeps the four existing Sales charts but presents them in
 * the shared two-column analytics dashboard layout.
 */
public class SalesPanel extends BaseAnalyticsPage {

    private final RevenueOverTimeChart revenueOverTimeChart =
            new RevenueOverTimeChart();

    private final RevenueByRegionChart revenueByRegionChart =
            new RevenueByRegionChart();

    private final ProfitMarginOverTimeChart profitMarginOverTimeChart =
            new ProfitMarginOverTimeChart();

    private final QuantityRevenueScatterChart quantityRevenueScatterChart =
            new QuantityRevenueScatterChart();

    public SalesPanel() {

        super(
                "Sales",
                "The trading revenue, and the regions and products driving it"
        );

        /*
         * Dashboard structure:
         *
         * Revenue Over Time       | Revenue by Region
         * Profit Margin Over Time | Quantity vs Revenue
         */
        charts.add(revenueOverTimeChart);
        charts.add(revenueByRegionChart);

        charts.add(profitMarginOverTimeChart);
        charts.add(quantityRevenueScatterChart);

        finishRefresh();
    }

    /**
     * Refreshes every Sales chart using the currently selected
     * global dashboard filter.
     */
    @Override
    protected void refreshData() {

        String selectedMonth =
                "Weekly".equals(filter.scope())
                        ? filter.month()
                        : null;

        revenueOverTimeChart.applyFilters(
                filter.year(),
                filter.scope(),
                selectedMonth,
                filter.period()
        );

        revenueByRegionChart.applyFilters(
                filter.year(),
                filter.scope(),
                selectedMonth,
                filter.period()
        );

        profitMarginOverTimeChart.applyFilters(
                filter.year(),
                filter.scope(),
                selectedMonth,
                filter.period(),
                filter.region()
        );

        quantityRevenueScatterChart.applyFilters(
                filter.year(),
                filter.scope(),
                selectedMonth,
                filter.period(),
                filter.region()
        );

        profitMarginOverTimeChart.applyFilter(filter);
        quantityRevenueScatterChart.applyFilter(filter);

        charts.revalidate();
        charts.repaint();
    }
}
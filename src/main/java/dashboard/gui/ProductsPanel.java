package dashboard.gui;

import java.util.List;

import dashboard.database.AnalyticsApi;

public class ProductsPanel extends BaseAnalyticsPage {

    public ProductsPanel() {
        super("Products", "Catalogue margins compared with what actually sold");
        refreshData();
    }

    @Override
    protected void refreshData() {
        var params = filter.toParams();

        loadAsync(() -> List.of(
            AnalyticsCharts.scatter(
                    "Price vs Cost (products)", "Price ($)", "Cost ($)",
                    AnalyticsApi.xyPoints("api/products/price-cost", params)
            ),

            AnalyticsCharts.bar(
                    "Catalogue Margin by Category (products)", "Category", "Margin %",
                    AnalyticsApi.points("api/products/catalogue-margin", params),
                    "Catalogue Margin %", false, null
            ),

            AnalyticsCharts.bar(
                    "Realised Profit Margin % by Category (products + sales)", "Category", "Margin %",
                    AnalyticsApi.points("api/products/realised-margin", params),
                    "Realised Margin %", false,
                    category -> DrilldownDialog.showSales(this, null, category, filter.region())
            ),

            AnalyticsCharts.bar(
                    "Revenue by Category (products + sales)", "Category", "Revenue ($)",
                    AnalyticsApi.points("api/products/revenue-category", params),
                    "Revenue", true,
                    category -> DrilldownDialog.showSales(this, null, category, filter.region())
            )
        ));
    }
}

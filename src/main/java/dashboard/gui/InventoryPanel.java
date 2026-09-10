package dashboard.gui;

import java.util.List;

import dashboard.database.AnalyticsApi;

public class InventoryPanel extends BaseAnalyticsPage {

    public InventoryPanel() {
        super("Inventory", "Solo inventory analysis and cross-table stock performance");
        refreshData();
    }

    @Override
    protected void refreshData() {
        var params = filter.toParams();

        loadAsync(() -> List.of(
            AnalyticsCharts.bar(
                    "Stock Levels by Category (inventory + products + sales)", "Category", "Weeks of Coverage",
                    AnalyticsApi.points("api/inventory/stock-cover-weeks", params),
                    "Weeks of Cover", false, 
                    category -> DrilldownDialog.showSales(this, null, category, filter.region())
            ),

            AnalyticsCharts.line(
                    "Stock Level Over Time (inventory)", "Month", "Average Stock Level",
                    AnalyticsApi.points("api/inventory/stock-trend", params),
                    "Average Stock", null
            ),

            AnalyticsCharts.line(
                    "Inventory Turnover Trend (inventory + products + sales)", "Month", "Turnover",
                    AnalyticsApi.points("api/inventory/turnover", params),
                    "Inventory Turnover",
                    month -> DrilldownDialog.showSales(this, month, null, filter.region())
            ),

            AnalyticsCharts.groupedBar(
                    "Stock Cover by Category (inventory + sales + products)", "Category", "Units",
                    AnalyticsApi.seriesPoints("api/inventory/stock-cover", params),
                    category -> DrilldownDialog.showSales(this, null, category, filter.region())
            )
        ));
    }
}

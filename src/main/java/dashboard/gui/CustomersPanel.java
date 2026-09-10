package dashboard.gui;

import java.util.List;

import dashboard.database.AnalyticsApi;

public class CustomersPanel extends BaseAnalyticsPage {

    public CustomersPanel() {
        super("Customers", "Customer acquisition, retention and revenue segments");
        refreshData();
    }

    @Override
    protected void refreshData() {
        var params = filter.toParams();

        loadAsync(() -> List.of(
            AnalyticsCharts.bar(
                "New Customers by Signup Month (customers)", "Month", "Customers",
                AnalyticsApi.points("api/customers/new-signups", params),
                "New Customers", false, null
            ),

            AnalyticsCharts.bar(
                "Customer Breakdown by Country (customers)", "Country", "Customers",
                AnalyticsApi.points("api/customers/country-breakdown", params),
                "Customers", false, null
            ),

            AnalyticsCharts.line(
                "Customer Retention % Trend (customers + sales)", "Month", "Retention %",
                AnalyticsApi.points("api/customers/retention", params),
                "Retention %",
                month -> DrilldownDialog.showSales(this, month, null, filter.region())
            ),

            AnalyticsCharts.bar(
                "Revenue by Customer Segment - Country (sales + customers)", "Country", "Revenue ($)",
                AnalyticsApi.points("api/customers/revenue-segment", params),
                "Revenue", false, null
            )
        ));
    }
}

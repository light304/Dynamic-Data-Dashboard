package dashboard.gui;

import java.util.List;

import dashboard.database.AnalyticsApi;

public class MarketingPanel extends BaseAnalyticsPage {

    public MarketingPanel() {
        super("Marketing", "Channel spend plus whole-business month-level sales comparison");
        refreshData();
    }

    @Override
    protected void refreshData() {
        var params = filter.toParams();

        loadAsync(() -> List.of(
            AnalyticsCharts.pie(
                "Spend by Channel (marketing)",
                AnalyticsApi.points("api/marketing/spend-channel", params)
            ),

            AnalyticsCharts.line(
                "Cost per Conversion Over Time (marketing)",
                "Month", "Cost per Conversion ($)",
                AnalyticsApi.points("api/marketing/cost-per-conversion-trend", params),
                "Cost per Conversion",
                null
            ),

            AnalyticsCharts.multiLine(
                "Marketing Spend vs Revenue Over Time (period-level only)", "Month", "Value ($)",
                AnalyticsApi.seriesPoints("api/marketing/spend-revenue", params),
                month -> DrilldownDialog.showSales(this, month, null, filter.region())
            )
        ));
    }
}

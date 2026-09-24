package dashboard.gui;

import dashboard.report.ChartSpec.Page;

public class SalesPanel extends BaseAnalyticsPage {

    public SalesPanel() {
        super("Sales",
              "The trading revenue, and the regions and products driving it",
              Page.SALES);
        refreshData();
    }

    @Override
    protected void refreshData() {
        loadAsync(() -> CatalogueRenderer.cardsFor(Page.SALES, filter, this));
    }
}
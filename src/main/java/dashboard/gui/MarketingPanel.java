package dashboard.gui;

import dashboard.report.ChartSpec.Page;

public class MarketingPanel extends BaseAnalyticsPage {

    public MarketingPanel() {
        super("Marketing",
              "The cost of acquisition, and how spend tracks against revenue",
              Page.MARKETING);
        refreshData();
    }

    @Override
    protected void refreshData() {
        loadAsync(() -> CatalogueRenderer.cardsFor(Page.MARKETING, filter, this));
    }
}
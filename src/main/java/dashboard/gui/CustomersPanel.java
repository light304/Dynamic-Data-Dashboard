package dashboard.gui;

import dashboard.report.ChartSpec.Page;

public class CustomersPanel extends BaseAnalyticsPage {

    public CustomersPanel() {
        super("Customers",
              "The shape of the customer base, and how much of it returns",
              Page.CUSTOMERS);
        refreshData();
    }

    @Override
    protected void refreshData() {
        loadAsync(() -> CatalogueRenderer.cardsFor(Page.CUSTOMERS, filter, this));
    }
}
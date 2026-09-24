package dashboard.gui;

import dashboard.report.ChartSpec.Page;

public class ProductsPanel extends BaseAnalyticsPage {

    public ProductsPanel() {
        super("Products",
              "The margin the catalogue promises, set against the margin it earns",
              Page.PRODUCTS);
        refreshData();
    }

    @Override
    protected void refreshData() {
        loadAsync(() -> CatalogueRenderer.cardsFor(Page.PRODUCTS, filter, this));
    }
}

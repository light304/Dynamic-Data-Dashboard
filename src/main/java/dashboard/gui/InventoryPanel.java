package dashboard.gui;

import dashboard.report.ChartSpec.Page;

public class InventoryPanel extends BaseAnalyticsPage {

    public InventoryPanel() {
        super("Inventory",
              "The stock held, measured against the rate it is selling",
              Page.INVENTORY);
        refreshData();
    }

    @Override
    protected void refreshData() {
        loadAsync(() -> CatalogueRenderer.cardsFor(Page.INVENTORY, filter, this));
    }
}
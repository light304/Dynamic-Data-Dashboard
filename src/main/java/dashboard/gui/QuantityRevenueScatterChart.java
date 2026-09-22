package dashboard.gui;

import dashboard.database.AnalyticsApi;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/** Cross-table scatter: sales + products; one point per product, coloured by category. */
public class QuantityRevenueScatterChart extends JPanel {

    private static final Color BORDER = Theme.BORDER;

    private final JPanel chartHost = new JPanel(new BorderLayout());
    private final JLabel status = new JLabel("Waiting for quantity/revenue data...");

    public QuantityRevenueScatterChart() {
        setLayout(new BorderLayout(0, 8));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(12, 12, 10, 12)
        ));
        chartHost.setBackground(Color.WHITE);
        add(chartHost, BorderLayout.CENTER);
        status.setFont(Theme.SMALL);
        status.setForeground(new Color(100, 116, 139));
        add(status, BorderLayout.SOUTH);
    }

    public void applyFilters(int year, String scope, String selectedMonth, String period, String region) {
        try {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("year", String.valueOf(year));
            params.put("scope", scope);
            params.put("period", period);
            params.put("region", region);
            if (selectedMonth != null) params.put("month", selectedMonth);

            var points = AnalyticsApi.xyPoints("api/sales/quantity-revenue", params);
            chartHost.removeAll();
            chartHost.add(
                    AnalyticsCharts.scatter(
                            "Quantity vs Revenue by Category (sales + products)",
                            "Units Sold",
                            "Revenue ($)",
                            points
                    ),
                    BorderLayout.CENTER
            );
            status.setText(filterText(year, scope, selectedMonth, period) + " • one point per product");
            chartHost.revalidate();
            chartHost.repaint();
        } catch (Exception ex) {
            ex.printStackTrace();
            chartHost.removeAll();
            chartHost.add(AnalyticsCharts.messageCard("Quantity vs Revenue", ex.getMessage()));
            status.setText("Unable to load quantity/revenue scatter");
            chartHost.revalidate();
            chartHost.repaint();
        }
    }

    private String filterText(int year, String scope, String month, String period) {
        return "Weekly".equals(scope)
                ? year + " • " + month + " • " + period
                : year + " • " + scope + " • " + period;
    }
    /** Receives the dashboard-wide filter and reuses the existing chart filter logic. */
    public void applyFilter(DashboardFilter filter) {
        if (filter == null) filter = DashboardFilter.defaults();
        String selectedMonth = "Weekly".equals(filter.scope()) ? filter.month() : null;
        applyFilters(filter.year(), filter.scope(), selectedMonth, filter.period(), filter.region());
    }

}

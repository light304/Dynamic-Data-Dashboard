package dashboard.gui;

import dashboard.database.AnalyticsApi;
import dashboard.database.SchemaIntrospector.TableMeta;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Map;

/** Overview page. Filtering is controlled by the shared GlobalFilterPanel. */
public class OverviewPanel extends JPanel implements FilterableDashboardPage {

    private static final Color BACKGROUND = new Color(245, 247, 250);
    private static final Color PRIMARY = new Color(31, 41, 55);

    private final KpiPanel kpiPanel = new KpiPanel();
    private final RevenueChartPanel revenueChartPanel = new RevenueChartPanel();
    private DashboardFilter currentFilter = DashboardFilter.defaults();

    public OverviewPanel(Map<String, TableMeta> schema) {
        setLayout(new BorderLayout(0, 15));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(20, 25, 25, 25));
        createLayout();
    }

    private void createLayout() {
        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setBackground(BACKGROUND);

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Overview");
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        title.setForeground(PRIMARY);

        JLabel subtitle = new JLabel("KPIs update from the dashboard-wide filter above");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(new Color(100, 116, 139));

        heading.add(title);
        heading.add(Box.createVerticalStrut(4));
        heading.add(subtitle);

        top.add(heading, BorderLayout.NORTH);
        top.add(kpiPanel, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(BACKGROUND);
        body.add(revenueChartPanel, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
    }

    /** Refreshes both the KPI cards and overview revenue chart from one filter. */
        @Override
    public void applyFilter(DashboardFilter filter) {
        currentFilter = filter == null ? DashboardFilter.defaults() : filter;

        new SwingWorker<AnalyticsApi.Kpis, Void>() {
            @Override
            protected AnalyticsApi.Kpis doInBackground() throws Exception {
                return AnalyticsApi.overview(currentFilter.toParams());
            }

            @Override
            protected void done() {
                try {
                    kpiPanel.update(get());
                } catch (Exception ex) {
                    ex.printStackTrace();
                    kpiPanel.showError();
                }
            }
        }.execute();

        revenueChartPanel.applyFilters(
                currentFilter.year(),
                currentFilter.scope(),
                currentFilter.period(),
                currentFilter.region()
        );
    }
}

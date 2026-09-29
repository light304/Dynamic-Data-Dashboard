package dashboard.gui;

import dashboard.database.AnalyticsApi;
import dashboard.database.AnalyticsApi.Kpis;
import dashboard.database.SchemaIntrospector.TableMeta;

import dashboard.report.ChartCatalogue;
import dashboard.report.ChartSpec;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The landing page: the seven KPI cards above a 2x2 grid of the four
 * headline charts, filling the window with no scrolling.
 *
 * The four charts are ordinary ChartCatalogue specs owned by Sales,
 * Products, Inventory and Customers (see ChartCatalogue.forOverview()),
 * so this page cannot drift from the page each chart really belongs to.
 */
public class OverviewPanel extends JPanel implements FilterableDashboardPage {

    private static final Color BACKGROUND = Theme.PAGE_BG;
    private static final Color PRIMARY    = Theme.TEXT;
    private static final Color SECONDARY  = Theme.TEXT_MUTED;
    private static final Color ACCENT     = Theme.ACCENT;

    private final KpiPanel kpiPanel = new KpiPanel();
    private final JPanel chartsGrid = ChartGrid.create(BACKGROUND);
    private final JButton refreshButton = new JButton("Refresh");

    private DashboardFilter currentFilter = DashboardFilter.defaults();

    public OverviewPanel(Map<String, TableMeta> schema) {

        setLayout(new BorderLayout(0, 12));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(14, 22, 16, 22));

        add(createHeader(), BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);
        content.add(kpiPanel, BorderLayout.NORTH);
        content.add(chartsGrid, BorderLayout.CENTER);

        add(content, BorderLayout.CENTER);
    }

    // =========================================================
    // HEADER
    // =========================================================

    private JPanel createHeader() {

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titleArea = new JPanel();
        titleArea.setOpaque(false);
        titleArea.setLayout(new BoxLayout(titleArea, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("Overview");
        titleLabel.setFont(Theme.PAGE_HEADING);
        titleLabel.setForeground(PRIMARY);

        JLabel subtitleLabel = new JLabel(
                "Headline numbers and the four charts that matter most, at a glance.");
        subtitleLabel.setFont(Theme.BODY);
        subtitleLabel.setForeground(SECONDARY);

        titleArea.add(titleLabel);
        titleArea.add(Box.createVerticalStrut(2));
        titleArea.add(subtitleLabel);

        refreshButton.setFont(Theme.BODY_STRONG);
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setBackground(ACCENT);
        refreshButton.setOpaque(true);
        refreshButton.setContentAreaFilled(true);
        refreshButton.setBorderPainted(false);
        refreshButton.setFocusPainted(false);
        refreshButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshButton.setPreferredSize(new Dimension(115, 36));
        refreshButton.addActionListener(e -> refreshEverything());

        JPanel buttonArea = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonArea.setOpaque(false);
        buttonArea.add(ExportButton.create(this, ChartSpec.Page.OVERVIEW, () -> currentFilter));
        buttonArea.add(refreshButton);

        header.add(titleArea, BorderLayout.WEST);
        header.add(buttonArea, BorderLayout.EAST);

        return header;
    }

    // =========================================================
    // FILTER
    // =========================================================

    @Override
    public void applyFilter(DashboardFilter filter) {

        currentFilter = filter == null ? DashboardFilter.defaults() : filter;

        refreshEverything();
    }

    // =========================================================
    // REFRESH
    // =========================================================

    private void refreshEverything() {

        refreshKpis();
        refreshOverviewCharts();
    }

    // =========================================================
    // KPI DATA
    // =========================================================

    private void refreshKpis() {

        new SwingWorker<Kpis, Void>() {

            @Override
            protected Kpis doInBackground() throws Exception {
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
    }

    // =========================================================
    // CHART DATA
    // =========================================================

    private void refreshOverviewCharts() {

        refreshButton.setEnabled(false);
        refreshButton.setText("Loading...");

        ChartGrid.showMessage(chartsGrid, "Loading", "Fetching dashboard analytics...");

        new SwingWorker<List<JPanel>, Void>() {

            @Override
            protected List<JPanel> doInBackground() throws Exception {

                List<JPanel> loaded = new ArrayList<>();

                for (ChartSpec spec : ChartCatalogue.overviewCharts()) {
                    loaded.add(CatalogueRenderer.card(spec, currentFilter, OverviewPanel.this));
                }

                return loaded;
            }

            @Override
            protected void done() {

                try {
                    ChartGrid.fill(chartsGrid, get());
                } catch (Exception ex) {
                    ex.printStackTrace();
                    ChartGrid.showMessage(chartsGrid, "Unable to load charts",
                            ex.getMessage() == null
                                    ? "Dashboard chart request failed."
                                    : ex.getMessage());
                }

                refreshButton.setEnabled(true);
                refreshButton.setText("Refresh");
            }

        }.execute();
    }
}

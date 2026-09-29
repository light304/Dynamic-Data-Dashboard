package dashboard.gui;

import dashboard.report.ChartSpec.Page;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Shared shell for the Sales, Products, Inventory, Marketing and Customers
 * pages: a header with Export and Refresh, and a grid of chart cards that
 * fills the rest of the window.
 *
 * There is no scroll pane. The cards divide whatever room the window gives
 * them (see ChartGrid), so a page is always seen whole.
 */
public abstract class BaseAnalyticsPage extends JPanel implements FilterableDashboardPage {

    protected static final Color BACKGROUND = Theme.PAGE_BG;
    protected static final Color PRIMARY = Theme.TEXT;
    protected static final Color SECONDARY = Theme.TEXT_MUTED;
    protected static final Color ACCENT = Theme.ACCENT;

    protected DashboardFilter filter = DashboardFilter.defaults();

    /** The chart area. Filled by ChartGrid. */
    protected final JPanel charts = ChartGrid.create(BACKGROUND);

    private final JButton refreshButton = new JButton("Refresh");

    protected BaseAnalyticsPage(String title, String subtitle, Page page) {

        setLayout(new BorderLayout(0, 10));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(14, 22, 16, 22));

        // ----- header -----
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titleArea = new JPanel();
        titleArea.setOpaque(false);
        titleArea.setLayout(new BoxLayout(titleArea, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.PAGE_HEADING);
        titleLabel.setForeground(PRIMARY);

        JLabel subtitleLabel = new JLabel(subtitle);
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
        refreshButton.addActionListener(e -> refreshData());

        JPanel buttonArea = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonArea.setOpaque(false);
        buttonArea.add(ExportButton.create(this, page, () -> filter));
        buttonArea.add(refreshButton);

        header.add(titleArea, BorderLayout.WEST);
        header.add(buttonArea, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);
        add(charts, BorderLayout.CENTER);
    }

    @Override
    public void applyFilter(DashboardFilter filter) {

        this.filter = filter == null ? DashboardFilter.defaults() : filter;

        refreshData();
    }

    /** Called when an asynchronous reload begins. */
    protected final void startRefresh() {

        refreshButton.setEnabled(false);
        refreshButton.setText("Loading...");
    }

    /** Called when loading is complete. */
    protected final void finishRefresh() {

        refreshButton.setEnabled(true);
        refreshButton.setText("Refresh");
    }

    protected final void showError(Exception ex) {

        ChartGrid.showMessage(charts, "Unable to load data",
                ex.getMessage() == null ? "The request failed." : ex.getMessage());
    }

    protected abstract void refreshData();

    /** Used by pages that fetch a list of chart cards in the background. */
    protected final void loadAsync(Callable<List<JPanel>> loader) {

        startRefresh();

        ChartGrid.showMessage(charts, "Loading", "Fetching data...");

        new SwingWorker<List<JPanel>, Void>() {

            @Override
            protected List<JPanel> doInBackground() throws Exception {
                return loader.call();
            }

            @Override
            protected void done() {

                try {
                    ChartGrid.fill(charts, get());
                } catch (Exception ex) {
                    showError(ex);
                }

                finishRefresh();
            }

        }.execute();
    }
}

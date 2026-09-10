package dashboard.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.concurrent.Callable;

public abstract class BaseAnalyticsPage extends JPanel implements FilterableDashboardPage {

    protected static final Color BACKGROUND = new Color(245, 247, 250);
    protected static final Color PRIMARY = new Color(31, 41, 55);
    protected static final Color SECONDARY = new Color(100, 116, 139);

    protected DashboardFilter filter = DashboardFilter.defaults();
    // Keep chart cards at a readable size instead of stretching them to fill the page.
    // BoxLayout also allows the user to move sideways when a page contains several charts.
    protected final JPanel charts = new JPanel();

    protected BaseAnalyticsPage(String title, String subtitle) {
        setLayout(new BorderLayout(0, 15));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(20, 25, 25, 25));

        JPanel header = new JPanel();
        header.setBackground(BACKGROUND);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        titleLabel.setForeground(PRIMARY);

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitleLabel.setForeground(SECONDARY);

        header.add(titleLabel);
        header.add(Box.createVerticalStrut(4));
        header.add(subtitleLabel);
        add(header, BorderLayout.NORTH);

        charts.setBackground(BACKGROUND);
        charts.setLayout(new BoxLayout(charts, BoxLayout.X_AXIS));

        JScrollPane scroll = new JScrollPane(charts);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BACKGROUND);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        // Horizontal scrolling prevents charts from becoming huge or being squeezed.
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.getHorizontalScrollBar().setUnitIncrement(20);
        add(scroll, BorderLayout.CENTER);
    }

    @Override
    public void applyFilter(DashboardFilter filter) {
        this.filter = filter == null ? DashboardFilter.defaults() : filter;
        refreshData();
    }

    protected final void startRefresh() {
        charts.removeAll();
    }

    protected final void finishRefresh() {
        // Give every analytics card a consistent dashboard-sized footprint.
        // The cards stay readable and extra charts are reached with horizontal scrolling.
        for (Component component : charts.getComponents()) {
            if (component instanceof JComponent chartCard) {
                chartCard.setPreferredSize(new Dimension(500, 330));
                chartCard.setMinimumSize(new Dimension(500, 330));
                chartCard.setMaximumSize(new Dimension(500, 330));
                chartCard.setAlignmentY(Component.TOP_ALIGNMENT);
            }
        }
        charts.revalidate();
        charts.repaint();
    }

    protected final void showError(Exception ex) {
        charts.add(AnalyticsCharts.messageCard("Unable to load data", ex.getMessage()));
    }

    protected abstract void refreshData();

    protected final void loadAsync(Callable<List<JPanel>> loader) {
        startRefresh();
        charts.add(AnalyticsCharts.messageCard("Loading", "Fetching data..."));
        charts.revalidate();
        charts.repaint();

        new SwingWorker<List<JPanel>, Void>() {
            @Override
            protected List<JPanel> doInBackground() throws Exception {
                return loader.call();
            }

            @Override
            protected void done() {
                charts.removeAll();
                try {
                    for (JPanel card : get()) charts.add(card);
                } catch (Exception ex) {
                    showError(ex);
                }
                finishRefresh();
            }
        }.execute();
    }
}

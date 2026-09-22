package dashboard.gui;

import dashboard.database.AnalyticsApi;
import dashboard.database.SchemaIntrospector;
import dashboard.database.SchemaIntrospector.TableMeta;

import dashboard.auth.AuthService;
import dashboard.auth.LoginFrame;
import dashboard.auth.UserSession;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Main application window.
 *
 * Layout:
 * - sidebar on the left, carrying the application branding at its top
 * - shared filter bar above the page content
 * - page content in the centre
 *
 * The branding used to sit in a full-width strip across the top of the
 * window. Only the leftmost 210px of that strip was ever used, so the
 * remaining ~1100x85 was empty background on every page. The branding now
 * lives inside SidebarPanel and that strip is gone.
 */
public class DashboardFrame extends JFrame {

    private static final Color BACKGROUND = new Color(245, 247, 250);

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<String, FilterableDashboardPage> filterablePages = new LinkedHashMap<>();

    // One filter state is remembered for the whole dashboard.
    private DashboardFilter globalFilter = DashboardFilter.defaults();
    private String currentPage = "Overview";
    private Map<String, TableMeta> schema = Map.of();

    public DashboardFrame() {
        try {
            schema = SchemaIntrospector.introspect();
            System.out.println("Schema loaded successfully.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    null,
                    "Cannot reach the backend service at http://localhost:3000\n\n"
                    + "Start it first, in a separate terminal:\n\n"
                    + "    cd src\\main\\java\\dashboard\\database\n"
                    + "    npm start\n\n"
                    + "Then run this application again.\n\n"
                    + "Details: " + ex,
                    "Backend Not Running",
                    JOptionPane.ERROR_MESSAGE
            );
            System.exit(1);
        }

        setTitle("Dynamic Retail Dashboard");
        setSize(1350, 850);
        setMinimumSize(new Dimension(1050, 700));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        createLayout();
    }

    private void createLayout() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BACKGROUND);

        // The sidebar spans the full window height and carries the branding.
        root.add(new SidebarPanel(this::showPage, this::uploadCsv, this::logout), BorderLayout.WEST);

        // The filter is only above the changing page content, not above the sidebar.
        JPanel centre = new JPanel(new BorderLayout());
        centre.setBackground(BACKGROUND);
        centre.add(new GlobalFilterPanel(this::onGlobalFilterChanged), BorderLayout.NORTH);
        centre.add(createContentArea(), BorderLayout.CENTER);

        root.add(centre, BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel createContentArea() {
        content.setBackground(BACKGROUND);

        OverviewPanel overview = new OverviewPanel(schema);
        SalesPanel sales = new SalesPanel();
        InventoryPanel inventory = new InventoryPanel();
        ProductsPanel products = new ProductsPanel();
        MarketingPanel marketing = new MarketingPanel();
        CustomersPanel customers = new CustomersPanel();
        ReportsPanel reports = new ReportsPanel();

        filterablePages.put("Overview", overview);
        filterablePages.put("Sales", sales);
        filterablePages.put("Inventory", inventory);
        filterablePages.put("Products", products);
        filterablePages.put("Marketing", marketing);
        filterablePages.put("Customers", customers);
        filterablePages.put("Reports", reports);

        content.add(overview, "Overview");
        content.add(sales, "Sales");
        content.add(inventory, "Inventory");
        content.add(products, "Products");
        content.add(marketing, "Marketing");
        content.add(customers, "Customers");
        content.add(reports, "Reports");

        if (UserSession.getInstance().isManager()) {
            AlertsPanel alerts = new AlertsPanel();
            content.add(alerts, "Alerts");
        }

        // PERFORMANCE: load only the visible page at startup. Previously all six
        // pages refreshed together, which caused several API calls and made it slow.
        SwingUtilities.invokeLater(() -> applyFilterToPage("Overview"));
        return content;
    }

    /**
     * Saves the new global filter and refreshes ONLY the page the user is viewing.
     * Other pages receive the same filter when the user opens them.
     */
    private void onGlobalFilterChanged(DashboardFilter filter) {
        globalFilter = filter == null ? DashboardFilter.defaults() : filter;
        System.out.println("GLOBAL FILTER -> " + globalFilter);
        applyFilterToPage(currentPage);
    }

    private void showPage(String page) {
        currentPage = page;
        cards.show(content, page);

        // Lazy refresh keeps navigation responsive and avoids loading every chart
        // whenever Apply Filters is clicked.
        applyFilterToPage(page);
    }

    private void applyFilterToPage(String pageName) {
        FilterableDashboardPage page = filterablePages.get(pageName);
        if (page == null) return;

        try {
            page.applyFilter(globalFilter);
        } catch (Exception ex) {
            System.err.println("Filter refresh failed for " + pageName + ": " + ex.getMessage());
        }
    }

    private JPanel placeholder(String title, String message) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        panel.add(new JLabel("<html><h1>" + title + "</h1><p>" + message + "</p></html>"), BorderLayout.NORTH);
        return panel;
    }

    private void uploadCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select a CSV file");
        chooser.setFileFilter(
                new javax.swing.filechooser.FileNameExtensionFilter("CSV files", "csv"));

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        java.io.File file = chooser.getSelectedFile();
        JDialog loading = new JDialog(this, "Uploading", true);
        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        body.add(new JLabel("Loading " + file.getName() + "..."), BorderLayout.NORTH);

        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        body.add(bar, BorderLayout.CENTER);

        loading.setContentPane(body);
        loading.pack();
        loading.setLocationRelativeTo(this);
        loading.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        new SwingWorker<AnalyticsApi.UploadResult, Void>() {
            @Override
            protected AnalyticsApi.UploadResult doInBackground() throws Exception {
                return AnalyticsApi.upload(file.getAbsolutePath());
            }

            @Override
            protected void done() {
                loading.dispose();
                setCursor(Cursor.getDefaultCursor());
                try {
                    AnalyticsApi.UploadResult result = get();

                    if (result.success()) {
                        String message =
                            "Loaded " + result.loaded() + " of " + result.totalRows()
                            + " rows into " + result.table() + ".\n"
                            + result.rejected() + " row(s) rejected.";

                        if (!result.rejectedDetail().isEmpty()) {
                            message += "\n\n" + String.join("\n", result.rejectedDetail());
                            if (result.rejected() > result.rejectedDetail().size()) {
                                message += "\n... and "
                                        + (result.rejected() - result.rejectedDetail().size())
                                        + " more";
                            }
                        }

                        JOptionPane.showMessageDialog(
                                DashboardFrame.this, message,
                                "Upload Complete", JOptionPane.INFORMATION_MESSAGE
                            );

                        filterablePages.keySet()
                                .forEach(DashboardFrame.this::applyFilterToPage);
                    } else {
                        JOptionPane.showMessageDialog(
                                DashboardFrame.this,
                                result.error(),
                                "Upload Failed",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                            DashboardFrame.this,
                            "Upload failed: " + ex,
                            "Upload Failed",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();

        loading.setVisible(true);
    }

    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Sign out and return to the login screen?",
                "Log Out",
                JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) return;

        UserSession.getInstance().logout();
        dispose();

        AuthService authService = new AuthService();
        SwingUtilities.invokeLater(() ->
                new LoginFrame(authService).setVisible(true));
    }

}

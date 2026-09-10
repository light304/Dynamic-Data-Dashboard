package dashboard.gui;

import dashboard.database.AnalyticsApi;
import dashboard.database.SchemaIntrospector;
import dashboard.database.SchemaIntrospector.TableMeta;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Main application window.
 *
 * The original dashboard structure is intentionally preserved:
 * - branding stays above the left sidebar
 * - sidebar stays on the left
 * - page content stays in the centre
 *
 * The only new layout feature is the shared filter bar above the page content.
 */
public class DashboardFrame extends JFrame {

    private static final Color SIDEBAR = new Color(17, 24, 39);
    private static final Color BACKGROUND = new Color(245, 247, 250);
    private static final Color ACTIVE = new Color(0, 212, 255);

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

        // ORIGINAL STRUCTURE: the top strip spans the whole window and contains
        // the dark dashboard branding directly above the sidebar.
        root.add(createTopPanel(), BorderLayout.NORTH);
        root.add(new SidebarPanel(this::showPage, this::uploadCsv), BorderLayout.WEST);

        // The filter is only above the changing page content, not above the sidebar.
        JPanel centre = new JPanel(new BorderLayout());
        centre.setBackground(BACKGROUND);
        centre.add(new GlobalFilterPanel(this::onGlobalFilterChanged), BorderLayout.NORTH);
        centre.add(createContentArea(), BorderLayout.CENTER);

        root.add(centre, BorderLayout.CENTER);
        setContentPane(root);
    }

    /** Recreates the original dark branding block above the sidebar. */
    private JPanel createTopPanel() {
        JPanel top = new JPanel(new BorderLayout());
        top.setPreferredSize(new Dimension(0, 85));
        top.setBackground(BACKGROUND);

        JPanel logo = new JPanel();
        logo.setLayout(new BoxLayout(logo, BoxLayout.Y_AXIS));
        logo.setPreferredSize(new Dimension(210, 85));
        logo.setBackground(SIDEBAR);
        logo.setBorder(BorderFactory.createEmptyBorder(17, 18, 15, 18));

        JLabel line1 = new JLabel("Dynamic Retail");
        line1.setForeground(Color.WHITE);
        line1.setFont(new Font("SansSerif", Font.BOLD, 18));

        JLabel line2 = new JLabel("Dashboard");
        line2.setForeground(ACTIVE);
        line2.setFont(new Font("SansSerif", Font.BOLD, 18));

        JLabel sub = new JLabel("Admin Dashboard");
        sub.setForeground(new Color(170, 180, 195));
        sub.setFont(new Font("SansSerif", Font.PLAIN, 11));

        logo.add(line1);
        logo.add(line2);
        logo.add(Box.createVerticalStrut(3));
        logo.add(sub);
        top.add(logo, BorderLayout.WEST);

        return top;
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
        content.add(placeholder("Alerts", "The low-stock backend route can be connected here."), "Alerts");

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

}

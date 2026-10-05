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
import javax.swing.table.DefaultTableModel;
import java.io.BufferedReader;
import java.io.FileReader;

/**
 * Main application window.
 *
 * Layout:
 * - sidebar on the left, carrying the application branding at its top
 * - shared filter bar above the page content
 * - page content in the centre
 */
public class DashboardFrame extends JFrame {

    private static final Color BACKGROUND = Theme.PAGE_BG;

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);

    private final Map<String, FilterableDashboardPage> filterablePages =
            new LinkedHashMap<>();

    // One filter state is remembered for the whole dashboard.
    private DashboardFilter globalFilter = DashboardFilter.defaults();

    private String currentPage = "Overview";

    private Map<String, TableMeta> schema = Map.of();

    public DashboardFrame() {

        try {

            schema = SchemaIntrospector.introspect();

            System.out.println(
                    "Schema loaded successfully."
            );

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

        setTitle(
                "Dynamic Retail Dashboard"
        );

        setSize(
                1350,
                850
        );

        setMinimumSize(
                new Dimension(
                        1050,
                        700
                )
        );

        setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE
        );

        setLocationRelativeTo(null);

        createLayout();
    }

    private void createLayout() {

        JPanel root =
                new JPanel(
                        new BorderLayout()
                );

        root.setBackground(
                BACKGROUND
        );

        // Sidebar
        root.add(
                new SidebarPanel(
                        this::showPage,
                        this::uploadCsv,
                        this::logout
                ),
                BorderLayout.WEST
        );

        // Centre area
        JPanel centre =
                new JPanel(
                        new BorderLayout()
                );

        centre.setBackground(
                BACKGROUND
        );

        centre.add(
                new GlobalFilterPanel(
                        this::onGlobalFilterChanged
                ),
                BorderLayout.NORTH
        );

        centre.add(
                createContentArea(),
                BorderLayout.CENTER
        );

        root.add(
                centre,
                BorderLayout.CENTER
        );

        setContentPane(root);
    }

    private JPanel createContentArea() {

        content.setBackground(
                BACKGROUND
        );

        OverviewPanel overview =
                new OverviewPanel(schema);

        SalesPanel sales =
                new SalesPanel();

        InventoryPanel inventory =
                new InventoryPanel();

        ProductsPanel products =
                new ProductsPanel();

        MarketingPanel marketing =
                new MarketingPanel();

        CustomersPanel customers =
                new CustomersPanel();

        filterablePages.put(
                "Overview",
                overview
        );

        filterablePages.put(
                "Sales",
                sales
        );

        filterablePages.put(
                "Inventory",
                inventory
        );

        filterablePages.put(
                "Products",
                products
        );

        filterablePages.put(
                "Marketing",
                marketing
        );

        filterablePages.put(
                "Customers",
                customers
        );

        content.add(
                overview,
                "Overview"
        );

        content.add(
                sales,
                "Sales"
        );

        content.add(
                inventory,
                "Inventory"
        );

        content.add(
                products,
                "Products"
        );

        content.add(
                marketing,
                "Marketing"
        );

        content.add(
                customers,
                "Customers"
        );

        if (
                UserSession
                        .getInstance()
                        .isManager()
        ) {

            AlertsPanel alerts =
                    new AlertsPanel();

            content.add(
                    alerts,
                    "Alerts"
            );
        }

        // Only load the visible page on startup.
        SwingUtilities.invokeLater(
                () ->
                        applyFilterToPage(
                                "Overview"
                        )
        );

        return content;
    }

    /**
     * Saves the new global filter and refreshes only
     * the page currently being viewed.
     */
    private void onGlobalFilterChanged(
            DashboardFilter filter
    ) {

        globalFilter =
                filter == null
                        ? DashboardFilter.defaults()
                        : filter;

        System.out.println(
                "GLOBAL FILTER -> "
                        + globalFilter
        );

        applyFilterToPage(
                currentPage
        );
    }

    private void showPage(
            String page
    ) {

        currentPage = page;

        cards.show(
                content,
                page
        );

        // Refresh only the page opened by the user.
        applyFilterToPage(
                page
        );
    }

    private void applyFilterToPage(
            String pageName
    ) {

        FilterableDashboardPage page =
                filterablePages.get(
                        pageName
                );

        if (page == null) {
            return;
        }

        try {

            page.applyFilter(
                    globalFilter
            );

        } catch (Exception ex) {

            System.err.println(
                    "Filter refresh failed for "
                            + pageName
                            + ": "
                            + ex.getMessage()
            );
        }
    }

    // ============================================================
    // CSV UPLOAD
    // ============================================================

    private void uploadCsv() {

        JFileChooser chooser =
                new JFileChooser();

        chooser.setDialogTitle(
                "Select a CSV file"
        );

        chooser.setFileFilter(
                new javax.swing.filechooser.FileNameExtensionFilter(
                        "CSV files",
                        "csv"
                )
        );

        if (
                chooser.showOpenDialog(this)
                        != JFileChooser.APPROVE_OPTION
        ) {

            return;
        }

        java.io.File file =
                chooser.getSelectedFile();

        // --------------------------------------------------------
        // PREVIEW CSV BEFORE UPLOAD
        // --------------------------------------------------------

        if (!showCsvPreview(file)) {
            return;
        }

        // --------------------------------------------------------
        // UPLOAD PROGRESS WINDOW
        // --------------------------------------------------------

        JDialog loading =
                new JDialog(
                        this,
                        "Uploading",
                        true
                );

        JPanel body =
                new JPanel(
                        new BorderLayout(
                                0,
                                10
                        )
                );

        body.setBorder(
                BorderFactory.createEmptyBorder(
                        20,
                        25,
                        20,
                        25
                )
        );

        body.add(
                new JLabel(
                        "Loading "
                                + file.getName()
                                + "..."
                ),
                BorderLayout.NORTH
        );

        JProgressBar bar =
                new JProgressBar();

        bar.setIndeterminate(
                true
        );

        body.add(
                bar,
                BorderLayout.CENTER
        );

        loading.setContentPane(
                body
        );

        loading.pack();

        loading.setLocationRelativeTo(
                this
        );

        loading.setDefaultCloseOperation(
                JDialog.DO_NOTHING_ON_CLOSE
        );

        // --------------------------------------------------------
        // PERFORM UPLOAD IN BACKGROUND
        // --------------------------------------------------------

        new SwingWorker<
                AnalyticsApi.UploadResult,
                Void>() {

            @Override
            protected AnalyticsApi.UploadResult
            doInBackground()
                    throws Exception {

                return AnalyticsApi.upload(
                        file.getAbsolutePath()
                );
            }

            @Override
            protected void done() {

                loading.dispose();

                setCursor(
                        Cursor.getDefaultCursor()
                );

                try {

                    AnalyticsApi.UploadResult result =
                            get();

                    if (result.success()) {

                        String message =
                                "Loaded "
                                        + result.loaded()
                                        + " of "
                                        + result.totalRows()
                                        + " rows into "
                                        + result.table()
                                        + ".\n"
                                        + result.rejected()
                                        + " row(s) rejected.";

                        if (
                                !result
                                        .rejectedDetail()
                                        .isEmpty()
                        ) {

                            message +=
                                    "\n\n"
                                            + String.join(
                                            "\n",
                                            result.rejectedDetail()
                                    );

                            if (
                                    result.rejected()
                                            > result
                                            .rejectedDetail()
                                            .size()
                            ) {

                                message +=
                                        "\n... and "
                                                + (
                                                result.rejected()
                                                        -
                                                        result
                                                                .rejectedDetail()
                                                                .size()
                                        )
                                                + " more";
                            }
                        }

                        JOptionPane.showMessageDialog(
                                DashboardFrame.this,
                                message,
                                "Upload Complete",
                                JOptionPane.INFORMATION_MESSAGE
                        );

                        // ========================================
                        // FIX:
                        // Refresh ONLY the currently visible page.
                        // Do not refresh every dashboard page.
                        // ========================================

                        DashboardFrame.this
                                .applyFilterToPage(
                                        currentPage
                                );

                    } else {

                        JOptionPane.showMessageDialog(
                                DashboardFrame.this,
                                result.error(),
                                "Upload Failed",
                                JOptionPane.ERROR_MESSAGE
                        );
                    }

                } catch (Exception ex) {

                    JOptionPane.showMessageDialog(
                            DashboardFrame.this,
                            "Upload failed: "
                                    + ex,
                            "Upload Failed",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }

        }.execute();

        loading.setVisible(
                true
        );
    }

    // ============================================================
    // CSV PREVIEW
    // ============================================================

    /**
     * Displays the selected CSV in a table before
     * it is uploaded.
     *
     * @return true if the user confirms the upload,
     * false if they cancel.
     */
    private boolean showCsvPreview(
            java.io.File file
    ) {

        try (
                BufferedReader reader =
                        new BufferedReader(
                                new FileReader(file)
                        )
        ) {

            // Read CSV header
            String headerLine =
                    reader.readLine();

            if (headerLine == null) {

                JOptionPane.showMessageDialog(
                        this,
                        "The selected CSV file is empty.",
                        "CSV Preview",
                        JOptionPane.WARNING_MESSAGE
                );

                return false;
            }

            // Create columns from first line
            String[] columns =
                    headerLine.split(
                            ",",
                            -1
                    );

            DefaultTableModel model =
                    new DefaultTableModel(
                            columns,
                            0
                    ) {

                        @Override
                        public boolean isCellEditable(
                                int row,
                                int column
                        ) {

                            return false;
                        }
                    };

            String line;

            int previewRows = 0;

            // Show up to the first 100 rows
            while (
                    (line = reader.readLine())
                            != null
                            &&
                            previewRows < 100
            ) {

                String[] row =
                        line.split(
                                ",",
                                -1
                        );

                model.addRow(
                        row
                );

                previewRows++;
            }

            // Create preview table
            JTable table =
                    new JTable(
                            model
                    );

            table.setAutoResizeMode(
                    JTable.AUTO_RESIZE_OFF
            );

            table.setRowHeight(
                    24
            );

            table.getTableHeader()
                    .setReorderingAllowed(
                            false
                    );

            // Give each column a readable width
            for (
                    int i = 0;
                    i < table.getColumnCount();
                    i++
            ) {

                table.getColumnModel()
                        .getColumn(i)
                        .setPreferredWidth(
                                140
                        );
            }

            JScrollPane scrollPane =
                    new JScrollPane(
                            table
                    );

            scrollPane.setPreferredSize(
                    new Dimension(
                            850,
                            450
                    )
            );

            // Main preview panel
            JPanel previewPanel =
                    new JPanel(
                            new BorderLayout(
                                    0,
                                    10
                            )
                    );

            JLabel fileNameLabel =
                    new JLabel(
                            "Selected file: "
                                    + file.getName()
                    );

            JLabel rowLabel =
                    new JLabel(
                            "Showing first "
                                    + previewRows
                                    + " rows"
                    );

            JPanel topPanel =
                    new JPanel(
                            new BorderLayout()
                    );

            topPanel.add(
                    fileNameLabel,
                    BorderLayout.WEST
            );

            topPanel.add(
                    rowLabel,
                    BorderLayout.EAST
            );

            previewPanel.add(
                    topPanel,
                    BorderLayout.NORTH
            );

            previewPanel.add(
                    scrollPane,
                    BorderLayout.CENTER
            );

            // ----------------------------------------------------
            // CONFIRM UPLOAD
            // ----------------------------------------------------

            int choice =
                    JOptionPane.showConfirmDialog(
                            this,
                            previewPanel,
                            "Preview CSV Before Upload",
                            JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.PLAIN_MESSAGE
                    );

            return choice
                    == JOptionPane.OK_OPTION;

        } catch (Exception ex) {

            JOptionPane.showMessageDialog(
                    this,
                    "Could not preview CSV:\n"
                            + ex.getMessage(),
                    "CSV Preview Error",
                    JOptionPane.ERROR_MESSAGE
            );

            return false;
        }
    }

    // ============================================================
    // LOGOUT
    // ============================================================

    private void logout() {

        int confirm =
                JOptionPane.showConfirmDialog(
                        this,
                        "Sign out and return to the login screen?",
                        "Log Out",
                        JOptionPane.YES_NO_OPTION
                );

        if (
                confirm
                        != JOptionPane.YES_OPTION
        ) {

            return;
        }

        UserSession
                .getInstance()
                .logout();

        dispose();

        AuthService authService =
                new AuthService();

        SwingUtilities.invokeLater(
                () ->
                        new LoginFrame(
                                authService
                        ).setVisible(
                                true
                        )
        );
    }
}
package dashboard.gui;

import dashboard.database.AnalyticsApi;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Map;

public final class DrilldownDialog {

    private DrilldownDialog() {}


    // =========================================================
    // SALES
    // =========================================================

    public static void showSales(
            Component parent,
            String month,
            String category,
            String region
    ) {

        showSales(
                parent,
                month,
                null,
                category,
                region
        );
    }


    public static void showSales(
            Component parent,
            String month,
            String week,
            String category,
            String region
    ) {

        try {

            AnalyticsApi.TableData data =
                    AnalyticsApi.drilldownSales(
                            month,
                            week,
                            category,
                            region
                    );

            String title =
                    "Sales drill-down";

            if (
                    month != null
                    && !month.isBlank()
            ) {
                title += " - " + month;
            }

            if (
                    week != null
                    && !week.isBlank()
            ) {
                title += " - " + week;
            }

            if (
                    category != null
                    && !category.isBlank()
            ) {
                title += " - " + category;
            }

            if (
                    region != null
                    && !region.isBlank()
                    && !"All Regions".equals(region)
            ) {
                title += " - " + region;
            }

            showTable(
                    parent,
                    title,
                    data
            );

        } catch (Exception ex) {

            showError(
                    parent,
                    "Sales drill-down",
                    ex
            );
        }
    }


    // =========================================================
    // INVENTORY
    // =========================================================

    public static void showInventory(
            Component parent,
            String warehouse,
            Map<String, String> filters
    ) {

        try {

            AnalyticsApi.TableData data =
                    AnalyticsApi.drilldownInventory(
                            warehouse,
                            filters
                    );

            String title =
                    "Inventory drill-down";

            if (
                    warehouse != null
                    && !warehouse.isBlank()
            ) {
                title += " - " + warehouse;
            }

            showTable(
                    parent,
                    title,
                    data
            );

        } catch (Exception ex) {

            showError(
                    parent,
                    "Inventory drill-down",
                    ex
            );
        }
    }


    // =========================================================
    // MARKETING
    // =========================================================

    public static void showMarketing(
            Component parent,
            String channel,
            Map<String, String> filters
    ) {

        try {

            AnalyticsApi.TableData data =
                    AnalyticsApi.drilldownMarketing(
                            channel,
                            filters
                    );

            String title =
                    "Marketing drill-down";

            if (
                    channel != null
                    && !channel.isBlank()
            ) {
                title += " - " + channel;
            }

            showTable(
                    parent,
                    title,
                    data
            );

        } catch (Exception ex) {

            showError(
                    parent,
                    "Marketing drill-down",
                    ex
            );
        }
    }


    // =========================================================
    // COMMON TABLE WINDOW
    // =========================================================

    private static void showTable(
            Component parent,
            String title,
            AnalyticsApi.TableData data
    ) {

        if (data.rows().isEmpty()) {

            JOptionPane.showMessageDialog(
                    parent,
                    "No records were found for this selection.",
                    title,
                    JOptionPane.INFORMATION_MESSAGE
            );

            return;
        }


        DefaultTableModel model =
                new DefaultTableModel(
                        data.columns(),
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


        for (
                Object[] row :
                data.rows()
        ) {
            model.addRow(row);
        }


        JTable table =
                new JTable(model);

        table.setAutoCreateRowSorter(true);

        table.setFillsViewportHeight(true);

        table.setRowHeight(26);

        table.getTableHeader()
                .setReorderingAllowed(false);


        JScrollPane scroll =
                new JScrollPane(table);

        scroll.setPreferredSize(
                new Dimension(
                        950,
                        520
                )
        );

        showNonModalWindow(parent, title, scroll);
    }

    private static void showNonModalWindow(
            Component parent,
            String title,
            JComponent content
    ) {

        Window owner = SwingUtilities.getWindowAncestor(parent);

        JDialog dialog =
                new JDialog(owner, title, Dialog.ModalityType.MODELESS);

        dialog.setDefaultCloseOperation(
                WindowConstants.DISPOSE_ON_CLOSE
        );

        dialog.setLayout(new BorderLayout());
        dialog.add(content, BorderLayout.CENTER);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dialog.dispose());

        JPanel buttonPanel =
                new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        buttonPanel.add(closeButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.getRootPane().setBorder(new EmptyBorder(15, 15, 15, 15));

        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }


    // =========================================================
    // ERROR
    // =========================================================

    private static void showError(
            Component parent,
            String title,
            Exception ex
    ) {

        JOptionPane.showMessageDialog(
                parent,
                "Unable to load drill-down data:\n"
                        + ex.getMessage(),
                title + " error",
                JOptionPane.ERROR_MESSAGE
        );
    }
}
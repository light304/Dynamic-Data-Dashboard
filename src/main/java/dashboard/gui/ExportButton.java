package dashboard.gui;

import dashboard.report.ChartSpec.Page;
import dashboard.report.ReportBuilder;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.util.function.Supplier;

/**
 * The Export PDF button, shared by every page.
 *
 * The page supplies only which Page it is and how to read the current
 * filter; ChartCatalogue.reportFor decides what goes in the document.
 *
 * Replaces the old Reports page, where the user chose sections from a
 * list of tickboxes that had to be kept in step with the charts by hand.
 */
public final class ExportButton {

    private ExportButton() {}

    public static JButton create(Component parent,
                                 Page page,
                                 Supplier<DashboardFilter> filter) {

        JButton button = new JButton("Export PDF");

        button.setFont(Theme.BODY_STRONG);
        button.setForeground(Color.WHITE);
        button.setBackground(Theme.ACCENT);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(130, 38));
        button.setToolTipText("Save this page as a PDF report");

        button.addActionListener(e -> export(parent, page, filter.get(), button));

        return button;
    }

    private static void export(Component parent,
                               Page page,
                               DashboardFilter filter,
                               JButton button) {

        String pageName = name(page);

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save " + pageName + " report");
        chooser.setSelectedFile(
                new File(pageName + "-Report-" + LocalDate.now() + ".pdf"));
        chooser.setFileFilter(new FileNameExtensionFilter("PDF files", "pdf"));

        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;

        File chosen = chooser.getSelectedFile();

        // A typed filename often arrives without the extension.
        if (!chosen.getName().toLowerCase().endsWith(".pdf")) {
            chosen = new File(chosen.getParentFile(), chosen.getName() + ".pdf");
        }

        if (chosen.exists()) {
            int replace = JOptionPane.showConfirmDialog(
                    parent,
                    chosen.getName() + " already exists. Replace it?",
                    "Replace File",
                    JOptionPane.YES_NO_OPTION);

            if (replace != JOptionPane.YES_OPTION) return;
        }

        final File target = chosen;

        button.setEnabled(false);
        button.setText("Exporting...");

        new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() throws Exception {

                ReportBuilder.writePdf(
                        target,
                        page,
                        filter.toParams(),
                        filter.periodLabel());

                return null;
            }

            @Override
            protected void done() {

                button.setEnabled(true);
                button.setText("Export PDF");

                try {
                    get();

                    int open = JOptionPane.showConfirmDialog(
                            parent,
                            "Saved to " + target.getAbsolutePath()
                                    + "\n\nOpen it now?",
                            "Report Saved",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.INFORMATION_MESSAGE);

                    if (open == JOptionPane.YES_OPTION) openFile(target, parent);

                } catch (Exception ex) {

                    ex.printStackTrace();

                    JOptionPane.showMessageDialog(
                            parent,
                            "Could not write the report.\n\n" + ex.getMessage(),
                            "Export Failed",
                            JOptionPane.ERROR_MESSAGE);
                }
            }

        }.execute();
    }

    private static void openFile(File file, Component parent) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    parent,
                    "The report was saved, but could not be opened here.\n"
                            + file.getAbsolutePath(),
                    "Saved",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private static String name(Page page) {
        return switch (page) {
            case OVERVIEW  -> "Overview";
            case SALES     -> "Sales";
            case PRODUCTS  -> "Products";
            case INVENTORY -> "Inventory";
            case MARKETING -> "Marketing";
            case CUSTOMERS -> "Customers";
        };
    }
}
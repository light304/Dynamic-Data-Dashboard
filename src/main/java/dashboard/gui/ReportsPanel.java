package dashboard.gui;

import dashboard.report.ReportBuilder;
import dashboard.report.ReportSection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.time.LocalDate;

public class ReportsPanel extends JPanel implements FilterableDashboardPage {

    private static final Color BACKGROUND = new Color(245, 247, 250);
    private static final Color PRIMARY = new Color(31, 41, 55);

    private final Map<ReportSection, JCheckBox> boxes = new LinkedHashMap<>();
    private final JLabel periodLabel = new JLabel(" ");
    private DashboardFilter filter = DashboardFilter.defaults();
    private static File lastDirectory = null;

    public ReportsPanel() {
        setLayout(new BorderLayout(0, 15));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(20, 25, 25, 25));
        build();
    }

    private void build() {
        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Reports");
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        title.setForeground(PRIMARY);

        periodLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        periodLabel.setForeground(new Color(100, 116, 139));

        heading.add(title);
        heading.add(Box.createVerticalStrut(4));
        heading.add(periodLabel);
        add(heading, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        JLabel pick = new JLabel("Choose what to include:");
        pick.setFont(new Font("SansSerif", Font.BOLD, 14));
        pick.setForeground(PRIMARY);
        pick.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(pick);
        body.add(Box.createVerticalStrut(12));

        addGroup(body, "Recommended", true);
        body.add(Box.createVerticalStrut(14));
        addGroup(body, "Additional", false);

        body.add(Box.createVerticalStrut(20));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        buttons.setOpaque(false);
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JButton pdf = new JButton("Export PDF");
        pdf.setPreferredSize(new Dimension(140, 36));
        pdf.addActionListener(e -> export(true));

        JButton csv = new JButton("Export CSV");
        csv.setPreferredSize(new Dimension(140, 36));
        csv.addActionListener(e -> export(false));

        buttons.add(pdf);
        buttons.add(csv);
        body.add(buttons);

        add(body, BorderLayout.CENTER);
    }

    @Override
    public void applyFilter(DashboardFilter newFilter) {
        filter = newFilter == null ? DashboardFilter.defaults() : newFilter;
        periodLabel.setText("Report covers: " + periodText()
                + "  ·  " + filter.region());
    }

    private String periodText() {
        if ("Yearly".equalsIgnoreCase(filter.scope())) {
            return String.valueOf(filter.year());
        }
        if ("Weekly".equalsIgnoreCase(filter.scope())) {
            return filter.period() + " of " + filter.month() + " " + filter.year();
        }
        return filter.period() + " " + filter.year();
    }

    private void export(boolean asPdf) {

        Set<ReportSection> chosen = EnumSet.noneOf(ReportSection.class);
        boxes.forEach((section, box) -> {
            if (box.isSelected()) chosen.add(section);
        });

        if (chosen.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Select at least one section to include.",
                    "Nothing to Export", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String extension = asPdf ? "pdf" : "csv";
        String suggested = "Dashboard-Report-"
                + periodText().replace(" ", "-")
                + "-" + LocalDate.now()
                + "." + extension;

        JFileChooser chooser = new JFileChooser(lastDirectory);
        chooser.setDialogTitle("Save report");
        chooser.setSelectedFile(new File(suggested));
        chooser.setFileFilter(new FileNameExtensionFilter(
                extension.toUpperCase() + " files", extension));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File target = chooser.getSelectedFile();
        if (!target.getName().toLowerCase().endsWith("." + extension)) {
            target = new File(target.getAbsolutePath() + "." + extension);
        }

        lastDirectory = target.getParentFile();

        final File output = target;
        final Map<String, String> params = filter.toParams();
        final String period = periodText();

        JDialog loading = new JDialog(
                SwingUtilities.getWindowAncestor(this), "Generating",
                Dialog.ModalityType.APPLICATION_MODAL);
        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        content.add(new JLabel("Building " + output.getName() + "..."), BorderLayout.NORTH);
        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        content.add(bar, BorderLayout.CENTER);
        loading.setContentPane(content);
        loading.pack();
        loading.setLocationRelativeTo(this);
        loading.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                if (asPdf) {
                    ReportBuilder.writePdf(output, params, period, chosen);
                } else {
                    ReportBuilder.writeCsv(output, params, period, chosen);
                }
                return null;
            }

            @Override
            protected void done() {
                loading.dispose();
                try {
                    get();
                    JOptionPane.showMessageDialog(ReportsPanel.this,
                            "Report saved to:\n" + output.getAbsolutePath(),
                            "Report Complete", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ReportsPanel.this,
                            "Could not generate the report.\n\n" + ex.getMessage(),
                            "Report Failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();

        loading.setVisible(true);
    }

    private void addGroup(JPanel body, String title, boolean defaults) {

        JLabel heading = new JLabel(title);
        heading.setFont(new Font("SansSerif", Font.BOLD, 12));
        heading.setForeground(new Color(100, 116, 139));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(heading);
        body.add(Box.createVerticalStrut(6));

        for (ReportSection section : ReportSection.values()) {
            if (section.onByDefault() != defaults) continue;

            JCheckBox box = new JCheckBox(section.label(), section.onByDefault());
            box.setOpaque(false);
            box.setFont(new Font("SansSerif", Font.PLAIN, 13));
            box.setAlignmentX(Component.LEFT_ALIGNMENT);

            box.setToolTipText("<html><div style='width:320px'>"
                    + section.description() + "</div></html>");
            boxes.put(section, box);
            body.add(box);
            body.add(Box.createVerticalStrut(4));

            boxes.put(section, box);
            body.add(box);
            body.add(Box.createVerticalStrut(4));
        }
    }
}
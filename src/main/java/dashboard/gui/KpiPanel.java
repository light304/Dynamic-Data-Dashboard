
package dashboard.gui;

import dashboard.database.AnalyticsApi.Kpis;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class KpiPanel extends JPanel {

    private static final Color PRIMARY_TEXT = Theme.TEXT;
    private static final Color SECONDARY_TEXT = Theme.TEXT_MUTED;
    private static final Color BORDER_COLOR = Theme.BORDER;

    private static final Color GREEN = new Color(22, 163, 74);
    private static final Color RED = new Color(220, 38, 38);

    private static final String[] TITLES = {
        "Revenue", "Growth", "Net Profit",
        "Gross Margin", "Turnover",
        "Retention", "Cost / Conv."
    };

    private static final String[] NAMES = {
        "Total Revenue",
        "Revenue Growth Rate",
        "Profit (Net)",
        "Gross Profit Margin",
        "Inventory Turnover",
        "Customer Retention",
        "Cost per Conversion"
    };

    private static final String[] DESCRIPTIONS = {
        "The total sales income before costs are deducted.",
        "The percentage change in revenue against the preceding period of equal length.",
        "Revenue remaining after cost of goods sold and marketing spend.",
        "Gross profit as a percentage of revenue, before marketing.",
        "The number of times average inventory was sold through.",
        "The proportion of pre-existing customers who purchased again.",
        "Average marketing spend required to secure one conversion."
    };

    private static final String[] SOURCES = {
        "sales",
        "sales",
        "sales + products + marketing",
        "sales + products",
        "inventory + products + sales",
        "customers + sales",
        "marketing"
    };

    private final JLabel[] values = new JLabel[7];
    private final JLabel[] changes = new JLabel[7];
    private final JPanel[] cards = new JPanel[7];

    public KpiPanel() {

        setLayout(new GridLayout(1, 7, 8, 0));
        setBackground(Theme.PAGE_BG);

        for (int i = 0; i < 7; i++) {

            values[i] = new JLabel("...");
            changes[i] = new JLabel(" ");

            cards[i] = createCard(
                TITLES[i],
                values[i],
                changes[i],
                i != 1
            );

            add(cards[i]);

            tooltip(i, null, SOURCES[i]);
        }
    }

    private JPanel createCard(
            String title,
            JLabel valueLabel,
            JLabel changeLabel,
            boolean showChange
    ) {

        JPanel card = new JPanel();

        card.setLayout(
            new BoxLayout(card, BoxLayout.Y_AXIS)
        );

        card.setBackground(Color.WHITE);

        card.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                new EmptyBorder(7, 10, 7, 10)
            )
        );

        JLabel titleLabel = new JLabel(title);

        titleLabel.setFont(Theme.KPI_TITLE);
        titleLabel.setForeground(SECONDARY_TEXT);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        valueLabel.setFont(Theme.KPI_VALUE);
        valueLabel.setForeground(PRIMARY_TEXT);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(titleLabel);
        card.add(Box.createVerticalStrut(2));
        card.add(valueLabel);

        if (showChange) {

            changeLabel.setFont(
                new Font("SansSerif", Font.BOLD, 11)
            );

            changeLabel.setForeground(SECONDARY_TEXT);
            changeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            card.add(Box.createVerticalStrut(4));
            card.add(changeLabel);
        }

        return card;
    }

    private void setChange(
            int index,
            Double change,
            boolean lowerIsBetter,
            boolean percentagePoints
    ) {

        JLabel label = changes[index];

        if (change == null || !Double.isFinite(change)) {
            label.setText("N/A");
            label.setForeground(SECONDARY_TEXT);
            return;
        }

        String unit = percentagePoints ? " pp" : "%";

        if (change > 0) {

            label.setText(
                String.format("↑ %.1f%s", change, unit)
            );

            label.setForeground(
                lowerIsBetter ? RED : GREEN
            );

        } else if (change < 0) {

            label.setText(
                String.format(
                    "↓ %.1f%s",
                    Math.abs(change),
                    unit
                )
            );

            label.setForeground(
                lowerIsBetter ? GREEN : RED
            );

        } else {

            label.setText("— 0.0" + unit);
            label.setForeground(SECONDARY_TEXT);
        }
    }

    public void update(Kpis kpis) {

        values[0].setText(AxisScale.money(kpis.revenue()));

        values[1].setText(
            kpis.growth() == null
                ? "N/A"
                : String.format("%.1f%%", kpis.growth())
        );

        values[2].setText(AxisScale.money(kpis.profit()));

        values[3].setText(
            String.format("%.1f%%", kpis.margin())
        );

        values[4].setText(
            String.format("%.2f", kpis.turnover())
        );

        values[5].setText(
            String.format("%.1f%%", kpis.retention())
        );

        values[6].setText(
            String.format("$%,.2f", kpis.costPerConversion())
        );

        // Green/red change indicators
        setChange(0, kpis.growth(), false, false);
        setChange(2, kpis.profitChange(), false, false);
        setChange(3, kpis.marginChange(), false, true);
        setChange(4, kpis.turnoverChange(), false, false);
        setChange(5, kpis.retentionChange(), false, true);
        setChange(6, kpis.costConversionChange(), true, false);

        tooltip(
            0,
            String.format("$%,.2f", kpis.revenue()),
            SOURCES[0]
        );

        tooltip(
            1,
            kpis.growth() == null
                ? "Not available for this period"
                : String.format("%.2f%%", kpis.growth()),
            SOURCES[1]
        );

        tooltip(
            2,
            String.format("$%,.2f", kpis.profit()),
            "sales + products" +
                (kpis.profitIncludesMarketing()
                    ? " − marketing"
                    : " (gross — marketing has no region)")
        );

        tooltip(
            3,
            String.format("%.2f%%", kpis.margin()),
            SOURCES[3]
        );

        tooltip(
            4,
            String.format("%.3f", kpis.turnover()),
            SOURCES[4] +
                (kpis.turnoverRegionIgnored()
                    ? " (national — inventory has no region)"
                    : "")
        );

        tooltip(
            5,
            String.format("%.2f%%", kpis.retention()),
            SOURCES[5]
        );

        tooltip(
            6,
            String.format("$%,.2f", kpis.costPerConversion()),
            SOURCES[6]
        );

        revalidate();
        repaint();
    }

    private void tooltip(
            int index,
            String exact,
            String calculation
    ) {

        String content =
            "<html><div style='width:260px'><b>"
            + NAMES[index]
            + "</b><br>"
            + DESCRIPTIONS[index]
            + (exact == null
                ? ""
                : "<br><br>Exact value: <b>"
                    + exact + "</b>")
            + "<br><br><i>Calculation: "
            + calculation
            + "</i></div></html>";

        cards[index].setToolTipText(content);

        // Tooltips also work when hovering over labels
        values[index].setToolTipText(content);
        changes[index].setToolTipText(content);
    }

    public void showError() {

        for (int i = 0; i < 7; i++) {
            values[i].setText("N/A");
            changes[i].setText("N/A");
            changes[i].setForeground(SECONDARY_TEXT);
        }
    }
}

package dashboard.gui;

import dashboard.database.AnalyticsApi.Kpis;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * The seven headline indicators, in one row.
 *
 * Each card shows a short title and a compact value ($1.23M). The full
 * name, a plain-English definition, the exact figure and the tables it is
 * calculated from are in the card's tooltip, which keeps the row to a
 * single line of the screen.
 */
public class KpiPanel extends JPanel {

    private static final Color PRIMARY_TEXT   = Theme.TEXT;
    private static final Color SECONDARY_TEXT = Theme.TEXT_MUTED;
    private static final Color BORDER_COLOR   = Theme.BORDER;

    // Plain-English explanation of each indicator, shown in the tooltip.
    private static final String REVENUE_DESC =
            "The total sales income (money earned) for the selected period, before costs are deducted.";

    private static final String GROWTH_DESC =
            "The percentage change in revenue against the preceding period of equal length.";

    private static final String PROFIT_DESC =
            "The revenue remaining after cost of goods sold and marketing spend.";

    private static final String MARGIN_DESC =
            "The gross profit expressed as a percentage of revenue, before marketing.";

    private static final String TURNOVER_DESC =
            "The number of times average inventory was sold through during the period.";

    private static final String RETENTION_DESC =
            "The proportion of pre-existing customers who purchased again in the period.";

    private static final String COST_PER_CONVERSION_DESC =
            "The average marketing spend required to secure one conversion (usually an interaction).";

    private final JLabel revenueValue = new JLabel("...");
    private final JLabel growthValue = new JLabel("...");
    private final JLabel profitValue = new JLabel("...");
    private final JLabel marginValue = new JLabel("...");
    private final JLabel turnoverValue = new JLabel("...");
    private final JLabel retentionValue = new JLabel("...");
    private final JLabel costPerConversionValue = new JLabel("...");

    private final JPanel revenueCard;
    private final JPanel growthCard;
    private final JPanel profitCard;
    private final JPanel marginCard;
    private final JPanel turnoverCard;
    private final JPanel retentionCard;
    private final JPanel costPerConversionCard;

    public KpiPanel() {

        setLayout(new GridLayout(1, 7, 8, 0));
        setBackground(Theme.PAGE_BG);

        revenueCard = createCard("Revenue", revenueValue);
        growthCard = createCard("Growth", growthValue);
        profitCard = createCard("Net Profit", profitValue);
        marginCard = createCard("Gross Margin", marginValue);
        turnoverCard = createCard("Turnover", turnoverValue);
        retentionCard = createCard("Retention", retentionValue);
        costPerConversionCard = createCard("Cost / Conv.", costPerConversionValue);

        add(revenueCard);
        add(growthCard);
        add(profitCard);
        add(marginCard);
        add(turnoverCard);
        add(retentionCard);
        add(costPerConversionCard);

        // Until the first load completes, the tooltips still explain each card.
        tooltip(revenueCard, "Total Revenue", REVENUE_DESC, null, "sales");
        tooltip(growthCard, "Revenue Growth Rate", GROWTH_DESC, null, "sales");
        tooltip(profitCard, "Profit (Net)", PROFIT_DESC, null, "sales + products + marketing");
        tooltip(marginCard, "Gross Profit Margin", MARGIN_DESC, null, "sales + products");
        tooltip(turnoverCard, "Inventory Turnover", TURNOVER_DESC, null,
                "inventory + products + sales");
        tooltip(retentionCard, "Customer Retention", RETENTION_DESC, null,
                "customers + sales");
        tooltip(costPerConversionCard, "Cost per Conversion", COST_PER_CONVERSION_DESC,
                null, "marketing");
    }

    private JPanel createCard(String title, JLabel valueLabel) {

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                new EmptyBorder(7, 10, 7, 10)));

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

        return card;
    }

    /**
     * Updates all seven cards from the analytics backend.
     *
     * Large money figures are shortened ($1.23M) so seven cards fit on one
     * row; the exact figure is in each tooltip.
     */
    public void update(Kpis kpis) {

        revenueValue.setText(AxisScale.money(kpis.revenue()));
        growthValue.setText(kpis.growth() == null
                ? "N/A"
                : String.format("%.1f%%", kpis.growth()));
        profitValue.setText(AxisScale.money(kpis.profit()));
        marginValue.setText(String.format("%.1f%%", kpis.margin()));
        turnoverValue.setText(String.format("%.2f", kpis.turnover()));
        retentionValue.setText(String.format("%.1f%%", kpis.retention()));
        costPerConversionValue.setText(String.format("$%,.2f", kpis.costPerConversion()));

        tooltip(revenueCard, "Total Revenue", REVENUE_DESC,
                String.format("$%,.2f", kpis.revenue()), "sales");

        tooltip(growthCard, "Revenue Growth Rate", GROWTH_DESC,
                kpis.growth() == null ? "Not available for this period"
                        : String.format("%.2f%%", kpis.growth()),
                "sales");

        tooltip(profitCard, "Profit (Net)", PROFIT_DESC,
                String.format("$%,.2f", kpis.profit()),
                "sales + products"
                        + (kpis.profitIncludesMarketing()
                                ? " \u2212 marketing"
                                : " (gross \u2014 marketing has no region)"));

        tooltip(marginCard, "Gross Profit Margin", MARGIN_DESC,
                String.format("%.2f%%", kpis.margin()), "sales + products");

        tooltip(turnoverCard, "Inventory Turnover", TURNOVER_DESC,
                String.format("%.3f", kpis.turnover()),
                "inventory + products + sales"
                        + (kpis.turnoverRegionIgnored()
                                ? " (national \u2014 inventory has no region)"
                                : ""));

        tooltip(retentionCard, "Customer Retention", RETENTION_DESC,
                String.format("%.2f%%", kpis.retention()), "customers + sales");

        tooltip(costPerConversionCard, "Cost per Conversion", COST_PER_CONVERSION_DESC,
                String.format("$%,.2f", kpis.costPerConversion()), "marketing");
    }

    /**
     * Tooltip: full name, definition, the exact figure, then the tables the
     * figure is calculated from.
     */
    private void tooltip(JPanel card, String name, String description,
                         String exact, String calculation) {

        if (card == null) return;

        card.setToolTipText(
                "<html><div style='width:260px'><b>" + name + "</b><br>"
                + description
                + (exact == null ? "" : "<br><br>Exact value: <b>" + exact + "</b>")
                + "<br><br><i>Calculation: " + calculation + "</i></div></html>");
    }

    public void showError() {

        revenueValue.setText("N/A");
        growthValue.setText("N/A");
        profitValue.setText("N/A");
        marginValue.setText("N/A");
        turnoverValue.setText("N/A");
        retentionValue.setText("N/A");
        costPerConversionValue.setText("N/A");
    }
}
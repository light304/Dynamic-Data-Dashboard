package dashboard.gui;

import dashboard.database.AnalyticsApi.Kpis;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class KpiPanel extends JPanel {

    private static final Color BACKGROUND_COLOR =
            new Color(245, 247, 250);

    private static final Color PRIMARY_TEXT =
            new Color(31, 41, 55);

    private static final Color SECONDARY_TEXT =
            new Color(100, 116, 139);

    private static final Color BORDER_COLOR =
            new Color(226, 232, 240);

//     private static final Color POSITIVE_COLOR =
//             new Color(22, 163, 74);

//     private static final Color NEGATIVE_COLOR =
//             new Color(220, 38, 38);

    private final JLabel revenueValue =
            new JLabel("Loading...");

    private final JLabel growthValue =
            new JLabel("Loading...");

    private final JLabel profitValue =
            new JLabel("Loading...");

    private final JLabel marginValue =
            new JLabel("Loading...");

    private final JLabel turnoverValue =
            new JLabel("Loading...");

    private final JLabel retentionValue =
            new JLabel("Loading...");

    private final JLabel costPerConversionValue =
            new JLabel("Loading...");

    private final JLabel revenueDelta =
            new JLabel("");

    private final JLabel growthDelta =
            new JLabel("");

    private final JLabel profitDelta =
            new JLabel("");

    private final JLabel marginDelta =
            new JLabel("");

    private final JLabel turnoverDelta =
            new JLabel("");

    private final JLabel retentionDelta =
            new JLabel("");

    private final JLabel costPerConversionDelta =
            new JLabel("");

    /*
     * The card panels are kept so that update() can put the calculation
     * provenance into each card's tooltip once the flags are known.
     */
    private JPanel revenueCard;
    private JPanel growthCard;
    private JPanel profitCard;
    private JPanel marginCard;
    private JPanel turnoverCard;
    private JPanel retentionCard;
    private JPanel costPerConversionCard;

    // =========================================================
    // CARD DESCRIPTIONS
    //
    // Plain-English explanation of each indicator, shown on the
    // card itself. Reword these freely - they are the only place
    // the wording lives.
    // =========================================================

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
            "The average marketing spend required to secure one conversion.";

    public KpiPanel() {

        setLayout(
                new GridLayout(
                        2,
                        4,
                        12,
                        12
                )
        );

        setBackground(
                BACKGROUND_COLOR
        );

        createCards();
    }

    private void createCards() {

        revenueCard = createCard(
                "Total Revenue",
                revenueValue,
                REVENUE_DESC,
                revenueDelta
        );

        growthCard = createCard(
                "Revenue Growth Rate",
                growthValue,
                GROWTH_DESC,
                growthDelta
        );

        profitCard = createCard(
                "Profit (Net)",
                profitValue,
                PROFIT_DESC,
                profitDelta
        );

        marginCard = createCard(
                "Gross Profit Margin",
                marginValue,
                MARGIN_DESC,
                marginDelta
        );

        turnoverCard = createCard(
                "Inventory Turnover",
                turnoverValue,
                TURNOVER_DESC,
                turnoverDelta
        );

        retentionCard = createCard(
                "Customer Retention",
                retentionValue,
                RETENTION_DESC,
                retentionDelta
        );

        costPerConversionCard = createCard(
                "Cost per Conversion",
                costPerConversionValue,
                COST_PER_CONVERSION_DESC,
                costPerConversionDelta
        );

        add(revenueCard);
        add(growthCard);
        add(profitCard);
        add(marginCard);
        add(turnoverCard);
        add(retentionCard);
        add(costPerConversionCard);

        JPanel emptyPanel =
                new JPanel();

        emptyPanel.setBackground(
                BACKGROUND_COLOR
        );

        add(emptyPanel);
    }

    private JPanel createCard(
            String title,
            JLabel valueLabel,
            String description,
            JLabel deltaLabel
    ) {

        JPanel card =
                new JPanel();

        card.setLayout(
                new BoxLayout(
                        card,
                        BoxLayout.Y_AXIS
                )
        );

        card.setBackground(
                Color.WHITE
        );

        card.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                BORDER_COLOR
                        ),
                        new EmptyBorder(
                                12,
                                15,
                                12,
                                15
                        )
                )
        );

        JLabel titleLabel =
                new JLabel(
                        title
                );

        titleLabel.setFont(
                Theme.BODY
        );

        titleLabel.setForeground(
                SECONDARY_TEXT
        );

        titleLabel.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        valueLabel.setFont(
                Theme.CARD_VALUE
        );

        valueLabel.setForeground(
                PRIMARY_TEXT
        );

        valueLabel.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        /*
         * JTextArea rather than JLabel: a JLabel will not wrap plain
         * text, and the HTML-in-JLabel alternative behaves badly inside
         * a fixed-size BoxLayout cell.
         */
        JTextArea descriptionArea =
                new JTextArea(description);

        descriptionArea.setFont(
                Theme.SMALL
        );

        descriptionArea.setForeground(
                SECONDARY_TEXT
        );

        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setEditable(false);
        descriptionArea.setFocusable(false);
        descriptionArea.setOpaque(false);
        descriptionArea.setBorder(null);

        descriptionArea.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        deltaLabel.setFont(
                Theme.SMALL
        );

        deltaLabel.setForeground(
                SECONDARY_TEXT
        );

        deltaLabel.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        card.add(titleLabel);

        card.add(
                Box.createVerticalStrut(
                        7
                )
        );

        card.add(valueLabel);

        card.add(
                Box.createVerticalStrut(
                        6
                )
        );

        card.add(descriptionArea);

        card.add(
                Box.createVerticalStrut(
                        6
                )
        );

        card.add(deltaLabel);

        return card;
    }


    /**
     * Updates all seven Overview cards from the analytics backend.
     *
     * The tables each figure touches, and any region caveat, go into the
     * card's tooltip rather than onto the card, so the delta line stays
     * free for the period-on-period arrows.
     */
    public void update(Kpis kpis) {
        revenueValue.setText(String.format("$%,.2f", kpis.revenue()));
        growthValue.setText(
                kpis.growth() == null
                        ? "N/A"
                        : String.format("%.2f%%", kpis.growth()));
        profitValue.setText(String.format("$%,.2f", kpis.profit()));
        marginValue.setText(String.format("%.2f%%", kpis.margin()));
        turnoverValue.setText(String.format("%.3f", kpis.turnover()));
        retentionValue.setText(String.format("%.2f%%", kpis.retention()));
        costPerConversionValue.setText(String.format("$%,.2f", kpis.costPerConversion()));

        tooltip(revenueCard, REVENUE_DESC,
                "Calculation: sales");

        tooltip(growthCard, GROWTH_DESC,
                "Calculation: sales");

        tooltip(profitCard, PROFIT_DESC,
                "Calculation: sales + products"
                + (kpis.profitIncludesMarketing()
                        ? " − marketing"
                        : " (gross — marketing has no region)"));

        tooltip(marginCard, MARGIN_DESC,
                "Calculation: sales + products");

        tooltip(turnoverCard, TURNOVER_DESC,
                "Calculation: inventory + products + sales"
                + (kpis.turnoverRegionIgnored()
                        ? " (national — inventory has no region)"
                        : ""));

        tooltip(retentionCard, RETENTION_DESC,
                "Calculation: customers + sales");

        tooltip(costPerConversionCard, COST_PER_CONVERSION_DESC,
                "Calculation: marketing");
    }

    /**
     * Builds a two-line tooltip: the plain-English description, then the
     * tables the figure is derived from.
     */
    private void tooltip(JPanel card, String description, String calculation) {

        if (card == null) return;

        card.setToolTipText(
                "<html><div style='width:260px'>"
                + description
                + "<br><br><i>"
                + calculation
                + "</i></div></html>"
        );
    }

    public void showError() {

        revenueValue.setText("Unavailable");
        growthValue.setText("Unavailable");
        profitValue.setText("Unavailable");
        marginValue.setText("Unavailable");
        turnoverValue.setText("Unavailable");
        retentionValue.setText("Unavailable");
        costPerConversionValue.setText("Unavailable");

        for (JLabel label : new JLabel[]{
                revenueDelta, growthDelta, profitDelta, marginDelta,
                turnoverDelta, retentionDelta, costPerConversionDelta}) {
            label.setText("");
        }
    }
}
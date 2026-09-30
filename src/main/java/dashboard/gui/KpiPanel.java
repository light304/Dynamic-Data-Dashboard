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

    private static final Color POSITIVE_COLOR =
            new Color(22, 163, 74);

    private static final Color NEGATIVE_COLOR =
            new Color(220, 38, 38);

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
            new JLabel("--");

    private final JLabel growthDelta =
            new JLabel("--");

    private final JLabel profitDelta =
            new JLabel("--");

    private final JLabel marginDelta =
            new JLabel("--");

    private final JLabel turnoverDelta =
            new JLabel("--");

    private final JLabel retentionDelta =
            new JLabel("--");

    private final JLabel costPerConversionDelta =
            new JLabel("--");

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

        add(
                createCard(
                        "Total Revenue",
                        revenueValue,
                        revenueDelta
                )
        );

        add(
                createCard(
                        "Revenue Growth Rate",
                        growthValue,
                        growthDelta
                )
        );

        add(
                createCard(
                        "Profit (Net)",
                        profitValue,
                        profitDelta
                )
        );

        add(
                createCard(
                        "Gross Profit Margin",
                        marginValue,
                        marginDelta
                )
        );

        add(
                createCard(
                        "Inventory Turnover",
                        turnoverValue,
                        turnoverDelta
                )
        );

        add(
                createCard(
                        "Customer Retention",
                        retentionValue,
                        retentionDelta
                )
        );

        add(
                createCard(
                        "Cost per Conversion",
                        costPerConversionValue,
                        costPerConversionDelta
                )
        );

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
                new JLabel(title);

        titleLabel.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        12
                )
        );

        titleLabel.setForeground(
                SECONDARY_TEXT
        );

        titleLabel.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        valueLabel.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        19
                )
        );

        valueLabel.setForeground(
                PRIMARY_TEXT
        );

        valueLabel.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        deltaLabel.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        11
                )
        );

        deltaLabel.setForeground(
                SECONDARY_TEXT
        );

        deltaLabel.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        card.add(titleLabel);

        card.add(
                Box.createVerticalStrut(7)
        );

        card.add(valueLabel);

        card.add(
                Box.createVerticalStrut(5)
        );

        card.add(deltaLabel);

        return card;
    }

    public void update(Kpis kpis) {

        // Existing comparison already supplied by the backend
        Double delta = kpis.growth();

        String arrow = "";

        if (delta != null) {

            if (delta > 0) {
                arrow = "↑";
            } else if (delta < 0) {
                arrow = "↓";
            } else {
                arrow = "→";
            }
        }

        /*
         * KPI VALUES
         */

        revenueValue.setText(
                String.format(
                        "$%,.2f %s",
                        kpis.revenue(),
                        arrow
                )
        );

        growthValue.setText(
                kpis.growth() == null
                        ? "N/A"
                        : String.format(
                                "%.2f%% %s",
                                Math.abs(kpis.growth()),
                                arrow
                        )
        );

        profitValue.setText(
                String.format(
                        "$%,.2f %s",
                        kpis.profit(),
                        arrow
                )
        );

        marginValue.setText(
                String.format(
                        "%.2f%% %s",
                        kpis.margin(),
                        arrow
                )
        );

        turnoverValue.setText(
                String.format(
                        "%.3f %s",
                        kpis.turnover(),
                        arrow
                )
        );

        retentionValue.setText(
                String.format(
                        "%.2f%% %s",
                        kpis.retention(),
                        arrow
                )
        );

        costPerConversionValue.setText(
                String.format(
                        "$%,.2f %s",
                        kpis.costPerConversion(),
                        arrow
                )
        );

        /*
         * KEEP EXISTING CALCULATION INFORMATION
         */

        revenueDelta.setText(
                "Calculation: sales"
        );

        growthDelta.setText(
                "Calculation: sales"
        );

        profitDelta.setText(
                "<html><div style='width:160px'>"
                        + "Calculation: sales + products"
                        + (kpis.profitIncludesMarketing()
                        ? " − marketing"
                        : " (gross — marketing has no region)")
                        + "</div></html>"
        );

        marginDelta.setText(
                "Calculation: sales + products"
        );

        turnoverDelta.setText(
                "<html><div style='width:160px'>"
                        + "Calculation: inventory + products + sales"
                        + (kpis.turnoverRegionIgnored()
                        ? " (national — inventory has no region)"
                        : "")
                        + "</div></html>"
        );

        retentionDelta.setText(
                "Calculation: customers + sales"
        );

        costPerConversionDelta.setText(
                "<html><div style='width:160px'>"
                        + "Calculation: marketing"
                        + "</div></html>"
        );

        /*
         * ARROW COLOUR
         */

        if (delta == null || delta == 0) {

            revenueValue.setForeground(PRIMARY_TEXT);
            growthValue.setForeground(PRIMARY_TEXT);
            profitValue.setForeground(PRIMARY_TEXT);
            marginValue.setForeground(PRIMARY_TEXT);
            turnoverValue.setForeground(PRIMARY_TEXT);
            retentionValue.setForeground(PRIMARY_TEXT);
            costPerConversionValue.setForeground(PRIMARY_TEXT);

        } else {

            Color normalColour =
                    delta > 0
                            ? POSITIVE_COLOR
                            : NEGATIVE_COLOR;

            revenueValue.setForeground(normalColour);
            growthValue.setForeground(normalColour);
            profitValue.setForeground(normalColour);
            marginValue.setForeground(normalColour);
            turnoverValue.setForeground(normalColour);
            retentionValue.setForeground(normalColour);

            /*
             * Cost per conversion is opposite:
             * lower cost = good
             * higher cost = bad
             */
            costPerConversionValue.setForeground(
                    delta > 0
                            ? NEGATIVE_COLOR
                            : POSITIVE_COLOR
            );
        }

        /*
         * Calculation text stays grey
         */

        for (JLabel label : new JLabel[]{
                revenueDelta,
                growthDelta,
                profitDelta,
                marginDelta,
                turnoverDelta,
                retentionDelta,
                costPerConversionDelta
        }) {

            label.setForeground(
                    SECONDARY_TEXT
            );
        }
    }

    public void showError() {

        revenueValue.setText("Unavailable");
        growthValue.setText("Unavailable");
        profitValue.setText("Unavailable");
        marginValue.setText("Unavailable");
        turnoverValue.setText("Unavailable");
        retentionValue.setText("Unavailable");
        costPerConversionValue.setText("Unavailable");

        revenueValue.setForeground(PRIMARY_TEXT);
        growthValue.setForeground(PRIMARY_TEXT);
        profitValue.setForeground(PRIMARY_TEXT);
        marginValue.setForeground(PRIMARY_TEXT);
        turnoverValue.setForeground(PRIMARY_TEXT);
        retentionValue.setForeground(PRIMARY_TEXT);
        costPerConversionValue.setForeground(PRIMARY_TEXT);
    }
}
package dashboard.gui;

import dashboard.database.ApiClient;
import dashboard.database.SchemaIntrospector;
import dashboard.database.SchemaIntrospector.ComparisonRow;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.NumberFormat;

import java.util.List;
import java.util.Locale;

public class RevenueByRegionChart extends JPanel {

    private static final Color ACTIVE_COLOR =
            new Color(0, 212, 255);

    private static final Color PRIMARY_TEXT =
            new Color(31, 41, 55);

    private static final Color SECONDARY_TEXT =
            new Color(100, 116, 139);

    private static final Color BORDER_COLOR =
            new Color(226, 232, 240);

    private DefaultCategoryDataset dataset;

    private JLabel statusLabel;

    public RevenueByRegionChart() {

        configurePanel();
        createChart();
    }

    private void configurePanel() {

        setLayout(
                new BorderLayout(
                        0,
                        8
                )
        );

        setBackground(
                Color.WHITE
        );

        setBorder(
                BorderFactory.createCompoundBorder(

                        BorderFactory.createLineBorder(
                                BORDER_COLOR
                        ),

                        new EmptyBorder(
                                15,
                                16,
                                12,
                                16
                        )
                )
        );
    }

    private void createChart() {

        JPanel titlePanel =
                new JPanel();

        titlePanel.setLayout(
                new BoxLayout(
                        titlePanel,
                        BoxLayout.Y_AXIS
                )
        );

        titlePanel.setBackground(
                Color.WHITE
        );

        JLabel title =
                new JLabel(
                        "Revenue By Region"
                );

        title.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        17
                )
        );

        title.setForeground(
                PRIMARY_TEXT
        );

        title.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        JLabel description =
                new JLabel(
                        "Total revenue grouped by sales region"
                );

        description.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        12
                )
        );

        description.setForeground(
                SECONDARY_TEXT
        );

        description.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        titlePanel.add(
                title
        );

        titlePanel.add(
                Box.createVerticalStrut(
                        2
                )
        );

        titlePanel.add(
                description
        );

        add(
                titlePanel,
                BorderLayout.NORTH
        );

        dataset =
                new DefaultCategoryDataset();

        JFreeChart chart =
                ChartFactory.createBarChart(
                        null,
                        "Region",
                        "Revenue ($)",
                        dataset,
                        PlotOrientation.HORIZONTAL,
                        false,
                        true,
                        false
                );

        chart.setBackgroundPaint(
                Color.WHITE
        );

        CategoryPlot plot =
                chart.getCategoryPlot();

        plot.setBackgroundPaint(
                Color.WHITE
        );

        plot.setOutlineVisible(
                false
        );

        plot.setRangeGridlinePaint(
                BORDER_COLOR
        );

        BarRenderer renderer =
                (BarRenderer)
                        plot.getRenderer();

        renderer.setSeriesPaint(
                0,
                ACTIVE_COLOR
        );

        renderer.setShadowVisible(
                false
        );

        renderer.setMaximumBarWidth(
                0.10
        );

        NumberAxis revenueAxis =
                (NumberAxis)
                        plot.getRangeAxis();

        revenueAxis.setNumberFormatOverride(
                NumberFormat.getCurrencyInstance(
                        Locale.US
                )
        );

        ChartPanel chartPanel =
                new ChartPanel(
                        chart
                );

        chartPanel.setMouseWheelEnabled(
                false
        );

        chartPanel.setBackground(
                Color.WHITE
        );

        add(
                chartPanel,
                BorderLayout.CENTER
        );

        statusLabel =
                new JLabel(
                        "Waiting for regional sales data..."
                );

        statusLabel.setForeground(
                SECONDARY_TEXT
        );

        statusLabel.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        12
                )
        );

        add(
                statusLabel,
                BorderLayout.SOUTH
        );
    }

    /*
     * Accepts the exact same Sales page filter.
     *
     * Current backend limitation:
     * /api/query/compare gives us region + revenue,
     * but not region + revenue + order_date together.
     *
     * So regional values are still real database
     * totals, but cannot yet be time-filtered.
     */
    public void applyFilters(
            int selectedYear,
            String scope,
            String selectedMonth,
            String period
    ) {

        try {
            java.util.Map<String, String> params =
                    new java.util.LinkedHashMap<>();

            params.put("year", String.valueOf(selectedYear));
            params.put("scope", scope);
            params.put("period", period);

            if (selectedMonth != null) {
                params.put("month", selectedMonth);
            }

            String json =
                    ApiClient.getData(
                            "api/sales/revenue-region",
                            params
                    );

            List<ComparisonRow> rows =
                    SchemaIntrospector
                            .parseCompareRows(json);

            dataset.clear();

            for (ComparisonRow row : rows) {
                dataset.addValue(
                        row.value,
                        "Revenue",
                        row.label
                );
            }

            if ("Weekly".equals(scope)) {
                statusLabel.setText(
                        selectedYear + " • " + selectedMonth + " • " + period
                                + " • Live filtered regional revenue"
                );
            } else {
                statusLabel.setText(
                        selectedYear + " • " + scope + " • " + period
                                + " • Live filtered regional revenue"
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
            dataset.clear();
            statusLabel.setText(
                    "Unable to load regional revenue"
            );
        }
    }
    /** Receives the dashboard-wide filter and reuses the existing chart filter logic. */
    public void applyFilter(DashboardFilter filter) {
        if (filter == null) filter = DashboardFilter.defaults();
        String selectedMonth = "Weekly".equals(filter.scope()) ? filter.month() : null;
        applyFilters(filter.year(), filter.scope(), selectedMonth, filter.period());
    }

}
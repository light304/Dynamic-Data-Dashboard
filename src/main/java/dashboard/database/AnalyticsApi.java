package dashboard.database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Thin wrapper around the Node/SQLite analytics endpoints.
 * Reuses the project's dependency-free MiniJson parser so no new JSON library
 * is required in Maven.
 */
public final class AnalyticsApi {

    private AnalyticsApi() {}

    // =========================================================
    // DATA RECORDS
    // =========================================================

    public record Point(
            String label,
            double value
    ) {}

    public record SeriesPoint(
            String label,
            String series,
            double value
    ) {}

    public record XYPoint(
            String label,
            String category,
            double x,
            double y
    ) {}

    public record LowStockAlert(
            int productId,
            String category,
            String warehouse,
            int stockLevel
    ) {}

    public record AlertThreshold(
            String key,
            String displayName,
            double value,
            String unit
    ) {}

    public record ActiveAlert(
            int alertId,
            String alertType,
            String entityKey,
            String title,
            String message,
            double currentValue,
            double thresholdValue,
            String severity,
            String status,
            String createdAt
    ) {}

    public record Kpis(
            double revenue,
            Double growth,
            double profit,
            double margin,
            double turnover,
            double retention,
            double costPerConversion,
            boolean profitIncludesMarketing,
            boolean turnoverRegionIgnored
    ) {}

    public record TableData(
            String[] columns,
            List<Object[]> rows
    ) {}


    // =========================================================
    // OVERVIEW KPIS
    // =========================================================

    public static Kpis overview(
            Map<String, String> filters
    ) throws Exception {

        Map<String, Object> root =
                getObject(
                        "api/dashboard/overview",
                        filters
                );

        Map<String, Object> data =
                asMap(
                        root.get("data")
                );

        return new Kpis(

                number(
                        data.get(
                                "total_revenue"
                        )
                ),

                data.get(
                        "revenue_growth_pct"
                ) == null
                        ? null
                        : number(
                                data.get(
                                        "revenue_growth_pct"
                                )
                        ),

                number(
                        data.get(
                                "profit"
                        )
                ),

                number(
                        data.get(
                                "profit_margin_pct"
                        )
                ),

                number(
                        data.get(
                                "inventory_turnover"
                        )
                ),

                number(
                        data.get(
                                "customer_retention_pct"
                        )
                ),

                number(
                        data.get(
                                "cost_per_conversion"
                        )
                ),

                bool(
                        data.get(
                                "profit_includes_marketing"
                        )
                ),

                bool(
                        data.get(
                                "inventory_turnover_region_ignored"
                        )
                )
        );
    }


    // =========================================================
    // GENERIC POINT DATA
    // =========================================================

    public static List<Point> points(
            String endpoint,
            Map<String, String> filters
    ) throws Exception {

        List<Point> result =
                new ArrayList<>();

        for (
                Object item :
                asList(
                        getObject(
                                endpoint,
                                filters
                        ).get(
                                "data"
                        )
                )
        ) {

            Map<String, Object> row =
                    asMap(
                            item
                    );

            result.add(
                    new Point(

                            text(
                                    row.get(
                                            "label"
                                    )
                            ),

                            number(
                                    row.get(
                                            "value"
                                    )
                            )
                    )
            );
        }

        return result;
    }


    // =========================================================
    // GENERIC SERIES DATA
    // =========================================================

    public static List<SeriesPoint> seriesPoints(
            String endpoint,
            Map<String, String> filters
    ) throws Exception {

        List<SeriesPoint> result =
                new ArrayList<>();

        for (
                Object item :
                asList(
                        getObject(
                                endpoint,
                                filters
                        ).get(
                                "data"
                        )
                )
        ) {

            Map<String, Object> row =
                    asMap(
                            item
                    );

            result.add(
                    new SeriesPoint(

                            text(
                                    row.get(
                                            "label"
                                    )
                            ),

                            text(
                                    row.get(
                                            "series"
                                    )
                            ),

                            number(
                                    row.get(
                                            "value"
                                    )
                            )
                    )
            );
        }

        return result;
    }


    // =========================================================
    // GENERIC XY DATA
    // =========================================================

    public static List<XYPoint> xyPoints(
            String endpoint,
            Map<String, String> filters
    ) throws Exception {

        List<XYPoint> result =
                new ArrayList<>();

        for (
                Object item :
                asList(
                        getObject(
                                endpoint,
                                filters
                        ).get(
                                "data"
                        )
                )
        ) {

            Map<String, Object> row =
                    asMap(
                            item
                    );

            result.add(
                    new XYPoint(

                            text(
                                    row.get(
                                            "label"
                                    )
                            ),

                            text(
                                    row.get(
                                            "category"
                                    )
                            ),

                            number(
                                    row.get(
                                            "x"
                                    )
                            ),

                            number(
                                    row.get(
                                            "y"
                                    )
                            )
                    )
            );
        }

        return result;
    }


    // =========================================================
    // SALES DRILL-DOWN
    // =========================================================

    /**
     * Existing method kept so current code does not break.
     */
    public static TableData drilldownSales(
            String month,
            String category,
            String region
    ) throws Exception {

        return drilldownSales(
                month,
                null,
                category,
                region
        );
    }


    /**
     * Sales drill-down with optional week support.
     */
    public static TableData drilldownSales(
            String month,
            String week,
            String category,
            String region
    ) throws Exception {

        Map<String, String> params =
                new LinkedHashMap<>();


        if (
                month != null
                && !month.isBlank()
        ) {

            params.put(
                    "month",
                    month
            );
        }


        if (
                week != null
                && !week.isBlank()
        ) {

            params.put(
                    "week",
                    week
            );
        }


        if (
                category != null
                && !category.isBlank()
        ) {

            params.put(
                    "category",
                    category
            );
        }


        if (
                region != null
                && !region.isBlank()
                && !"All Regions".equals(
                        region
                )
        ) {

            params.put(
                    "region",
                    region
            );
        }


        Map<String, Object> root =
                getObject(
                        "api/drilldown/sales",
                        params
                );


        List<Object> columnsRaw =
                asList(
                        root.get(
                                "columns"
                        )
                );


        String[] columns =
                new String[
                        columnsRaw.size()
                ];


        for (
                int i = 0;
                i < columns.length;
                i++
        ) {

            columns[i] =
                    text(
                            columnsRaw.get(
                                    i
                            )
                    );
        }


        List<Object[]> rows =
                new ArrayList<>();


        for (
                Object item :
                asList(
                        root.get(
                                "data"
                        )
                )
        ) {

            Map<String, Object> row =
                    asMap(
                            item
                    );


            Object[] values =
                    new Object[
                            columns.length
                    ];


            for (
                    int c = 0;
                    c < columns.length;
                    c++
            ) {

                values[c] =
                        row.get(
                                columns[c]
                        );
            }


            rows.add(
                    values
            );
        }


        return new TableData(
                columns,
                rows
        );
    }


    // =========================================================
    // FILTER HELPERS
    // =========================================================

    public static Map<String, String> copyFilters(
            Map<String, String> filters
    ) {

        return filters == null
                ? new HashMap<>()
                : new HashMap<>(
                        filters
                );
    }


    // =========================================================
    // HTTP GET HELPER
    // =========================================================

    private static Map<String, Object> getObject(
            String endpoint,
            Map<String, String> params
    ) throws Exception {

        String json =
                ApiClient.getData(
                        endpoint,
                        params
                );


        Map<String, Object> root =
                asMap(
                        SchemaIntrospector
                                .MiniJson
                                .parse(
                                        json
                                )
                );


        Object success =
                root.get(
                        "success"
                );


        if (
                !(success instanceof Boolean)
                || !((Boolean) success)
        ) {

            throw new IllegalStateException(

                    text(
                            root.get(
                                    "error"
                            )
                    ).isBlank()

                            ? "Backend request failed"

                            : text(
                                    root.get(
                                            "error"
                                    )
                            )
            );
        }


        return root;
    }


    // =========================================================
    // JSON HELPERS
    // =========================================================

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(
            Object value
    ) {

        return value instanceof Map
                ? (Map<String, Object>) value
                : Map.of();
    }


    @SuppressWarnings("unchecked")
    private static List<Object> asList(
            Object value
    ) {

        return value instanceof List
                ? (List<Object>) value
                : List.of();
    }


    private static double number(
            Object value
    ) {

        if (
                value instanceof Number n
        ) {

            return n.doubleValue();
        }


        if (
                value == null
        ) {

            return 0.0;
        }


        try {

            return Double.parseDouble(
                    String.valueOf(
                            value
                    )
            );

        } catch (
                NumberFormatException ex
        ) {

            return 0.0;
        }
    }


    private static boolean bool(
            Object value
    ) {

        if (
                value instanceof Boolean b
        ) {

            return b;
        }


        return value != null
                && Boolean.parseBoolean(
                        String.valueOf(
                                value
                        )
                );
    }


    private static String text(
            Object value
    ) {

        return value == null
                ? ""
                : String.valueOf(
                        value
                );
    }


    // =========================================================
    // CSV UPLOAD
    // =========================================================

    public record UploadResult(
            boolean success,
            String error,
            String table,
            int totalRows,
            int loaded,
            int rejected,
            java.util.List<String> rejectedDetail
    ) {}


    public static UploadResult upload(
            String absolutePath
    ) throws Exception {

        String body =
                "{\"path\":\""
                        + absolutePath.replace(
                                "\\",
                                "\\\\"
                        )
                        + "\"}";


        Map<String, Object> root =
                asMap(
                        SchemaIntrospector
                                .MiniJson
                                .parse(
                                        ApiClient.postJson(
                                                "api/upload",
                                                body
                                        )
                                )
                );


        boolean ok =
                Boolean.TRUE.equals(
                        root.get(
                                "success"
                        )
                );


        java.util.List<String> detail =
                new ArrayList<>();


        for (
                Object item :
                asList(
                        root.get(
                                "rejected_detail"
                        )
                )
        ) {

            Map<String, Object> r =
                    asMap(
                            item
                    );


            detail.add(
                    "Rejected Line "
                            + (int) number(
                                    r.get(
                                            "line"
                                    )
                            )
                            + ": "
                            + text(
                                    r.get(
                                            "reason"
                                    )
                            )
            );
        }


        return new UploadResult(

                ok,

                text(
                        root.get(
                                "error"
                        )
                ),

                text(
                        root.get(
                                "table"
                        )
                ),

                (int) number(
                        root.get(
                                "total_rows"
                        )
                ),

                (int) number(
                        root.get(
                                "loaded"
                        )
                ),

                (int) number(
                        root.get(
                                "rejected"
                        )
                ),

                detail
        );
    }


    // =========================================================
    // INVENTORY DRILL-DOWN
    // =========================================================

    public static TableData drilldownInventory(
            String warehouse,
            Map<String, String> filters
    ) throws Exception {

        Map<String, String> params =
                new LinkedHashMap<>();


        if (filters != null) {

            params.putAll(
                    filters
            );
        }


        if (
                warehouse != null
                && !warehouse.isBlank()
        ) {

            params.put(
                    "warehouse",
                    warehouse
            );
        }


        return readTableData(
                "api/drilldown/inventory",
                params
        );
    }


    // =========================================================
    // MARKETING DRILL-DOWN
    // =========================================================

    public static TableData drilldownMarketing(
            String channel,
            Map<String, String> filters
    ) throws Exception {

        Map<String, String> params =
                new LinkedHashMap<>();


        if (filters != null) {

            params.putAll(
                    filters
            );
        }


        if (
                channel != null
                && !channel.isBlank()
        ) {

            params.put(
                    "channel",
                    channel
            );
        }


        return readTableData(
                "api/drilldown/marketing",
                params
        );
    }


    // =========================================================
    // GENERIC TABLE DATA
    // =========================================================

    private static TableData readTableData(
            String endpoint,
            Map<String, String> params
    ) throws Exception {

        Map<String, Object> root =
                getObject(
                        endpoint,
                        params
                );


        List<Object> columnsRaw =
                asList(
                        root.get(
                                "columns"
                        )
                );


        String[] columns =
                new String[
                        columnsRaw.size()
                ];


        for (
                int i = 0;
                i < columns.length;
                i++
        ) {

            columns[i] =
                    text(
                            columnsRaw.get(
                                    i
                            )
                    );
        }


        List<Object[]> rows =
                new ArrayList<>();


        for (
                Object item :
                asList(
                        root.get(
                                "data"
                        )
                )
        ) {

            Map<String, Object> row =
                    asMap(
                            item
                    );


            Object[] values =
                    new Object[
                            columns.length
                    ];


            for (
                    int c = 0;
                    c < columns.length;
                    c++
            ) {

                values[c] =
                        row.get(
                                columns[c]
                        );
            }


            rows.add(
                    values
            );
        }


        return new TableData(
                columns,
                rows
        );
    }


    // =========================================================
    // LEGACY LOW STOCK ALERTS
    // =========================================================

    /**
     * Existing low-stock endpoint retained so existing callers
     * elsewhere in the dashboard continue to work.
     */
    public static List<LowStockAlert> lowStockAlerts()
            throws Exception {

        Map<String, Object> root =
                getObject(
                        "api/alerts/low-stock",
                        Map.of()
                );


        List<LowStockAlert> alerts =
                new ArrayList<>();


        for (
                Object item :
                asList(
                        root.get(
                                "data"
                        )
                )
        ) {

            Map<String, Object> row =
                    asMap(
                            item
                    );


            alerts.add(
                    new LowStockAlert(

                            (int) number(
                                    row.get(
                                            "product_id"
                                    )
                            ),

                            text(
                                    row.get(
                                            "category"
                                    )
                            ),

                            text(
                                    row.get(
                                            "warehouse"
                                    )
                            ),

                            (int) number(
                                    row.get(
                                            "stock_level"
                                    )
                            )
                    )
            );
        }


        return alerts;
    }


    // =========================================================
    // GET ALERT THRESHOLDS
    // =========================================================

    public static List<AlertThreshold> alertThresholds()
            throws Exception {

        Map<String, Object> root =
                getObject(
                        "api/alerts/thresholds",
                        Map.of()
                );


        List<AlertThreshold> thresholds =
                new ArrayList<>();


        for (
                Object item :
                asList(
                        root.get(
                                "data"
                        )
                )
        ) {

            Map<String, Object> row =
                    asMap(
                            item
                    );


            thresholds.add(
                    new AlertThreshold(

                            text(
                                    row.get(
                                            "threshold_key"
                                    )
                            ),

                            text(
                                    row.get(
                                            "display_name"
                                    )
                            ),

                            number(
                                    row.get(
                                            "threshold_value"
                                    )
                            ),

                            text(
                                    row.get(
                                            "unit"
                                    )
                            )
                    )
            );
        }


        return thresholds;
    }


    // =========================================================
    // SAVE ALERT THRESHOLD
    // =========================================================

    public static void saveAlertThreshold(
            String thresholdKey,
            double thresholdValue
    ) throws Exception {

        String safeKey =
                thresholdKey == null
                        ? ""
                        : thresholdKey
                                .replace(
                                        "\\",
                                        "\\\\"
                                )
                                .replace(
                                        "\"",
                                        "\\\""
                                );


        String body =
                "{"
                        + "\"threshold_key\":\""
                        + safeKey
                        + "\","
                        + "\"threshold_value\":"
                        + thresholdValue
                        + "}";


        Map<String, Object> root =
                asMap(
                        SchemaIntrospector
                                .MiniJson
                                .parse(
                                        ApiClient.postJson(
                                                "api/alerts/thresholds",
                                                body
                                        )
                                )
                );


        Object success =
                root.get(
                        "success"
                );


        if (
                !(success instanceof Boolean)
                || !((Boolean) success)
        ) {

            String error =
                    text(
                            root.get(
                                    "error"
                            )
                    );


            throw new IllegalStateException(
                    error.isBlank()
                            ? "Unable to save alert threshold."
                            : error
            );
        }
    }


    // =========================================================
    // ACTIVE ALERTS
    // =========================================================

    public static List<ActiveAlert> activeAlerts()
            throws Exception {

        Map<String, Object> root =
                getObject(
                        "api/alerts/active",
                        Map.of()
                );


        List<ActiveAlert> alerts =
                new ArrayList<>();


        for (
                Object item :
                asList(
                        root.get(
                                "data"
                        )
                )
        ) {

            Map<String, Object> row =
                    asMap(
                            item
                    );


            alerts.add(
                    new ActiveAlert(

                            (int) number(
                                    row.get(
                                            "alert_id"
                                    )
                            ),

                            text(
                                    row.get(
                                            "alert_type"
                                    )
                            ),

                            text(
                                    row.get(
                                            "entity_key"
                                    )
                            ),

                            text(
                                    row.get(
                                            "title"
                                    )
                            ),

                            text(
                                    row.get(
                                            "message"
                                    )
                            ),

                            number(
                                    row.get(
                                            "current_value"
                                    )
                            ),

                            number(
                                    row.get(
                                            "threshold_value"
                                    )
                            ),

                            text(
                                    row.get(
                                            "severity"
                                    )
                            ),

                            text(
                                    row.get(
                                            "status"
                                    )
                            ),

                            text(
                                    row.get(
                                            "created_at"
                                    )
                            )
                    )
            );
        }


        return alerts;
    }


    // =========================================================
    // MANUAL ALERT DETECTION
    // =========================================================

    public static int detectAlerts()
            throws Exception {

        Map<String, Object> root =
                asMap(
                        SchemaIntrospector
                                .MiniJson
                                .parse(
                                        ApiClient.postJson(
                                                "api/alerts/detect",
                                                "{}"
                                        )
                                )
                );


        Object success =
                root.get(
                        "success"
                );


        if (
                !(success instanceof Boolean)
                || !((Boolean) success)
        ) {

            String error =
                    text(
                            root.get(
                                    "error"
                            )
                    );


            throw new IllegalStateException(
                    error.isBlank()
                            ? "Alert detection failed."
                            : error
            );
        }


        return (int) number(
                root.get(
                        "active_alerts"
                )
        );
    }
}
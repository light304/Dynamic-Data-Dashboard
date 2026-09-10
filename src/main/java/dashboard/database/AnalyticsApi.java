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

    public record Point(String label, double value) {}
    public record SeriesPoint(String label, String series, double value) {}
    public record XYPoint(String label, String category, double x, double y) {}
    public record Kpis(double revenue, Double growth, double profit, double margin,
                       double turnover, double retention, double costPerConversion,
                       boolean profitIncludesMarketing, boolean turnoverRegionIgnored) {}
    public record TableData(String[] columns, List<Object[]> rows) {}

    public static Kpis overview(Map<String, String> filters) throws Exception {
        Map<String, Object> root = getObject("api/dashboard/overview", filters);
        Map<String, Object> data = asMap(root.get("data"));
        return new Kpis(
                number(data.get("total_revenue")),
                data.get("revenue_growth_pct") == null 
                    ? null 
                    : number(data.get("revenue_growth_pct")),
                number(data.get("profit")),
                number(data.get("profit_margin_pct")),
                number(data.get("inventory_turnover")),
                number(data.get("customer_retention_pct")),
                number(data.get("cost_per_conversion")),
                bool(data.get("profit_includes_marketing")),
                bool(data.get("inventory_turnover_region_ignored"))
        );
    }

    public static List<Point> points(String endpoint, Map<String, String> filters) throws Exception {
        List<Point> result = new ArrayList<>();
        for (Object item : asList(getObject(endpoint, filters).get("data"))) {
            Map<String, Object> row = asMap(item);
            result.add(new Point(text(row.get("label")), number(row.get("value"))));
        }
        return result;
    }

    public static List<SeriesPoint> seriesPoints(String endpoint, Map<String, String> filters) throws Exception {
        List<SeriesPoint> result = new ArrayList<>();
        for (Object item : asList(getObject(endpoint, filters).get("data"))) {
            Map<String, Object> row = asMap(item);
            result.add(new SeriesPoint(
                    text(row.get("label")),
                    text(row.get("series")),
                    number(row.get("value"))
            ));
        }
        return result;
    }

    public static List<XYPoint> xyPoints(String endpoint, Map<String, String> filters) throws Exception {
        List<XYPoint> result = new ArrayList<>();
        for (Object item : asList(getObject(endpoint, filters).get("data"))) {
            Map<String, Object> row = asMap(item);
            result.add(new XYPoint(
                    text(row.get("label")),
                    text(row.get("category")),
                    number(row.get("x")),
                    number(row.get("y"))
            ));
        }
        return result;
    }

    public static TableData drilldownSales(String month, String category, String region) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        if (month != null && !month.isBlank()) params.put("month", month);
        if (category != null && !category.isBlank()) params.put("category", category);
        if (region != null && !region.isBlank() && !"All Regions".equals(region)) params.put("region", region);

        Map<String, Object> root = getObject("api/drilldown/sales", params);
        List<Object> columnsRaw = asList(root.get("columns"));
        String[] columns = new String[columnsRaw.size()];
        for (int i = 0; i < columns.length; i++) columns[i] = text(columnsRaw.get(i));

        List<Object[]> rows = new ArrayList<>();
        for (Object item : asList(root.get("data"))) {
            Map<String, Object> row = asMap(item);
            Object[] values = new Object[columns.length];
            for (int c = 0; c < columns.length; c++) values[c] = row.get(columns[c]);
            rows.add(values);
        }
        return new TableData(columns, rows);
    }

    public static Map<String, String> copyFilters(Map<String, String> filters) {
        return filters == null ? new HashMap<>() : new HashMap<>(filters);
    }

    private static Map<String, Object> getObject(String endpoint, Map<String, String> params) throws Exception {
        String json = ApiClient.getData(endpoint, params);
        Map<String, Object> root = asMap(SchemaIntrospector.MiniJson.parse(json));
        Object success = root.get("success");
        if (!(success instanceof Boolean) || !((Boolean) success)) {
            throw new IllegalStateException(text(root.get("error")).isBlank()
                    ? "Backend request failed"
                    : text(root.get("error")));
        }
        return root;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Object> asList(Object value) {
        return value instanceof List ? (List<Object>) value : List.of();
    }

    private static double number(Object value) {
        if (value instanceof Number n) return n.doubleValue();
        if (value == null) return 0.0;
        try { return Double.parseDouble(String.valueOf(value)); }
        catch (NumberFormatException ex) { return 0.0; }
    }

    private static boolean bool(Object value) {
        if (value instanceof Boolean b) return b;
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    public record UploadResult(boolean success, String error, String table, int totalRows, int loaded, int rejected, java.util.List<String> rejectedDetail) {}

    public static UploadResult upload(String absolutePath) throws Exception {
        String body = "{\"path\":\""
            + absolutePath.replace("\\", "\\\\")
            + "\"}";

        Map<String, Object> root =
            asMap(SchemaIntrospector.MiniJson.parse(
                ApiClient.postJson("api/upload", body)));

        boolean ok = Boolean.TRUE.equals(root.get("success"));

        java.util.List<String> detail = new ArrayList<>();
        for (Object item : asList(root.get("rejected_detail"))) {
            Map<String, Object> r = asMap(item);
            detail.add("Rejected Line " + (int) number(r.get("line")) + ": " + text(r.get("reason")));
        }

        return new UploadResult(
            ok,
            text(root.get("error")),
            text(root.get("table")),
            (int) number(root.get("total_rows")),
            (int) number(root.get("loaded")),
            (int) number(root.get("rejected")),
            detail
        );
    }
}

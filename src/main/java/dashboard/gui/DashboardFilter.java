package dashboard.gui;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable value object shared by every analytics page.
 * Keeping the filter values in one class prevents each page from interpreting
 * Year / Scope / Month / Period / Region differently.
 */
public record DashboardFilter(int year, String scope, String month, String period, String region) {

    /** Default view used when the application first opens. */
    public static DashboardFilter defaults() {
        return new DashboardFilter(2023, "Yearly", "January", "Full Year", "All Regions");
    }

    /**
     * Converts the Swing filter selection into the query parameters expected by
     * server.js. All BaseAnalyticsPage pages can therefore reuse the same map.
     */
    public Map<String, String> toParams() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("year", String.valueOf(year));
        params.put("scope", scope);
        params.put("period", period);
        params.put("region", region);
        if (month != null && !month.isBlank()) params.put("month", month);
        return params;
    }

    /** Human-readable description of the selected period, for report headers. */
    public String periodLabel() {
        if ("Weekly".equals(scope))  return period + ", " + month + " " + year;
        if ("Yearly".equals(scope))  return "Full Year " + year;
        return period + " " + year;
    }
}

package dashboard.report;

import dashboard.database.AnalyticsApi;
import dashboard.database.AnalyticsApi.Point;
import dashboard.database.AnalyticsApi.SeriesPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads the data for a multi-series chart.
 *
 * The screen (CatalogueRenderer) and the PDF (ReportBuilder) both call
 * this, so a chart that merges two endpoints is built identically in
 * both places.
 */
public final class ChartData {

    private ChartData() {}

    /**
     * Series data for GROUPED_BAR, MULTI_LINE and COMBO specs.
     *
     * With a second endpoint the two single-series responses are merged:
     * the first is named by spec.seriesName(), the second by
     * spec.secondSeries(). Without one, the endpoint already returns
     * label / series / value rows.
     */
    public static List<SeriesPoint> series(ChartSpec spec,
                                           Map<String, String> params) throws Exception {

        if (!spec.hasSecondSource()) {
            return AnalyticsApi.seriesPoints(spec.endpoint(), params);
        }

        List<SeriesPoint> merged = new ArrayList<>();

        for (Point p : AnalyticsApi.points(spec.endpoint(), params)) {
            merged.add(new SeriesPoint(p.label(), spec.seriesName(), p.value()));
        }

        for (Point p : AnalyticsApi.points(spec.secondEndpoint(), params)) {
            merged.add(new SeriesPoint(p.label(), spec.secondSeries(), p.value()));
        }

        return merged;
    }
}

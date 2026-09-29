package dashboard.gui;

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

/**
 * Number formatting and tick maths for chart axes.
 *
 * Deliberately free of any JFreeChart types so it can be unit-tested on
 * its own.
 *
 *   1,250,000  ->  1.25M      (axis)     $1.25M   (money axis)
 *   48,000     ->  48K
 *   0.512      ->  0.51
 *   62.5       ->  62.5%      (percent axis)
 */
public final class AxisScale {

    private AxisScale() {}

    /** Axis start, axis end and the gap between ticks. */
    public record Bounds(double min, double max, double step) {}

    // ---------------------------------------------------------------
    // Compact number text
    // ---------------------------------------------------------------

    /** 1250000 -> "1.25M", 48000 -> "48K", 0.5 -> "0.5". No prefix or suffix. */
    public static String compact(double value) {

        double abs = Math.abs(value);

        if (abs >= 1_000_000_000d) return trim(value / 1_000_000_000d) + "B";
        if (abs >= 1_000_000d)     return trim(value / 1_000_000d) + "M";
        if (abs >= 1_000d)         return trim(value / 1_000d) + "K";

        if (abs >= 100d) return trim(round(value, 0));
        if (abs >= 10d)  return trim(round(value, 1));

        return trim(round(value, 2));
    }

    /** Compact text with a currency sign: "$1.25M", "-$3K". */
    public static String money(double value) {
        String text = compact(Math.abs(value));
        return (value < 0 ? "-$" : "$") + text;
    }

    private static double round(double v, int places) {
        double f = Math.pow(10, places);
        return Math.round(v * f) / f;
    }

    /** Up to two decimals, with trailing zeros removed. */
    private static String trim(double v) {
        String s = String.format("%.2f", v);
        if (s.indexOf('.') >= 0) {
            s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return s.equals("-0") ? "0" : s;
    }

    // ---------------------------------------------------------------
    // Tick bounds
    // ---------------------------------------------------------------

    /**
     * Picks an axis start, end and tick gap that all land on round
     * numbers and enclose the data.
     *
     * @param includeZero true for bars and areas, where the bar length
     *                    is the value and a cut-off baseline would mislead.
     *                    Lines and scatters pass false and get a tight range.
     */
    public static Bounds bounds(double dataMin, double dataMax,
                                boolean includeZero, int targetTicks) {

        if (Double.isNaN(dataMin) || Double.isNaN(dataMax)
                || Double.isInfinite(dataMin) || Double.isInfinite(dataMax)) {
            return new Bounds(0, 1, 0.2);
        }

        double lo = dataMin;
        double hi = dataMax;

        if (includeZero) {
            lo = Math.min(lo, 0);
            hi = Math.max(hi, 0);

            // Headroom, so the tallest bar does not touch the top frame.
            double room = (hi - lo) * 0.04;
            if (dataMax > 0) hi += room;
            if (dataMin < 0) lo -= room;
        }

        // A flat series still needs a visible range around it.
        if (hi - lo < 1e-12) {
            double pad = Math.abs(hi) > 0 ? Math.abs(hi) * 0.1 : 1;
            lo -= pad;
            hi += pad;
            if (includeZero && dataMin >= 0) lo = Math.max(lo, 0);
        } else if (!includeZero) {
            // Breathing room so the line does not sit on the frame.
            double pad = (hi - lo) * 0.08;
            lo -= pad;
            hi += pad;

            // Never dip below zero for data that never did.
            if (dataMin >= 0 && lo < 0) lo = 0;
        }

        double step = niceStep((hi - lo) / Math.max(2, targetTicks));

        double start = Math.floor(lo / step) * step;
        double end   = Math.ceil(hi / step) * step;

        // Guard against floating-point crumbs such as 0.30000000000000004.
        start = clean(start, step);
        end   = clean(end, step);

        if (end <= start) end = start + step;

        return new Bounds(start, end, step);
    }

    /** Rounds a raw gap up to 1, 2, 2.5, 5 or 10 times a power of ten. */
    static double niceStep(double raw) {

        if (raw <= 0) return 1;

        double exp  = Math.floor(Math.log10(raw));
        double base = Math.pow(10, exp);
        double frac = raw / base;

        double nice;
        if      (frac <= 1.0) nice = 1;
        else if (frac <= 2.0) nice = 2;
        else if (frac <= 2.5) nice = 2.5;
        else if (frac <= 5.0) nice = 5;
        else                  nice = 10;

        return nice * base;
    }

    private static double clean(double v, double step) {
        int places = Math.max(0, 2 - (int) Math.floor(Math.log10(step)));
        double f = Math.pow(10, Math.min(places, 10));
        return Math.round(v * f) / f;
    }

    // ---------------------------------------------------------------
    // NumberFormat for JFreeChart axes
    // ---------------------------------------------------------------

    /**
     * Formatter for tick labels, chosen from the axis label:
     *   label contains "$" -> money,  "%" -> percent,  else plain compact.
     */
    public static NumberFormat formatFor(String axisLabel) {

        String label = axisLabel == null ? "" : axisLabel;

        if (label.contains("$")) return new Compact("$", "");
        if (label.contains("%")) return new Compact("", "%");
        return new Compact("", "");
    }

    /** Full-precision text for tooltips: $1,234,567.89 / 62.50% / 1,234.5 */
    public static NumberFormat exact(String axisLabel) {

        String label = axisLabel == null ? "" : axisLabel;

        if (label.contains("$")) return new java.text.DecimalFormat("$#,##0.00");
        if (label.contains("%")) return new java.text.DecimalFormat("#,##0.00'%'");
        return new java.text.DecimalFormat("#,##0.##");
    }

    private static final class Compact extends NumberFormat {

        private final String prefix;
        private final String suffix;

        Compact(String prefix, String suffix) {
            this.prefix = prefix;
            this.suffix = suffix;
        }

        @Override
        public StringBuffer format(double number, StringBuffer out, FieldPosition pos) {
            String body = compact(Math.abs(number));
            if (number < 0 && !body.equals("0")) out.append('-');
            return out.append(prefix).append(body).append(suffix);
        }

        @Override
        public StringBuffer format(long number, StringBuffer out, FieldPosition pos) {
            return format((double) number, out, pos);
        }

        @Override
        public Number parse(String source, ParsePosition parsePosition) {
            return null;
        }
    }
}

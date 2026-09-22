package dashboard.gui;

import java.awt.Font;

/**
 * Single source of truth for fonts across the dashboard.
 *
 * Change a size here and it changes everywhere.
 * Colours are added to this class later.
 */
public final class Theme {

    private Theme() {}

    private static final String FAMILY = "SansSerif";

    // Headings
    public static final Font PAGE_TITLE  = new Font(FAMILY, Font.BOLD,  28);
    public static final Font SECTION     = new Font(FAMILY, Font.BOLD,  20);
    public static final Font BRAND       = new Font(FAMILY, Font.BOLD,  18);
    public static final Font CHART_TITLE = new Font(FAMILY, Font.BOLD,  17);
    public static final Font SUBHEAD     = new Font(FAMILY, Font.BOLD,  15);

    // KPI cards
    public static final Font CARD_VALUE  = new Font(FAMILY, Font.BOLD,  22);

    // Body text
    public static final Font BODY_STRONG = new Font(FAMILY, Font.BOLD,  14);
    public static final Font BODY        = new Font(FAMILY, Font.PLAIN, 14);
    public static final Font SMALL_BOLD  = new Font(FAMILY, Font.BOLD,  13);
    public static final Font SMALL       = new Font(FAMILY, Font.PLAIN, 13);

    // Charts
    public static final Font AXIS_LABEL  = new Font(FAMILY, Font.BOLD,  14);
    public static final Font AXIS_TICK   = new Font(FAMILY, Font.PLAIN, 13);
    public static final Font LEGEND      = new Font(FAMILY, Font.PLAIN, 13);

    public static final Font MONO        = new Font(Font.MONOSPACED, Font.PLAIN, 13);
}
package dashboard.gui;

import java.awt.Font;
import java.awt.Color;

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

    // Chrome
    public static final Color SIDEBAR        = new Color(0x0D, 0x1B, 0x2A);
    public static final Color SIDEBAR_PANEL  = new Color(0x1E, 0x3A, 0x5F);
    public static final Color SIDEBAR_MUTED  = new Color(0x8B, 0xAA, 0xB9);
    public static final Color SIDEBAR_BUTTON = new Color(0x26, 0x3A, 0x4E);
    
    // Accent
    public static final Color ACCENT       = new Color(0x1E, 0x3A, 0x5F);
    public static final Color ACCENT_HOVER = new Color(0x2A, 0x4C, 0x75);

    // Highlight
    public static final Color HIGHLIGHT = new Color(0x4F, 0xC3, 0xF7);

    // Light fills - dark text only
    public static final Color FILL_CREAM = new Color(0xF6, 0xDA, 0xC0);
    public static final Color FILL_PEACH = new Color(0xFE, 0xAF, 0x76);

    // Surfaces + silver
    public static final Color PAGE_BG = new Color(245, 247, 250);
    public static final Color CARD_BG = Color.WHITE;
    public static final Color BORDER  = new Color(226, 232, 240);
    public static final Color GRID    = new Color(226, 232, 240);
    public static final Color SILVER  = new Color(0xCB, 0xD5, 0xE1);

    // Text
    public static final Color TEXT         = new Color(31, 41, 55);
    public static final Color TEXT_MUTED   = new Color(100, 116, 139);
    public static final Color TEXT_ON_FILL = new Color(0x11, 0x18, 0x27);

    // Status - reserved
    public static final Color GOOD     = new Color(0x15, 0x80, 0x3D);
    public static final Color WARNING  = new Color(0xB4, 0x53, 0x09);
    public static final Color CRITICAL = new Color(0xC8, 0x1E, 0x1E);

    // Chart series - identity only
    public static final Color SERIES_1 = new Color(0x0D, 0x47, 0xA1);
    public static final Color SERIES_2 = new Color(0x21, 0x38, 0x43);
    public static final Color SERIES_3 = new Color(0xDA, 0x6D, 0x58);
    public static final Color SERIES_4 = new Color(0x7C, 0x3A, 0xED);
    public static final Color SERIES_5 = new Color(0xA2, 0x1C, 0xAF);
}
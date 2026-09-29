package dashboard.gui;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Lays chart cards out in equal cells that fill the space they are given,
 * two to a row, with no scrolling.
 *
 *   4 charts:   [ 1 ][ 2 ]      3 charts:   [ 1 ][ 2 ]
 *               [ 3 ][ 4 ]                  [    3    ]
 *
 * An odd last chart spans the full row rather than leaving a hole.
 *
 * GridLayout is used at both levels, rather than GridBagLayout, because it
 * ignores preferred sizes: every row and every cell is exactly the same
 * size however the cards' contents differ.
 */
final class ChartGrid {

    static final int GAP = 12;
    static final int COLUMNS = 2;

    private ChartGrid() {}

    /** Replaces whatever the grid holds with the given cards. */
    static void fill(JPanel grid, List<? extends JComponent> cards) {

        grid.removeAll();

        if (cards.isEmpty()) {
            grid.revalidate();
            grid.repaint();
            return;
        }

        int rows = (cards.size() + COLUMNS - 1) / COLUMNS;

        grid.setLayout(new GridLayout(rows, 1, 0, GAP));

        for (int start = 0; start < cards.size(); start += COLUMNS) {

            int end = Math.min(start + COLUMNS, cards.size());

            JPanel row = new JPanel(new GridLayout(1, end - start, GAP, 0));
            row.setOpaque(false);

            for (int i = start; i < end; i++) {
                JComponent card = cards.get(i);

                // Cell size comes from the grid; drop any fixed size.
                card.setPreferredSize(null);
                card.setMinimumSize(new Dimension(0, 0));

                row.add(card);
            }

            grid.add(row);
        }

        grid.revalidate();
        grid.repaint();
    }

    /** A single full-size message card, for loading and error states. */
    static void showMessage(JPanel grid, String title, String message) {
        fill(grid, List.of(AnalyticsCharts.messageCard(title, message)));
    }

    /** A grid panel to place in a page. */
    static JPanel create(Color background) {
        JPanel grid = new JPanel(new GridLayout(1, 1));
        grid.setBackground(background);
        return grid;
    }
}

package app.kohi;

import android.content.Context;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The taste compass: where a cup landed on two axes. Left to right is extraction (sour and
 * under-extracted, balanced, bitter and over-extracted); top to bottom is strength (strong,
 * just right, weak). The middle is the goal, and the advice says which way to move.
 * The idea follows Barista Hustle's Coffee Compass.
 *
 * It fills in a brew's taste ("Sour", "Balanced", "Bitter") and strength ("Strong",
 * "Just right", "Weak"): the same values the iPhone app keeps.
 */
final class Compass {

    static final String[] TASTES = {"Sour", "Balanced", "Bitter"};
    static final String[] STRENGTHS = {"Strong", "Just right", "Weak"};   // top to bottom

    /** The words in each cell, by row (strength) then column (taste). */
    static final String[][] WORDS = {
            {"sour, heavy", "heavy, intense", "bitter, harsh"},
            {"sour, salty, sharp", "balanced, sweet", "bitter, dry, ashy"},
            {"sour, watery", "watery, thin", "bitter, hollow"},
    };

    static final String FOOTNOTE = "If a fault stays after a few changes, it may come from the beans "
            + "or the roast rather than from how you brewed.";

    // Words you might use for a cup, and where they point. "" leaves that axis as it is.
    private static final String[][] LOOKUP = {
            // word, taste, strength
            {"sour", "Sour", ""}, {"tart", "Sour", ""}, {"lemony", "Sour", ""}, {"sharp", "Sour", ""},
            {"salty", "Sour", ""}, {"grassy", "Sour", ""}, {"peanut", "Sour", ""}, {"raw", "Sour", ""},
            {"underdeveloped", "Sour", ""}, {"under", "Sour", ""}, {"zuur", "Sour", ""},
            {"bitter", "Bitter", ""}, {"harsh", "Bitter", ""}, {"dry", "Bitter", ""}, {"drying", "Bitter", ""},
            {"astringent", "Bitter", ""}, {"ashy", "Bitter", ""}, {"burnt", "Bitter", ""},
            {"chalky", "Bitter", ""}, {"woody", "Bitter", ""}, {"over", "Bitter", ""}, {"bitterig", "Bitter", ""},
            {"hollow", "Bitter", "Weak"}, {"empty", "Bitter", "Weak"},
            {"muddy", "Bitter", "Strong"}, {"murky", "Bitter", "Strong"},
            {"watery", "", "Weak"}, {"thin", "", "Weak"}, {"weak", "", "Weak"}, {"tea-like", "", "Weak"},
            {"tealike", "", "Weak"}, {"light", "", "Weak"}, {"flat", "", "Weak"}, {"waterig", "", "Weak"},
            {"slap", "", "Weak"},
            {"strong", "", "Strong"}, {"heavy", "", "Strong"}, {"intense", "", "Strong"},
            {"overpowering", "", "Strong"}, {"thick", "", "Strong"}, {"sterk", "", "Strong"},
            {"balanced", "Balanced", "Just right"}, {"sweet", "Balanced", "Just right"},
            {"smooth", "Balanced", "Just right"}, {"juicy", "Balanced", "Just right"},
            {"clean", "Balanced", "Just right"}, {"round", "Balanced", "Just right"},
            {"perfect", "Balanced", "Just right"}, {"good", "Balanced", "Just right"},
    };

    private Compass() {}

    /** {taste, strength} the words point to; either may be "" when the words say nothing about it. */
    static String[] lookup(String words) {
        String taste = "", strength = "";
        for (String w : words.toLowerCase(Locale.ROOT).split("[^\\p{L}-]+")) {
            if (w.isEmpty()) continue;
            String[] hit = null;
            for (String[] row : LOOKUP) {
                if (w.equals(row[0])) { hit = row; break; }      // an exact word wins
            }
            if (hit == null && w.length() > 4) {
                for (String[] row : LOOKUP) {                       // else the longest word it starts with
                    if (w.startsWith(row[0]) && (hit == null || row[0].length() > hit[0].length())) hit = row;
                }
            }
            if (hit == null) continue;
            if (!hit[1].isEmpty()) taste = hit[1];
            if (!hit[2].isEmpty()) strength = hit[2];
        }
        return new String[]{taste, strength};
    }

    static String describe(String taste, String strength) {
        int r = index(STRENGTHS, strength), c = index(TASTES, taste);
        if (r < 0 && c < 0) return "";
        if (r < 0) return taste.toLowerCase(Locale.ROOT);
        if (c < 0) return strength.toLowerCase(Locale.ROOT);
        return WORDS[r][c];
    }

    static int index(String[] list, String value) {
        for (int i = 0; i < list.length; i++) if (list[i].equals(value)) return i;
        return -1;
    }

    // ---- the grid ---------------------------------------------------------------------------

    interface OnPick {
        void pick(String taste, String strength);
    }

    /**
     * A 3 × 3 grid of outlined cells. With onPick it can be tapped; with counts it shows how many
     * brews landed in each cell. The cell of (taste, strength) is drawn filled.
     */
    static final class Grid {
        final LinearLayout view;
        private final Context c;
        private final List<TextView> cells = new ArrayList<>();

        Grid(Context c, OnPick onPick) {
            this.c = c;
            view = Ui.column(c);
            TextView top = Ui.text(c, "↑ stronger", 14);
            top.setGravity(Gravity.CENTER);
            view.addView(top, Ui.wide());
            for (int r = 0; r < 3; r++) {
                LinearLayout row = Ui.row(c);
                for (int col = 0; col < 3; col++) {
                    TextView cell = Ui.text(c, "", 13);
                    cell.setGravity(Gravity.CENTER);
                    cell.setPadding(Ui.dp(c, 4), Ui.dp(c, 4), Ui.dp(c, 4), Ui.dp(c, 4));
                    cell.setMinHeight(Ui.dp(c, 58));
                    final int fr = r, fc = col;
                    if (onPick != null) {
                        cell.setClickable(true);
                        cell.setOnClickListener(v -> onPick.pick(TASTES[fc], STRENGTHS[fr]));
                    }
                    cells.add(cell);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.WRAP, 1f);
                    lp.setMargins(Ui.dp(c, 3), Ui.dp(c, 3), Ui.dp(c, 3), Ui.dp(c, 3));
                    row.addView(cell, lp);
                }
                view.addView(row, Ui.wide());
            }
            LinearLayout bottom = Ui.row(c);
            TextView left = Ui.text(c, "← sour: extract more", 13);
            TextView weak = Ui.text(c, "↓ weaker", 14);
            weak.setGravity(Gravity.CENTER);
            TextView right = Ui.text(c, "extract less: bitter →", 13);
            right.setGravity(Gravity.END);
            bottom.addView(left, Ui.weight(1));
            bottom.addView(weak, Ui.weight(1));
            bottom.addView(right, Ui.weight(1));
            view.addView(bottom, Ui.wide());
            show("", "", null);
        }

        /** Marks the chosen cell; counts (by row then column), if given, are shown under the words. */
        void show(String taste, String strength, int[][] counts) {
            int sr = index(STRENGTHS, strength), sc = index(TASTES, taste);
            for (int r = 0; r < 3; r++) {
                for (int col = 0; col < 3; col++) {
                    TextView cell = cells.get(r * 3 + col);
                    // Only one axis noted: mark the whole row or column lightly (dashed).
                    boolean exact = r == sr && col == sc;
                    boolean partial = !exact && ((sr < 0 && col == sc) || (sc < 0 && r == sr));
                    String text = WORDS[r][col];
                    if (counts != null && counts[r][col] > 0) {
                        text += "\n" + counts[r][col] + (counts[r][col] == 1 ? " brew" : " brews");
                    }
                    cell.setText(text);
                    cell.setBackground(Ui.box(c, exact, r == 1 && col == 1 ? 2.5f : 1.5f, 10, partial));
                    cell.setTextColor(exact ? Ui.PAPER : Ui.INK);
                    cell.setTypeface(android.graphics.Typeface.DEFAULT,
                            exact || (r == 1 && col == 1) ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
                }
            }
        }
    }

    /** How many of these brews landed in each cell (only brews with both axes noted). */
    static int[][] counts(List<Brew> brews) {
        int[][] n = new int[3][3];
        for (Brew b : brews) {
            int r = index(STRENGTHS, b.strength), c = index(TASTES, b.taste);
            if (r >= 0 && c >= 0) n[r][c]++;
        }
        return n;
    }
}

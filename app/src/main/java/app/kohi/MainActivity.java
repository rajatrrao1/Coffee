package app.kohi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final String[] FILTERS = {"All", "Light", "Medium", "Dark", "Finished"};

    private String filter = "All";
    private EditText search;
    private LinearLayout list;
    private ScrollView scroll;
    private TextView count;
    private final List<TextView> chips = new ArrayList<>();

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        int pad = Ui.dp(this, 14);

        LinearLayout root = Ui.column(this);
        root.setBackgroundColor(Ui.PAPER);

        // Mark and search, as in the tea app.
        LinearLayout top = Ui.row(this);
        top.setPadding(pad, Ui.dp(this, 10), pad, Ui.dp(this, 8));
        TextView mark = Ui.bold(this, "珈", 26);
        mark.setTextColor(Ui.PAPER);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(Ui.box(this, true, 0, 6, false));
        top.addView(mark, new LinearLayout.LayoutParams(Ui.dp(this, 46), Ui.dp(this, 46)));

        search = Ui.field(this, "Search name, origin, taste");
        search.setBackground(Ui.box(this, false, 1.5f, 999, false));
        search.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), 0);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { render(); }
        });
        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(0, Ui.dp(this, 46), 1f);
        searchLp.leftMargin = Ui.dp(this, 10);
        top.addView(search, searchLp);
        root.addView(top, Ui.wide());

        // Filter chips.
        HorizontalScrollView chipScroll = new HorizontalScrollView(this);
        chipScroll.setHorizontalScrollBarEnabled(false);
        chipScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout chipRow = Ui.row(this);
        chipRow.setPadding(pad, 0, pad, Ui.dp(this, 10));
        for (String f : FILTERS) {
            TextView chip = Ui.pill(this, f, f.equals(filter), 17);
            chip.setOnClickListener(v -> {
                filter = f;
                for (TextView t : chips) Ui.setPill(this, t, t.getText().toString().equals(filter));
                render();
                scroll.scrollTo(0, 0);
            });
            chips.add(chip);
            LinearLayout.LayoutParams lp = Ui.wrap();
            lp.rightMargin = Ui.dp(this, 8);
            chipRow.addView(chip, lp);
        }
        chipScroll.addView(chipRow);
        root.addView(chipScroll, Ui.wide());
        root.addView(Ui.rule(this));

        // The list.
        list = Ui.column(this);
        list.setPadding(pad, 0, pad, Ui.dp(this, 16));
        scroll = Ui.scroller(this, list);
        root.addView(scroll, new LinearLayout.LayoutParams(Ui.MATCH, 0, 1f));
        root.addView(Ui.rule(this));

        // Footer.
        LinearLayout foot = Ui.row(this);
        foot.setPadding(pad, Ui.dp(this, 8), pad, Ui.dp(this, 8));
        count = Ui.oneLine(Ui.text(this, "", 15));
        foot.addView(count, Ui.weight(1));
        TextView more = Ui.pill(this, "More", false, 16);
        more.setOnClickListener(v -> startActivity(new Intent(this, MoreActivity.class)));
        foot.addView(more, Ui.wrap());
        TextView add = Ui.pill(this, "+ Add", true, 16);
        add.setOnClickListener(v -> startActivity(new Intent(this, EditActivity.class)));
        foot.addView(add, Ui.gapLeft(this, Ui.wrap(), 8));
        root.addView(foot, Ui.wide());

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        list.removeAllViews();
        List<Bean> all = Store.all(this);
        String q = search.getText().toString().trim();

        List<Bean> shown = new ArrayList<>();
        for (Bean b : all) if (q.isEmpty() || b.matches(q)) shown.add(b);
        shown.sort((a, b) -> Long.compare(b.added, a.added)); // newest bag first

        int sections = 0;
        if (!filter.equals("Finished")) {
            for (String roast : new String[]{"Light", "Medium", "Dark", ""}) {
                if (!filter.equals("All") && !filter.equals(roast)) continue;
                List<Bean> in = new ArrayList<>();
                for (Bean b : shown) if (!b.finished && b.roast.equals(roast)) in.add(b);
                if (in.isEmpty()) continue;
                section(roast.isEmpty() ? "Other" : roast, in);
                sections++;
            }
        }
        if (filter.equals("All") || filter.equals("Finished")) {
            List<Bean> in = new ArrayList<>();
            for (Bean b : shown) if (b.finished) in.add(b);
            if (!in.isEmpty()) {
                section("Finished", in);
                sections++;
            }
        }

        if (sections == 0) {
            String msg = all.isEmpty()
                    ? "No beans yet.\n\nPress + Add and paste the link to a coffee's page, "
                    + "or share the page here from your browser."
                    : "Nothing here matches.";
            TextView empty = Ui.text(this, msg, 17);
            empty.setPadding(0, Ui.dp(this, 24), 0, 0);
            list.addView(empty, Ui.wide());
        }

        int n = all.size();
        int origins = Store.originCount(this);
        count.setText(n + (n == 1 ? " bean" : " beans") + " · " + origins + (origins == 1 ? " origin" : " origins"));
    }

    private void section(String title, List<Bean> beans) {
        list.addView(Ui.header(this, title + " (" + beans.size() + ")"), Ui.wide());
        for (Bean b : beans) list.addView(card(b), Ui.gapBelow(this, Ui.wide(), 10));
    }

    /** One coffee, laid out like a tea card: name, origin line, notes; temperature and grind on the right. */
    private View card(Bean b) {
        LinearLayout card = Ui.row(this);
        card.setBackground(Ui.box(this, false, 1.5f, 14, false));
        card.setPadding(Ui.dp(this, 16), Ui.dp(this, 10), Ui.dp(this, 14), Ui.dp(this, 11));
        card.setClickable(true);
        card.setOnClickListener(v -> {
            Intent i = new Intent(this, BeanActivity.class);
            i.putExtra("id", b.id);
            startActivity(i);
        });

        LinearLayout left = Ui.column(this);
        left.addView(Ui.oneLine(Ui.bold(this, b.name.isEmpty() ? "Unnamed" : b.name, 20)), Ui.wide());

        LinearLayout line2 = Ui.row(this);
        TextView sub = Ui.oneLine(Ui.text(this, b.subtitle(), 16));
        line2.addView(sub, Ui.weight(1));
        int badges = 0;
        if (b.untried()) {
            line2.addView(Ui.badge(this, "Untried", false, true), Ui.gapLeft(this, Ui.wrap(), 6));
            badges++;
        }
        if (!b.method.trim().isEmpty()) {
            line2.addView(Ui.badge(this, b.method.trim(), false, false), Ui.gapLeft(this, Ui.wrap(), 6));
            badges++;
        }
        if (b.rating > 0 && badges < 2) {
            line2.addView(Ui.badge(this, "★ " + b.rating, true, false), Ui.gapLeft(this, Ui.wrap(), 6));
        }
        left.addView(line2, Ui.wide());

        if (!b.notes.isEmpty()) {
            left.addView(Ui.oneLine(Ui.italic(this, b.notes, 16)), Ui.wide());
        }
        card.addView(left, Ui.weight(1));

        String temp = b.tempLabel();
        if (!temp.isEmpty() || !b.grind.trim().isEmpty()) {
            LinearLayout right = Ui.column(this);
            right.setGravity(Gravity.END);
            if (!temp.isEmpty()) {
                TextView t = Ui.bold(this, temp, 19);
                t.setGravity(Gravity.END);
                right.addView(t, Ui.wrap());
            }
            if (!b.grind.trim().isEmpty()) {
                TextView g = Ui.oneLine(Ui.text(this, b.grind.trim(), 16));
                g.setMaxWidth(Ui.dp(this, 120));
                g.setGravity(Gravity.END);
                right.addView(g, Ui.wrap());
            }
            card.addView(right, Ui.gapLeft(this, Ui.wrap(), 10));
        }
        return card;
    }
}

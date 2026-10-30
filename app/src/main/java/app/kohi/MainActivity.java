package app.kohi;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
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
import android.widget.Toast;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int REQ_SAVE = 1;
    private static final int REQ_OPEN = 2;
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
        TextView backup = Ui.pill(this, "Backup", false, 16);
        backup.setOnClickListener(v -> backupMenu());
        foot.addView(backup, Ui.wrap());
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

    // ---- backup ---------------------------------------------------------------------------

    private void backupMenu() {
        String[] items = {"Save all beans to a file", "Bring beans in from a file", "Send the list as text"};
        new AlertDialog.Builder(this)
                .setTitle("Backup")
                .setItems(items, (d, which) -> {
                    if (which == 0) saveFile();
                    else if (which == 1) openFile();
                    else sendText();
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void saveFile() {
        String day = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date());
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");
        i.putExtra(Intent.EXTRA_TITLE, "coffee-beans-" + day + ".json");
        try {
            startActivityForResult(i, REQ_SAVE);
        } catch (ActivityNotFoundException e) {
            toast("This phone has no app for saving files.");
        }
    }

    private void openFile() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        try {
            startActivityForResult(i, REQ_OPEN);
        } catch (ActivityNotFoundException e) {
            toast("This phone has no app for opening files.");
        }
    }

    private void sendText() {
        if (Store.all(this).isEmpty()) {
            toast("There are no beans to send yet.");
            return;
        }
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT, "My coffee beans");
        i.putExtra(Intent.EXTRA_TEXT, Store.asText(this));
        startActivity(Intent.createChooser(i, "Send the list"));
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (request == REQ_SAVE) {
            try (OutputStream out = getContentResolver().openOutputStream(uri, "w")) {
                if (out == null) throw new java.io.IOException("no file");
                out.write(Store.exportJson(this).getBytes(StandardCharsets.UTF_8));
                int n = Store.all(this).size();
                toast("Saved " + n + (n == 1 ? " bean." : " beans."));
            } catch (Exception e) {
                toast("Could not save the file: " + e.getMessage());
            }
        } else if (request == REQ_OPEN) {
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                if (in == null) throw new java.io.IOException("no file");
                int n = Store.importJson(this, Store.readAll(in));
                toast("Brought in " + n + (n == 1 ? " bean." : " beans."));
                render();
            } catch (Exception e) {
                toast("That file is not a Coffee backup.");
            }
        }
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }
}

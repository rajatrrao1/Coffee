package app.kohi;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class BeanActivity extends Activity {

    private String id;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        id = getIntent().getStringExtra("id");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Bean b = Store.find(this, id);
        if (b == null) {
            finish();
            return;
        }
        build(b);
        TwoTone.sync(this, false, () -> {
            Bean now = Store.find(this, id);
            if (now != null) build(now);
        });
    }

    private void build(Bean b) {
        int pad = Ui.dp(this, 16);
        LinearLayout root = Ui.column(this);
        root.setBackgroundColor(Ui.PAPER);

        TextView back = Ui.pill(this, "‹ Back", false, 16);
        back.setOnClickListener(v -> finish());
        TextView edit = Ui.pill(this, "Edit", true, 16);
        edit.setOnClickListener(v -> {
            Intent i = new Intent(this, EditActivity.class);
            i.putExtra("id", b.id);
            startActivity(i);
        });
        root.addView(Ui.topBar(this, back, "", edit), Ui.wide());
        root.addView(Ui.rule(this));

        LinearLayout body = Ui.column(this);
        body.setPadding(pad, Ui.dp(this, 12), pad, Ui.dp(this, 24));

        body.addView(Ui.bold(this, b.name.isEmpty() ? "Unnamed" : b.name, 26), Ui.wide());
        if (!b.roaster.isEmpty()) body.addView(Ui.text(this, b.roaster, 17), Ui.wide());

        LinearLayout tags = Ui.row(this);
        tags.setPadding(0, Ui.dp(this, 8), 0, 0);
        if (!b.roast.isEmpty()) tags.addView(Ui.badge(this, b.roast + " roast", false, false), Ui.wrap());
        if (b.untried()) tags.addView(Ui.badge(this, "Untried", false, true), gap());
        if (b.finished) tags.addView(Ui.badge(this, "Finished", true, false), gap());
        if (tags.getChildCount() > 0) body.addView(tags, Ui.wide());

        TextView log = Ui.pill(this, "+ Log a brew", true, 17);
        log.setOnClickListener(v -> openBrew(b, null));
        LinearLayout.LayoutParams logLp = Ui.wide();
        logLp.topMargin = Ui.dp(this, 14);
        body.addView(log, logLp);

        // Your usual recipe: what you read at the kettle.
        body.addView(Ui.header(this, "My recipe"), Ui.wide());
        Brew recipe = b.recipe();
        if (recipe.empty()) {
            body.addView(Ui.italic(this, "No usual recipe yet. Press Edit to write one down, or "
                    + "log a brew and choose Also make this my usual recipe.", 16), Ui.wide());
        } else {
            showBrew(body, recipe);
        }

        brews(body, b);

        // The bag: how much, when it was opened and finished, how long it lasted.
        body.addView(Ui.header(this, "The bag"), Ui.wide());
        int bagLines = 0;
        bagLines += line(body, "Bag", b.bagLabel());
        bagLines += line(body, "Opened", b.opened > 0 ? Store.day(b.opened) : "");
        bagLines += line(body, "Finished", b.finishedOn > 0 ? Store.day(b.finishedOn) : "");
        bagLines += line(body, b.finishedOn > 0 ? "Lasted" : "Open for", b.lasted());
        if (bagLines == 0) {
            body.addView(Ui.italic(this, "Press Opened the bag today when you start it, and Mark "
                    + "the bag as finished when it runs out.", 16), Ui.wide());
        }

        body.addView(Ui.header(this, "The bean"), Ui.wide());
        int shown = 0;
        shown += line(body, "Origin", b.origin);
        shown += line(body, "Region", b.region);
        shown += line(body, "Altitude", b.altitude);
        shown += line(body, "Process", b.process);
        shown += line(body, "Variety", b.variety);
        shown += line(body, "Producer", b.producer);
        if (shown == 0) body.addView(Ui.italic(this, "No details yet.", 16), Ui.wide());

        if (!b.notes.isEmpty()) {
            body.addView(Ui.header(this, "Tasting notes"), Ui.wide());
            body.addView(Ui.italic(this, b.notes, 18), Ui.wide());
        }

        if (b.rating > 0 || !b.myNotes.isEmpty()) {
            body.addView(Ui.header(this, "My notes"), Ui.wide());
            if (b.rating > 0) body.addView(Ui.text(this, b.stars(), 22), Ui.wide());
            if (!b.myNotes.isEmpty()) body.addView(Ui.text(this, b.myNotes, 17), Ui.wide());
        }

        // Actions.
        LinearLayout actions = Ui.column(this);
        actions.setPadding(0, Ui.dp(this, 22), 0, 0);
        if (!b.url.isEmpty()) {
            TextView open = Ui.pill(this, "Open the roaster's page", false, 16);
            open.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(b.url)));
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(this, "No browser on this phone.", Toast.LENGTH_LONG).show();
                }
            });
            actions.addView(open, Ui.gapBelow(this, Ui.wide(), 10));
        }
        if (b.opened <= 0 && !b.finished) {
            TextView open = Ui.pill(this, "Opened the bag today", true, 16);
            open.setOnClickListener(v -> {
                b.opened = Bean.today();
                Store.save(this, b);
                build(b);
            });
            actions.addView(open, Ui.gapBelow(this, Ui.wide(), 10));
        }
        TextView done = Ui.pill(this, b.finished ? "Not finished after all" : "Mark the bag as finished", false, 16);
        done.setOnClickListener(v -> {
            b.setFinished(b.finished ? 0 : Bean.today());
            Store.save(this, b);
            build(b);
        });
        actions.addView(done, Ui.gapBelow(this, Ui.wide(), 10));
        TextView again = Ui.pill(this, "Add another bag of this coffee", false, 16);
        again.setOnClickListener(v -> {
            Bean next = b.anotherBag();
            Store.save(this, next);
            Toast.makeText(this, "Added a new bag. Its dates start empty.", Toast.LENGTH_LONG).show();
            Intent i = new Intent(this, BeanActivity.class);
            i.putExtra("id", next.id);
            startActivity(i);
            finish();
        });
        actions.addView(again, Ui.gapBelow(this, Ui.wide(), 10));
        TextView delete = Ui.pill(this, "Delete", false, 16);
        delete.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Delete " + (b.name.isEmpty() ? "this coffee" : b.name) + "?")
                .setMessage("It is removed from this phone. A backup file you saved keeps it.")
                .setPositiveButton("Delete", (d, w) -> {
                    Store.delete(this, b.id);
                    finish();
                })
                .setNegativeButton("Keep", null)
                .show());
        actions.addView(delete, Ui.wide());
        body.addView(actions, Ui.wide());

        TextView saved = Ui.text(this, "Added " + Store.day(b.added)
                + (b.url.isEmpty() ? "" : " from " + PageReader.host(b.url)), 14);
        saved.setPadding(0, Ui.dp(this, 16), 0, 0);
        body.addView(saved, Ui.wide());

        root.addView(Ui.scroller(this, body), new LinearLayout.LayoutParams(Ui.MATCH, 0, 1f));
        setContentView(root);
    }

    /** Every field of a brew that has a value, labelled the way its method names it. */
    private void showBrew(LinearLayout body, Brew r) {
        Methods.Method m = Methods.get(r.method);
        line(body, "Method", r.method);
        line(body, m.brewerLabel, r.brewer);
        if (m.hasTemp) line(body, "Temperature", r.tempLabel());
        line(body, "Grind", r.grind);
        line(body, "Coffee", Bean.grams(r.dose));
        line(body, Methods.plain(m.waterLabel), Bean.grams(r.water));
        line(body, "Ratio", r.ratio());
        line(body, Methods.plain(m.timeLabel), Methods.show(r.time, m.timeUnit));
        for (Methods.Field f : m.extras) {
            String v = r.extras.get(f.id);
            if (v != null) line(body, Methods.plain(f.label), Methods.show(v, f.unit));
        }
    }

    // ---- the brew log ---------------------------------------------------------------------

    private static final int DAYS_SHOWN = 5;
    private boolean showAll = false;

    /** Brews grouped by day, newest first; each opens to show everything about it. */
    private void brews(LinearLayout body, Bean b) {
        body.addView(Ui.header(this, "Brews (" + b.brews.size() + ")"), Ui.wide());
        if (b.brews.isEmpty()) {
            body.addView(Ui.italic(this, "Each time you make this coffee, press + Log a brew. "
                    + "Your brews show here by date, with how you rated each one.", 16), Ui.wide());
            return;
        }
        double avg = b.averageRating();
        if (avg > 0) {
            body.addView(Ui.text(this, "Average " + Bean.starsOf((int) Math.round(avg)) + "  "
                    + String.format(java.util.Locale.getDefault(), "%.1f", avg), 16), Ui.wide());
        }

        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate current = null;
        int days = 0;
        for (Brew r : b.brews) {
            java.time.LocalDate d = Bean.date(r.at);
            if (!d.equals(current)) {
                days++;
                if (!showAll && days > DAYS_SHOWN) break;
                current = d;
                TextView h = Ui.bold(this, dayTitle(d, today), 16);
                h.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 6));
                body.addView(h, Ui.wide());
            }
            body.addView(brewCard(b, r), Ui.gapBelow(this, Ui.wide(), 8));
        }
        int allDays = 0;
        current = null;
        for (Brew r : b.brews) {
            java.time.LocalDate d = Bean.date(r.at);
            if (!d.equals(current)) {
                allDays++;
                current = d;
            }
        }
        if (allDays > DAYS_SHOWN) {
            TextView more = Ui.pill(this, showAll ? "Show fewer" : "Show all " + allDays + " days", false, 16);
            more.setOnClickListener(v -> {
                showAll = !showAll;
                build(b);
            });
            body.addView(more, Ui.wide());
        }
    }

    private static String dayTitle(java.time.LocalDate d, java.time.LocalDate today) {
        if (d.equals(today)) return "Today";
        if (d.equals(today.minusDays(1))) return "Yesterday";
        String pattern = d.getYear() == today.getYear() ? "EEEE d MMMM" : "EEEE d MMMM yyyy";
        return d.format(java.time.format.DateTimeFormatter.ofPattern(pattern, java.util.Locale.getDefault()));
    }

    private LinearLayout brewCard(Bean b, Brew r) {
        LinearLayout card = Ui.column(this);
        card.setBackground(Ui.box(this, false, 1.5f, 12, false));
        card.setPadding(Ui.dp(this, 14), Ui.dp(this, 8), Ui.dp(this, 14), Ui.dp(this, 9));
        card.setClickable(true);
        card.setOnClickListener(v -> openBrew(b, r.id));

        LinearLayout top = Ui.row(this);
        String clock = java.time.Instant.ofEpochMilli(r.at).atZone(java.time.ZoneId.systemDefault())
                .toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        String what = Bean.joinDots(clock, r.brewer.isEmpty() ? r.method : r.brewer, r.source);
        top.addView(Ui.oneLine(Ui.bold(this, what, 17)), Ui.weight(1));
        if (r.rating > 0) top.addView(Ui.text(this, r.stars(), 17), Ui.gapLeft(this, Ui.wrap(), 8));
        card.addView(top, Ui.wide());

        Methods.Method m = Methods.get(r.method);
        String how = Bean.joinDots(Methods.amounts(r), Methods.show(r.time, m.timeUnit),
                m.hasTemp ? r.tempLabel() : "", r.grind.isEmpty() ? "" : "grind " + r.grind);
        if (!how.isEmpty()) card.addView(Ui.oneLine(Ui.text(this, how, 15)), Ui.wide());
        if (!r.notes.isEmpty()) card.addView(Ui.oneLine(Ui.italic(this, r.notes, 15)), Ui.wide());
        return card;
    }

    private void openBrew(Bean b, String brewId) {
        Intent i = new Intent(this, BrewActivity.class);
        i.putExtra("bean", b.id);
        if (brewId != null) i.putExtra("brew", brewId);
        startActivity(i);
    }

    /** "Altitude    1600–1900 m", left out when empty. Returns 1 if shown. */
    private int line(LinearLayout body, String label, String value) {
        if (value == null || value.trim().isEmpty()) return 0;
        LinearLayout row = Ui.row(this);
        row.setGravity(android.view.Gravity.TOP);
        TextView l = Ui.text(this, label, 16);
        row.addView(l, new LinearLayout.LayoutParams(Ui.dp(this, 118), Ui.WRAP));
        row.addView(Ui.bold(this, value.trim(), 17), Ui.weight(1));
        body.addView(row, Ui.gapBelow(this, Ui.wide(), 6));
        return 1;
    }

    private LinearLayout.LayoutParams gap() {
        return Ui.gapLeft(this, Ui.wrap(), 6);
    }
}

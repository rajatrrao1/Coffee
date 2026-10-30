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

        // Your brew comes first: it is what you open this screen for at the kettle.
        body.addView(Ui.header(this, "My brew"), Ui.wide());
        if (b.untried()) {
            body.addView(Ui.italic(this, "Nothing written down yet. Press Edit to add the "
                    + "water temperature and grind once you have brewed it.", 16), Ui.wide());
        } else {
            line(body, "Method", b.method);
            line(body, "Water", b.tempLabel());
            line(body, "Grind", b.grind);
            line(body, "Coffee", Bean.grams(b.dose));
            line(body, "Water amount", Bean.grams(b.water));
            line(body, "Ratio", b.ratio());
            line(body, "Brew time", b.time);
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
        TextView done = Ui.pill(this, b.finished ? "Back in rotation" : "Mark the bag as finished", false, 16);
        done.setOnClickListener(v -> {
            b.finished = !b.finished;
            Store.save(this, b);
            build(b);
        });
        actions.addView(done, Ui.gapBelow(this, Ui.wide(), 10));
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

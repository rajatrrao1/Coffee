package app.kohi;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Log a brew of one coffee, or open one you logged. A new brew starts from your last brew of
 * the bag (or your usual recipe), dated today, so you only change what you changed.
 */
public class BrewActivity extends Activity {

    private Bean bean;
    private Brew brew;
    private boolean isNew;
    private String initial = "";

    private BrewForm form;
    private DateField day;
    private EditText notes;
    private int rating = 0;
    private String taste = "";
    private String strength = "";
    private Compass.Grid compass;
    private TextView advice;
    private EditText words;
    private boolean makeRecipe = false;
    private final List<TextView> ratingChips = new ArrayList<>();
    private TextView recipePill;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        bean = Store.find(this, getIntent().getStringExtra("bean"));
        if (bean == null) {
            finish();
            return;
        }
        Brew existing = bean.findBrew(getIntent().getStringExtra("brew"));
        isNew = existing == null;
        if (isNew) {
            brew = bean.startingPoint();
            brew.at = System.currentTimeMillis();
        } else {
            brew = existing;
        }
        build();
        form.load(brew);
        day.set(Bean.noon(Bean.date(brew.at)));
        notes.setText(brew.notes);
        taste = brew.taste;
        strength = brew.strength;
        showCompass();
        setRating(brew.rating);
        initial = snapshot();
    }

    private void build() {
        int pad = Ui.dp(this, 16);
        LinearLayout root = Ui.column(this);
        root.setBackgroundColor(Ui.PAPER);
        TextView cancel = Ui.pill(this, "Cancel", false, 16);
        cancel.setOnClickListener(v -> onBackPressed());
        TextView save = Ui.pill(this, "Save", true, 16);
        save.setOnClickListener(v -> save());
        root.addView(Ui.topBar(this, cancel, isNew ? "New brew" : "Brew", save), Ui.wide());
        root.addView(Ui.rule(this));

        LinearLayout body = Ui.column(this);
        body.setPadding(pad, Ui.dp(this, 10), pad, Ui.dp(this, 40));
        body.addView(Ui.oneLine(Ui.bold(this, bean.name.isEmpty() ? "Unnamed" : bean.name, 22)), Ui.wide());
        if (!brew.source.isEmpty()) {
            body.addView(Ui.italic(this, "Logged on " + brew.source + ". A change you make here stays, "
                    + "even if it changes there too.", 15), Ui.wide());
        }
        if (isNew) {
            body.addView(Ui.italic(this, bean.brews.isEmpty()
                    ? "Filled in from your usual recipe. Change what you did differently."
                    : "Filled in from your last brew. Change what you did differently.", 15), Ui.wide());
        }
        if (isNew && !bean.brews.isEmpty()) {
            String last = Advice.next(bean.brews.get(0));
            if (!last.isEmpty()) {
                TextView t = Ui.bold(this, "Last time: " + last, 16);
                t.setPadding(0, Ui.dp(this, 8), 0, 0);
                body.addView(t, Ui.wide());
            }
        }
        day = new DateField(this, body, "Brewed on", false);

        body.addView(Ui.header(this, "How you made it"), Ui.wide());
        form = new BrewForm(this);
        body.addView(form.view(), Ui.wide());

        body.addView(Ui.header(this, "How it was"), Ui.wide());
        body.addView(Ui.italic(this, "Tap where this cup landed. The middle is the goal.", 15), Ui.wide());
        compass = new Compass.Grid(this, (t, s) -> {
            boolean same = t.equals(taste) && s.equals(strength);
            taste = same ? "" : t;
            strength = same ? "" : s;
            showCompass();
        });
        LinearLayout.LayoutParams compassLp = Ui.wide();
        compassLp.topMargin = Ui.dp(this, 8);
        body.addView(compass.view, compassLp);
        words = Form.labeled(this, body, "Or say it in a word", Ui.field(this, "e.g. lemony, watery, dry"));
        words.addTextChangedListener(Form.after(() -> {
            String[] found = Compass.lookup(Form.val(words));
            if (found[0].isEmpty() && found[1].isEmpty()) return;
            if (!found[0].isEmpty()) taste = found[0];
            if (!found[1].isEmpty()) strength = found[1];
            showCompass();
        }));
        advice = Ui.bold(this, "", 16);
        advice.setPadding(0, Ui.dp(this, 10), 0, 0);
        body.addView(advice, Ui.wide());
        TextView foot = Ui.italic(this, Compass.FOOTNOTE, 14);
        foot.setPadding(0, Ui.dp(this, 6), 0, 0);
        body.addView(foot, Ui.wide());

        Form.label(this, body, "Rating");
        LinearLayout ratingRow = Ui.row(this);
        for (int i = 1; i <= 5; i++) {
            int value = i;
            TextView chip = Ui.pill(this, "★ " + i, false, 16);
            chip.setOnClickListener(v -> setRating(rating == value ? 0 : value));
            ratingChips.add(chip);
            ratingRow.addView(chip, Ui.gapLeft(this, Ui.wrap(), i == 1 ? 0 : 6));
        }
        body.addView(Form.sideways(this, ratingRow), Ui.wide());
        notes = Form.labeled(this, body, "Notes", Ui.area(this, "e.g. a bit sour, grind finer next time", 3));

        LinearLayout actions = Ui.column(this);
        actions.setPadding(0, Ui.dp(this, 20), 0, 0);
        recipePill = Ui.pill(this, "", false, 16);
        recipePill.setOnClickListener(v -> setMakeRecipe(!makeRecipe));
        actions.addView(recipePill, Ui.gapBelow(this, Ui.wide(), 10));
        setMakeRecipe(false);
        if (!isNew) {
            TextView delete = Ui.pill(this, "Delete this brew", false, 16);
            delete.setOnClickListener(v -> new AlertDialog.Builder(this)
                    .setTitle("Delete this brew?")
                    .setMessage("Only this one brew of " + bean.name + " is removed.")
                    .setPositiveButton("Delete", (d, w) -> {
                        TwoTone.deleted(this, brew);
                        bean.removeBrew(brew.id);
                        Store.save(this, bean);
                        finish();
                    })
                    .setNegativeButton("Keep", null)
                    .show());
            actions.addView(delete, Ui.wide());
        }
        body.addView(actions, Ui.wide());

        root.addView(Ui.scroller(this, body), new LinearLayout.LayoutParams(Ui.MATCH, 0, 1f));
        setContentView(root);
    }

    private void setRating(int r) {
        rating = r;
        for (int i = 0; i < ratingChips.size(); i++) Ui.setPill(this, ratingChips.get(i), i + 1 == r);
    }

    private void showCompass() {
        compass.show(taste, strength, null);
        Brew probe = new Brew();
        form.into(probe);
        probe.taste = taste;
        probe.strength = strength;
        String next = Advice.next(probe);
        advice.setText(next.isEmpty() ? "" : "Next time: " + next);
        advice.setVisibility(next.isEmpty() ? android.view.View.GONE : android.view.View.VISIBLE);
    }

    private void setMakeRecipe(boolean on) {
        makeRecipe = on;
        recipePill.setText(on ? "✓ Also my usual recipe from now on" : "Also make this my usual recipe");
        Ui.setPill(this, recipePill, on);
    }

    private String snapshot() {
        return form.snapshot() + '\u0001' + day.value + '\u0001' + rating + '\u0001'
                + Form.val(notes) + '\u0001' + makeRecipe + '\u0001' + taste + '\u0001' + strength;
    }

    /** The chosen day, at the time of day the brew was logged (now, for a new one). */
    private long when() {
        ZoneId zone = ZoneId.systemDefault();
        LocalTime clock = Instant.ofEpochMilli(brew.at).atZone(zone).toLocalTime();
        return Bean.date(day.value).atTime(clock).atZone(zone).toInstant().toEpochMilli();
    }

    private void save() {
        form.into(brew);
        brew.at = when();
        brew.rating = rating;
        brew.notes = Form.val(notes);
        brew.taste = taste;
        brew.strength = strength;
        if (!brew.source.isEmpty() && !snapshot().equals(initial)) brew.editedHere = true;
        bean.putBrew(brew);
        if (makeRecipe) bean.setRecipe(brew.copy());
        Store.save(this, bean);
        Toast.makeText(this, isNew ? "Brew saved." : "Brew changed.", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void onBackPressed() {
        if (snapshot().equals(initial)) {
            super.onBackPressed();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Leave without saving?")
                .setMessage("This brew will not be kept.")
                .setPositiveButton("Leave", (d, w) -> finish())
                .setNegativeButton("Keep editing", null)
                .show();
    }
}

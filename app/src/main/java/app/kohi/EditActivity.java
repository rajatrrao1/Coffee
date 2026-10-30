package app.kohi;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EditActivity extends Activity {

    private Bean bean;
    private boolean isNew;
    private boolean fromShare;
    private String initial = "";

    private EditText url, name, roaster, origin, region, altitude, process, variety, producer, notes;
    private EditText method, temp, grind, dose, water, time, myNotes, bag, price;
    private DateField opened, finished;
    private TextView status, fetchButton;
    private ScrollView scroll;

    private String roast = "";
    private int rating = 0;
    private final List<TextView> roastChips = new ArrayList<>();
    private final List<TextView> ratingChips = new ArrayList<>();

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        String id = getIntent().getStringExtra("id");
        bean = id != null ? Store.find(this, id) : null;
        isNew = bean == null;
        if (isNew) bean = new Bean();

        build();
        fill();
        initial = snapshot();

        Intent in = getIntent();
        if (Intent.ACTION_SEND.equals(in.getAction())) {
            fromShare = true;
            String link = firstLink(in.getStringExtra(Intent.EXTRA_TEXT));
            if (link != null) {
                url.setText(link);
                fetch();
            } else {
                say("There was no link in what was shared. Paste the page's address above.");
            }
        }
    }

    // ---- layout ---------------------------------------------------------------------------

    private void build() {
        int pad = Ui.dp(this, 16);
        LinearLayout root = Ui.column(this);
        root.setBackgroundColor(Ui.PAPER);

        TextView cancel = Ui.pill(this, "Cancel", false, 16);
        cancel.setOnClickListener(v -> onBackPressed());
        TextView save = Ui.pill(this, "Save", true, 16);
        save.setOnClickListener(v -> save());
        root.addView(Ui.topBar(this, cancel, isNew ? "New coffee" : "Edit coffee", save), Ui.wide());
        root.addView(Ui.rule(this));

        LinearLayout body = Ui.column(this);
        body.setPadding(pad, Ui.dp(this, 6), pad, Ui.dp(this, 40));

        // The link, and Fetch.
        label(body, "Link to the coffee's page");
        LinearLayout linkRow = Ui.row(this);
        url = Ui.field(this, "https://…");
        url.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        url.setSingleLine(true);
        linkRow.addView(url, Ui.weight(1));
        fetchButton = Ui.pill(this, "Fetch", false, 16);
        fetchButton.setOnClickListener(v -> fetch());
        linkRow.addView(fetchButton, Ui.gapLeft(this, Ui.wrap(), 8));
        body.addView(linkRow, Ui.wide());
        status = Ui.italic(this, "", 15);
        status.setVisibility(View.GONE);
        status.setPadding(0, Ui.dp(this, 8), 0, 0);
        body.addView(status, Ui.wide());

        body.addView(Ui.header(this, "The bean"), Ui.wide());
        name = labeled(body, "Name", Ui.field(this, "e.g. Discover Monthly – Mexico"));
        roaster = labeled(body, "Roaster", Ui.field(this, "e.g. Wakuli"));
        origin = labeled(body, "Origin", Ui.field(this, "Country"));
        region = labeled(body, "Region", Ui.field(this, "e.g. Oaxaca"));
        altitude = labeled(body, "Altitude", Ui.field(this, "e.g. 1800 m or 1600–1900 m"));
        process = labeled(body, "Process", Ui.field(this, "Washed, natural, honey…"));
        variety = labeled(body, "Variety", Ui.field(this, "e.g. Bourbon, Typica"));
        producer = labeled(body, "Producer", Ui.field(this, "Farm, washing station or cooperative"));
        notes = labeled(body, "Tasting notes", Ui.area(this, "e.g. green apple, cinnamon, cola", 2));

        label(body, "Roast");
        LinearLayout roastRow = Ui.row(this);
        for (String r : new String[]{"Light", "Medium", "Dark"}) {
            TextView chip = Ui.pill(this, r, false, 16);
            chip.setOnClickListener(v -> setRoast(roast.equals(r) ? "" : r));
            roastChips.add(chip);
            roastRow.addView(chip, Ui.gapLeft(this, Ui.wrap(), roastChips.size() == 1 ? 0 : 8));
        }
        body.addView(roastRow, Ui.wide());

        body.addView(Ui.header(this, "My brew"), Ui.wide());
        method = labeled(body, "Method", Ui.field(this, "e.g. V60"));
        HorizontalScrollView methodScroll = new HorizontalScrollView(this);
        methodScroll.setHorizontalScrollBarEnabled(false);
        methodScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout methodRow = Ui.row(this);
        methodRow.setPadding(0, Ui.dp(this, 8), 0, 0);
        String[] methods = {"V60", "AeroPress", "Espresso", "French press", "Moka pot", "Chemex", "Kalita", "Cold brew"};
        for (int i = 0; i < methods.length; i++) {
            String m = methods[i];
            TextView chip = Ui.pill(this, m, false, 15);
            chip.setOnClickListener(v -> method.setText(m));
            methodRow.addView(chip, Ui.gapLeft(this, Ui.wrap(), i == 0 ? 0 : 8));
        }
        methodScroll.addView(methodRow);
        body.addView(methodScroll, Ui.wide());

        temp = Ui.field(this, "93");
        temp.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        time = Ui.field(this, "e.g. 3:00");
        pair(body, "Water temperature (°C)", temp, "Brew time", time);
        grind = labeled(body, "Grind", Ui.field(this, "e.g. 22 clicks, medium-fine"));
        dose = Ui.field(this, "15");
        dose.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        water = Ui.field(this, "250");
        water.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        pair(body, "Coffee (g)", dose, "Water (g)", water);

        body.addView(Ui.header(this, "The bag"), Ui.wide());
        bag = Ui.field(this, "250");
        bag.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        price = Ui.field(this, "10,90");
        price.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        pair(body, "Bag size (g)", bag, "Price (€)", price);
        opened = new DateField(body, "Opened");
        finished = new DateField(body, "Finished");

        body.addView(Ui.header(this, "My notes"), Ui.wide());
        label(body, "Rating");
        LinearLayout ratingRow = Ui.row(this);
        for (int i = 1; i <= 5; i++) {
            int value = i;
            TextView chip = Ui.pill(this, "★ " + i, false, 16);
            chip.setOnClickListener(v -> setRating(rating == value ? 0 : value));
            ratingChips.add(chip);
            ratingRow.addView(chip, Ui.gapLeft(this, Ui.wrap(), i == 1 ? 0 : 6));
        }
        HorizontalScrollView ratingScroll = new HorizontalScrollView(this);
        ratingScroll.setHorizontalScrollBarEnabled(false);
        ratingScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        ratingScroll.addView(ratingRow);
        body.addView(ratingScroll, Ui.wide());
        myNotes = labeled(body, "Notes", Ui.area(this, "e.g. a bit sour at 92, better at 95", 3));

        scroll = Ui.scroller(this, body);
        root.addView(scroll, new LinearLayout.LayoutParams(Ui.MATCH, 0, 1f));
        setContentView(root);
    }

    private void label(LinearLayout body, String text) {
        TextView l = Ui.text(this, text, 15);
        l.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 4));
        body.addView(l, Ui.wide());
    }

    private EditText labeled(LinearLayout body, String text, EditText field) {
        label(body, text);
        body.addView(field, Ui.wide());
        return field;
    }

    /** Two short fields side by side. */
    private void pair(LinearLayout body, String labelA, EditText a, String labelB, EditText b) {
        LinearLayout row = Ui.row(this);
        row.setGravity(android.view.Gravity.BOTTOM);
        LinearLayout colA = Ui.column(this);
        LinearLayout colB = Ui.column(this);
        label(colA, labelA);
        colA.addView(a, Ui.wide());
        label(colB, labelB);
        colB.addView(b, Ui.wide());
        row.addView(colA, Ui.weight(1));
        row.addView(colB, Ui.gapLeft(this, Ui.weight(1), 10));
        body.addView(row, Ui.wide());
    }

    private void setRoast(String r) {
        roast = r;
        for (TextView chip : roastChips) Ui.setPill(this, chip, chip.getText().toString().equals(r));
    }

    private void setRating(int r) {
        rating = r;
        for (int i = 0; i < ratingChips.size(); i++) Ui.setPill(this, ratingChips.get(i), i + 1 == r);
    }

    private void say(String s) {
        status.setText(s);
        status.setVisibility(s.isEmpty() ? View.GONE : View.VISIBLE);
    }

    // ---- data in and out ------------------------------------------------------------------

    private void fill() {
        url.setText(bean.url);
        name.setText(bean.name);
        roaster.setText(bean.roaster);
        origin.setText(bean.origin);
        region.setText(bean.region);
        altitude.setText(bean.altitude);
        process.setText(bean.process);
        variety.setText(bean.variety);
        producer.setText(bean.producer);
        notes.setText(bean.notes);
        method.setText(bean.method);
        temp.setText(bean.temp);
        grind.setText(bean.grind);
        dose.setText(bean.dose);
        water.setText(bean.water);
        time.setText(bean.time);
        myNotes.setText(bean.myNotes);
        bag.setText(bean.bag);
        price.setText(bean.price);
        opened.set(bean.opened);
        finished.set(bean.finishedOn);
        setRoast(bean.roast);
        setRating(bean.rating);
    }

    private static String val(EditText e) {
        return e.getText().toString().trim();
    }

    private void collect() {
        bean.url = val(url);
        bean.name = val(name);
        bean.roaster = val(roaster);
        bean.origin = val(origin);
        bean.region = val(region);
        bean.altitude = val(altitude);
        bean.process = val(process);
        bean.variety = val(variety);
        bean.producer = val(producer);
        bean.notes = val(notes);
        bean.method = val(method);
        bean.temp = val(temp);
        bean.grind = val(grind);
        bean.dose = val(dose);
        bean.water = val(water);
        bean.time = val(time);
        bean.myNotes = val(myNotes);
        bean.bag = val(bag);
        bean.price = val(price);
        bean.opened = opened.value;
        bean.setFinished(finished.value);
        bean.roast = roast;
        bean.rating = rating;
    }

    private String snapshot() {
        StringBuilder s = new StringBuilder();
        for (EditText e : new EditText[]{url, name, roaster, origin, region, altitude, process,
                variety, producer, notes, method, temp, grind, dose, water, time, myNotes, bag, price}) {
            s.append(val(e)).append('\u0001');
        }
        return s.append(roast).append('\u0001').append(rating).append('\u0001')
                .append(opened.value).append('\u0001').append(finished.value).toString();
    }

    private void save() {
        if (val(name).isEmpty()) {
            say("Give the coffee a name first.");
            scroll.scrollTo(0, 0);
            name.requestFocus();
            return;
        }
        if (opened.value > 0 && finished.value > 0
                && Bean.date(finished.value).isBefore(Bean.date(opened.value))) {
            say("The bag is finished before it was opened. Check the two dates.");
            return;
        }
        collect();
        Store.save(this, bean);
        if (fromShare) {
            Toast.makeText(this, "Saved to Coffee.", Toast.LENGTH_SHORT).show();
        } else if (isNew) {
            Intent i = new Intent(this, BeanActivity.class);
            i.putExtra("id", bean.id);
            startActivity(i);
        }
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
                .setMessage("What you typed here will be lost.")
                .setPositiveButton("Leave", (d, w) -> finish())
                .setNegativeButton("Keep editing", null)
                .show();
    }

    /** A date you set with a calendar, or Today, and can clear again. */
    private final class DateField {
        long value = 0;
        private final TextView shown;
        private final TextView clear;

        DateField(LinearLayout body, String title) {
            label(body, title);
            LinearLayout row = Ui.row(EditActivity.this);
            shown = Ui.pill(EditActivity.this, "", false, 16);
            shown.setOnClickListener(v -> pick());
            row.addView(shown, Ui.weight(1));
            TextView today = Ui.pill(EditActivity.this, "Today", false, 16);
            today.setOnClickListener(v -> set(Bean.today()));
            row.addView(today, Ui.gapLeft(EditActivity.this, Ui.wrap(), 8));
            clear = Ui.pill(EditActivity.this, "Clear", false, 16);
            clear.setOnClickListener(v -> set(0));
            row.addView(clear, Ui.gapLeft(EditActivity.this, Ui.wrap(), 8));
            body.addView(row, Ui.wide());
        }

        void set(long day) {
            value = day;
            shown.setText(day > 0 ? Store.day(day) : "Choose a date");
            clear.setVisibility(day > 0 ? View.VISIBLE : View.GONE);
        }

        void pick() {
            LocalDate d = value > 0 ? Bean.date(value) : LocalDate.now();
            new DatePickerDialog(EditActivity.this,
                    (picker, y, m, dayOfMonth) -> set(Bean.noon(LocalDate.of(y, m + 1, dayOfMonth))),
                    d.getYear(), d.getMonthValue() - 1, d.getDayOfMonth()).show();
        }
    }

    // ---- reading the page -----------------------------------------------------------------

    private static final Pattern LINK = Pattern.compile("https?://\\S+", Pattern.CASE_INSENSITIVE);

    static String firstLink(String text) {
        if (text == null) return null;
        Matcher m = LINK.matcher(text);
        return m.find() ? m.group().replaceAll("[)\\].,;]+$", "") : null;
    }

    private void fetch() {
        String u = val(url);
        if (u.isEmpty()) {
            say("Paste the link to the coffee's page first.");
            return;
        }
        if (!u.matches("(?i)^https?://.*")) {
            u = "https://" + u;
            url.setText(u);
        }
        final String address = u;
        fetchButton.setEnabled(false);
        say("Reading the page…");
        new Thread(() -> {
            PageReader.Result found = null;
            String error = null;
            try {
                found = PageReader.read(address);
            } catch (Exception e) {
                error = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            }
            final PageReader.Result r = found;
            final String err = error;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                fetchButton.setEnabled(true);
                if (r == null) {
                    say("Could not open the page (" + err + "). Check the link, or type the details in.");
                } else {
                    apply(r, PageReader.host(address));
                }
            });
        }, "fetch").start();
    }

    /** Fills only the fields that are still empty, and says what the page did not have. */
    private void apply(PageReader.Result r, String host) {
        int n = 0;
        n += put(name, r.name);
        n += put(roaster, r.roaster);
        n += put(origin, r.origin);
        n += put(region, r.region);
        n += put(altitude, r.altitude);
        n += put(process, r.process);
        n += put(variety, r.variety);
        n += put(producer, r.producer);
        n += put(notes, r.notes);
        if (roast.isEmpty() && !r.roast.isEmpty()) {
            setRoast(r.roast);
            n++;
        }

        List<String> missing = new ArrayList<>();
        if (r.origin.isEmpty() && val(origin).isEmpty()) missing.add("origin");
        if (r.altitude.isEmpty() && val(altitude).isEmpty()) missing.add("altitude");
        if (r.process.isEmpty() && val(process).isEmpty()) missing.add("process");
        if (r.variety.isEmpty() && val(variety).isEmpty()) missing.add("variety");
        if (r.notes.isEmpty() && val(notes).isEmpty()) missing.add("tasting notes");

        String msg = n == 0
                ? "Found nothing new to fill in on " + host + "."
                : "Filled in " + n + (n == 1 ? " field" : " fields") + " from " + host + ". Check them before saving.";
        if (!missing.isEmpty()) msg += "\nNot on the page: " + String.join(", ", missing) + ".";
        say(msg);
    }

    private static int put(EditText field, String value) {
        if (value == null || value.isEmpty() || !val(field).isEmpty()) return 0;
        field.setText(value);
        return 1;
    }
}

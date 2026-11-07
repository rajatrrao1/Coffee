package app.kohi;

import android.app.Activity;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Method buttons, and below them the fields that method needs. Coffee, ratio and yield stay
 * in step: change the coffee or tap a ratio and the yield follows; type a yield and the ratio
 * follows. Used for a coffee's usual recipe and for every brew you log.
 */
final class BrewForm {

    private final Activity a;
    private final LinearLayout root;
    private final LinearLayout box;
    private final List<TextView> methodChips = new ArrayList<>();
    private final EditText brewer, temp, grind, dose, ratio, water, time;
    private final Map<String, String> extraValues = new LinkedHashMap<>();
    private String method = "";
    private boolean syncing;

    BrewForm(Activity a) {
        this.a = a;
        root = Ui.column(a);
        Form.label(a, root, "Method");
        LinearLayout methodRow = Ui.row(a);
        for (Methods.Method m : Methods.ALL) {
            TextView chip = Ui.pill(a, m.name, false, 16);
            chip.setOnClickListener(v -> choose(method.equals(m.name) ? "" : m.name));
            methodChips.add(chip);
            methodRow.addView(chip, Ui.gapLeft(a, Ui.wrap(), methodChips.size() == 1 ? 0 : 8));
        }
        root.addView(Form.sideways(a, methodRow), Ui.wide());

        brewer = Ui.field(a, "");
        temp = Form.numberField(a, "");
        grind = Ui.field(a, "e.g. 22 clicks, medium-fine");
        dose = Form.numberField(a, "");
        ratio = Form.numberField(a, "");
        water = Form.numberField(a, "");
        time = Ui.field(a, "");
        dose.addTextChangedListener(Form.after(() -> { if (!syncing) doseChanged(); }));
        ratio.addTextChangedListener(Form.after(() -> { if (!syncing) ratioChanged(); }));
        water.addTextChangedListener(Form.after(() -> { if (!syncing) waterChanged(); }));

        box = Ui.column(a);
        root.addView(box, Ui.wide());
        choose("");
    }

    View view() {
        return root;
    }

    void load(Brew b) {
        syncing = true;
        brewer.setText(b.brewer);
        temp.setText(b.temp);
        grind.setText(b.grind);
        dose.setText(b.dose);
        water.setText(b.water);
        String r = b.ratio();
        ratio.setText(r.isEmpty() ? "" : r.substring(2));
        time.setText(b.time);
        syncing = false;
        extraValues.clear();
        extraValues.putAll(b.extras);
        choose(b.method);
    }

    /** Copies what is in the form into b (method, amounts, extras); leaves date, rating and notes alone. */
    void into(Brew b) {
        b.method = method;
        b.brewer = Form.val(brewer);
        b.temp = Form.val(temp);
        b.grind = Form.val(grind);
        b.dose = Form.val(dose);
        b.water = Form.val(water);
        b.time = Form.val(time);
        b.extras = new LinkedHashMap<>(extraValues);
        Methods.keepOnly(b.extras, Methods.get(method));
    }

    String snapshot() {
        StringBuilder s = new StringBuilder(method).append('\u0001');
        for (EditText e : new EditText[]{brewer, temp, grind, dose, water, time}) {
            s.append(Form.val(e)).append('\u0001');
        }
        Map<String, String> extras = new TreeMap<>(extraValues);
        Methods.keepOnly(extras, Methods.get(method));
        return s.append(extras).toString();
    }

    // ---- layout ---------------------------------------------------------------------------

    private void choose(String name) {
        method = name;
        for (TextView chip : methodChips) Ui.setPill(a, chip, chip.getText().toString().equals(name));
        layout(Methods.get(name));
    }

    private static void detach(View v) {
        if (v.getParent() instanceof ViewGroup) ((ViewGroup) v.getParent()).removeView(v);
    }

    private void layout(Methods.Method m) {
        for (View v : new View[]{brewer, temp, grind, dose, ratio, water, time}) detach(v);
        box.removeAllViews();

        if (!method.isEmpty() && !m.tip.isEmpty()) {
            TextView tip = Ui.italic(a, m.tip, 15);
            tip.setPadding(0, Ui.dp(a, 10), 0, 0);
            box.addView(tip, Ui.wide());
        }

        brewer.setHint(m.brewerHint);
        Form.labeled(a, box, m.brewerLabel, brewer);
        if (m.brewers.length > 0) {
            LinearLayout row = Ui.row(a);
            for (int i = 0; i < m.brewers.length; i++) {
                String b = m.brewers[i];
                TextView chip = Ui.pill(a, b, false, 15);
                chip.setOnClickListener(v -> brewer.setText(b));
                row.addView(chip, Ui.gapLeft(a, Ui.wrap(), i == 0 ? 0 : 8));
            }
            box.addView(Form.sideways(a, row), Ui.wide());
        }

        dose.setHint(m.doseHint);
        water.setHint(m.waterHint);
        ratio.setHint(Methods.number(Bean.firstNumber(m.waterHint) / Bean.firstNumber(m.doseHint)));
        Form.columns(a, box, new String[]{"Coffee (g)", "Ratio 1 :", m.waterLabel}, new EditText[]{dose, ratio, water});
        if (m.ratios.length > 0) {
            LinearLayout row = Ui.row(a);
            for (int i = 0; i < m.ratios.length; i++) {
                String r = m.ratios[i];
                TextView chip = Ui.pill(a, r, false, 15);
                chip.setOnClickListener(v -> ratio.setText(r.substring(r.indexOf(':') + 1)));
                row.addView(chip, Ui.gapLeft(a, Ui.wrap(), i == 0 ? 0 : 8));
            }
            LinearLayout.LayoutParams lp = Ui.wide();
            lp.topMargin = Ui.dp(a, 8);
            box.addView(Form.sideways(a, row), lp);
        }

        temp.setHint(m.tempHint);
        time.setHint(m.timeHint);
        time.setInputType(m.timeUnit.isEmpty() ? InputType.TYPE_CLASS_TEXT
                : InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (m.hasTemp) {
            Form.columns(a, box, new String[]{"Water temperature (°C)", m.timeLabel}, new EditText[]{temp, time});
        } else {
            Form.labeled(a, box, m.timeLabel, time);
        }
        Form.labeled(a, box, "Grind", grind);

        List<Methods.Field> fields = m.extras;
        for (int i = 0; i < fields.size(); i++) {
            Methods.Field f = fields.get(i);
            if (f.kind == Methods.CHOICE) {
                choiceRow(f);
            } else if (f.kind == Methods.NUMBER && i + 1 < fields.size() && fields.get(i + 1).kind == Methods.NUMBER) {
                Methods.Field g = fields.get(i + 1);
                Form.columns(a, box, new String[]{f.label, g.label}, new EditText[]{extraField(f), extraField(g)});
                i++;
            } else {
                Form.labeled(a, box, f.label, extraField(f));
            }
        }
    }

    private EditText extraField(Methods.Field f) {
        EditText e = f.kind == Methods.NUMBER ? Form.numberField(a, f.hint) : Ui.field(a, f.hint);
        String v = extraValues.get(f.id);
        e.setText(v == null ? "" : v);
        e.addTextChangedListener(Form.after(() -> {
            String t = Form.val(e);
            if (t.isEmpty()) extraValues.remove(f.id);
            else extraValues.put(f.id, t);
        }));
        return e;
    }

    private void choiceRow(Methods.Field f) {
        Form.label(a, box, f.label);
        LinearLayout row = Ui.row(a);
        List<TextView> chips = new ArrayList<>();
        for (int i = 0; i < f.choices.length; i++) {
            String c = f.choices[i];
            TextView chip = Ui.pill(a, c, c.equals(extraValues.get(f.id)), 15);
            chip.setOnClickListener(v -> {
                if (c.equals(extraValues.get(f.id))) extraValues.remove(f.id);
                else extraValues.put(f.id, c);
                for (TextView t : chips) Ui.setPill(a, t, t.getText().toString().equals(extraValues.get(f.id)));
            });
            chips.add(chip);
            row.addView(chip, Ui.gapLeft(a, Ui.wrap(), i == 0 ? 0 : 8));
        }
        box.addView(Form.sideways(a, row), Ui.wide());
    }

    // ---- coffee, ratio and yield in step ---------------------------------------------------

    private void doseChanged() {
        double d = num(dose), r = num(ratio), w = num(water);
        if (d > 0 && r > 0) quietly(water, Methods.number(d * r));
        else if (d > 0 && w > 0) quietly(ratio, Methods.number(w / d));
    }

    private void ratioChanged() {
        double d = num(dose), r = num(ratio);
        if (d > 0 && r > 0) quietly(water, Methods.number(d * r));
    }

    private void waterChanged() {
        double d = num(dose), w = num(water);
        if (d > 0 && w > 0) quietly(ratio, Methods.number(w / d));
    }

    private void quietly(EditText e, String value) {
        if (Form.val(e).equals(value)) return;
        syncing = true;
        e.setText(value);
        syncing = false;
    }

    private static double num(EditText e) {
        return Bean.firstNumber(Form.val(e));
    }
}

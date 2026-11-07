package app.kohi;

import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Small layout helpers shared by the forms. */
final class Form {

    private Form() {}

    static void label(Context c, LinearLayout body, String text) {
        TextView l = Ui.text(c, text, 15);
        l.setPadding(0, Ui.dp(c, 12), 0, Ui.dp(c, 4));
        body.addView(l, Ui.wide());
    }

    static EditText labeled(Context c, LinearLayout body, String text, EditText field) {
        label(c, body, text);
        body.addView(field, Ui.wide());
        return field;
    }

    /** Short fields side by side, each with its label above it. */
    static void columns(Context c, LinearLayout body, String[] labels, EditText[] fields) {
        LinearLayout row = Ui.row(c);
        row.setGravity(Gravity.BOTTOM);
        for (int i = 0; i < fields.length; i++) {
            LinearLayout col = Ui.column(c);
            label(c, col, labels[i]);
            col.addView(fields[i], Ui.wide());
            row.addView(col, i == 0 ? Ui.weight(1) : Ui.gapLeft(c, Ui.weight(1), 10));
        }
        body.addView(row, Ui.wide());
    }

    static HorizontalScrollView sideways(Context c, LinearLayout row) {
        HorizontalScrollView h = new HorizontalScrollView(c);
        h.setHorizontalScrollBarEnabled(false);
        h.setOverScrollMode(View.OVER_SCROLL_NEVER);
        row.setPadding(0, Ui.dp(c, 2), 0, Ui.dp(c, 2));
        h.addView(row);
        return h;
    }

    static EditText numberField(Context c, String hint) {
        EditText e = Ui.field(c, hint);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        return e;
    }

    static String val(EditText e) {
        return e.getText().toString().trim();
    }

    static TextWatcher after(Runnable r) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { r.run(); }
        };
    }
}

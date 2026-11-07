package app.kohi;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.time.LocalDate;

/** A day chosen with a calendar or Today, kept as noon of that day; optionally clearable. */
final class DateField {

    long value = 0;
    private final Activity a;
    private final TextView shown;
    private final TextView clear;

    DateField(Activity a, LinearLayout body, String title, boolean clearable) {
        this.a = a;
        Form.label(a, body, title);
        LinearLayout row = Ui.row(a);
        shown = Ui.pill(a, "", false, 16);
        shown.setOnClickListener(v -> pick());
        row.addView(shown, Ui.weight(1));
        TextView today = Ui.pill(a, "Today", false, 16);
        today.setOnClickListener(v -> set(Bean.today()));
        row.addView(today, Ui.gapLeft(a, Ui.wrap(), 8));
        clear = Ui.pill(a, "Clear", false, 16);
        clear.setOnClickListener(v -> set(0));
        if (clearable) row.addView(clear, Ui.gapLeft(a, Ui.wrap(), 8));
        body.addView(row, Ui.wide());
    }

    void set(long day) {
        value = day;
        shown.setText(day > 0 ? Store.day(day) : "Choose a date");
        clear.setVisibility(day > 0 ? View.VISIBLE : View.GONE);
    }

    private void pick() {
        LocalDate d = value > 0 ? Bean.date(value) : LocalDate.now();
        new DatePickerDialog(a,
                (picker, y, m, dayOfMonth) -> set(Bean.noon(LocalDate.of(y, m + 1, dayOfMonth))),
                d.getYear(), d.getMonthValue() - 1, d.getDayOfMonth()).show();
    }
}

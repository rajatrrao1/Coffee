package app.kohi;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Pure black on white, solid outlines, no shading: everything an e-ink screen draws crisply. */
final class Ui {

    static final int INK = Color.BLACK;
    static final int PAPER = Color.WHITE;
    static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private Ui() {}

    static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    static TextView text(Context c, String s, float sp) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextColor(INK);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        return t;
    }

    static TextView bold(Context c, String s, float sp) {
        TextView t = text(c, s, sp);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    static TextView italic(Context c, String s, float sp) {
        TextView t = text(c, s, sp);
        t.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
        return t;
    }

    static TextView oneLine(TextView t) {
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        return t;
    }

    static GradientDrawable box(Context c, boolean filled, float strokeDp, float radiusDp, boolean dashed) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setColor(filled ? INK : PAPER);
        d.setCornerRadius(dp(c, radiusDp));
        if (strokeDp > 0) {
            if (dashed) d.setStroke(dp(c, strokeDp), INK, dp(c, 3), dp(c, 2));
            else d.setStroke(dp(c, strokeDp), INK);
        }
        return d;
    }

    /** A rounded button or filter chip. Filled = black with white text. */
    static TextView pill(Context c, String s, boolean filled, float sp) {
        TextView t = text(c, s, sp);
        t.setGravity(Gravity.CENTER);
        t.setSingleLine(true);
        t.setMinHeight(dp(c, 40));
        t.setPadding(dp(c, 15), dp(c, 6), dp(c, 15), dp(c, 6));
        setPill(c, t, filled);
        t.setClickable(true);
        return t;
    }

    static void setPill(Context c, TextView t, boolean filled) {
        t.setBackground(box(c, filled, 1.5f, 999, false));
        t.setTextColor(filled ? PAPER : INK);
        t.setTypeface(Typeface.DEFAULT, filled ? Typeface.BOLD : Typeface.NORMAL);
    }

    /** A small tag on a card, like "Untried" or "V60". */
    static TextView badge(Context c, String s, boolean filled, boolean dashed) {
        TextView t = text(c, s, 13);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        t.setMaxWidth(dp(c, 110));
        t.setPadding(dp(c, 7), dp(c, 1), dp(c, 7), dp(c, 2));
        t.setBackground(box(c, filled, 1.2f, 999, dashed));
        t.setTextColor(filled ? PAPER : INK);
        return t;
    }

    static EditText field(Context c, String hint) {
        EditText e = new EditText(c);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        e.setSingleLine(true);
        e.setHint(hint);
        e.setTextColor(INK);
        e.setHintTextColor(0xFF555555);
        e.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        e.setBackground(box(c, false, 1.5f, 10, false));
        e.setPadding(dp(c, 12), dp(c, 9), dp(c, 12), dp(c, 9));
        return e;
    }

    static EditText area(Context c, String hint, int lines) {
        EditText e = field(c, hint);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        e.setSingleLine(false);
        e.setMinLines(lines);
        e.setGravity(Gravity.TOP | Gravity.START);
        return e;
    }

    /** A section title like "LIGHT (3)", as in the tea app. */
    static TextView header(Context c, String s) {
        TextView t = bold(c, s.toUpperCase(), 17);
        t.setLetterSpacing(0.06f);
        t.setPadding(0, dp(c, 18), 0, dp(c, 8));
        return t;
    }

    static View rule(Context c) {
        View v = new View(c);
        v.setBackgroundColor(INK);
        v.setLayoutParams(new LinearLayout.LayoutParams(MATCH, Math.max(1, dp(c, 1.5f))));
        return v;
    }

    static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    static LinearLayout column(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    static ScrollView scroller(Context c, View content) {
        ScrollView s = new ScrollView(c);
        s.setOverScrollMode(View.OVER_SCROLL_NEVER);
        s.setFillViewport(true);
        s.addView(content, new ScrollView.LayoutParams(MATCH, WRAP));
        return s;
    }

    static LinearLayout.LayoutParams weight(float w) {
        return new LinearLayout.LayoutParams(0, WRAP, w);
    }

    static LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(WRAP, WRAP);
    }

    static LinearLayout.LayoutParams wide() {
        return new LinearLayout.LayoutParams(MATCH, WRAP);
    }

    static LinearLayout.LayoutParams gapLeft(Context c, LinearLayout.LayoutParams lp, float dp) {
        lp.leftMargin = dp(c, dp);
        return lp;
    }

    static LinearLayout.LayoutParams gapBelow(Context c, LinearLayout.LayoutParams lp, float dp) {
        lp.bottomMargin = dp(c, dp);
        return lp;
    }

    /** The bar at the top of a screen: a button on the left, a title, a button on the right. */
    static LinearLayout topBar(Context c, View left, String title, View right) {
        LinearLayout bar = row(c);
        bar.setPadding(dp(c, 14), dp(c, 8), dp(c, 14), dp(c, 8));
        bar.addView(left, wrap());
        TextView t = oneLine(bold(c, title, 19));
        t.setGravity(Gravity.CENTER);
        bar.addView(t, weight(1));
        if (right != null) bar.addView(right, wrap());
        return bar;
    }
}

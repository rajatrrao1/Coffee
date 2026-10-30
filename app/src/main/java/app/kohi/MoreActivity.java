package app.kohi;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Totals (how much coffee you buy, how long a bag lasts) and where the backup copy goes. */
public class MoreActivity extends Activity {

    private static final int REQ_FOLDER = 1;
    private static final int REQ_OPEN = 2;

    @Override
    protected void onResume() {
        super.onResume();
        build();
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
    }

    private void build() {
        int pad = Ui.dp(this, 16);
        LinearLayout root = Ui.column(this);
        root.setBackgroundColor(Ui.PAPER);
        TextView back = Ui.pill(this, "‹ Back", false, 16);
        back.setOnClickListener(v -> finish());
        root.addView(Ui.topBar(this, back, "More", null), Ui.wide());
        root.addView(Ui.rule(this));

        LinearLayout body = Ui.column(this);
        body.setPadding(pad, Ui.dp(this, 4), pad, Ui.dp(this, 28));
        totals(body);
        backup(body);
        root.addView(Ui.scroller(this, body), new LinearLayout.LayoutParams(Ui.MATCH, 0, 1f));
        setContentView(root);
    }

    // ---- totals ---------------------------------------------------------------------------

    private void totals(LinearLayout body) {
        body.addView(Ui.header(this, "Your coffee"), Ui.wide());
        List<Bean> all = Store.all(this);
        int year = LocalDate.now().getYear();

        int bagsYear = 0, bagsAll = 0;
        double gramsYear = 0, gramsAll = 0, moneyYear = 0, moneyAll = 0;
        long daysFinished = 0, bagsFinished = 0;
        double gramsMeasured = 0;
        long daysMeasured = 0;
        for (Bean b : all) {
            boolean thisYear = Bean.date(b.added).getYear() == year;
            bagsAll++;
            gramsAll += b.grams();
            moneyAll += b.money();
            if (thisYear) {
                bagsYear++;
                gramsYear += b.grams();
                moneyYear += b.money();
            }
            if (b.finishedOn > 0 && b.opened > 0) {
                long d = b.days();
                daysFinished += d;
                bagsFinished++;
                if (b.grams() > 0) {
                    gramsMeasured += b.grams();
                    daysMeasured += d;
                }
            }
        }

        if (bagsAll == 0) {
            body.addView(Ui.italic(this, "Nothing yet. Each coffee you add counts as one bag "
                    + "bought on the day you add it.", 16), Ui.wide());
            return;
        }
        line(body, "This year", summary(bagsYear, gramsYear, moneyYear));
        line(body, "All time", summary(bagsAll, gramsAll, moneyAll));
        if (bagsFinished > 0) {
            line(body, "A bag lasts", Math.round((double) daysFinished / bagsFinished) + " days on average");
        }
        if (daysMeasured > 0) {
            line(body, "You drink", "about " + Math.round(gramsMeasured / daysMeasured) + " g a day");
        }
        if (bagsFinished == 0) {
            TextView hint = Ui.italic(this, "Add the bag size, and press Opened the bag today and "
                    + "Mark the bag as finished on a coffee, to see how long a bag lasts.", 15);
            hint.setPadding(0, Ui.dp(this, 6), 0, 0);
            body.addView(hint, Ui.wide());
        }
    }

    private static String summary(int bags, double grams, double money) {
        String g = grams <= 0 ? "" : grams < 1000 ? Math.round(grams) + " g"
                : String.format(Locale.getDefault(), "%.2f kg", grams / 1000);
        String m = money <= 0 ? "" : String.format(Locale.getDefault(), "€ %.2f", money);
        return Bean.joinDots(bags + (bags == 1 ? " bag" : " bags"), g, m);
    }

    // ---- backup ---------------------------------------------------------------------------

    private void backup(LinearLayout body) {
        body.addView(Ui.header(this, "Backup"), Ui.wide());
        body.addView(Ui.text(this, "Coffee keeps a copy of all your beans as "
                + Backup.FILE_NAME + " in a folder you choose. The copy stays there even if "
                + "the app is removed, and Bring beans in from a file reads it back.", 16), Ui.wide());

        Uri folder = Backup.folder(this);
        LinearLayout box = Ui.column(this);
        box.setPadding(0, Ui.dp(this, 12), 0, 0);
        line(box, "Folder", folder == null ? "Not chosen yet" : Backup.folderName(this));
        if (folder != null) {
            long last = Backup.lastSaved(this);
            String error = Backup.lastError(this);
            if (!error.isEmpty()) line(box, "Last try", "Failed: " + error);
            else if (last > 0) line(box, "Last saved", new SimpleDateFormat("d MMM yyyy, HH:mm",
                    Locale.getDefault()).format(new Date(last)));
        }
        body.addView(box, Ui.wide());

        LinearLayout actions = Ui.column(this);
        actions.setPadding(0, Ui.dp(this, 10), 0, 0);
        button(actions, folder == null ? "Choose a folder" : "Choose another folder", folder == null, this::chooseFolder);
        if (folder != null) {
            boolean auto = Backup.auto(this);
            TextView autoPill = Ui.pill(this, auto ? "Update after every change: on" : "Update after every change: off", auto, 16);
            autoPill.setOnClickListener(v -> {
                Backup.setAuto(this, !Backup.auto(this));
                build();
            });
            actions.addView(autoPill, Ui.gapBelow(this, Ui.wide(), 10));
            button(actions, "Save the copy now", false, this::saveNow);
        }
        button(actions, "Bring beans in from a file", false, this::openFile);
        button(actions, "Send the list as text", false, this::sendText);
        if (folder != null) {
            button(actions, "Stop saving a copy", false, () -> {
                Backup.forgetFolder(this);
                build();
            });
        }
        body.addView(actions, Ui.wide());
    }

    private void button(LinearLayout parent, String label, boolean filled, Runnable action) {
        TextView t = Ui.pill(this, label, filled, 16);
        t.setOnClickListener(v -> action.run());
        parent.addView(t, Ui.gapBelow(this, Ui.wide(), 10));
    }

    private void chooseFolder() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try {
            startActivityForResult(i, REQ_FOLDER);
        } catch (ActivityNotFoundException e) {
            toast("This phone has no folder chooser.");
        }
    }

    private void saveNow() {
        toast("Saving…");
        new Thread(() -> {
            String msg;
            try {
                Backup.saveNow(this);
                msg = "Saved " + Backup.FILE_NAME + " in " + Backup.folderName(this) + ".";
            } catch (Exception e) {
                msg = "Could not save: " + e.getMessage();
            }
            final String m = msg;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                toast(m);
                build();
            });
        }, "backup").start();
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
        if (request == REQ_FOLDER) {
            try {
                Backup.setFolder(this, uri);
                saveNow();
            } catch (SecurityException e) {
                toast("The app was not allowed to use that folder. Try another one.");
            }
        } else if (request == REQ_OPEN) {
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                if (in == null) throw new java.io.IOException("no file");
                int n = Store.importJson(this, Store.readAll(in));
                toast("Brought in " + n + (n == 1 ? " bean." : " beans."));
            } catch (Exception e) {
                toast("That file is not a Coffee backup.");
            }
        }
        build();
    }

    private void line(LinearLayout body, String label, String value) {
        LinearLayout row = Ui.row(this);
        row.setGravity(Gravity.TOP);
        row.addView(Ui.text(this, label, 16), new LinearLayout.LayoutParams(Ui.dp(this, 118), Ui.WRAP));
        row.addView(Ui.bold(this, value, 17), Ui.weight(1));
        body.addView(row, Ui.gapBelow(this, Ui.wide(), 6));
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }
}

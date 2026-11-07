package app.kohi;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Brews logged in the Two Tone launcher. Two Tone writes them to coffee-brews.json in the
 * backup folder; this app only reads that file, never writes it. Each brew joins the brew log
 * of its coffee (matched by beanId, else by beanName) with the id "tt-" + its Two Tone id, so
 * it is never added twice. A brew changed in Two Tone is updated here unless you changed it
 * here; a brew you delete here stays deleted.
 */
final class TwoTone {

    static final String FILE_NAME = "coffee-brews.json";
    static final String SOURCE = "Two Tone";
    private static final String ID_PREFIX = "tt-";

    private static final String PREFS = "twotone";
    private static final String KEY_HASH = "hash";
    private static final String KEY_STATUS = "status";
    private static final String KEY_WHEN = "when";
    private static final String KEY_DELETED = "deleted";

    private TwoTone() {}

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /**
     * Reads Two Tone's file in the background and merges it on the main thread. Skips the work
     * when the file has not changed since the last time, unless force is set. Calls onChanged
     * when any brew was added or updated.
     */
    static void sync(Activity a, boolean force, Runnable onChanged) {
        Context app = a.getApplicationContext();
        Uri tree = Backup.folder(app);
        if (tree == null) return;
        new Thread(() -> {
            String text = null;
            String problem = null;
            try {
                Uri file = Backup.find(app.getContentResolver(), tree, FILE_NAME);
                if (file == null) {
                    problem = "No " + FILE_NAME + " in the backup folder yet.";
                } else {
                    try (InputStream in = app.getContentResolver().openInputStream(file)) {
                        if (in != null) text = Store.readAll(in);
                    }
                }
            } catch (Exception e) {
                problem = "Could not read " + FILE_NAME + ": " + e.getMessage();
            }
            final String json = text;
            final String trouble = problem;
            a.runOnUiThread(() -> {
                if (trouble != null) {
                    if (force) {
                        note(app, trouble);
                        if (onChanged != null && !a.isFinishing() && !a.isDestroyed()) onChanged.run();
                    }
                    return;
                }
                if (json == null) return;
                String hash = json.length() + ":" + json.hashCode();
                if (!force && hash.equals(prefs(app).getString(KEY_HASH, ""))) return;
                int changed = merge(app, json, hash);
                if ((changed > 0 || force) && onChanged != null && !a.isFinishing() && !a.isDestroyed()) onChanged.run();
            });
        }, "two-tone").start();
    }

    /** Merges the file's brews into the coffees. Returns how many brews were added or updated. */
    static int merge(Context c, String json, String hash) {
        JSONArray list;
        try {
            String t = json.trim();
            list = t.startsWith("[") ? new JSONArray(t) : new JSONObject(t).optJSONArray("brews");
        } catch (JSONException e) {
            note(c, FILE_NAME + " could not be read as a brew list.");
            return 0;
        }
        if (list == null) list = new JSONArray();

        Set<String> deleted = new HashSet<>(prefs(c).getStringSet(KEY_DELETED, new HashSet<>()));
        Set<Bean> touched = new LinkedHashSet<>();
        int added = 0, updated = 0, unmatched = 0;
        for (int i = 0; i < list.length(); i++) {
            JSONObject o = list.optJSONObject(i);
            if (o == null) continue;
            Object rawId = o.opt("id");
            if (rawId == null) continue;
            String sourceId = String.valueOf(rawId);
            String id = ID_PREFIX + sourceId;
            if (deleted.contains(id)) continue;

            Bean bean = beanFor(c, o);
            if (bean == null) {
                unmatched++;
                continue;
            }
            String stamp = o.toString();
            Brew existing = bean.findBrew(id);
            if (existing != null && (existing.editedHere || stamp.equals(existing.sourceStamp))) continue;

            Brew b = fromLauncher(o);
            b.id = id;
            b.source = SOURCE;
            b.sourceId = sourceId;
            b.sourceStamp = stamp;
            bean.putBrew(b);
            touched.add(bean);
            if (existing == null) added++;
            else updated++;
        }

        if (!touched.isEmpty()) {
            long now = System.currentTimeMillis();
            for (Bean b : touched) b.updated = now;
            Store.saveAll(c);
        }

        StringBuilder msg = new StringBuilder();
        if (added + updated == 0) msg.append("Nothing new from Two Tone.");
        else {
            msg.append("Brought in ").append(added).append(added == 1 ? " new brew" : " new brews");
            if (updated > 0) msg.append(" and ").append(updated).append(" changed");
            msg.append(" from Two Tone.");
        }
        if (unmatched > 0) {
            msg.append(' ').append(unmatched).append(unmatched == 1 ? " brew does" : " brews do")
                    .append(" not match a coffee here yet.");
        }
        note(c, msg.toString());
        // With unmatched brews, read the file again next time: their coffee may have been added since.
        prefs(c).edit().putString(KEY_HASH, unmatched > 0 ? "" : hash).apply();
        return added + updated;
    }

    /** Remembers that you deleted a Two Tone brew here, so it is not brought in again. */
    static void deleted(Context c, Brew b) {
        if (b.source.isEmpty()) return;
        Set<String> ids = new HashSet<>(prefs(c).getStringSet(KEY_DELETED, new HashSet<>()));
        ids.add(b.id);
        prefs(c).edit().putStringSet(KEY_DELETED, ids).apply();
    }

    static String status(Context c) {
        return prefs(c).getString(KEY_STATUS, "");
    }

    static long lastRead(Context c) {
        return prefs(c).getLong(KEY_WHEN, 0);
    }

    private static void note(Context c, String s) {
        prefs(c).edit().putString(KEY_STATUS, s).putLong(KEY_WHEN, System.currentTimeMillis()).apply();
    }

    // ---- one Two Tone brew ---------------------------------------------------------------

    private static Bean beanFor(Context c, JSONObject o) {
        Bean b = Store.find(c, o.optString("beanId", ""));
        if (b != null) return b;
        String name = o.optString("beanName", "").trim();
        if (name.isEmpty()) return null;
        Bean finishedMatch = null;
        for (Bean x : Store.all(c)) {
            if (!x.name.trim().equalsIgnoreCase(name)) continue;
            if (!x.finished) return x;          // prefer the bag that is still open
            if (finishedMatch == null) finishedMatch = x;
        }
        return finishedMatch;
    }

    static Brew fromLauncher(JSONObject o) {
        Brew b = new Brew();
        String raw = o.optString("method", "").trim();
        b.method = Methods.kindOf(raw);
        // "v60" or "chemex" names the dripper; "espresso" or "aeropress" just names the method.
        boolean isDripper = b.method.equals(Methods.POUR_OVER.name) || b.method.equals(Methods.OTHER.name);
        if (isDripper && !raw.isEmpty() && !raw.equalsIgnoreCase(b.method)) b.brewer = niceName(raw);
        b.temp = plain(o.optString("temp", ""));
        b.grind = plain(o.optString("grind", ""));
        b.dose = plain(o.optString("dose", ""));
        b.water = plain(o.optString("water", ""));
        b.rating = Math.max(0, Math.min(5, o.optInt("rating", 0)));
        b.notes = o.optString("note", o.optString("notes", "")).trim();

        long time = o.optLong("time", 0);
        LocalDate day = null;
        try {
            String d = o.optString("date", "");
            if (!d.isEmpty()) day = LocalDate.parse(d.length() > 10 ? d.substring(0, 10) : d);
        } catch (RuntimeException ignored) {
            day = null;
        }
        ZoneId zone = ZoneId.systemDefault();
        if (time > 0 && (day == null || Bean.date(time).equals(day))) {
            b.at = time;
        } else if (time > 0) {
            // The day says one thing, the clock another: keep the day, at the clock's time of day.
            LocalTime clock = Instant.ofEpochMilli(time).atZone(zone).toLocalTime();
            b.at = day.atTime(clock).atZone(zone).toInstant().toEpochMilli();
        } else if (day != null) {
            b.at = Bean.noon(day);
        }
        return b;
    }

    /** "93.0" becomes "93"; text stays as it is. */
    private static String plain(String s) {
        String t = s == null ? "" : s.trim();
        return t.matches("-?\\d+\\.0+") ? t.substring(0, t.indexOf('.')) : t;
    }

    /** "v60" becomes "V60", "kalita wave" becomes "Kalita Wave". */
    private static String niceName(String raw) {
        String low = raw.toLowerCase(Locale.ROOT);
        if (low.equals("v60")) return "V60";
        StringBuilder out = new StringBuilder();
        for (String w : raw.replace('_', ' ').replace('-', ' ').split("\\s+")) {
            if (w.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return out.toString();
    }
}

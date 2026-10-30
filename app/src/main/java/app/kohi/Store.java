package app.kohi;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** All beans, kept as one JSON file in the app's own storage. */
public final class Store {

    private static final String FILE = "beans.json";
    private static List<Bean> cache;

    private Store() {}

    public static synchronized List<Bean> all(Context c) {
        if (cache == null) cache = load(c);
        return cache;
    }

    public static synchronized Bean find(Context c, String id) {
        if (id == null) return null;
        for (Bean b : all(c)) if (b.id.equals(id)) return b;
        return null;
    }

    public static synchronized void save(Context c, Bean bean) {
        List<Bean> list = all(c);
        bean.updated = System.currentTimeMillis();
        boolean replaced = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(bean.id)) {
                list.set(i, bean);
                replaced = true;
                break;
            }
        }
        if (!replaced) list.add(bean);
        write(c);
    }

    public static synchronized void delete(Context c, String id) {
        List<Bean> list = all(c);
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).id.equals(id)) list.remove(i);
        }
        write(c);
    }

    public static synchronized int originCount(Context c) {
        Set<String> seen = new HashSet<>();
        for (Bean b : all(c)) {
            String o = b.origin.trim().toLowerCase(Locale.ROOT);
            if (!o.isEmpty()) seen.add(o);
        }
        return seen.size();
    }

    // ---- backup ---------------------------------------------------------------------------

    public static synchronized String exportJson(Context c) throws JSONException {
        JSONArray arr = new JSONArray();
        for (Bean b : all(c)) arr.put(b.toJson());
        JSONObject root = new JSONObject();
        root.put("app", "coffee");
        root.put("version", 1);
        root.put("beans", arr);
        return root.toString(2);
    }

    /** Brings beans in from a backup. A bean already here (same id) is replaced. */
    public static synchronized int importJson(Context c, String json) throws JSONException {
        String t = json.trim();
        JSONArray arr = t.startsWith("[") ? new JSONArray(t) : new JSONObject(t).getJSONArray("beans");
        int n = 0;
        List<Bean> list = all(c);
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            Bean b = Bean.fromJson(o);
            boolean replaced = false;
            for (int j = 0; j < list.size(); j++) {
                if (list.get(j).id.equals(b.id)) {
                    list.set(j, b);
                    replaced = true;
                    break;
                }
            }
            if (!replaced) list.add(b);
            n++;
        }
        write(c);
        return n;
    }

    /** A plain list, for sending to a notes app or a message. */
    public static synchronized String asText(Context c) {
        StringBuilder s = new StringBuilder();
        for (Bean b : all(c)) {
            s.append(b.name).append('\n');
            String sub = b.subtitle();
            if (!sub.isEmpty()) s.append(sub).append('\n');
            if (!b.notes.isEmpty()) s.append(b.notes).append('\n');
            String brew = Bean.joinDots(b.method, b.tempLabel(), b.grind, Bean.grams(b.dose),
                    b.ratio(), b.time);
            if (!brew.isEmpty()) s.append("Brew: ").append(brew).append('\n');
            if (!b.myNotes.isEmpty()) s.append(b.myNotes).append('\n');
            if (!b.url.isEmpty()) s.append(b.url).append('\n');
            s.append('\n');
        }
        return s.toString().trim();
    }

    public static String day(long millis) {
        return new SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(new Date(millis));
    }

    // ---- file -----------------------------------------------------------------------------

    private static List<Bean> load(Context c) {
        List<Bean> list = new ArrayList<>();
        File f = new File(c.getFilesDir(), FILE);
        if (!f.exists()) return list;
        try (InputStream in = new FileInputStream(f)) {
            String text = readAll(in);
            JSONArray arr = new JSONArray(text);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o != null) list.add(Bean.fromJson(o));
            }
        } catch (IOException | JSONException e) {
            // A damaged file: start empty rather than crash. The old file stays on disk
            // until the next save, under a different name so it is not lost.
            File broken = new File(c.getFilesDir(), FILE + ".broken");
            //noinspection ResultOfMethodCallIgnored
            f.renameTo(broken);
        }
        return list;
    }

    private static void write(Context c) {
        File dir = c.getFilesDir();
        File tmp = new File(dir, FILE + ".tmp");
        File f = new File(dir, FILE);
        try {
            JSONArray arr = new JSONArray();
            for (Bean b : cache) arr.put(b.toJson());
            try (FileOutputStream out = new FileOutputStream(tmp)) {
                out.write(arr.toString().getBytes(StandardCharsets.UTF_8));
                out.getFD().sync();
            }
            if (!tmp.renameTo(f)) throw new IOException("could not replace " + FILE);
        } catch (IOException | JSONException e) {
            throw new RuntimeException("Could not save the beans: " + e.getMessage(), e);
        }
    }

    public static String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] chunk = new byte[16384];
        int n;
        while ((n = in.read(chunk)) != -1) buf.write(chunk, 0, n);
        return new String(buf.toByteArray(), StandardCharsets.UTF_8);
    }
}

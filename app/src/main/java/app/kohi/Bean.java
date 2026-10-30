package app.kohi;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** One coffee: what the roaster says about it, and how you brew it. */
public final class Bean {

    public String id = UUID.randomUUID().toString();

    // The bean, mostly read from the roaster's page.
    public String name = "";
    public String roaster = "";
    public String url = "";
    public String origin = "";
    public String region = "";
    public String altitude = "";
    public String process = "";
    public String variety = "";
    public String producer = "";
    public String roast = "";      // "Light", "Medium", "Dark" or ""
    public String notes = "";      // the roaster's tasting notes

    // Your brew.
    public String method = "";
    public String temp = "";
    public String grind = "";
    public String dose = "";
    public String water = "";
    public String time = "";
    public String myNotes = "";
    public int rating = 0;         // 0 = not rated, else 1..5
    public boolean finished = false;

    public long added = System.currentTimeMillis();
    public long updated = added;

    /** No brew written down yet. */
    public boolean untried() {
        return temp.trim().isEmpty() && grind.trim().isEmpty() && method.trim().isEmpty();
    }

    /** "93" becomes "93 °C"; anything already carrying a unit is left alone. */
    public String tempLabel() {
        String t = temp.trim();
        if (t.isEmpty()) return "";
        if (t.matches("[0-9.,\\s–-]+")) return t + " °C";
        return t;
    }

    public static String grams(String s) {
        String t = s.trim();
        if (t.isEmpty()) return "";
        if (t.matches("[0-9.,\\s]+")) return t + " g";
        return t;
    }

    /** 15 g of coffee to 250 g of water gives "1:16.7". */
    public String ratio() {
        double d = firstNumber(dose), w = firstNumber(water);
        if (d <= 0 || w <= 0) return "";
        double r = Math.round(w / d * 10.0) / 10.0;
        String s = (r == Math.floor(r)) ? String.valueOf((long) r) : String.valueOf(r);
        return "1:" + s;
    }

    private static final Pattern NUMBER = Pattern.compile("(\\d+(?:[.,]\\d+)?)");

    static double firstNumber(String s) {
        Matcher m = NUMBER.matcher(s == null ? "" : s);
        if (!m.find()) return 0;
        try {
            return Double.parseDouble(m.group(1).replace(',', '.'));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public String stars() {
        if (rating <= 0) return "";
        StringBuilder b = new StringBuilder();
        for (int i = 1; i <= 5; i++) b.append(i <= rating ? '★' : '☆');
        return b.toString();
    }

    /** Origin · altitude · roaster, leaving out what is missing. */
    public String subtitle() {
        return joinDots(origin, altitude, roaster);
    }

    static String joinDots(String... parts) {
        List<String> keep = new ArrayList<>();
        for (String p : parts) if (p != null && !p.trim().isEmpty()) keep.add(p.trim());
        return String.join(" · ", keep);
    }

    /** Every word of the query appears somewhere in the bean. */
    public boolean matches(String query) {
        String hay = (name + " " + roaster + " " + origin + " " + region + " " + altitude + " "
                + process + " " + variety + " " + producer + " " + roast + " " + notes + " "
                + method + " " + grind + " " + myNotes).toLowerCase(Locale.ROOT);
        for (String word : query.toLowerCase(Locale.ROOT).trim().split("\\s+")) {
            if (!word.isEmpty() && !hay.contains(word)) return false;
        }
        return true;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("name", name);
        o.put("roaster", roaster);
        o.put("url", url);
        o.put("origin", origin);
        o.put("region", region);
        o.put("altitude", altitude);
        o.put("process", process);
        o.put("variety", variety);
        o.put("producer", producer);
        o.put("roast", roast);
        o.put("notes", notes);
        o.put("method", method);
        o.put("temp", temp);
        o.put("grind", grind);
        o.put("dose", dose);
        o.put("water", water);
        o.put("time", time);
        o.put("myNotes", myNotes);
        o.put("rating", rating);
        o.put("finished", finished);
        o.put("added", added);
        o.put("updated", updated);
        return o;
    }

    public static Bean fromJson(JSONObject o) {
        Bean b = new Bean();
        b.id = o.optString("id", b.id);
        if (b.id.isEmpty()) b.id = UUID.randomUUID().toString();
        b.name = o.optString("name", "");
        b.roaster = o.optString("roaster", "");
        b.url = o.optString("url", "");
        b.origin = o.optString("origin", "");
        b.region = o.optString("region", "");
        b.altitude = o.optString("altitude", "");
        b.process = o.optString("process", "");
        b.variety = o.optString("variety", "");
        b.producer = o.optString("producer", "");
        b.roast = o.optString("roast", "");
        b.notes = o.optString("notes", "");
        b.method = o.optString("method", "");
        b.temp = o.optString("temp", "");
        b.grind = o.optString("grind", "");
        b.dose = o.optString("dose", "");
        b.water = o.optString("water", "");
        b.time = o.optString("time", "");
        b.myNotes = o.optString("myNotes", "");
        b.rating = Math.max(0, Math.min(5, o.optInt("rating", 0)));
        b.finished = o.optBoolean("finished", false);
        b.added = o.optLong("added", b.added);
        b.updated = o.optLong("updated", b.added);
        return b;
    }
}

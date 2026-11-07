package app.kohi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    public String method = "";     // one of Methods.ALL by name, or ""
    public String brewer = "";     // the machine, dripper or pot: "V60", "Gaggia Classic"
    public Map<String, String> extras = new LinkedHashMap<>(); // the method's own fields
    public String temp = "";
    public String grind = "";
    public String dose = "";
    public String water = "";
    public String time = "";
    public String myNotes = "";
    public int rating = 0;         // 0 = not rated, else 1..5
    public boolean finished = false; // kept in step with finishedOn

    // The bag.
    public String bag = "";        // size in grams, as typed: "250"
    public String price = "";      // as typed: "10,90"
    public long opened = 0;        // day the bag was opened, 0 = not yet
    public long finishedOn = 0;    // day it ran out, 0 = still going

    // Every brew you logged of this bag, newest first.
    public List<Brew> brews = new ArrayList<>();

    public long added = System.currentTimeMillis();
    public long updated = added;

    /** No brew written down yet. */
    public boolean untried() {
        return temp.trim().isEmpty() && grind.trim().isEmpty() && method.trim().isEmpty()
                && dose.trim().isEmpty() && brews.isEmpty();
    }

    /** "93" becomes "93 °C"; anything already carrying a unit is left alone. */
    public String tempLabel() {
        return tempOf(temp);
    }

    static String tempOf(String temp) {
        String t = temp == null ? "" : temp.trim();
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
        return ratioOf(dose, water);
    }

    static String ratioOf(String dose, String water) {
        double d = firstNumber(dose), w = firstNumber(water);
        if (d <= 0 || w <= 0) return "";
        return "1:" + Methods.number(w / d);
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

    // ---- the bag ----------------------------------------------------------------------------

    public void setFinished(long day) {
        finishedOn = day;
        finished = day > 0;
    }

    static LocalDate date(long millis) {
        return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /** Noon of today, so a date never slips a day when the clocks change. */
    public static long today() {
        return noon(LocalDate.now());
    }

    public static long noon(LocalDate d) {
        return d.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /** Days from opening to finishing (or to today while open), at least 1; 0 if never opened. */
    public long days() {
        if (opened <= 0) return 0;
        LocalDate end = finishedOn > 0 ? date(finishedOn) : LocalDate.now();
        return Math.max(1, ChronoUnit.DAYS.between(date(opened), end));
    }

    public double grams() {
        return firstNumber(bag);
    }

    public double money() {
        return firstNumber(price);
    }

    /** "18 days · about 14 g a day" once finished, or "open 5 days" while it lasts. */
    public String lasted() {
        long d = days();
        if (d == 0) return "";
        String span = d + (d == 1 ? " day" : " days");
        if (finishedOn <= 0) return span + " so far";
        double g = grams();
        if (g <= 0) return span;
        return span + " · about " + Math.round(g / d) + " g a day";
    }

    public String bagLabel() {
        String p = price.trim();
        if (!p.isEmpty() && p.matches("[0-9.,\\s]+")) p = "€ " + p;
        return joinDots(grams(bag), p);
    }

    /** A fresh bag of the same coffee: the bean and your brew, without the dates. */
    public Bean anotherBag() {
        Bean b = new Bean();
        b.name = name; b.roaster = roaster; b.url = url; b.origin = origin; b.region = region;
        b.altitude = altitude; b.process = process; b.variety = variety; b.producer = producer;
        b.roast = roast; b.notes = notes; b.method = method; b.brewer = brewer;
        b.extras = new LinkedHashMap<>(extras); b.temp = temp; b.grind = grind;
        b.dose = dose; b.water = water; b.time = time; b.myNotes = myNotes; b.rating = rating;
        b.bag = bag; b.price = price;
        return b;
    }

    public String stars() {
        return starsOf(rating);
    }

    static String starsOf(int rating) {
        if (rating <= 0) return "";
        StringBuilder b = new StringBuilder();
        for (int i = 1; i <= 5; i++) b.append(i <= rating ? '★' : '☆');
        return b.toString();
    }

    // ---- recipe and brews -----------------------------------------------------------------

    /** Your usual way of brewing this coffee, as a Brew. */
    public Brew recipe() {
        Brew r = new Brew();
        r.method = method;
        r.brewer = brewer;
        r.temp = temp;
        r.grind = grind;
        r.dose = dose;
        r.water = water;
        r.time = time;
        r.extras = new LinkedHashMap<>(extras);
        return r;
    }

    public void setRecipe(Brew r) {
        method = r.method;
        brewer = r.brewer;
        temp = r.temp;
        grind = r.grind;
        dose = r.dose;
        water = r.water;
        time = r.time;
        extras = new LinkedHashMap<>(r.extras);
    }

    public Brew findBrew(String brewId) {
        if (brewId == null) return null;
        for (Brew b : brews) if (b.id.equals(brewId)) return b;
        return null;
    }

    /** Adds or replaces a brew, keeps the newest first, and dates the bag's opening if it had none. */
    public void putBrew(Brew brew) {
        boolean replaced = false;
        for (int i = 0; i < brews.size(); i++) {
            if (brews.get(i).id.equals(brew.id)) {
                brews.set(i, brew);
                replaced = true;
                break;
            }
        }
        if (!replaced) brews.add(brew);
        brews.sort((a, b) -> Long.compare(b.at, a.at));
        // Your first brew is the day the bag was opened, unless you already set that day.
        if (opened <= 0) opened = noon(date(brews.get(brews.size() - 1).at));
    }

    public void removeBrew(String brewId) {
        brews.removeIf(b -> b.id.equals(brewId));
    }

    /** Where a new brew starts: your last brew of this bag, or else your usual recipe. */
    public Brew startingPoint() {
        return brews.isEmpty() ? recipe() : brews.get(0).copy();
    }

    /** Average rating of the rated brews, or 0. */
    public double averageRating() {
        int n = 0, sum = 0;
        for (Brew b : brews) {
            if (b.rating > 0) {
                n++;
                sum += b.rating;
            }
        }
        return n == 0 ? 0 : (double) sum / n;
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
                + method + " " + brewer + " " + grind + " " + myNotes + " " + extras.values())
                .toLowerCase(Locale.ROOT);
        StringBuilder brewText = new StringBuilder();
        for (Brew b : brews) brewText.append(' ').append(b.notes);
        hay = hay + brewText.toString().toLowerCase(Locale.ROOT);
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
        o.put("brewer", brewer);
        JSONObject ex = new JSONObject();
        for (Map.Entry<String, String> e : extras.entrySet()) ex.put(e.getKey(), e.getValue());
        o.put("extras", ex);
        o.put("temp", temp);
        o.put("grind", grind);
        o.put("dose", dose);
        o.put("water", water);
        o.put("time", time);
        o.put("myNotes", myNotes);
        o.put("rating", rating);
        o.put("finished", finished);
        o.put("bag", bag);
        o.put("price", price);
        o.put("opened", opened);
        o.put("finishedOn", finishedOn);
        JSONArray log = new JSONArray();
        for (Brew b : brews) log.put(b.toJson());
        o.put("brews", log);
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
        String rawMethod = o.optString("method", "");
        b.brewer = o.optString("brewer", "");
        b.method = Methods.kindOf(rawMethod);
        // Version 1 kept the dripper as the method ("V60"): it becomes Pour-over with V60 as brewer.
        if (b.brewer.isEmpty() && !rawMethod.trim().isEmpty() && !rawMethod.trim().equalsIgnoreCase(b.method)) {
            b.brewer = rawMethod.trim();
        }
        JSONObject ex = o.optJSONObject("extras");
        if (ex != null) {
            Iterator<String> keys = ex.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                String v = ex.optString(k, "");
                if (!v.isEmpty()) b.extras.put(k, v);
            }
        }
        b.temp = o.optString("temp", "");
        b.grind = o.optString("grind", "");
        b.dose = o.optString("dose", "");
        b.water = o.optString("water", "");
        b.time = o.optString("time", "");
        b.myNotes = o.optString("myNotes", "");
        b.rating = Math.max(0, Math.min(5, o.optInt("rating", 0)));
        b.added = o.optLong("added", b.added);
        b.updated = o.optLong("updated", b.added);
        b.bag = o.optString("bag", "");
        b.price = o.optString("price", "");
        b.opened = o.optLong("opened", 0);
        long done = o.optLong("finishedOn", 0);
        // Bags marked finished before there were dates: take the day they were last changed.
        if (done == 0 && o.optBoolean("finished", false)) done = b.updated;
        b.setFinished(done);
        JSONArray log = o.optJSONArray("brews");
        if (log != null) {
            for (int i = 0; i < log.length(); i++) {
                JSONObject one = log.optJSONObject(i);
                if (one != null) b.brews.add(Brew.fromJson(one));
            }
            b.brews.sort((x, y) -> Long.compare(y.at, x.at));
        }
        return b;
    }
}

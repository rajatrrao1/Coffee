package app.kohi;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * How a coffee was brewed. A coffee's usual recipe is one of these (without date or rating),
 * and every brew you log is another, with its date, its rating and what you noticed.
 */
public final class Brew {

    public String id = UUID.randomUUID().toString();
    public long at = System.currentTimeMillis();   // when it was brewed

    public String method = "";
    public String brewer = "";
    public String temp = "";
    public String grind = "";
    public String dose = "";
    public String water = "";
    public String time = "";
    public Map<String, String> extras = new LinkedHashMap<>();

    public int rating = 0;      // 0 = not rated, else 1..5
    public String notes = "";

    // Brews that came from another app (Two Tone): where from, its own id there, the exact
    // entry as last read (to notice changes made there), and whether you changed it here.
    public String source = "";
    public String sourceId = "";
    public String sourceStamp = "";
    public boolean editedHere = false;

    public Brew copy() {
        Brew b = new Brew();
        b.method = method;
        b.brewer = brewer;
        b.temp = temp;
        b.grind = grind;
        b.dose = dose;
        b.water = water;
        b.time = time;
        b.extras = new LinkedHashMap<>(extras);
        return b;
    }

    public boolean empty() {
        return method.isEmpty() && brewer.isEmpty() && temp.isEmpty() && grind.isEmpty()
                && dose.isEmpty() && water.isEmpty() && time.isEmpty() && extras.isEmpty();
    }

    public String ratio() {
        return Bean.ratioOf(dose, water);
    }

    public String tempLabel() {
        return Bean.tempOf(temp);
    }

    public String stars() {
        return Bean.starsOf(rating);
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("at", at);
        o.put("method", method);
        o.put("brewer", brewer);
        o.put("temp", temp);
        o.put("grind", grind);
        o.put("dose", dose);
        o.put("water", water);
        o.put("time", time);
        JSONObject ex = new JSONObject();
        for (Map.Entry<String, String> e : extras.entrySet()) ex.put(e.getKey(), e.getValue());
        o.put("extras", ex);
        o.put("rating", rating);
        o.put("notes", notes);
        if (!source.isEmpty()) {
            o.put("source", source);
            o.put("sourceId", sourceId);
            o.put("sourceStamp", sourceStamp);
            o.put("editedHere", editedHere);
        }
        return o;
    }

    public static Brew fromJson(JSONObject o) {
        Brew b = new Brew();
        b.id = o.optString("id", b.id);
        if (b.id.isEmpty()) b.id = UUID.randomUUID().toString();
        b.at = o.optLong("at", b.at);
        b.method = Methods.kindOf(o.optString("method", ""));
        b.brewer = o.optString("brewer", "");
        b.temp = o.optString("temp", "");
        b.grind = o.optString("grind", "");
        b.dose = o.optString("dose", "");
        b.water = o.optString("water", "");
        b.time = o.optString("time", "");
        JSONObject ex = o.optJSONObject("extras");
        if (ex != null) {
            Iterator<String> keys = ex.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                String v = ex.optString(k, "");
                if (!v.isEmpty()) b.extras.put(k, v);
            }
        }
        b.rating = Math.max(0, Math.min(5, o.optInt("rating", 0)));
        b.notes = o.optString("notes", "");
        b.source = o.optString("source", "");
        b.sourceId = o.optString("sourceId", "");
        b.sourceStamp = o.optString("sourceStamp", "");
        b.editedHere = o.optBoolean("editedHere", false);
        return b;
    }
}

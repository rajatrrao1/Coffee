package app.kohi;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The brewing methods and what each one needs written down. Choosing a method in the edit
 * form shows its fields; the values of its extra fields are kept in Bean.extras by id.
 */
final class Methods {

    static final int TEXT = 0, NUMBER = 1, CHOICE = 2;

    static final class Field {
        final String id, label, hint, unit;
        final int kind;
        final String[] choices;

        Field(String id, String label, String hint, String unit, int kind, String... choices) {
            this.id = id;
            this.label = label;
            this.hint = hint;
            this.unit = unit;
            this.kind = kind;
            this.choices = choices;
        }
    }

    static Field number(String id, String label, String hint, String unit) {
        return new Field(id, label, hint, unit, NUMBER);
    }

    static Field text(String id, String label, String hint) {
        return new Field(id, label, hint, "", TEXT);
    }

    static Field choice(String id, String label, String... choices) {
        return new Field(id, label, "", "", CHOICE, choices);
    }

    static final class Method {
        final String name;
        String brewerLabel = "Brewer", brewerHint = "";
        String[] brewers = {};
        boolean hasTemp = false;
        String tempHint = "";
        String doseHint = "15", waterLabel = "Water (g)", waterHint = "250";
        String[] ratios = {};
        String timeLabel = "Brew time", timeHint = "3:00", timeUnit = "";
        String tip = "";
        final List<Field> extras = new ArrayList<>();

        Method(String name) {
            this.name = name;
        }

        Method brewer(String label, String hint, String... chips) {
            brewerLabel = label;
            brewerHint = hint;
            brewers = chips;
            return this;
        }

        Method temp(String hint) {
            hasTemp = true;
            tempHint = hint;
            return this;
        }

        Method amounts(String doseHint, String waterLabel, String waterHint, String... ratios) {
            this.doseHint = doseHint;
            this.waterLabel = waterLabel;
            this.waterHint = waterHint;
            this.ratios = ratios;
            return this;
        }

        Method time(String label, String hint, String unit) {
            timeLabel = label;
            timeHint = hint;
            timeUnit = unit;
            return this;
        }

        Method tip(String t) {
            tip = t;
            return this;
        }

        Method extra(Field f) {
            extras.add(f);
            return this;
        }

        Field field(String id) {
            for (Field f : extras) if (f.id.equals(id)) return f;
            return null;
        }
    }

    static final Method ESPRESSO = new Method("Espresso")
            .brewer("Machine", "e.g. Gaggia Classic")
            .temp("93")
            .amounts("18", "In the cup (g)", "36", "1:1.5", "1:2", "1:2.5", "1:3")
            .time("Shot time (s)", "28", " s")
            .extra(number("preinfusion", "Preinfusion (s)", "5", " s"))
            .extra(number("pressure", "Pressure (bar)", "9", " bar"))
            .extra(text("basket", "Basket", "e.g. 18 g"))
            .extra(choice("drink", "Drink", "Straight", "Americano", "Cortado", "Flat white", "Cappuccino", "Latte"))
            .tip("A common start: 1:2 in about 25–30 s at 90–94 °C. A shorter ratio tastes "
                    + "heavier, a longer one lighter. Sour: grind finer. Bitter: grind coarser.");

    static final Method POUR_OVER = new Method("Pour-over")
            .brewer("Dripper", "e.g. V60 02", "V60", "Kalita", "Chemex", "Origami", "April")
            .temp("94")
            .amounts("15", "Water (g)", "250", "1:15", "1:16", "1:17")
            .time("Total time", "3:00", "")
            .extra(number("bloomWater", "Bloom water (g)", "45", " g"))
            .extra(number("bloomTime", "Bloom time (s)", "40", " s"))
            .extra(number("pours", "Pours", "3", ""))
            .extra(text("filter", "Filter", "e.g. Hario paper"))
            .tip("A common start: 1:16 at 92–96 °C. Bloom with two to three times the coffee's "
                    + "weight in water for 30–45 s, and finish in about 3 minutes (Chemex 4–5).");

    static final Method AEROPRESS = new Method("AeroPress")
            .brewer("Brewer", "e.g. AeroPress Go")
            .temp("85")
            .amounts("15", "Water (g)", "225", "1:12", "1:15", "1:16")
            .time("Steep time", "1:30", "")
            .extra(choice("orientation", "Way up", "Standard", "Inverted"))
            .extra(number("stirs", "Stirs", "10", ""))
            .extra(number("press", "Press time (s)", "30", " s"))
            .extra(number("bypass", "Water added after (g)", "0", " g"))
            .extra(choice("filter", "Filter", "Paper", "Metal"))
            .tip("A common start: 1:15 at 80–90 °C, steep 1–2 minutes, then press gently for "
                    + "about 30 s. A strong brew topped up with water afterwards also works well.");

    static final Method FRENCH_PRESS = new Method("French press")
            .brewer("Press", "e.g. Bodum Chambord")
            .temp("95")
            .amounts("30", "Water (g)", "500", "1:12", "1:15", "1:16")
            .time("Steep time", "4:00", "")
            .extra(text("settle", "Wait after breaking the crust", "e.g. 5:00"))
            .extra(choice("skim", "Skim the foam", "Yes", "No"))
            .tip("A common start: 1:15, water just off the boil, steep 4 minutes, break the "
                    + "crust and skim, wait a few minutes more, then pour slowly.");

    static final Method MOKA = new Method("Moka pot")
            .brewer("Pot", "e.g. Bialetti 3-cup")
            .amounts("16", "Water (g)", "150", "1:7", "1:8", "1:10")
            .time("Brew time", "4:00", "")
            .extra(choice("startWater", "Start with", "Hot water", "Cold water"))
            .extra(choice("heat", "Heat", "Low", "Medium", "High"))
            .tip("Fill the basket level without pressing, fill the base with hot water to just "
                    + "below the valve, use low heat, and take it off as soon as it sputters.");

    static final Method COLD_BREW = new Method("Cold brew")
            .brewer("Vessel", "e.g. Hario Mizudashi")
            .amounts("100", "Water (g)", "800", "1:5", "1:8", "1:12")
            .time("Steep time (h)", "16", " h")
            .extra(choice("steepIn", "Steep in", "Fridge", "Room"))
            .extra(text("dilute", "Serve diluted", "e.g. 1:1 with water or milk"))
            .extra(text("filter", "Filter", "e.g. paper, cloth"))
            .tip("1:5 to 1:8 makes a concentrate to dilute when serving; around 1:12 is ready "
                    + "to drink. Steep 12–24 hours, coarse grind.");

    static final Method OTHER = new Method("Other")
            .brewer("Brewer", "e.g. Clever Dripper")
            .temp("93")
            .amounts("15", "Water (g)", "250", "1:15", "1:16");

    static final Method[] ALL = {ESPRESSO, POUR_OVER, AEROPRESS, FRENCH_PRESS, MOKA, COLD_BREW, OTHER};

    private Methods() {}

    /** The method with this name; for an empty or unknown name, the plain Other layout. */
    static Method get(String name) {
        for (Method m : ALL) if (m.name.equalsIgnoreCase(name == null ? "" : name.trim())) return m;
        return OTHER;
    }

    /** Maps what the first versions stored as free text ("V60", "Chemex") onto a method. */
    static String kindOf(String old) {
        String t = old == null ? "" : old.trim().toLowerCase(Locale.ROOT);
        if (t.isEmpty()) return "";
        for (Method m : ALL) if (m.name.equalsIgnoreCase(t)) return m.name;
        if (t.contains("espresso")) return ESPRESSO.name;
        if (t.contains("aero")) return AEROPRESS.name;
        if (t.contains("moka")) return MOKA.name;
        if (t.contains("cold")) return COLD_BREW.name;
        if (t.contains("french") || t.contains("press")) return FRENCH_PRESS.name;
        if (t.matches(".*(v60|kalita|chemex|origami|april|pour|filter|hario|melitta|wave|switch|drip).*")) {
            return POUR_OVER.name;
        }
        return OTHER.name;
    }

    /** "28" with unit " s" becomes "28 s"; anything that is not a plain number stays as typed. */
    static String show(String value, String unit) {
        String v = value == null ? "" : value.trim();
        if (v.isEmpty() || unit.isEmpty()) return v;
        return v.matches("[0-9.,\\s–-]+") ? v + unit : v;
    }

    /** "Shot time (s)" becomes "Shot time", for the read-only screen where units follow values. */
    static String plain(String label) {
        return label.replaceAll("\\s*\\([^)]*\\)$", "");
    }

    /** 34.0 becomes "34", 42.5 stays "42.5". */
    static String number(double v) {
        double r = Math.round(v * 10) / 10.0;
        return r == Math.floor(r) ? String.valueOf((long) r) : String.valueOf(r);
    }

    /** The brew as one line, for the shared text list. */
    static String summary(Bean b) {
        Method m = get(b.method);
        List<String> parts = new ArrayList<>();
        parts.add(Bean.joinDots(b.method, b.brewer));
        if (m.hasTemp) parts.add(b.tempLabel());
        parts.add(b.grind);
        String amounts = amounts(b);
        parts.add(amounts);
        parts.add(show(b.time, m.timeUnit));
        for (Field f : m.extras) {
            String v = b.extras.get(f.id);
            if (v != null && !v.isEmpty()) parts.add(plain(f.label).toLowerCase(Locale.ROOT) + " " + show(v, f.unit));
        }
        return Bean.joinDots(parts.toArray(new String[0]));
    }

    /** "18 → 36 g (1:2)", or what is known of it. */
    static String amounts(Bean b) {
        double d = Bean.firstNumber(b.dose), w = Bean.firstNumber(b.water);
        if (d > 0 && w > 0) return number(d) + " → " + number(w) + " g (" + b.ratio() + ")";
        if (d > 0) return number(d) + " g";
        return "";
    }

    static void keepOnly(Map<String, String> extras, Method m) {
        extras.keySet().removeIf(id -> m.field(id) == null);
    }
}

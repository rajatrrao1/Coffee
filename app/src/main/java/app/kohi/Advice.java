package app.kohi;

/**
 * One plain suggestion for the next brew, from where it landed on the taste compass and how long
 * it took. Sour usually means too little was drawn out of the coffee, bitter too much; weak or
 * strong is about how much coffee went in for the water. Same rules as the iPhone app.
 */
final class Advice {

    private Advice() {}

    static String next(Brew b) {
        StringBuilder out = new StringBuilder();
        switch (b.taste) {
            case "Sour": add(out, drawMore(b)); break;
            case "Bitter": add(out, drawLess(b)); break;
            case "Balanced": break;
            default: add(out, timing(b)); break;
        }
        if (b.strength.equals("Weak")) add(out, stronger(b));
        else if (b.strength.equals("Strong")) add(out, weaker(b));
        if (out.length() == 0 && b.taste.equals("Balanced")) return "Tasted right: keep these settings.";
        return out.toString();
    }

    private static void add(StringBuilder out, String s) {
        if (s == null || s.isEmpty()) return;
        if (out.length() > 0) out.append(' ');
        out.append(s);
    }

    private static boolean isImmersion(String m) {
        return m.equals(Methods.AEROPRESS.name) || m.equals(Methods.FRENCH_PRESS.name);
    }

    private static String drawMore(Brew b) {
        String m = b.method;
        if (m.equals(Methods.ESPRESSO.name)) {
            Double s = seconds(b.time);
            if (s != null && s >= 32) return "Sour even though it ran long: try hotter water or a longer ratio.";
            return "Sour: grind finer, so the shot runs slower.";
        }
        if (isImmersion(m)) return "Sour: steep longer or grind finer.";
        if (m.equals(Methods.MOKA.name)) return "Sour: grind a little finer, or start with hotter water.";
        if (m.equals(Methods.COLD_BREW.name)) return "Sour or thin: steep a few hours longer.";
        return "Sour: grind finer or use hotter water.";
    }

    private static String drawLess(Brew b) {
        String m = b.method;
        if (m.equals(Methods.ESPRESSO.name)) {
            Double s = seconds(b.time);
            if (s != null && s <= 22) return "Bitter even though it ran fast: try cooler water or a shorter ratio.";
            return "Bitter: grind coarser, so the shot runs faster.";
        }
        if (isImmersion(m)) return "Bitter: steep shorter or grind coarser.";
        if (m.equals(Methods.MOKA.name)) return "Bitter: use lower heat and take it off as soon as it sputters.";
        if (m.equals(Methods.COLD_BREW.name)) return "Bitter: steep a few hours less.";
        return "Bitter: grind coarser or use slightly cooler water.";
    }

    /** Without a taste noted: was the shot or the pour faster or slower than usual? */
    private static String timing(Brew b) {
        Double s = seconds(b.time);
        if (s == null) return "";
        if (b.method.equals(Methods.ESPRESSO.name)) {
            if (s < 24) return "Ran fast: grind finer.";
            if (s > 32) return "Ran slow: grind coarser.";
            return "";
        }
        if (b.method.equals(Methods.POUR_OVER.name)) {
            boolean chemex = b.brewer.toLowerCase(java.util.Locale.ROOT).contains("chemex");
            double lo = chemex ? 240 : 150, hi = chemex ? 330 : 210;
            if (s < lo) return "Finished fast: grind finer.";
            if (s > hi) return "Finished slow: grind coarser.";
        }
        return "";
    }

    private static String stronger(Brew b) {
        double r = ratio(b);
        if (b.method.equals(Methods.ESPRESSO.name)) {
            return r > 0 ? "Weak: use a shorter ratio, about 1:" + Methods.number(Math.max(1, r - 0.25)) + "."
                    : "Weak: use a shorter ratio.";
        }
        return r > 0 ? "Weak: use more coffee, about 1:" + Methods.number(Math.max(1, r - 1)) + "."
                : "Weak: use more coffee for the same water.";
    }

    private static String weaker(Brew b) {
        double r = ratio(b);
        if (b.method.equals(Methods.ESPRESSO.name)) {
            return r > 0 ? "Strong: use a longer ratio, about 1:" + Methods.number(r + 0.25) + "."
                    : "Strong: use a longer ratio.";
        }
        return r > 0 ? "Strong: use more water, about 1:" + Methods.number(r + 1) + "."
                : "Strong: use more water for the same coffee.";
    }

    private static double ratio(Brew b) {
        double d = Bean.firstNumber(b.dose), w = Bean.firstNumber(b.water);
        return d > 0 && w > 0 ? w / d : 0;
    }

    /** "2:45" → 165; "28" → 28 (seconds); null when there is no time. */
    static Double seconds(String s) {
        String t = s == null ? "" : s.trim();
        String[] parts = t.split(":");
        if (parts.length == 2) {
            try {
                return Double.parseDouble(parts[0].trim()) * 60 + Double.parseDouble(parts[1].trim());
            } catch (NumberFormatException ignored) {
                // fall through to the plain number
            }
        }
        double v = Bean.firstNumber(t);
        return v > 0 ? v : null;
    }
}

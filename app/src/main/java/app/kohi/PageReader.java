package app.kohi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fetches a coffee's page once and reads what it can: first the structured data a shop
 * publishes for search engines, then labelled lines in the page text ("Origin Mexico",
 * "Altitude: 1,800 masl", "Tasting notes ..."), in English, Dutch, German, French and Spanish.
 * Nothing is guessed silently: whatever is found only fills empty fields, for you to check.
 */
public final class PageReader {

    public static final class Result {
        public String name = "", roaster = "", origin = "", region = "", altitude = "",
                process = "", variety = "", producer = "", roast = "", notes = "";
    }

    private PageReader() {}

    public static Result read(String address) throws IOException {
        return parse(fetch(address), address);
    }

    // ---- fetching -------------------------------------------------------------------------

    static String fetch(String address) throws IOException {
        URL url = new URL(address);
        HttpURLConnection c = null;
        for (int hop = 0; hop < 6; hop++) {
            c = (HttpURLConnection) url.openConnection();
            c.setInstanceFollowRedirects(false);
            c.setConnectTimeout(15000);
            c.setReadTimeout(20000);
            c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) "
                    + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36");
            c.setRequestProperty("Accept", "text/html,application/xhtml+xml;q=0.9,*/*;q=0.8");
            c.setRequestProperty("Accept-Language", "en,nl;q=0.8,de;q=0.6");
            int code = c.getResponseCode();
            if (code >= 300 && code < 400) {
                String location = c.getHeaderField("Location");
                c.disconnect();
                if (location == null) throw new IOException("the site sent us elsewhere without an address");
                url = new URL(url, location);
                continue;
            }
            if (code >= 400) {
                c.disconnect();
                throw new IOException("the site answered " + code);
            }
            break;
        }
        if (c == null) throw new IOException("no connection");
        Charset cs = StandardCharsets.UTF_8;
        String type = c.getContentType();
        if (type != null) {
            Matcher m = Pattern.compile("charset=([\\w-]+)", Pattern.CASE_INSENSITIVE).matcher(type);
            if (m.find()) {
                try {
                    cs = Charset.forName(m.group(1));
                } catch (Exception ignored) {
                    // keep UTF-8
                }
            }
        }
        try (InputStream in = c.getInputStream()) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] chunk = new byte[16384];
            int n;
            while ((n = in.read(chunk)) != -1 && buf.size() < 6_000_000) buf.write(chunk, 0, n);
            return new String(buf.toByteArray(), cs);
        } finally {
            c.disconnect();
        }
    }

    // ---- reading --------------------------------------------------------------------------

    static final String[] ORIGIN = {"country of origin", "origin country", "origin", "origins",
            "herkomst", "land van herkomst", "land", "country", "herkunft", "herkunftsland",
            "ursprung", "origine", "pays", "origen", "país", "pais"};
    static final String[] REGION = {"region", "regions", "regio", "regionen", "région", "región",
            "regione", "area", "gebied", "provincie", "province", "departamento", "department"};
    static final String[] ALTITUDE = {"altitude", "altitudes", "elevation", "growing altitude",
            "hoogte", "teelthoogte", "höhe", "anbauhöhe", "höhenlage", "altura", "altitud", "masl"};
    static final String[] PROCESS = {"processing method", "process method", "processing", "process",
            "processed", "proces", "verwerking", "verwerkingsmethode", "aufbereitung",
            "verarbeitung", "procédé", "traitement", "proceso", "beneficio"};
    static final String[] VARIETY = {"varieties", "variety", "varietals", "varietal", "cultivars",
            "cultivar", "variëteiten", "variëteit", "varieteit", "varietät", "varietäten",
            "sorte", "sorten", "variété", "variedad", "variedades"};
    static final String[] PRODUCER = {"producers", "producer", "producent", "producenten", "farm",
            "farmer", "farmers", "finca", "estate", "washing station", "washing station / farm",
            "cooperative", "coöperatie", "cooperatie", "boer", "boeren", "produzent", "erzeuger",
            "productor", "producteur"};
    static final String[] NOTES = {"tasting notes", "taste notes", "flavour notes", "flavor notes",
            "cup notes", "cupping notes", "notes", "tasting", "tastes like", "flavours", "flavors",
            "flavour", "flavor", "smaaktonen", "smaaknotities", "proefnotities", "smaakprofiel",
            "smaak", "geschmacksnoten", "geschmack", "aromen", "notes de dégustation",
            "notas de cata", "notas", "in the cup"};
    static final String[] ROAST = {"roast level", "roast", "roast profile", "branding",
            "brandingsgraad", "röstgrad", "röstung", "torréfaction", "tueste"};

    public static Result parse(String html, String address) {
        Result r = new Result();
        Map<String, String> meta = metas(html);
        JSONObject product = jsonLdProduct(html);

        String ldName = product != null ? product.optString("name", "") : "";
        String ldDesc = product != null ? decode(stripTags(product.optString("description", ""))) : "";
        String brand = brandOf(product);

        String title = titleTag(html);
        String name = firstNonEmpty(ldName, meta.get("og:title"), meta.get("twitter:title"));
        if (name.isEmpty()) name = title.split("\\s+[|–—-]\\s+")[0];
        r.name = squash(name);

        r.roaster = squash(firstNonEmpty(brand, meta.get("og:site_name"), siteName(address)));
        if (!r.roaster.isEmpty() && r.name.toLowerCase(Locale.ROOT).endsWith(r.roaster.toLowerCase(Locale.ROOT))) {
            String cut = r.name.substring(0, r.name.length() - r.roaster.length()).replaceAll("[\\s|–—-]+$", "");
            if (!cut.isEmpty()) r.name = cut;
        }

        String desc = squash(firstNonEmpty(ldDesc, meta.get("og:description"), meta.get("description")));
        List<String> lines = segments(textLines(html));
        String all = String.join("\n", lines);

        // Origin: a labelled line that names a coffee country wins ("Origin Mexico");
        // otherwise the first coffee country in the name, the description, then the page.
        String originLine = "";
        for (String o : labeledAll(lines, ORIGIN, 50)) {
            if (!countryIn(o).isEmpty()) {
                originLine = o;
                break;
            }
        }
        r.origin = !originLine.isEmpty() ? cap(originLine)
                : firstNonEmpty(countryIn(r.name), countryIn(desc), countryIn(all));

        r.region = cap(labeled(lines, REGION, 50));
        r.altitude = altitude(lines, all);
        r.process = cap(firstNonEmpty(labeled(lines, PROCESS, 40), processIn(r.name), processIn(desc), processIn(all)));
        String varietyLine = labeled(lines, VARIETY, 80);
        r.variety = cap(firstNonEmpty(varietyLine, varietiesIn(desc), varietiesIn(all)));
        r.producer = cap(labeled(lines, PRODUCER, 60));
        r.roast = firstNonEmpty(roastIn(r.name), roastIn(labeled(lines, ROAST, 40)), roastIn(desc), roastIn(all));
        r.notes = tastingList(labeled(lines, NOTES, 120));
        return r;
    }

    // ---- labelled lines -------------------------------------------------------------------

    /**
     * Finds a line that starts with one of the labels and returns what follows it:
     * "Origin Mexico", "Altitude: 1800 m", or a label alone on its line with the value on the next.
     */
    static String labeled(List<String> lines, String[] labels, int max) {
        List<String> all = labeledAll(lines, labels, max);
        return all.isEmpty() ? "" : all.get(0);
    }

    /** Every value found for these labels, in page order. */
    static List<String> labeledAll(List<String> lines, String[] labels, int max) {
        List<String> found = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String low = line.toLowerCase(Locale.ROOT);
            for (String label : labels) {
                if (!low.startsWith(label)) continue;
                String rest = line.substring(label.length());
                String bare = rest.replaceAll("[\\s:：\u00a0]", "");
                if (bare.isEmpty()) {
                    if (i + 1 < lines.size()) {
                        String next = lines.get(i + 1).trim();
                        if (fits(next, max) && !isLabel(next)) {
                            found.add(next);
                            break;
                        }
                    }
                    continue;
                }
                char ch = rest.charAt(0);
                if (ch == ':' || ch == '：' || Character.isWhitespace(ch) || ch == '\u00a0') {
                    String value = rest.replaceFirst("^[\\s:：\u00a0]+", "").trim();
                    if (fits(value, max)) {
                        found.add(value);
                        break;
                    }
                }
            }
        }
        return found;
    }

    private static final Set<String> ALL_LABELS = new LinkedHashSet<>();

    static {
        for (String[] group : new String[][]{ORIGIN, REGION, ALTITUDE, PROCESS, VARIETY, PRODUCER, NOTES, ROAST}) {
            for (String s : group) ALL_LABELS.add(s);
        }
    }

    private static boolean isLabel(String s) {
        return ALL_LABELS.contains(s.toLowerCase(Locale.ROOT).replaceAll("[:：]\\s*$", "").trim());
    }

    private static boolean fits(String v, int max) {
        return !v.isEmpty() && v.length() <= max && v.matches(".*[\\p{L}\\d].*");
    }

    /** Splits "Origin: Kenya | Process: Washed | Altitude: 1800 m" into its parts. */
    static List<String> segments(List<String> lines) {
        List<String> out = new ArrayList<>();
        for (String line : lines) {
            for (String part : line.split("\\s+[|•]\\s+")) {
                String p = part.trim();
                if (!p.isEmpty()) out.add(p);
            }
        }
        return out;
    }

    // ---- altitude -------------------------------------------------------------------------

    private static final String NUM = "(\\d{1,2}[.,\u00a0\u202f]\\d{3}|\\d{3,4})";
    private static final Pattern ALT_RANGE = Pattern.compile(
            NUM + "(?:\\s*(?:-|–|—|~|to|tot|bis|a|à|y)\\s*" + NUM + ")?", Pattern.CASE_INSENSITIVE);
    private static final Pattern ALT_UNIT = Pattern.compile(
            NUM + "(?:\\s*(?:-|–|—|~|to|tot|bis|a|à|y)\\s*" + NUM + ")?\\s*"
                    + "(?:m\\.?\\s?a\\.?\\s?s\\.?\\s?l\\.?|masl|mosl|msnm|m\\.s\\.n\\.m\\.?|m\\.?\\s?ü\\.?\\s?(?:nn|m|meer)"
                    + "|(?:meters?|metres?|metros|m)\\s+(?:above|boven|über|sobre|au-dessus))",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ALT_WORD = Pattern.compile(
            "(?:altitude|elevation|hoogte|höhe|höhenlage|altura|altitud|grown at|groeit op)[^\\d\\n]{0,25}"
                    + NUM + "(?:\\s*(?:-|–|—|~|to|tot|bis|a|à|y)\\s*" + NUM + ")?\\s*(?:m\\b|meters?|metres?|metros|meter)",
            Pattern.CASE_INSENSITIVE);

    static String altitude(List<String> lines, String all) {
        String v = labeled(lines, ALTITUDE, 60);
        if (!v.isEmpty()) {
            Matcher m = ALT_RANGE.matcher(v);
            if (m.find()) {
                String a = altFormat(m.group(1), m.group(2));
                if (!a.isEmpty()) return a;
            }
        }
        for (Pattern p : new Pattern[]{ALT_UNIT, ALT_WORD}) {
            Matcher m = p.matcher(all);
            while (m.find()) {
                String a = altFormat(m.group(1), m.group(2));
                if (!a.isEmpty()) return a;
            }
        }
        return "";
    }

    private static String altFormat(String lo, String hi) {
        int a = altNumber(lo), b = altNumber(hi);
        if (a < 300 || a > 3500) return "";
        if (b >= 300 && b <= 3500 && b != a) {
            return Math.min(a, b) + "–" + Math.max(a, b) + " m";
        }
        return a + " m";
    }

    private static int altNumber(String s) {
        if (s == null) return -1;
        try {
            return Integer.parseInt(s.replaceAll("[^\\d]", ""));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // ---- countries, processes, varieties, roast -------------------------------------------

    // Each row: the name to show, then other spellings (Dutch, German, Spanish, French).
    private static final String[][] COUNTRIES = {
            {"Ethiopia", "Ethiopië", "Äthiopien", "Etiopía", "Éthiopie", "Etiopia"},
            {"Kenya", "Kenia"},
            {"Colombia", "Kolumbien", "Colombie"},
            {"Brazil", "Brazilië", "Brasilien", "Brasil", "Brésil"},
            {"Peru", "Perú", "Pérou"},
            {"Bolivia", "Bolivien", "Bolivie"},
            {"Ecuador", "Équateur"},
            {"Mexico", "Mexiko", "México", "Mexique"},
            {"Guatemala"},
            {"Honduras"},
            {"El Salvador", "Salvador"},
            {"Nicaragua"},
            {"Costa Rica"},
            {"Panama", "Panamá"},
            {"Rwanda", "Ruanda"},
            {"Burundi"},
            {"Tanzania", "Tansania", "Tanzanie"},
            {"Uganda", "Ouganda"},
            {"DR Congo", "Congo", "Kongo", "DRC"},
            {"Yemen", "Jemen", "Yémen"},
            {"India", "Indien", "Inde"},
            {"Indonesia", "Indonesië", "Indonesien", "Indonésie", "Sumatra", "Sulawesi", "Bali", "Flores"},
            {"Papua New Guinea", "Papoea-Nieuw-Guinea", "Papua-Neuguinea", "PNG"},
            {"Vietnam", "Viëtnam", "Việt Nam"},
            {"China", "Yunnan"},
            {"Myanmar", "Burma"},
            {"Thailand", "Thaïlande"},
            {"Laos"},
            {"Timor-Leste", "East Timor", "Oost-Timor", "Osttimor"},
            {"Malawi"},
            {"Zambia", "Sambia"},
            {"Zimbabwe", "Simbabwe"},
            {"Cameroon", "Kameroen", "Kamerun", "Cameroun"},
            {"Haiti", "Haïti"},
            {"Jamaica", "Jamaika", "Jamaïque"},
            {"Dominican Republic", "Dominicaanse Republiek", "Dominikanische Republik"},
            {"Cuba", "Kuba"},
            {"Hawaii", "Kona"},
            {"Nepal"},
            {"Philippines", "Filipijnen", "Philippinen"},
            {"Taiwan"},
            {"Venezuela"},
            {"Puerto Rico"},
    };

    private static final List<Object[]> COUNTRY_PATTERNS = new ArrayList<>();

    static {
        for (String[] row : COUNTRIES) {
            StringBuilder alt = new StringBuilder();
            for (String s : row) {
                if (alt.length() > 0) alt.append('|');
                alt.append(Pattern.quote(s));
            }
            COUNTRY_PATTERNS.add(new Object[]{row[0],
                    Pattern.compile("(?<![\\p{L}])(?:" + alt + ")(?![\\p{L}])", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)});
        }
    }

    /** The coffee country mentioned first in the text, or "". */
    static String countryIn(String text) {
        if (text == null || text.isEmpty()) return "";
        int best = Integer.MAX_VALUE;
        String found = "";
        for (Object[] row : COUNTRY_PATTERNS) {
            Matcher m = ((Pattern) row[1]).matcher(text);
            if (m.find() && m.start() < best) {
                best = m.start();
                found = (String) row[0];
            }
        }
        return found;
    }

    private static final String[][] PROCESSES = {
            {"Carbonic maceration", "carbonic maceration", "koolzuurmaceratie"},
            {"Anaerobic", "anaerobic", "anaeroob", "anaerob", "anaeróbico"},
            {"Wet-hulled", "wet-hulled", "wet hulled", "giling basah", "semi-washed"},
            {"Honey", "honey process", "honey processed", "black honey", "red honey", "yellow honey", "white honey", "honey"},
            {"Natural", "natural process", "naturally processed", "natural", "naturel", "natuurlijk verwerkt", "dry process", "sun-dried", "natural/dry"},
            {"Washed", "fully washed", "washed", "gewassen", "gewaschen", "lavado", "lavé", "wet process"},
    };

    /** The processing named first in the text, or "". */
    static String processIn(String text) {
        if (text == null || text.isEmpty()) return "";
        String low = text.toLowerCase(Locale.ROOT);
        int best = Integer.MAX_VALUE;
        String found = "";
        for (String[] row : PROCESSES) {
            for (int i = 1; i < row.length; i++) {
                Matcher m = Pattern.compile("(?<![\\p{L}])" + Pattern.quote(row[i]) + "(?![\\p{L}])").matcher(low);
                if (m.find() && m.start() < best) {
                    best = m.start();
                    found = row[0];
                }
            }
        }
        return found;
    }

    private static final String[] VARIETIES = {"Pink Bourbon", "Red Bourbon", "Yellow Bourbon",
            "Bourbon", "Typica", "Caturra", "Catuai", "Catuaí", "Gesha", "Geisha", "SL28", "SL-28",
            "SL34", "SL-34", "Ruiru 11", "Batian", "Heirloom", "Landrace", "74110", "74112",
            "Pacamara", "Pacas", "Maragogype", "Castillo", "Colombia", "Mundo Novo", "Pluma Hidalgo",
            "Sidra", "Wush Wush", "Laurina", "Java", "Catimor", "Sarchimor", "Villa Sarchi",
            "Tabi", "Marsellesa", "Parainema", "Obatã", "Icatu", "Acaiá", "Yellow Catuai",
            "Red Catuai", "Kent", "S795", "Ateng", "Abyssinia", "Jember", "Robusta"};

    /** Up to three known varieties, in the order the text names them. Only used without a label. */
    static String varietiesIn(String text) {
        if (text == null || text.isEmpty()) return "";
        List<int[]> hits = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (String v : VARIETIES) {
            if (v.equals("Colombia") || v.equals("Java") || v.equals("Kent")) continue; // too often something else
            Matcher m = Pattern.compile("(?<![\\p{L}\\d])" + Pattern.quote(v) + "(?![\\p{L}\\d])",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(text);
            if (m.find()) {
                String shown = v.equals("Geisha") ? "Gesha" : v.replace("SL-", "SL");
                boolean covered = false;
                for (String n : names) {
                    if (n.equalsIgnoreCase(shown) || n.toLowerCase(Locale.ROOT).contains(shown.toLowerCase(Locale.ROOT))) covered = true;
                }
                if (covered) continue;
                hits.add(new int[]{m.start(), names.size()});
                names.add(shown);
            }
        }
        hits.sort((a, b) -> Integer.compare(a[0], b[0]));
        List<String> out = new ArrayList<>();
        for (int[] h : hits) {
            if (out.size() == 3) break;
            out.add(names.get(h[1]));
        }
        return String.join(", ", out);
    }

    private static final Pattern ROAST_WORDS = Pattern.compile(
            "(light|licht|lichte|hell|helle|claro|clair|medium|middel|mittel|medio|dark|donker|donkere|dunkel|dunkle|oscuro|foncé)"
                    + "[\\s-]*(roast|roasted|gebrand|branding|röstung|geröstet|tueste|torréfaction|$)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    static String roastIn(String text) {
        if (text == null || text.isEmpty()) return "";
        Matcher m = ROAST_WORDS.matcher(text);
        if (!m.find()) {
            String t = text.trim().toLowerCase(Locale.ROOT);
            // A roast label whose whole value is one word: "Roast: Light".
            if (t.matches("light|licht|hell|medium|middel|mittel|dark|donker|dunkel")) return roastOf(t);
            return "";
        }
        return roastOf(m.group(1).toLowerCase(Locale.ROOT));
    }

    private static String roastOf(String w) {
        if (w.startsWith("li") || w.startsWith("he") || w.startsWith("cla")) return "Light";
        if (w.startsWith("me") || w.startsWith("mi")) return "Medium";
        return "Dark";
    }

    /** "Groene appel, kaneel & cola" becomes "groene appel, kaneel, cola". */
    static String tastingList(String v) {
        if (v == null || v.trim().isEmpty()) return "";
        String[] parts = v.split("(?i)\\s*(?:,|;|/|&|\\+|•|·|\\band\\b|\\ben\\b|\\bund\\b|\\bet\\b|\\by\\b)\\s*");
        List<String> keep = new ArrayList<>();
        for (String p : parts) {
            String t = p.trim().replaceAll("[.!]+$", "").toLowerCase(Locale.ROOT);
            if (!t.isEmpty() && !keep.contains(t)) keep.add(t);
        }
        return String.join(", ", keep);
    }

    // ---- html -----------------------------------------------------------------------------

    private static final Pattern META = Pattern.compile("<meta\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ATTR = Pattern.compile("([a-zA-Z_:][-a-zA-Z0-9_:.]*)\\s*=\\s*(\"([^\"]*)\"|'([^']*)')");

    static Map<String, String> metas(String html) {
        Map<String, String> out = new HashMap<>();
        Matcher m = META.matcher(html);
        while (m.find()) {
            Matcher a = ATTR.matcher(m.group());
            String key = null, content = null;
            while (a.find()) {
                String k = a.group(1).toLowerCase(Locale.ROOT);
                String v = a.group(3) != null ? a.group(3) : a.group(4);
                if (k.equals("property") || k.equals("name") || k.equals("itemprop")) key = v.toLowerCase(Locale.ROOT);
                else if (k.equals("content")) content = v;
            }
            if (key != null && content != null && !out.containsKey(key)) out.put(key, decode(content).trim());
        }
        return out;
    }

    static String titleTag(String html) {
        Matcher m = Pattern.compile("<title[^>]*>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(html);
        return m.find() ? squash(decode(m.group(1))) : "";
    }

    private static final Pattern LD = Pattern.compile(
            "<script[^>]*application/ld\\+json[^>]*>(.*?)</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    static JSONObject jsonLdProduct(String html) {
        Matcher m = LD.matcher(html);
        while (m.find()) {
            try {
                Object v = new JSONTokener(m.group(1).trim()).nextValue();
                JSONObject p = findProduct(v, 0);
                if (p != null) return p;
            } catch (JSONException | RuntimeException ignored) {
                // a broken block: try the next one
            }
        }
        return null;
    }

    private static JSONObject findProduct(Object v, int depth) {
        if (depth > 6 || v == null) return null;
        if (v instanceof JSONArray) {
            JSONArray a = (JSONArray) v;
            for (int i = 0; i < a.length(); i++) {
                JSONObject p = findProduct(a.opt(i), depth + 1);
                if (p != null) return p;
            }
            return null;
        }
        if (!(v instanceof JSONObject)) return null;
        JSONObject o = (JSONObject) v;
        Object type = o.opt("@type");
        if (isProduct(type)) return o;
        JSONObject p = findProduct(o.opt("@graph"), depth + 1);
        if (p != null) return p;
        return findProduct(o.opt("mainEntity"), depth + 1);
    }

    private static boolean isProduct(Object type) {
        if (type instanceof String) return ((String) type).equalsIgnoreCase("Product") || ((String) type).equalsIgnoreCase("ProductGroup");
        if (type instanceof JSONArray) {
            JSONArray a = (JSONArray) type;
            for (int i = 0; i < a.length(); i++) if (isProduct(a.opt(i))) return true;
        }
        return false;
    }

    private static String brandOf(JSONObject product) {
        if (product == null) return "";
        Object b = product.opt("brand");
        if (b instanceof JSONArray) b = ((JSONArray) b).opt(0);
        if (b instanceof JSONObject) return ((JSONObject) b).optString("name", "");
        if (b instanceof String) return (String) b;
        return "";
    }

    static List<String> textLines(String html) {
        String s = html;
        s = s.replaceAll("(?is)<(script|style|noscript|svg|template|iframe|head)\\b.*?</\\1\\s*>", "\n");
        s = s.replaceAll("(?s)<!--.*?-->", " ");
        s = s.replaceAll("(?i)<br\\s*/?>", "\n");
        s = s.replaceAll("(?i)</?(p|div|li|ul|ol|h[1-6]|tr|table|thead|tbody|section|article|header|footer"
                + "|dt|dd|dl|blockquote|figure|figcaption|main|nav|aside|details|summary|form|label|option"
                + "|select|button|hr)\\b[^>]*>", "\n");
        s = s.replaceAll("<[^>]*>", " ");
        s = decode(s);
        List<String> out = new ArrayList<>();
        for (String line : s.split("\n")) {
            String t = squash(line);
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    static String stripTags(String s) {
        return s == null ? "" : s.replaceAll("<[^>]*>", " ");
    }

    private static final Map<String, String> ENTITIES = new HashMap<>();

    static {
        String[] pairs = {
                "amp", "&", "lt", "<", "gt", ">", "quot", "\"", "apos", "'", "nbsp", " ",
                "ndash", "–", "mdash", "—", "lsquo", "‘", "rsquo", "’", "ldquo", "“", "rdquo", "”",
                "hellip", "…", "deg", "°", "middot", "·", "bull", "•", "times", "×", "copy", "©",
                "reg", "®", "trade", "™", "eacute", "é", "egrave", "è", "ecirc", "ê", "euml", "ë",
                "aacute", "á", "agrave", "à", "acirc", "â", "auml", "ä", "atilde", "ã",
                "iacute", "í", "igrave", "ì", "icirc", "î", "iuml", "ï",
                "oacute", "ó", "ograve", "ò", "ocirc", "ô", "ouml", "ö", "otilde", "õ",
                "uacute", "ú", "ugrave", "ù", "ucirc", "û", "uuml", "ü",
                "ntilde", "ñ", "ccedil", "ç", "szlig", "ß",
                "Eacute", "É", "Auml", "Ä", "Ouml", "Ö", "Uuml", "Ü", "Ntilde", "Ñ"
        };
        for (int i = 0; i < pairs.length; i += 2) ENTITIES.put(pairs[i], pairs[i + 1]);
    }

    private static final Pattern ENTITY = Pattern.compile("&(#x[0-9a-fA-F]+|#\\d+|[a-zA-Z]+);");

    static String decode(String s) {
        if (s == null || s.indexOf('&') < 0) return s == null ? "" : s;
        Matcher m = ENTITY.matcher(s);
        StringBuilder out = new StringBuilder();
        int last = 0;
        while (m.find()) {
            out.append(s, last, m.start());
            String e = m.group(1);
            String rep = null;
            try {
                if (e.startsWith("#x") || e.startsWith("#X")) rep = new String(Character.toChars(Integer.parseInt(e.substring(2), 16)));
                else if (e.startsWith("#")) rep = new String(Character.toChars(Integer.parseInt(e.substring(1))));
                else rep = ENTITIES.get(e);
            } catch (IllegalArgumentException ignored) {
                rep = null;
            }
            out.append(rep != null ? rep : m.group());
            last = m.end();
        }
        out.append(s.substring(last));
        return out.toString().replace('\u00a0', ' ');
    }

    // ---- small helpers --------------------------------------------------------------------

    static String squash(String s) {
        return s == null ? "" : s.replaceAll("[\\s\u00a0\u202f]+", " ").trim();
    }

    static String firstNonEmpty(String... values) {
        for (String v : values) if (v != null && !v.trim().isEmpty()) return v.trim();
        return "";
    }

    static String cap(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }

    static String siteName(String address) {
        try {
            String host = new URL(address).getHost().toLowerCase(Locale.ROOT);
            if (host.startsWith("www.")) host = host.substring(4);
            int dot = host.indexOf('.');
            return cap(dot > 0 ? host.substring(0, dot) : host);
        } catch (IOException e) {
            return "";
        }
    }

    static String host(String address) {
        try {
            String host = new URL(address).getHost().toLowerCase(Locale.ROOT);
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (IOException e) {
            return "the page";
        }
    }
}

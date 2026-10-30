package app.kohi;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A copy of every bean, kept as coffee-beans.json in a folder you choose (Documents, an SD
 * card, a synced folder…). It is rewritten after every change, so it survives uninstalling
 * the app, and Bring beans in from a file reads it back.
 */
public final class Backup {

    public static final String FILE_NAME = "coffee-beans.json";

    private static final String PREFS = "backup";
    private static final String KEY_FOLDER = "folder";
    private static final String KEY_AUTO = "auto";
    private static final String KEY_LAST = "last";
    private static final String KEY_ERROR = "error";

    private static final ExecutorService WRITER = Executors.newSingleThreadExecutor();

    private Backup() {}

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static Uri folder(Context c) {
        String s = prefs(c).getString(KEY_FOLDER, null);
        return s == null ? null : Uri.parse(s);
    }

    /** Remembers the folder picked in the system's folder chooser, and keeps the right to write there. */
    public static void setFolder(Context c, Uri tree) {
        int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
        c.getContentResolver().takePersistableUriPermission(tree, flags);
        Uri old = folder(c);
        if (old != null && !old.equals(tree)) {
            try {
                c.getContentResolver().releasePersistableUriPermission(old, flags);
            } catch (SecurityException ignored) {
                // it had already gone
            }
        }
        prefs(c).edit().putString(KEY_FOLDER, tree.toString()).putBoolean(KEY_AUTO, true)
                .remove(KEY_ERROR).apply();
    }

    public static void forgetFolder(Context c) {
        prefs(c).edit().remove(KEY_FOLDER).remove(KEY_ERROR).apply();
    }

    /** The folder's name as the phone shows it, e.g. "Documents". */
    public static String folderName(Context c) {
        Uri tree = folder(c);
        if (tree == null) return "";
        try {
            String id = DocumentsContract.getTreeDocumentId(tree);
            Uri doc = DocumentsContract.buildDocumentUriUsingTree(tree, id);
            try (Cursor cur = c.getContentResolver().query(doc,
                    new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME}, null, null, null)) {
                if (cur != null && cur.moveToFirst()) return cur.getString(0);
            }
            int colon = id.indexOf(':');
            return colon >= 0 ? id.substring(colon + 1) : id;
        } catch (Exception e) {
            return "the chosen folder";
        }
    }

    public static boolean auto(Context c) {
        return prefs(c).getBoolean(KEY_AUTO, true);
    }

    public static void setAuto(Context c, boolean on) {
        prefs(c).edit().putBoolean(KEY_AUTO, on).apply();
        if (on) changed(c);
    }

    public static long lastSaved(Context c) {
        return prefs(c).getLong(KEY_LAST, 0);
    }

    public static String lastError(Context c) {
        return prefs(c).getString(KEY_ERROR, "");
    }

    /** Called after every change to the beans: refreshes the copy in the background. */
    public static void changed(Context c) {
        Context app = c.getApplicationContext();
        if (folder(app) == null || !auto(app)) return;
        WRITER.execute(() -> {
            try {
                saveNow(app);
            } catch (Exception ignored) {
                // saveNow has already noted what went wrong
            }
        });
    }

    /** Writes the copy straight away. Throws with a readable reason if it cannot. */
    public static synchronized void saveNow(Context c) throws Exception {
        Uri tree = folder(c);
        if (tree == null) throw new IllegalStateException("No folder is chosen yet.");
        try {
            byte[] data = Store.exportJson(c).getBytes(StandardCharsets.UTF_8);
            ContentResolver cr = c.getContentResolver();
            Uri file = findOrCreate(cr, tree);
            try {
                write(cr, file, "wt", data);
            } catch (Exception truncateNotSupported) {
                // Some storage apps cannot overwrite in place: replace the file instead.
                DocumentsContract.deleteDocument(cr, file);
                file = findOrCreate(cr, tree);
                write(cr, file, "w", data);
            }
            prefs(c).edit().putLong(KEY_LAST, System.currentTimeMillis()).remove(KEY_ERROR).apply();
        } catch (SecurityException e) {
            note(c, "The app may no longer write to that folder. Choose it again.");
            throw new Exception("The app may no longer write to that folder. Choose it again.", e);
        } catch (Exception e) {
            String why = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            note(c, why);
            throw new Exception(why, e);
        }
    }

    private static void note(Context c, String error) {
        prefs(c).edit().putString(KEY_ERROR, error).apply();
    }

    private static Uri findOrCreate(ContentResolver cr, Uri tree) throws Exception {
        String dirId = DocumentsContract.getTreeDocumentId(tree);
        Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, dirId);
        try (Cursor cur = cr.query(children, new String[]{
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME}, null, null, null)) {
            while (cur != null && cur.moveToNext()) {
                if (FILE_NAME.equals(cur.getString(1))) {
                    return DocumentsContract.buildDocumentUriUsingTree(tree, cur.getString(0));
                }
            }
        }
        Uri dir = DocumentsContract.buildDocumentUriUsingTree(tree, dirId);
        Uri made = DocumentsContract.createDocument(cr, dir, "application/json", FILE_NAME);
        if (made == null) throw new Exception("Could not make " + FILE_NAME + " in that folder.");
        return made;
    }

    private static void write(ContentResolver cr, Uri file, String mode, byte[] data) throws Exception {
        try (OutputStream out = cr.openOutputStream(file, mode)) {
            if (out == null) throw new Exception("Could not open " + FILE_NAME + ".");
            out.write(data);
        }
    }
}

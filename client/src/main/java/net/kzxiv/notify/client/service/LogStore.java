package net.kzxiv.notify.client.service;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class LogStore {

    private static final int MAX_LINES = 500;
    private static final SimpleDateFormat FMT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    private static final Object LOCK = new Object();

    private LogStore() {}

    private static File file(Context ctx) {
        return new File(ctx.getFilesDir(), "forwarder_log.txt");
    }

    public static void append(Context ctx, String line) {
        synchronized (LOCK) {
            try {
                File f = file(ctx);
                FileWriter w = new FileWriter(f, true);
                w.write(FMT.format(new Date()) + "  " + line + "\n");
                w.close();

                // Trim if too long
                List<String> lines = readRaw(f);
                if (lines.size() > MAX_LINES) {
                    List<String> kept = lines.subList(lines.size() - MAX_LINES, lines.size());
                    FileWriter trim = new FileWriter(f, false);
                    for (String l : kept) {
                        trim.write(l + "\n");
                    }
                    trim.close();
                }
            } catch (Exception ignored) {
            }
        }
    }

    public static List<String> read(Context ctx) {
        synchronized (LOCK) {
            List<String> out = new ArrayList<>();
            try {
                List<String> all = readRaw(file(ctx));
                // Newest first
                for (int i = all.size() - 1; i >= 0; i--) {
                    out.add(all.get(i));
                }
            } catch (Exception ignored) {
            }
            return out;
        }
    }

    public static void clear(Context ctx) {
        synchronized (LOCK) {
            try {
                FileWriter w = new FileWriter(file(ctx), false);
                w.write("");
                w.close();
            } catch (Exception ignored) {
            }
        }
    }

    private static List<String> readRaw(File f) throws Exception {
        List<String> lines = new ArrayList<>();
        if (!f.exists()) return lines;
        BufferedReader r = new BufferedReader(new FileReader(f));
        String l;
        while ((l = r.readLine()) != null) {
            lines.add(l);
        }
        r.close();
        return lines;
    }
}

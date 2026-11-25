package com.homegym.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Locale;

/** Lưu trạng thái cây; app và widget dùng chung. */
final class PlantStore {

    private static final String PREF = "plant";
    static final long SPARKLE_MS = 3000;

    static long today() {
        return LocalDate.now().toEpochDay();
    }

    static synchronized PlantEngine load(Context c) {
        SharedPreferences p = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        PlantEngine e = new PlantEngine();
        String id = p.getString("plantId", PlantEngine.PLANTS[0][0]);
        for (int i = 0; i < PlantEngine.PLANTS.length; i++) {
            if (PlantEngine.PLANTS[i][0].equals(id)) e.plant = i;
        }
        e.frame = p.getInt("frame", PlantEngine.SEED);
        e.mature = p.getBoolean("mature", false);
        e.miss = p.getInt("miss", 0);
        e.hold = p.getInt("hold", 0);
        e.start = p.getLong("start", -1);
        e.resolved = p.getLong("resolved", -1);
        e.done = p.getLong("done", -1);
        e.recovDay = p.getLong("recovDay", -1);
        e.sparkleUntil = p.getLong("sparkleUntil", 0);
        for (String s : p.getString("collection", "").split(",")) {
            if (!s.isEmpty()) e.collection.add(s);
        }
        e.advance(today());
        save(c, e);
        return e;
    }

    private static void save(Context c, PlantEngine e) {
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                .putString("plantId", PlantEngine.PLANTS[e.plant][0])
                .putInt("frame", e.frame)
                .putBoolean("mature", e.mature)
                .putInt("miss", e.miss)
                .putInt("hold", e.hold)
                .putLong("start", e.start)
                .putLong("resolved", e.resolved)
                .putLong("done", e.done)
                .putLong("recovDay", e.recovDay)
                .putLong("sparkleUntil", e.sparkleUntil)
                .putString("collection", String.join(",", e.collection))
                .apply();
    }

    static synchronized void complete(Context c) {
        PlantEngine e = load(c);
        if (e.complete(today())) {
            if (e.recovDay == today()) e.sparkleUntil = System.currentTimeMillis() + SPARKLE_MS;
            save(c, e);
        }
    }

    static String framePath(String plantId, int frame) {
        return String.format(Locale.US, "plants/%s/%02d.png", plantId, frame);
    }

    static Bitmap bitmap(Context c, String plantId, int frame) {
        try (InputStream in = c.getAssets().open(framePath(plantId, frame))) {
            return BitmapFactory.decodeStream(in);
        } catch (IOException ex) {
            return null;
        }
    }

    private static String nameOf(String id) {
        for (String[] p : PlantEngine.PLANTS) if (p[0].equals(id)) return p[1];
        return id;
    }

    static String json(Context c) {
        PlantEngine e = load(c);
        try {
            String id = PlantEngine.PLANTS[e.plant][0];
            JSONArray col = new JSONArray();
            for (String s : e.collection) {
                String[] a = s.split(":");
                col.put(new JSONObject()
                        .put("name", nameOf(a[0]))
                        .put("img", framePath(a[0], PlantEngine.FULL))
                        .put("date", LocalDate.ofEpochDay(Long.parseLong(a[1])).toString()));
            }
            return new JSONObject()
                    .put("name", PlantEngine.PLANTS[e.plant][1])
                    .put("img", framePath(id, e.frame))
                    .put("frame", e.frame)
                    .put("status", e.statusText(today()))
                    .put("collection", col)
                    .toString();
        } catch (JSONException ex) {
            return "null";
        }
    }
}

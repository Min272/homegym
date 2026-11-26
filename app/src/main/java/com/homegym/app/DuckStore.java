package com.homegym.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;

/** Trạng thái con vịt: chỉ nhớ ngày gần nhất đã tập. App và widget dùng chung. */
final class DuckStore {

    private static final String PREF = "plant"; // giữ tên cũ để không mất dữ liệu "đã tập hôm nay" khi cập nhật
    private static final String KEY_DONE = "done";

    private DuckStore() {}

    static long today() {
        return LocalDate.now().toEpochDay();
    }

    /** Lịch trong index.html (WK): Thứ 2 đến Thứ 7, Chủ nhật nghỉ. Đổi lịch thì sửa cả ở đây. */
    static boolean isTrainingDay(long day) {
        // 1970-01-01 là Thứ Năm; 0 = Thứ Hai ... 6 = Chủ nhật
        return Math.floorMod(day + 3, 7L) != 6;
    }

    private static long done(Context c) {
        SharedPreferences p = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        return p.getLong(KEY_DONE, -1);
    }

    static boolean isDone(Context c) {
        return done(c) == today();
    }

    /** Vịt mệt khi hôm nay là ngày tập mà chưa tập. Ngày nghỉ thì vịt vẫn khỏe. */
    static boolean isTired(Context c) {
        return !isDone(c) && isTrainingDay(today());
    }

    static synchronized void complete(Context c) {
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putLong(KEY_DONE, today()).apply();
    }

    static String json(Context c) {
        boolean tired = isTired(c);
        String title = tired ? "Vịt đang mệt" : "Vịt khỏe mạnh";
        String status = isDone(c) ? "✔ Hôm nay bạn đã tập xong"
                : tired ? "Hôm nay bạn chưa tập, vịt đang chờ bạn"
                : "Hôm nay là ngày nghỉ";
        try {
            return new JSONObject()
                    .put("tired", tired)
                    .put("title", title)
                    .put("status", status)
                    .toString();
        } catch (JSONException ex) {
            return "null";
        }
    }
}

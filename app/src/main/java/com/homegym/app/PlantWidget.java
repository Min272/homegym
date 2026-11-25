package com.homegym.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.View;
import android.os.Bundle;
import android.widget.RemoteViews;

/**
 * Widget cây ngoài màn hình chính. Tự làm mới mỗi 30 phút.
 * Co được tới 1x1 (ẩn chữ khi quá nhỏ). Chưa tập: tối. Đã tập: sáng + sao lấp lánh.
 */
public class PlantWidget extends AppWidgetProvider {

    static final String ACTION_SPARKLE_END = "com.homegym.app.SPARKLE_END";
    private static final int[] SPARK_IDS = {R.id.spark0, R.id.spark1, R.id.spark2, R.id.spark3};
    private static final Handler UI = new Handler(Looper.getMainLooper());

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        render(context, manager, ids);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager, int id, Bundle opts) {
        render(context, manager, new int[]{id});
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String a = intent.getAction();
        if (Intent.ACTION_TIME_CHANGED.equals(a) || Intent.ACTION_TIMEZONE_CHANGED.equals(a)
                || Intent.ACTION_BOOT_COMPLETED.equals(a) || ACTION_SPARKLE_END.equals(a)) {
            refresh(context);
        }
    }

    static void refresh(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        render(c, m, m.getAppWidgetIds(new ComponentName(c, PlantWidget.class)));
    }

    private static void render(Context c, AppWidgetManager m, int[] ids) {
        if (ids.length == 0) return;
        PlantEngine e = PlantStore.load(c);
        long today = PlantStore.today();
        long now = System.currentTimeMillis();
        boolean dim = e.isDim(today);
        boolean lit = e.isLit(today);
        int sparkle = e.sparkle(today, now);

        Bitmap plant = PlantStore.bitmap(c, PlantEngine.PLANTS[e.plant][0], e.frame);
        Bitmap img = plant == null ? null : prepare(plant, dim);
        Bitmap[] sparks = sparkle == PlantEngine.SPARKLE_NONE ? null : sparkFrames(SPARK_IDS.length);
        String label = e.widgetLabel(today);

        for (int id : ids) {
            Bundle o = m.getAppWidgetOptions(id);
            int minW = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110);
            int minH = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110);
            boolean tiny = minH < 100;            // hàng 1 ô: bỏ chữ, chỉ còn cây
            boolean narrow = minW < 100;          // cột 1 ô: chữ nhỏ hơn
            boolean compact = tiny || narrow;

            RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_plant);
            if (img != null) v.setImageViewBitmap(R.id.plant_img, img);

            v.setInt(R.id.plant_root, "setBackgroundResource",
                    dim ? R.drawable.widget_bg_dark : lit ? R.drawable.widget_bg_lit : R.drawable.widget_bg);
            int pad = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    compact ? 4 : 8, c.getResources().getDisplayMetrics());
            v.setViewPadding(R.id.plant_root, pad, pad, pad, pad);

            v.setTextViewText(R.id.plant_text, label);
            v.setTextColor(R.id.plant_text, dim ? Color.parseColor("#9AA6B8") : Color.parseColor("#26384A"));
            v.setTextViewTextSize(R.id.plant_text, TypedValue.COMPLEX_UNIT_SP, narrow ? 10 : 12);
            v.setViewVisibility(R.id.plant_text, tiny ? View.GONE : View.VISIBLE);

            if (sparks != null) {
                for (int i = 0; i < sparks.length; i++) v.setImageViewBitmap(SPARK_IDS[i], sparks[i]);
                v.setViewVisibility(R.id.spark_flip, View.VISIBLE);
            } else {
                v.setViewVisibility(R.id.spark_flip, View.GONE);
            }

            v.setOnClickPendingIntent(R.id.plant_root, PendingIntent.getActivity(
                    c, 0, new Intent(c, MainActivity.class),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            m.updateAppWidget(id, v);
        }

        if (sparkle == PlantEngine.SPARKLE_TIMED) scheduleEnd(c, Math.max(0, e.sparkleUntil - now) + 150);
    }

    /** Hết 3 giây lấp lánh thì vẽ lại: Handler khi app còn sống, báo thức dự phòng khi không. */
    private static void scheduleEnd(Context c, long delayMs) {
        final Context app = c.getApplicationContext();
        UI.removeCallbacksAndMessages(null);
        UI.postDelayed(() -> refresh(app), delayMs);
        AlarmManager am = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent i = new Intent(app, PlantWidget.class).setAction(ACTION_SPARKLE_END);
        PendingIntent pi = PendingIntent.getBroadcast(app, 1, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + delayMs + 500, pi);
    }

    // ---------- Vẽ ----------

    /** Ảnh gốc nền trắng: xoá nền trắng (loang từ mép) để đặt lên nền sáng/tối, rồi làm tối nếu cần. */
    private static Bitmap prepare(Bitmap src, boolean dim) {
        int w = src.getWidth() * 2, h = src.getHeight() * 2;
        Bitmap b = Bitmap.createScaledBitmap(src, w, h, true).copy(Bitmap.Config.ARGB_8888, true);
        int[] px = new int[w * h];
        b.getPixels(px, 0, w, 0, 0, w, h);
        boolean[] seen = new boolean[w * h];
        int[] stack = new int[4 * w * h + 2 * (w + h) + 8];
        int sp = 0;
        for (int x = 0; x < w; x++) {
            stack[sp++] = x;
            stack[sp++] = (h - 1) * w + x;
        }
        for (int y = 0; y < h; y++) {
            stack[sp++] = y * w;
            stack[sp++] = y * w + w - 1;
        }
        while (sp > 0) {
            int p = stack[--sp];
            if (seen[p]) continue;
            int c = px[p];
            int lo = Math.min(Color.red(c), Math.min(Color.green(c), Color.blue(c)));
            if (lo < 225) continue;
            seen[p] = true;
            // 250+ trong suốt hẳn; 225..250 mờ dần để viền không bị răng cưa
            int alpha = lo >= 250 ? 0 : (250 - lo) * 255 / 25;
            px[p] = (alpha << 24) | (c & 0xFFFFFF);
            int x = p % w, y = p / w;
            if (x > 0) stack[sp++] = p - 1;
            if (x < w - 1) stack[sp++] = p + 1;
            if (y > 0) stack[sp++] = p - w;
            if (y < h - 1) stack[sp++] = p + w;
        }
        b.setPixels(px, 0, w, 0, 0, w, h);
        if (!dim) return b;
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Paint p = new Paint(Paint.FILTER_BITMAP_FLAG);
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0.45f);
        ColorMatrix shade = new ColorMatrix(new float[]{
                0.32f, 0, 0, 0, 0,
                0, 0.34f, 0, 0, 0,
                0, 0, 0.42f, 0, 4,
                0, 0, 0, 1, 0});
        cm.postConcat(shade);
        p.setColorFilter(new ColorMatrixColorFilter(cm));
        new Canvas(out).drawBitmap(b, 0, 0, p);
        return out;
    }

    /** Vị trí sao (tỉ lệ 0..1), kích thước tương đối, pha lệch nhau. */
    private static final float[][] STARS = {
            {0.14f, 0.20f, 1.0f, 0.00f}, {0.86f, 0.16f, 0.8f, 0.30f},
            {0.50f, 0.05f, 0.7f, 0.60f}, {0.92f, 0.52f, 0.9f, 0.15f},
            {0.07f, 0.58f, 0.7f, 0.75f}, {0.76f, 0.80f, 0.8f, 0.45f},
            {0.24f, 0.86f, 0.9f, 0.90f}, {0.68f, 0.33f, 0.5f, 0.55f},
    };

    /** n khung trong suốt vuông; mỗi sao nhấp nháy theo pha riêng. */
    private static Bitmap[] sparkFrames(int n) {
        final int S = 192;
        Bitmap[] out = new Bitmap[n];
        for (int k = 0; k < n; k++) {
            Bitmap b = Bitmap.createBitmap(S, S, Bitmap.Config.ARGB_8888);
            Canvas cv = new Canvas(b);
            for (float[] st : STARS) {
                double ph = (k / (double) n + st[3]) * 2 * Math.PI;
                float t = (float) (0.5 + 0.5 * Math.sin(ph)); // 0..1
                if (t < 0.12f) continue;
                star(cv, st[0] * S, st[1] * S, S * 0.085f * st[2] * (0.4f + 0.6f * t), (int) (255 * t));
            }
            out[k] = b;
        }
        return out;
    }

    private static void star(Canvas cv, float cx, float cy, float r, int alpha) {
        Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
        glow.setColor(Color.argb(alpha / 3, 255, 214, 90));
        cv.drawCircle(cx, cy, r * 0.9f, glow);
        Path path = new Path();
        float in = r * 0.22f;
        for (int i = 0; i < 8; i++) {
            double a = Math.PI / 4 * i - Math.PI / 2;
            float rad = (i % 2 == 0) ? r : in;
            float x = cx + (float) Math.cos(a) * rad, y = cy + (float) Math.sin(a) * rad;
            if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
        }
        path.close();
        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setColor(Color.argb(alpha, 255, 200, 40));
        cv.drawPath(path, fill);
        Paint core = new Paint(Paint.ANTI_ALIAS_FLAG);
        core.setColor(Color.argb(alpha, 255, 255, 235));
        cv.drawCircle(cx, cy, r * 0.2f, core);
    }
}

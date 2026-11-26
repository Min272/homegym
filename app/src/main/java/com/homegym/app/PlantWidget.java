package com.homegym.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;

/**
 * Widget con vịt ngoài màn hình chính, nền trong suốt. Tự làm mới mỗi 30 phút.
 * Hôm nay là ngày tập mà chưa tập: vịt mệt mỏi. Đã tập (hoặc ngày nghỉ theo lịch): vịt khỏe mạnh.
 */
public class PlantWidget extends AppWidgetProvider {

    private static final int[] DUCK_IDS = {R.id.duck0, R.id.duck1, R.id.duck2, R.id.duck3};

    private static final int[] FIT = {
            R.drawable.duck_fit_0, R.drawable.duck_fit_1, R.drawable.duck_fit_2, R.drawable.duck_fit_3};
    private static final int[] TIRED = {
            R.drawable.duck_tired_0, R.drawable.duck_tired_1, R.drawable.duck_tired_2, R.drawable.duck_tired_3};

    /** Thời gian mỗi khung (ms): vịt khỏe nhanh nhẹn, vịt mệt chậm rãi. */
    private static final int FIT_INTERVAL = 350, TIRED_INTERVAL = 650;

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
                || Intent.ACTION_BOOT_COMPLETED.equals(a)) {
            refresh(context);
        }
    }

    static void refresh(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        render(c, m, m.getAppWidgetIds(new ComponentName(c, PlantWidget.class)));
    }

    private static void render(Context c, AppWidgetManager m, int[] ids) {
        if (ids.length == 0) return;
        boolean tired = DuckStore.isTired(c); // ngày tập mà chưa tập
        int[] frames = tired ? TIRED : FIT;

        PendingIntent open = PendingIntent.getActivity(
                c, 0, new Intent(c, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        for (int id : ids) {
            RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_plant);
            for (int i = 0; i < DUCK_IDS.length; i++) v.setImageViewResource(DUCK_IDS[i], frames[i]);
            v.setInt(R.id.duck_flip, "setFlipInterval", tired ? TIRED_INTERVAL : FIT_INTERVAL);
            v.setOnClickPendingIntent(R.id.plant_root, open);
            m.updateAppWidget(id, v);
        }
    }
}

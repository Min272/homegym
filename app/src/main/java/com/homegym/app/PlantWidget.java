package com.homegym.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/** Widget cây ngoài màn hình chính. Tự làm mới mỗi 30 phút. */
public class PlantWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        render(context, manager, ids);
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
        PlantEngine e = PlantStore.load(c);
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_plant);
        v.setImageViewBitmap(R.id.plant_img,
                PlantStore.bitmap(c, PlantEngine.PLANTS[e.plant][0], e.frame));
        v.setTextViewText(R.id.plant_text, e.statusText(PlantStore.today()));
        v.setOnClickPendingIntent(R.id.plant_root, PendingIntent.getActivity(
                c, 0, new Intent(c, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        m.updateAppWidget(ids, v);
    }
}

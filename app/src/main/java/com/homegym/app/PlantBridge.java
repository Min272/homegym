package com.homegym.app;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

/** Cầu nối cho JavaScript: window.PlantBridge */
final class PlantBridge {

    private final Activity activity;

    PlantBridge(Activity activity) {
        this.activity = activity;
    }

    @JavascriptInterface
    public String state() {
        return PlantStore.json(activity);
    }

    @JavascriptInterface
    public void completeSession() {
        PlantStore.complete(activity);
        activity.runOnUiThread(() -> PlantWidget.refresh(activity));
    }

    @JavascriptInterface
    public void pinWidget() {
        activity.runOnUiThread(() -> {
            AppWidgetManager m = AppWidgetManager.getInstance(activity);
            if (!m.isRequestPinAppWidgetSupported()
                    || !m.requestPinAppWidget(new ComponentName(activity, PlantWidget.class), null, null)) {
                Toast.makeText(activity, "Hãy nhấn giữ màn hình chính, chọn Tiện ích, rồi chọn HomeGym",
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}

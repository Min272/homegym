package com.homegym.app;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import java.util.Locale;

/** Đọc giọng nói bằng TextToSpeech của Android (WebView thường không hỗ trợ speechSynthesis). window.TtsBridge */
final class TtsBridge {

    private final Context ctx;
    private TextToSpeech tts;
    private boolean ready = false, warned = false;
    private String pending;

    TtsBridge(Context ctx) {
        this.ctx = ctx;
        tts = new TextToSpeech(ctx, status -> {
            if (status != TextToSpeech.SUCCESS) return;
            int r = tts.setLanguage(new Locale("vi", "VN"));
            if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(Locale.getDefault());
                if (!warned) {
                    warned = true;
                    toast("Máy chưa có giọng tiếng Việt: vào Cài đặt > Chuyển văn bản thành giọng nói để tải");
                }
            }
            ready = true;
            if (pending != null) {
                tts.speak(pending, TextToSpeech.QUEUE_FLUSH, null, "u");
                pending = null;
            }
        });
    }

    private void toast(String s) {
        android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
        h.post(() -> Toast.makeText(ctx, s, Toast.LENGTH_LONG).show());
    }

    @JavascriptInterface
    public void speak(String text) {
        if (tts == null) return;
        if (!ready) { pending = text; return; }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "u");
    }

    @JavascriptInterface
    public void stop() {
        if (tts != null) tts.stop();
    }

    void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
    }
}

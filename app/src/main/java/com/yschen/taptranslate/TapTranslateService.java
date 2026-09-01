package com.yschen.taptranslate;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class TapTranslateService extends AccessibilityService {
    private WindowManager wm;
    private TextView bubble;
    private View picker;
    private View card;
    private boolean chromeForeground = false;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        createBubble();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        CharSequence pkg = event.getPackageName();
        chromeForeground = pkg != null && "com.android.chrome".contentEquals(pkg);
        if (bubble != null) bubble.setVisibility(chromeForeground ? View.VISIBLE : View.GONE);
        if (!chromeForeground) {
            removePicker();
            removeCard();
        }
    }

    @Override
    public void onInterrupt() { }

    @Override
    public void onDestroy() {
        removePicker();
        removeCard();
        if (bubble != null) {
            try { wm.removeView(bubble); } catch (Exception ignored) { }
            bubble = null;
        }
        super.onDestroy();
    }

    private void createBubble() {
        bubble = new TextView(this);
        bubble.setText("译");
        bubble.setTextSize(18);
        bubble.setTextColor(Color.WHITE);
        bubble.setGravity(Gravity.CENTER);
        bubble.setBackgroundColor(Color.rgb(38, 38, 38));
        bubble.setElevation(dp(8));
        bubble.setVisibility(View.GONE);
        bubble.setOnClickListener(v -> beginPick());

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                dp(48), dp(48),
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
        lp.x = dp(8);
        wm.addView(bubble, lp);
    }

    private void beginPick() {
        if (!chromeForeground || picker != null) return;
        removeCard();
        picker = new View(this);
        picker.setBackgroundColor(Color.TRANSPARENT);
        picker.setOnTouchListener((v, e) -> {
            if (e.getAction() == MotionEvent.ACTION_UP) {
                float x = e.getRawX();
                float y = e.getRawY();
                removePicker();
                String word = findWordAt(x, y);
                if (word == null || word.isBlank()) {
                    Toast.makeText(this, "这里没识别到英文单词", Toast.LENGTH_SHORT).show();
                } else {
                    showCard(word, "翻译中…", x, y);
                    translate(word, x, y);
                }
                return true;
            }
            return true;
        });

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        wm.addView(picker, lp);
    }

    private String findWordAt(float x, float y) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return null;
        try {
            return findWordRecursive(root, x, y);
        } finally {
            root.recycle();
        }
    }

    private String findWordRecursive(AccessibilityNodeInfo node, float x, float y) {
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        if (!bounds.contains((int) x, (int) y)) return null;

        for (int i = node.getChildCount() - 1; i >= 0; i--) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) continue;
            try {
                String found = findWordRecursive(child, x, y);
                if (found != null) return found;
            } finally {
                child.recycle();
            }
        }

        CharSequence cs = node.getText();
        if (cs == null || cs.length() == 0) return null;
        String text = cs.toString();
        int len = Math.min(text.length(), 1000);
        Bundle args = new Bundle();
        args.putInt(AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_START_INDEX, 0);
        args.putInt(AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_LENGTH, len);
        if (!node.refreshWithExtraData(AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY, args)) return null;
        Parcelable[] locs = node.getExtras().getParcelableArray(AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY);
        if (locs == null) return null;

        int hit = -1;
        for (int i = 0; i < Math.min(locs.length, len); i++) {
            if (!(locs[i] instanceof RectF)) continue;
            RectF r = (RectF) locs[i];
            if (r.contains(x, y)) { hit = i; break; }
        }
        if (hit < 0 || !isWordChar(text.charAt(hit))) return null;

        int start = hit;
        int end = hit + 1;
        while (start > 0 && isWordChar(text.charAt(start - 1))) start--;
        while (end < text.length() && isWordChar(text.charAt(end))) end++;
        String word = text.substring(start, end).replaceAll("^[\\-']+|[\\-']+$", "");
        return word.matches(".*[A-Za-z].*") ? word : null;
    }

    private boolean isWordChar(char c) {
        return Character.isLetter(c) || c == '\'' || c == '-';
    }

    private void translate(String word, float x, float y) {
        new Thread(() -> {
            String result;
            try {
                String q = URLEncoder.encode(word, StandardCharsets.UTF_8.name());
                URL url = new URL("https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=zh-CN&dt=t&q=" + q);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);
                conn.setRequestProperty("User-Agent", "TapTranslate/0.1");
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();
                JSONArray outer = new JSONArray(sb.toString());
                result = outer.getJSONArray(0).getJSONArray(0).getString(0);
                if (result == null || result.isBlank()) result = "暂无翻译";
            } catch (Exception e) {
                result = "翻译失败，请检查网络";
            }
            String finalResult = result;
            getMainExecutor().execute(() -> showCard(word, finalResult, x, y));
        }).start();
    }

    private void showCard(String word, String translation, float x, float y) {
        removeCard();
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(12), dp(16), dp(12));
        box.setBackgroundColor(Color.WHITE);
        box.setElevation(dp(12));

        TextView original = new TextView(this);
        original.setText(word);
        original.setTextColor(Color.BLACK);
        original.setTextSize(17);
        box.addView(original);

        TextView translated = new TextView(this);
        translated.setText(translation);
        translated.setTextColor(Color.DKGRAY);
        translated.setTextSize(16);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(-2, -2);
        tlp.topMargin = dp(4);
        box.addView(translated, tlp);
        box.setOnClickListener(v -> removeCard());
        card = box;

        int width = dp(220);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                width, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        int screenW = getResources().getDisplayMetrics().widthPixels;
        int screenH = getResources().getDisplayMetrics().heightPixels;
        lp.x = Math.max(dp(8), Math.min((int) x - width / 2, screenW - width - dp(8)));
        lp.y = Math.max(dp(8), Math.min((int) y + dp(16), screenH - dp(130)));
        wm.addView(card, lp);
    }

    private void removePicker() {
        if (picker != null) {
            try { wm.removeView(picker); } catch (Exception ignored) { }
            picker = null;
        }
    }

    private void removeCard() {
        if (card != null) {
            try { wm.removeView(card); } catch (Exception ignored) { }
            card = null;
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

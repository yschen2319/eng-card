package com.yschen.taptranslate;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(36), dp(24), dp(24));
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(this);
        title.setText("点译");
        title.setTextSize(30);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView body = new TextView(this);
        body.setText("Chrome 点词翻译\n\n1. 开启无障碍服务\n2. 回到 Chrome\n3. 点右侧“译”悬浮球\n4. 再点网页里的英文单词");
        body.setTextSize(17);
        body.setTextColor(Color.DKGRAY);
        body.setLineSpacing(0, 1.25f);
        LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bodyLp.topMargin = dp(28);
        root.addView(body, bodyLp);

        Button button = new Button(this);
        button.setText("开启无障碍服务");
        button.setAllCaps(false);
        button.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        LinearLayout.LayoutParams buttonLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        buttonLp.topMargin = dp(32);
        root.addView(button, buttonLp);

        TextView privacy = new TextView(this);
        privacy.setText("仅在 Chrome 前台响应。不会读取历史记录、密码、Cookie 或账号；单词会发送到 Google 翻译服务。\n\nv0.1.0");
        privacy.setTextSize(13);
        privacy.setTextColor(Color.GRAY);
        LinearLayout.LayoutParams privacyLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        privacyLp.topMargin = dp(28);
        root.addView(privacy, privacyLp);

        setContentView(root);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

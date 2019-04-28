package ru.luckycactus.telegramcontest;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import static ru.luckycactus.telegramcontest.chartview.common.Utils.dp;

public class ActionBar extends LinearLayout {

    private final TextView textView;
    private final ImageView btnTheme;

    public ActionBar(Context context) {
        this(context, null);
    }

    public ActionBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ActionBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setGravity(Gravity.CENTER_VERTICAL);
        setOrientation(HORIZONTAL);
        setPadding(dp(16), 0, dp(4), 0);

        inflate(context, R.layout.actionbar, this);

        textView = findViewById(R.id.title);
        btnTheme = findViewById(R.id.btnTheme);
    }

    public void setTitle(String title) {
        textView.setText(title);
    }

    public void setTitle(int titleResId) {
        textView.setText(titleResId);
    }

    public void setOnThemeClickListener(OnClickListener clickListener) {
        btnTheme.setOnClickListener(clickListener);
    }
}

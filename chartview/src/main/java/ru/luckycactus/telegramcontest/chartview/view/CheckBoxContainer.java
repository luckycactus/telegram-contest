package ru.luckycactus.telegramcontest.chartview.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import ru.luckycactus.telegramcontest.chartview.R;
import ru.luckycactus.telegramcontest.chartview.common.ChartTheme;
import ru.luckycactus.telegramcontest.chartview.common.Utils;

public class CheckBoxContainer extends LinearLayout {

    private final static int dividerHeight = 1;

    public final CheckBox checkBox;
    public final TextView textView;
    public final ViewGroup textViewContainer;
    public final View divider;

    public CheckBoxContainer(Context context) {
        this(context, null);
    }

    public CheckBoxContainer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CheckBoxContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setGravity(Gravity.CENTER_VERTICAL);
        setOrientation(HORIZONTAL);

        setBackgroundResource(R.drawable.selectable_item_background);

        checkBox = new CheckBox(context);
        textViewContainer = new FrameLayout(context);
        textView = new TextView(context);
        divider = new View(context);

        checkBox.setClickable(false);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dividerHeight,
            Gravity.BOTTOM
        );
        textViewContainer.addView(divider, lp);
        lp = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER_VERTICAL
        );
        textViewContainer.addView(textView, lp);

        LayoutParams lp1 = new LayoutParams(Utils.dp(18), Utils.dp(18));
        addView(checkBox, lp1);
        lp1 = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        addView(textViewContainer, lp1);

        updateTheme();
    }

    public void updateTheme() {
        divider.setBackgroundColor(ChartTheme.dividerColor);
        textView.setTextColor(ChartTheme.textColor);
        checkBox.updateTheme();
    }
}

package ru.luckycactus.telegramcontest.chartview.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import ru.luckycactus.telegramcontest.chartview.common.ChartTheme;
import ru.luckycactus.telegramcontest.chartview.common.Utils;
import ru.luckycactus.telegramcontest.chartview.common.ValueFormatter;
import ru.luckycactus.telegramcontest.chartview.model.ChartData;
import ru.luckycactus.telegramcontest.chartview.model.LineData;
import ru.luckycactus.telegramcontest.chartview.renderer.MarkerRenderer;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class ChartLayout extends LinearLayout {

    private final TextView header;
    private final ChartView chartView;
    private final ChartScrollbarView scrollbar;
    private final LinearLayout checkboxLayout;

    private final int dp16 = Utils.dp(16);
    private final int dp8 = Utils.dp(8);

    private ChartData chartData;

    public ChartLayout(Context context) {
        this(context, null);
    }

    public ChartLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChartLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        if (!ChartTheme.isInitialized()) {
            ChartTheme.update(context);
        }

        setOrientation(LinearLayout.VERTICAL);


        header = new TextView(context, attrs, defStyleAttr);
        chartView = new ChartView(context, attrs, defStyleAttr);
        scrollbar = new ChartScrollbarView(context, attrs, defStyleAttr);
        checkboxLayout = new LinearLayout(context);
        checkboxLayout.setOrientation(VERTICAL);

        header.setTextSize(18);
        header.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        header.setPadding(dp16, dp16, dp16, dp16);
        chartView.setHorizontalPadding(dp16);
        scrollbar.setPadding(dp16, dp16, dp16, dp16);
        addView(header, new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
        addView(chartView, new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        lp.leftMargin = lp.rightMargin = lp.topMargin = lp.bottomMargin = dp16;
        addView(scrollbar, lp);
        addView(checkboxLayout, new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));

        updateColors();

        scrollbar.setOnSelectionChangedListener(new ChartScrollbarView.OnSelectionChangedListener() {
            @Override
            public void onSelectionChanged(float from, float to) {
                chartView.setSelection(from, to);
            }
        });
    }

    public void setData(ChartData data) {
        if (this.chartData != data) {
            this.chartData = data;
            header.setText(chartData.name);
            checkboxLayout.removeAllViews();
            List<LineData> lines = chartData.lines;
            for (int i = 0; i < lines.size(); i++) {
                LineData lineData = lines.get(i);
                CheckBoxContainer container = createCheckBox(lineData);
                checkboxLayout.addView(container, new LayoutParams(MATCH_PARENT, WRAP_CONTENT));
                if (i == lines.size() - 1) {
                    container.divider.setVisibility(GONE);
                }
            }
            scrollbar.setChartData(chartData);
            chartView.setChartData(chartData);
        }
    }

    public void setXAxisFormatter(ValueFormatter formatter) {
        chartView.setXAxisFormatter(formatter);
    }

    public void setYAxisFormatter(ValueFormatter formatter) {
        chartView.setYAxisFormatter(formatter);
    }

    public void setBubbleFormatter(ValueFormatter formatter) {
        chartView.setBubbleFormatter(formatter);
    }

    public void setSelection(float from, float to) {
        scrollbar.setSelection(from, to);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        return new SavedState(
            superState,
            scrollbar.getFrom(),
            scrollbar.getTo(),
            chartView.getMarkerIndex(),
            chartView.getMarkerOffset(),
            chartView.getChartHeight(),
            chartView.getBubbleState(),
            chartView.getMarkerAlpha()
        );
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        scrollbar.setSelection(ss.from, ss.to);
        chartView.restoreMarker(ss.markerIndex, ss.markerOffset, ss.markerAlpha, ss.bubbleState);
        chartView.restoreChartHeight(ss.chartHeight);
    }

    public void updateTheme() {
        updateColors();
        for (int i = 0; i < checkboxLayout.getChildCount(); i++) {
            ((CheckBoxContainer)checkboxLayout.getChildAt(i)).updateTheme();
        }

        chartView.updateTheme();
        scrollbar.updateTheme();
    }

    private void updateColors() {
        setBackgroundColor(ChartTheme.chartBackgroundColor);
        header.setTextColor(ChartTheme.chartTitleColor);
    }

    static class SavedState extends BaseSavedState {
        final float from;
        final float to;
        final int markerIndex;
        final float markerOffset;
        final float chartHeight;
        final MarkerRenderer.BubbleState bubbleState;
        final int markerAlpha;

        public SavedState(
            Parcelable source,
            float from,
            float to,
            int markerIndex,
            float markerOffset,
            float chartHeight,
            MarkerRenderer.BubbleState bubbleState,
            int markerAlpha
        ) {
            super(source);
            this.from = from;
            this.to = to;
            this.markerIndex = markerIndex;
            this.markerOffset = markerOffset;
            this.chartHeight = chartHeight;
            this.bubbleState = bubbleState;
            this.markerAlpha = markerAlpha;
        }

        private SavedState(Parcel in) {
            super(in);
            from = in.readFloat();
            to = in.readFloat();
            markerIndex = in.readInt();
            markerOffset = in.readFloat();
            chartHeight = in.readFloat();
            bubbleState = MarkerRenderer.BubbleState.fromInt(in.readInt());
            markerAlpha = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeFloat(from);
            out.writeFloat(to);
            out.writeInt(markerIndex);
            out.writeFloat(markerOffset);
            out.writeFloat(chartHeight);
            out.writeInt(bubbleState.ordinal());
            out.writeInt(markerAlpha);
        }

        public static final Parcelable.Creator<SavedState> CREATOR
            = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };

    }

    @SuppressLint("RestrictedApi")
    private CheckBoxContainer createCheckBox(final LineData lineData) {
        CheckBoxContainer container = new CheckBoxContainer(getContext());
        container.setPadding(dp16, 0, 0, 0);
        container.setMinimumHeight(Utils.dp(48));

        container.textViewContainer.setPadding(dp8, 0, 0, 0);
        container.textView.setPadding(0, 0, dp16, 0);
        container.textView.setTextSize(16f);
        container.textView.setText(lineData.label);

        final CheckBox cb = container.checkBox;
        cb.setColor(lineData.color);
        cb.setChecked(lineData.isChecked(), false);

        container.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isChecked = !cb.isChecked();
                cb.setChecked(isChecked, true);
                lineData.setChecked(isChecked);
                scrollbar.onCheckedChange(lineData);
                chartView.onCheckedChange(lineData);
            }
        });

        return container;
    }
}

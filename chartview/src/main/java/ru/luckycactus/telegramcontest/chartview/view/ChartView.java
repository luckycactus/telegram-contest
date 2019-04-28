package ru.luckycactus.telegramcontest.chartview.view;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import ru.luckycactus.telegramcontest.chartview.common.Utils;
import ru.luckycactus.telegramcontest.chartview.common.ValueFormatter;
import ru.luckycactus.telegramcontest.chartview.model.ChartData;
import ru.luckycactus.telegramcontest.chartview.model.LineData;
import ru.luckycactus.telegramcontest.chartview.renderer.ChartRenderer;
import ru.luckycactus.telegramcontest.chartview.renderer.MarkerRenderer;
import ru.luckycactus.telegramcontest.chartview.renderer.XAxisRenderer;
import ru.luckycactus.telegramcontest.chartview.renderer.YAxisRenderer;
import ru.luckycactus.telegramcontest.chartview.state.ChartAnimator;
import ru.luckycactus.telegramcontest.chartview.state.ChartState;
import ru.luckycactus.telegramcontest.chartview.state.ChartTransformer;

public class ChartView extends View {

    private final static float lineWidth = Utils.dpF(2);
    private final static float verticalPadding = Utils.dpF(4);

    private final ChartState chartState = new ChartState();
    private final ChartTransformer chartTransformer = new ChartTransformer(chartState);
    private final ChartAnimator chartAnimator = new ChartAnimator(
        this,
        chartState,
        chartTransformer
    );
    private final ChartRenderer chartRenderer = new ChartRenderer(
        this,
        chartState,
        chartTransformer,
        lineWidth
    );
    private final XAxisRenderer xAxisRenderer = new XAxisRenderer(this, chartState, chartTransformer);
    private final YAxisRenderer yAxisRenderer = new YAxisRenderer(this, chartState, chartTransformer);
    private final MarkerRenderer markerRenderer = new MarkerRenderer(this, chartState, chartTransformer);

    public ChartView(Context context) {
        this(context, null);
    }

    public ChartView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        chartRenderer.disableBitmapCache();
    }

    void setChartData(ChartData chartData) {
        if (chartState.chartData == chartData)
            return;

        chartAnimator.jumpToTargetState();
        chartState.setChartData(chartData);
        chartTransformer.prepareMatrix();
        chartAnimator.init();
        chartRenderer.init();
        yAxisRenderer.init();
        xAxisRenderer.init();
        markerRenderer.init();
        invalidate();
    }


    public void onCheckedChange(LineData lineData) {
        chartState.updateMaxVisibleEntry();
        chartTransformer.prepareMatrix();
        chartAnimator.animateChartSize();
        chartAnimator.animateLineVisibility(lineData);
        yAxisRenderer.animateChanges();
        markerRenderer.notifyCheckedChanged();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h;
        int hSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int hSpecSize = MeasureSpec.getSize(heightMeasureSpec);
        if (hSpecMode == MeasureSpec.EXACTLY) {
            h = hSpecSize;
        } else {
            h = (int) (w * 0.9f);
            if (hSpecMode == MeasureSpec.AT_MOST) {
                h = Math.min(h, hSpecSize);
            }
        }
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        chartAnimator.jumpToTargetState();
        chartState.setBounds(
            0,
            verticalPadding,
            w,
            h - xAxisRenderer.getTextHeight() - verticalPadding
        );
        chartTransformer.prepareMatrix();
        chartRenderer.notifySizeChanged();
        yAxisRenderer.notifySizeChanged();
        xAxisRenderer.notifySizeChanged();
        markerRenderer.notifySizeChanged();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        xAxisRenderer.draw(canvas);
        yAxisRenderer.prepareBuffers();
        yAxisRenderer.drawLines(canvas);
        markerRenderer.prepareBuffer();
        markerRenderer.drawLine(canvas);
        chartRenderer.draw(canvas);
        markerRenderer.drawDots(canvas);
        yAxisRenderer.drawNumbers(canvas);
        yAxisRenderer.releaseBuffers();
        markerRenderer.drawBubble(canvas);
    }

    void setSelection(float from, float to) {
        chartState.setSelection(from, to);
        chartAnimator.animateChartSize();
        if (!chartAnimator.animateChartSize()) {
            chartTransformer.prepareMatrix();
            invalidate();
        }
        chartRenderer.invalidateBitmapCache();
        yAxisRenderer.animateChanges();
        xAxisRenderer.animateChanges();
        markerRenderer.notifySelectionChanged();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return markerRenderer.onTouchEvent(event) || super.onTouchEvent(event);
    }

    public void setXAxisFormatter(ValueFormatter formatter) {
        xAxisRenderer.setFormatter(formatter);
    }

    public void setYAxisFormatter(ValueFormatter formatter) {
        yAxisRenderer.setFormatter(formatter);
        markerRenderer.setYAxisFormatter(formatter);
    }

    public void setBubbleFormatter(ValueFormatter formatter) {
        markerRenderer.setLabelFormatter(formatter);
    }

    public void setHorizontalPadding(float paddingPx) {
        chartState.setHorizontalPadding(paddingPx);
        chartTransformer.prepareMatrix();
    }

    public int getMarkerIndex() {
        return markerRenderer.getCurrentIndex();
    }

    public float getMarkerOffset() {
        return markerRenderer.getOffset();
    }

    public int getMarkerAlpha() {
        return markerRenderer.getAlpha();
    }

    public MarkerRenderer.BubbleState getBubbleState() {
        return markerRenderer.getState();
    }

    public void restoreMarker(int markerIndex, float markerOffset, int markerAlpha, MarkerRenderer.BubbleState bubbleState) {
        markerRenderer.restore(markerIndex, markerOffset, markerAlpha, bubbleState);
    }

    public float getChartHeight() {
        return chartState.getChartHeight();
    }

    public void restoreChartHeight(float chartHeight) {
        chartState.restoreChartHeight(chartHeight);
    }

    public void updateTheme() {
        chartRenderer.updateTheme();
        markerRenderer.updateTheme();
        xAxisRenderer.updateTheme();
        yAxisRenderer.updateTheme();
        invalidate();
    }
}

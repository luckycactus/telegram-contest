package ru.luckycactus.telegramcontest.chartview.renderer;

import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;

import java.util.Arrays;
import java.util.List;

import ru.luckycactus.telegramcontest.chartview.R;
import ru.luckycactus.telegramcontest.chartview.common.ChartTheme;
import ru.luckycactus.telegramcontest.chartview.common.Utils;
import ru.luckycactus.telegramcontest.chartview.common.ValueFormatter;
import ru.luckycactus.telegramcontest.chartview.model.LineData;
import ru.luckycactus.telegramcontest.chartview.state.ChartAnimator;
import ru.luckycactus.telegramcontest.chartview.state.ChartState;
import ru.luckycactus.telegramcontest.chartview.state.ChartTransformer;
import ru.luckycactus.telegramcontest.chartview.state.LineState;
import ru.luckycactus.telegramcontest.chartview.view.ChartView;

import static ru.luckycactus.telegramcontest.chartview.common.Utils.getThemeColorOrThrow;
import static ru.luckycactus.telegramcontest.chartview.common.Utils.sp;
import static ru.luckycactus.telegramcontest.chartview.renderer.MarkerRenderer.BubbleState.*;

public class MarkerRenderer {

    private static final float lineWidth = Utils.dpF(2);

    private static final float dotRadius = Utils.dpF(4);
    private static final float dotStrokeWidth = Utils.dpF(2);
    private static float fullRadius = dotRadius + dotStrokeWidth / 2f;

    private static final float bubbleCornerRadius = Utils.dpF(6);
    private static final float bubbleVerticalPadding = Utils.dpF(10);
    private static final float bubbleHorizontalPadding = Utils.dpF(12);
    private static final float bubbleItemInnerSpacing = Utils.dpF(2); // spacing between label and value
    private static final float bubbleVerticalSpacing = Utils.dpF(16); // vertical spacing between elements
    private static final float bubbleHorizontalMargin = Utils.dpF(8);

    private static final float bubbleLabelTextSize = sp(12);
    private static final float bubbleYValueTextSize = sp(17);
    private static final float bubbleTitleTextSize = sp(13);

    private static final float bubbleShadowRadius = Utils.dpF(2f);
    private static final float bubbleShadowDx = 0;
    private static final float bubbleShadowDy = Utils.dpF(1f);
    private static final RectF bubbleShadowOffsets = new RectF();
    private static final int bubbleShadowColor = 0x55000000;

    static {
        bubbleShadowOffsets.set(
            Math.max(0f, bubbleShadowRadius - bubbleShadowDx),
            Math.max(0f, bubbleShadowRadius - bubbleShadowDy),
            Math.max(0f, bubbleShadowRadius + bubbleShadowDx),
            Math.max(0f, bubbleShadowRadius + bubbleShadowDy)
        );
    }

    private final View view;
    private final ChartState chartState;
    private final ChartTransformer chartTransformer;
    private final float[] pointBuffer = new float[2];
    private final Paint dotPaint = new Paint();
    private final Paint linePaint = new Paint();
    private final Bubble bubble = new Bubble();

    private ValueFormatter labelFormatter = ValueFormatter.DEFAULT;
    private ValueFormatter yAxisFormatter = ValueFormatter.DEFAULT;
    private float[] dotsBuffer;
    private boolean scrollHandled = false;
    private boolean scrollIntercepted = false;
    private float touchStartX;
    private float touchStartY;

    private int currentIndex;
    private boolean bubbleTouched = false;
    private boolean anyChecked;
    private int alpha;
    private int dstAlpha;
    private ValueAnimator animator;
    private BubbleState state = GONE;

    private boolean indexChanged = true;
    private boolean checkChanged = true;
    private boolean selectionChanged = true;
    private boolean themeChanged = true;

    public MarkerRenderer(
        View view,
        ChartState chartState,
        ChartTransformer chartTransformer
    ) {
        this.view = view;
        this.chartState = chartState;
        this.chartTransformer = chartTransformer;

        dotPaint.setStrokeWidth(dotStrokeWidth);
        linePaint.setStrokeWidth(lineWidth);

        updateTheme();
    }

    public void updateTheme() {
        linePaint.setColor(ChartTheme.bubbleLineColor);
        bubble.updateTheme();
        themeChanged = true;
    }

    public void init() {
        if (chartState.chartData == null)
            return;

        int lineCount = chartState.chartData.lines.size();
        if (dotsBuffer == null || dotsBuffer.length < lineCount * 2 + 4) {
            dotsBuffer = new float[lineCount * 2 + 4];
        }

        state = GONE;
        alpha = dstAlpha = 0;
        updateAnyChecked();
    }

    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = false;

        float touchX = event.getX();
        float touchY = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                touchStartX = touchX;
                touchStartY = touchY;
                handled = true;
                scrollHandled = false;
                scrollIntercepted = false;
                bubbleTouched = state == VISIBLE && alpha > 0 && bubble.bitmapBounds.contains(touchX, touchY);
                break;
            case MotionEvent.ACTION_MOVE:
                handled = true;
                if (!scrollHandled) {
                    if (touchStartX != touchX || touchStartY != touchY) {
                        if (Math.abs(touchY - touchStartY) > Math.abs(touchX - touchStartX)) {
                            handled = false;
                        } else {
                            view.getParent().requestDisallowInterceptTouchEvent(true);
                            scrollIntercepted = true;
                        }
                        scrollHandled = true;
                    }
                }
                if (scrollIntercepted) {
                    if (bubbleTouched && !bubble.bitmapBounds.contains(touchX, touchY)) {
                        bubbleTouched = false;
                    }
                    if (!bubbleTouched) {
                        updateBubble(touchX, touchY);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                handled = true;
                if (anyChecked) {
                    if (state == VISIBLE && bubbleTouched) {
                        animateAlpha(false);
                        state = GONE;
                    } else {
                        updateBubble(touchX, touchY);
                    }
                }
                break;
        }
        return handled;
    }

    private void updateBubble(float touchX, float touchY) {
        if (!anyChecked)
            return;

        pointBuffer[0] = touchX;
        pointBuffer[1] = touchY;
        chartTransformer.transformPixelsToPoints(pointBuffer);

        int index = chartState.findIndexOfXValueInsideSelection(pointBuffer[0], ChartState.Rounding.Closest);
        if (currentIndex == index && state == VISIBLE)
            return;

        if (state == GONE) {
            animateAlpha(true);
            state = VISIBLE;
        } else {
            if (bubble.bitmapBounds.right < chartState.getBounds().left ||
                bubble.bitmapBounds.left > chartState.getBounds().right) {
                Utils.endAnimator(animator);
                alpha = dstAlpha = 0;
                animateAlpha(true);
            }
        }

        currentIndex = index;
        indexChanged = true;
        view.invalidate();
    }

    public void notifySizeChanged() {
        Utils.endAnimator(animator);
        selectionChanged = true;
    }

    public void notifyCheckedChanged() {
        updateAnyChecked();
        if (anyChecked) {
            if (dstAlpha == 0 && state == VISIBLE) {
                animateAlpha(true);
            }
        } else if (state == VISIBLE) {
            animateAlpha(false);
        }
        checkChanged = true;
    }

    public void notifySelectionChanged() {
        selectionChanged = true;
    }

    public void setLabelFormatter(ValueFormatter labelFormatter) {
        this.labelFormatter = labelFormatter;
    }

    public void restore(int index, float offset, int bubbleAlpha, BubbleState bubbleState) {
        if (index < 0)
            return;
        currentIndex = index;
        alpha = dstAlpha = bubbleAlpha;
        state = bubbleState;
        bubble.offset = offset;
        updateAnyChecked();
        checkChanged = true;
        indexChanged = false;
        selectionChanged = false;
    }

    private void updateAnyChecked() {
        for (LineData line : chartState.chartData.lines) {
            if (line.isChecked()) {
                anyChecked = true;
                return;
            }
        }
        anyChecked = false;
    }

    public void prepareBuffer() {
        if (chartState.chartData == null)
            return;
        if (alpha < 0)
            return;

        float x = chartState.chartData.xValues[currentIndex];
        int j = 0;
        dotsBuffer[j++] = x;
        dotsBuffer[j++] = 0;
        dotsBuffer[j++] = x;
        dotsBuffer[j++] = chartState.getChartHeight();
        for (LineData line : chartState.chartData.lines) {
            dotsBuffer[j++] = x;
            dotsBuffer[j++] = line.entries[currentIndex];
        }
        chartTransformer.transformPointsToPixels(dotsBuffer);
    }

    public void drawLine(Canvas canvas) {
        if (alpha == 0)
            return;

        linePaint.setAlpha(alpha);
        canvas.drawLine(
            dotsBuffer[0],
            dotsBuffer[1],
            dotsBuffer[2],
            dotsBuffer[3],
            linePaint
        );
    }

    public void drawDots(Canvas canvas) {
        if (chartState.chartData == null)
            return;
        if (alpha == 0)
            return;

        List<LineState> lineStates = chartState.getLineStates();
        for (int i = 0; i < lineStates.size(); i++) {
            LineState lineState = lineStates.get(i);
            if (lineState.getAlpha() > 0) {

                float x = dotsBuffer[4 + i * 2];
                float y = dotsBuffer[4 + i * 2 + 1];

                boolean withAlpha = lineState.getAlpha() < 255 || alpha < 255;
                if (withAlpha) {
                    canvas.saveLayerAlpha(
                        x - fullRadius,
                        y - fullRadius,
                        x + fullRadius,
                        y + fullRadius,
                        (int) (alpha / 255f * lineState.getAlpha()),
                        Canvas.ALL_SAVE_FLAG
                    );
                }

                dotPaint.setStyle(Paint.Style.FILL);
                dotPaint.setColor(ChartTheme.chartBackgroundColor);
                canvas.drawCircle(x, y, dotRadius, dotPaint);

                dotPaint.setStyle(Paint.Style.STROKE);
                dotPaint.setColor(lineState.getLineData().color);
                canvas.drawCircle(x, y, dotRadius, dotPaint);

                if (withAlpha) {
                    canvas.restore();
                }
            }
        }
    }

    public void drawBubble(Canvas canvas) {
        if (alpha == 0)
            return;

        if ((indexChanged || checkChanged) && anyChecked && state == VISIBLE) {
            bubble.measureSizes();
        }

        if ((indexChanged || checkChanged || themeChanged) && anyChecked && state == VISIBLE) {
            bubble.refreshBitmap();
        }

        if (indexChanged) {
            bubble.measureOffset();
        }

        if ((indexChanged || checkChanged || selectionChanged) && anyChecked) {
            bubble.measureBounds();
        }

        indexChanged = checkChanged = selectionChanged = themeChanged = false;

        bubble.draw(canvas);
    }

    private void animateAlpha(boolean on) {
        int newAlpha = on ? 255 : 0;
        if (newAlpha == dstAlpha)
            return;
        Utils.cancelAnimator(animator);
        dstAlpha = newAlpha;
        int duration = Math.abs(dstAlpha - alpha) * ChartAnimator.ANIM_DURATION / 255;
        animator = ValueAnimator.ofInt(alpha, newAlpha);
        animator.setDuration(duration);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                alpha = (int) animation.getAnimatedValue();
                view.invalidate();
            }
        });
        animator.start();
    }


    public int getCurrentIndex() {
        return currentIndex;
    }

    public float getOffset() {
        return bubble.offset;
    }

    public int getAlpha() {
        return dstAlpha;
    }

    public BubbleState getState() {
        return state;
    }

    public void setYAxisFormatter(ValueFormatter formatter) {
        this.yAxisFormatter = formatter;
    }

    private class Bubble {
        final static int COLUMN_COUNT = 2;
        final float[] columnWidth = new float[COLUMN_COUNT];
        final float itemHeight;
        final float titleHeight;
        final float yValueHeight;
        final float labelHeight;
        final RectF bubbleRectF = new RectF();
        final RectF bitmapBounds = new RectF();
        private static final float defaultOffset = -0.2f;
        private final TextPaint labelPaint = new TextPaint();
        private final TextPaint yValuePaint = new TextPaint();
        private final TextPaint titlePaint = new TextPaint();
        private final Paint bubblePaint = new Paint();
        private final Paint bitmapPaint = new Paint();

        String label;
        float offset;
        Bitmap bitmap;
        Canvas bitmapCanvas;
        float bitmapWidth;
        float bitmapHeight;

        Bubble() {
            updateTheme();

            bubblePaint.setShadowLayer(
                bubbleShadowRadius,
                bubbleShadowDx,
                bubbleShadowDy,
                bubbleShadowColor
            );
            labelPaint.setTextSize(bubbleLabelTextSize);
            labelPaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            yValuePaint.setTextSize(bubbleYValueTextSize);
            yValuePaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            titlePaint.setTextSize(bubbleTitleTextSize);
            titlePaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));

            Paint.FontMetrics labelFm = labelPaint.getFontMetrics();
            Paint.FontMetrics yValueFm = labelPaint.getFontMetrics();
            Paint.FontMetrics titleFm = labelPaint.getFontMetrics();

            titleHeight = titleFm.descent - titleFm.ascent - titleFm.bottom;
            yValueHeight = yValueFm.descent - yValueFm.ascent - yValueFm.bottom;
            labelHeight = labelFm.descent - labelFm.ascent - labelFm.bottom;

            itemHeight = labelHeight + yValueHeight + bubbleItemInnerSpacing;
        }

        void updateTheme() {
            bubblePaint.setColor(ChartTheme.bubbleColor);
            titlePaint.setColor(ChartTheme.bubbleTitleColor);
        }

        void refreshBitmap() {
            prepareBitmap();
            drawOnBitmap();
        }

        void measureSizes() {
            Arrays.fill(columnWidth, 0f);
            int itemCount = 0;
            for (LineData line : chartState.chartData.lines) {
                if (!line.isChecked())
                    continue;

                int columnIndex = itemCount++ % COLUMN_COUNT;

                float yValue = line.entries[currentIndex];

                float itemWidth = Math.max(
                    yValuePaint.measureText(yAxisFormatter.getLabel(yValue)),
                    labelPaint.measureText(line.label)
                );

                columnWidth[columnIndex] = Math.max(
                    columnWidth[columnIndex],
                    itemWidth
                );
            }

            label = labelFormatter.getLabel(chartState.chartData.xValues[currentIndex]);
            float titleWidth = titlePaint.measureText(label);
            int columnCount = Math.min(itemCount, COLUMN_COUNT);
            float bubbleWidth = bubbleHorizontalPadding * 2 +
                Math.max(arraySum(columnWidth) + (columnCount - 1) * bubbleHorizontalPadding, titleWidth);
            int rowCount = itemCount / COLUMN_COUNT + (itemCount % COLUMN_COUNT > 0 ? 1 : 0);
            float bubbleHeight = 2 * bubbleVerticalPadding + titleHeight + bubbleVerticalSpacing +
                rowCount * itemHeight + (rowCount - 1) * bubbleVerticalSpacing;
            bubbleRectF.set(
                0,
                0,
                bubbleWidth,
                bubbleHeight
            );
            bitmapWidth = bubbleWidth + bubbleShadowOffsets.left + bubbleShadowOffsets.right;
            bitmapHeight = bubbleHeight + bubbleShadowOffsets.top + bubbleShadowOffsets.bottom;
        }

        float arraySum(float[] array) {
            float sum = 0;
            for (float v : array) {
                sum += v;
            }
            return sum;
        }

        void measureOffset() {
            pointBuffer[0] = chartState.chartData.xValues[currentIndex];
            chartTransformer.transformPointsToPixels(pointBuffer);

            float offsetPx = defaultOffset * bitmapWidth;
            float left = pointBuffer[0] + offsetPx;
            float right = left + bitmapWidth;

            float fromPx = chartState.getBounds().left + bubbleHorizontalMargin;
            float toPx = chartState.getBounds().right - bubbleHorizontalMargin;
            if (left < fromPx) {
                offsetPx += (fromPx - left);
            } else if (right > toPx) {
                offsetPx -= (right - toPx);
            }

            this.offset = offsetPx / bitmapWidth;
        }

        void measureBounds() {
            pointBuffer[0] = chartState.chartData.xValues[currentIndex];
            chartTransformer.transformPointsToPixels(pointBuffer);
            float offsetPx = offset * bitmapWidth;
            bitmapBounds.set(
                pointBuffer[0] + offsetPx,
                0,
                pointBuffer[0] + offsetPx + bitmapWidth,
                bitmapHeight
            );
        }

        void draw(Canvas canvas) {
            if (bitmapBounds.right < chartState.getBounds().left ||
                bitmapBounds.left > chartState.getBounds().right)
                return;

            bitmapPaint.setAlpha(alpha);
            canvas.drawBitmap(bitmap, bitmapBounds.left, bitmapBounds.top, bitmapPaint);
        }

        void prepareBitmap() {
            if (bitmap == null || bitmap.getWidth() != (int) bitmapWidth || bitmap.getHeight() != (int) bitmapHeight) {
                if (bitmap != null) {
                    bitmap.recycle();
                }
                bitmap = Bitmap.createBitmap((int) bitmapWidth, (int) bitmapHeight, Bitmap.Config.ARGB_8888);
                bitmapCanvas = new Canvas(bitmap);
            } else {
                bitmap.eraseColor(Color.TRANSPARENT);
            }
        }

        void drawOnBitmap() {
            Canvas canvas = bitmapCanvas;
            int restoreCount = canvas.save();
            canvas.translate(bubbleShadowOffsets.left, bubbleShadowOffsets.top);
            canvas.drawRoundRect(
                bubbleRectF,
                bubbleCornerRadius,
                bubbleCornerRadius,
                bubblePaint
            );

            canvas.translate(
                bubbleHorizontalPadding,
                bubbleVerticalPadding
            );
            canvas.drawText(label, 0f, titleHeight, titlePaint);
            canvas.translate(0f, titleHeight + bubbleVerticalSpacing);
            int j = 0;
            for (LineData line : chartState.chartData.lines) {
                if (line.isChecked()) {
                    int columnIndex = j % COLUMN_COUNT;
                    if (columnIndex == 0)
                        canvas.save();
                    if (columnIndex >= 1) {
                        canvas.translate(columnWidth[columnIndex - 1] + bubbleHorizontalPadding, 0f);
                    }
                    canvas.save();
                    yValuePaint.setColor(line.color);
                    labelPaint.setColor(line.color);
                    float yValue = line.entries[currentIndex];
                    canvas.drawText(yAxisFormatter.getLabel(yValue), 0, yValueHeight, yValuePaint);
                    canvas.translate(0f, yValueHeight + bubbleItemInnerSpacing);
                    canvas.drawText(line.label, 0, labelHeight, labelPaint);
                    canvas.restore();
                    j++;
                    if (j % COLUMN_COUNT == 0) {
                        canvas.restore();
                        canvas.translate(0f, itemHeight + bubbleVerticalSpacing);
                    }
                }
            }
            canvas.restoreToCount(restoreCount);
        }
    }


    public enum BubbleState {
        VISIBLE,
        GONE;

        private static SparseArray<BubbleState> entriesMap;

        static {
            entriesMap = new SparseArray<>();
            for (BubbleState value : BubbleState.values()) {
                entriesMap.put(value.ordinal(), value);
            }
        }

        public static BubbleState fromInt(int ordinal) {
            return entriesMap.get(ordinal);
        }
    }
}

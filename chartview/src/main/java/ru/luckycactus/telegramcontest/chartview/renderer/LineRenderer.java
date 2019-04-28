package ru.luckycactus.telegramcontest.chartview.renderer;

import android.graphics.Canvas;
import android.graphics.Paint;

import ru.luckycactus.telegramcontest.chartview.state.ChartState;
import ru.luckycactus.telegramcontest.chartview.state.ChartTransformer;
import ru.luckycactus.telegramcontest.chartview.state.LineState;

class LineRenderer {

    private final ChartState chartState;
    private final LineState lineState;
    private final ChartTransformer chartTransformer;
    private final float[] originalBuffer;
    private final float[] screenBuffer;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /**
     * @param cacheLineToBuffer - true, if you need to cache the initial state of the line
     *                          (before the transformation) into a separate buffer
     *                          (improves performance but requires additional memory).
     */
    LineRenderer(
        ChartState chartState,
        LineState lineState,
        ChartTransformer chartTransformer,
        boolean cacheLineToBuffer,
        float lineWidth
    ) {
        this.chartState = chartState;
        this.lineState = lineState;
        this.chartTransformer = chartTransformer;

        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(lineState.getLineData().color);
        paint.setStrokeWidth(lineWidth);
        paint.setStrokeCap(Paint.Cap.SQUARE);

        int pointCount = lineState.getLineData().entries.length;
        int bufferSize = (pointCount - 1) * 4;

        screenBuffer = new float[bufferSize];
        if (cacheLineToBuffer) {
            originalBuffer = new float[bufferSize];
            preparePointsBuffer(originalBuffer, 0, pointCount - 1);
        } else {
            originalBuffer = null;
        }
    }

    void draw(Canvas canvas, int fromIndex, int toIndex) {
        int alpha = lineState.getAlpha();
        if (alpha == 0)
            return;

        paint.setAlpha(alpha);
        int pointCount = (toIndex - fromIndex) * 2;
        int bufferStartIndex = fromIndex * 4;
        if (originalBuffer != null) {
            chartTransformer.transformPointsToPixels(
                originalBuffer,
                bufferStartIndex,
                screenBuffer,
                0,
                pointCount
            );
        } else {
            preparePointsBuffer(screenBuffer, fromIndex, toIndex);
            chartTransformer.transformPointsToPixels(screenBuffer, pointCount);
        }

        canvas.drawLines(screenBuffer, 0, pointCount * 2, paint);
    }

    private void preparePointsBuffer(float[] buffer, int fromIndex, int toIndex) {
        int j = 0;
        float e1 = lineState.getLineData().entries[fromIndex];
        float x1 = chartState.chartData.xValues[fromIndex];
        float e2;
        float x2;
        for (int i = fromIndex; i < toIndex; i++) {
            e2 = lineState.getLineData().entries[i + 1];
            x2 = chartState.chartData.xValues[i + 1];

            buffer[j++] = x1;
            buffer[j++] = e1;
            buffer[j++] = x2;
            buffer[j++] = e2;

            e1 = e2;
            x1 = x2;
        }
    }
}

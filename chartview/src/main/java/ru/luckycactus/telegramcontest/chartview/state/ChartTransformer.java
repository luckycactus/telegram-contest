package ru.luckycactus.telegramcontest.chartview.state;

import android.graphics.Matrix;

public class ChartTransformer {

    private final ChartState state;
    private final Matrix pointToPxMatrix = new Matrix();
    private final Matrix pxToPointMatrix = new Matrix();
    private boolean pxToValueInvalidated = true;

    public ChartTransformer(ChartState state) {
        this.state = state;
    }

    public void prepareMatrix() {
        if (!state.isInitialized())
            return;

        pointToPxMatrix.reset();
        pointToPxMatrix.postTranslate(-state.getSelectionStartValue(), 0f);
        pointToPxMatrix.postScale(
            state.getContentWidth() / state.getChartWidth(),
            state.getWindowHeight() / state.getChartHeight()
        );
        pointToPxMatrix.postScale(
            1f,
            -1f,
            state.getContentWidth() / 2f,
            state.getWindowHeight() / 2f
        );
        pointToPxMatrix.postTranslate(state.getHorizontalPaddingPx(), state.getBounds().top);

        pxToValueInvalidated = true;
    }

    public void transformPointsToPixels(float[] points) {
        pointToPxMatrix.mapPoints(points);
    }

    public void transformPointsToPixels(float[] points, int pointCount) {
        pointToPxMatrix.mapPoints(points, 0, points, 0, pointCount);
    }

    public void transformPointsToPixels(float[] src, int srcIndex,
                                        float[] dst, int dstIndex,
                                        int pointCount) {
        pointToPxMatrix.mapPoints(dst, dstIndex, src, srcIndex, pointCount);
    }

    public void transformPixelsToPoints(float[] pixels) {
        if (pxToValueInvalidated) {
            pointToPxMatrix.invert(pxToPointMatrix);
            pxToValueInvalidated = false;
        }
        pxToPointMatrix.mapPoints(pixels);
    }
}

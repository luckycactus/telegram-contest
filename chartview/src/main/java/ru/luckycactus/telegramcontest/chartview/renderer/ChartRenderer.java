package ru.luckycactus.telegramcontest.chartview.renderer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import ru.luckycactus.telegramcontest.chartview.R;
import ru.luckycactus.telegramcontest.chartview.common.ChartTheme;
import ru.luckycactus.telegramcontest.chartview.state.ChartState;
import ru.luckycactus.telegramcontest.chartview.state.ChartTransformer;
import ru.luckycactus.telegramcontest.chartview.state.LineState;
import ru.luckycactus.telegramcontest.chartview.view.ChartView;

import static ru.luckycactus.telegramcontest.chartview.common.Utils.getThemeColorOrThrow;

public class ChartRenderer {

    private static final boolean CACHE_LINE_TO_BUFFER = true;

    private final View view;
    private final ChartState chartState;
    private final ChartTransformer chartTransformer;
    private final List<LineRenderer> lines = new ArrayList<>();
    private final float lineWidth;

    private boolean bitmapCacheEnabled;
    private boolean bitmapCacheInvalidated = true; //true if it needs to redraw bitmap
    // workaround for redraw bitmap a frame later after the end of animation.
    // It can help to avoid possible lag at the end of animation.
    private boolean oneFramePassed = true;
    private Bitmap bitmap;
    private Canvas bitmapCanvas;

    public ChartRenderer(
        View view,
        ChartState chartState,
        ChartTransformer chartTransformer,
        float lineWidth
    ) {
        this.view = view;
        this.chartState = chartState;
        this.chartTransformer = chartTransformer;
        this.lineWidth = lineWidth;
    }

    public void init() {
        lines.clear();
        for (LineState lineState : this.chartState.getLineStates()) {
            lines.add(
                new LineRenderer(
                    chartState,
                    lineState,
                    chartTransformer,
                    CACHE_LINE_TO_BUFFER,
                    lineWidth
                )
            );
        }
        invalidateBitmapCache();
        view.invalidate();
    }

    public void notifySizeChanged() {
        invalidateBitmapCache();
    }

    public void invalidateBitmapCache() {
        bitmapCacheInvalidated = true;
        oneFramePassed = false;
    }

    public void disableBitmapCache() {
        bitmapCacheEnabled = false;
        invalidateBitmapCache();
    }

    public void enableBitmapCache() {
        bitmapCacheEnabled = true;
    }

    public void draw(Canvas canvas) {
        if (!bitmapCacheEnabled || bitmapCacheInvalidated) {
            Canvas c;
            if (!bitmapCacheEnabled || !oneFramePassed) {
                c = canvas;
            } else {
                prepareBitmap();
                c = bitmapCanvas;
                bitmapCacheInvalidated = false;
            }
            int fromIndex = Math.max(
                0,
                chartState.getFirstVisibleIndex() - 1
            );
            int toIndex = Math.min(
                chartState.getXValuesCount() - 1,
                chartState.getLastVisibleIndex() + 1
            );
            for (LineRenderer line : lines) {
                line.draw(c, fromIndex, toIndex);
            }
        }
        if (bitmapCacheEnabled) {
            if (!oneFramePassed) {
                oneFramePassed = true;
            } else {
                canvas.drawBitmap(bitmap, 0, 0, null);
            }
        }
    }

    public void updateTheme() {
        invalidateBitmapCache();
        oneFramePassed = true;
    }

    private void prepareBitmap() {
        int viewWidth = view.getWidth();
        int viewHeight = view.getHeight();
        if (bitmap == null || bitmap.getWidth() != viewWidth || bitmap.getHeight() != viewHeight) {
            if (bitmap != null) {
                bitmap.recycle();
            }
            bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.RGB_565);
            bitmapCanvas = new Canvas(bitmap);
        }
        bitmap.eraseColor(ChartTheme.chartBackgroundColor);
    }
}

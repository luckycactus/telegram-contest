package ru.luckycactus.telegramcontest.chartview.renderer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

import java.util.HashSet;
import java.util.Set;

import ru.luckycactus.telegramcontest.chartview.R;
import ru.luckycactus.telegramcontest.chartview.common.ChartTheme;
import ru.luckycactus.telegramcontest.chartview.common.Utils;
import ru.luckycactus.telegramcontest.chartview.common.ValueFormatter;
import ru.luckycactus.telegramcontest.chartview.common.WeakSingletonHolder;
import ru.luckycactus.telegramcontest.chartview.common.ObjectPool;
import ru.luckycactus.telegramcontest.chartview.state.ChartAnimator;
import ru.luckycactus.telegramcontest.chartview.state.ChartState;
import ru.luckycactus.telegramcontest.chartview.state.ChartTransformer;

import static ru.luckycactus.telegramcontest.chartview.common.Utils.getThemeColorOrThrow;
import static ru.luckycactus.telegramcontest.chartview.common.Utils.sp;

public class YAxisRenderer {

    private static final float topPadding = Utils.dpF(8);
    private static final float textSize = sp(13);
    static final float textBottomPadding = Utils.dpF(8); // spacing between line and text
    //spacing between top line and chart's top
    private static final float topLineTopPadding = topPadding + textSize + textBottomPadding;
    private static final float lineWidth = Utils.dpF(1);

    private static final int LINE_COUNT = 5;

    private final View view;
    final ChartState chartState;
    final ChartTransformer transformer;
    private final LineBufferPool lineBufferPool = LineBufferPool.getInstance();
    private final GridPool gridPool = GridPool.getInstance();
    final Paint linePaint = new Paint();
    final Paint textPaint = new Paint();

    private final Set<Grid> disappearingGrids = new HashSet<>();
    private Grid currentGrid; // Теущая сетка

    ValueFormatter formatter = ValueFormatter.DEFAULT;
    // keep last visible entry and step to avoid unnecessary grid re-creation
    private float maxVisibleEntry = 0;
    private float step = 0;

    public YAxisRenderer(View view, ChartState chartState, ChartTransformer transformer) {
        this.view = view;
        this.chartState = chartState;
        this.transformer = transformer;

        linePaint.setStrokeWidth(lineWidth);
        textPaint.setTextSize(textSize);
        updateTheme();
    }

    public void init() {
        if (!chartState.isInitialized())
            return;

        maxVisibleEntry = step = 0;
        endAnimators();
        disappearingGrids.clear();
        if (currentGrid != null) {
            gridPool.release(currentGrid);
            currentGrid = null;
            // grids from disappearingGrids already released on onAnimationEnd call
        }
        updateMaxVisibleEntry();
        updateStep();
        currentGrid = gridPool.acquire(this, step, 255);
    }

    public void updateTheme() {
        linePaint.setColor(ChartTheme.yAxisLineColor);
        textPaint.setColor(ChartTheme.labelColor);
    }

    public void notifySizeChanged() {
        init();
    }

    public void animateChanges() {
        if (!chartState.isInitialized())
            return;

        if (!updateMaxVisibleEntry())
            return;

        if (!updateStep())
            return;

        removeCurrentGridWithAnimation();

        final Grid grid = gridPool.acquire(this, step, 0);
        currentGrid = grid;
        currentGrid.animator = ValueAnimator.ofInt(0, 255);
        currentGrid.animator.setDuration(ChartAnimator.ANIM_DURATION);
        currentGrid.animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                grid.alpha = (int) animation.getAnimatedValue();
                view.invalidate();
            }
        });
        currentGrid.animator.start();
    }

    public void prepareBuffers() {
        if (currentGrid != null) {
            currentGrid.prepareBuffer();
        }
        for (Grid grid : disappearingGrids) {
            grid.prepareBuffer();
        }
    }

    public void releaseBuffers() {
        if (currentGrid != null) {
            currentGrid.releaseBuffer();
        }
        for (Grid grid : disappearingGrids) {
            grid.releaseBuffer();
        }
    }

    public void drawLines(Canvas canvas) {
        if (currentGrid != null) {
            currentGrid.drawZeroLine(canvas);
            currentGrid.drawLines(canvas);
        }
        for (Grid grid : disappearingGrids) {
            grid.drawLines(canvas);
        }
    }

    public void drawNumbers(Canvas canvas) {
        if (currentGrid != null) {
            currentGrid.drawZeroNumber(canvas);
            currentGrid.drawNumbers(canvas);
        }
        for (Grid grid : disappearingGrids) {
            grid.drawNumbers(canvas);
        }
    }

    public void setFormatter(ValueFormatter formatter) {
        this.formatter = formatter;
    }


    private boolean updateMaxVisibleEntry() {
        float maxVisibleEntry = chartState.getMaxVisibleEntry();
        if (this.maxVisibleEntry == maxVisibleEntry)
            return false;

        this.maxVisibleEntry = maxVisibleEntry;
        return true;
    }

    private boolean updateStep() {
        float step = calculateStep();
        if (this.step == step)
            return false;

        this.step = step;
        return true;
    }

    private float calculateStep() {
        float yFactor = chartState.getMaxVisibleEntry() / chartState.getWindowHeight();
        float topValue = chartState.getMaxVisibleEntry() - topLineTopPadding * yFactor;
        float maxStep = topValue / (LINE_COUNT);
        float minStep = topValue / (LINE_COUNT + 0.6f);
        return findStep(minStep, maxStep);
    }

    // in current implementation if minStep and maxStep differ only in fraction then result will be maxStep.
    // This case needs extra handling.
    private float findStep(float minStep, float maxStep) {
        int min = (int) Math.ceil(minStep);
        int max = (int) maxStep;
        if (min > max) {
            return maxStep;
        }

        int p = Utils.getFloorPowerOfTen(max);

        // Decrease p while older digits are equal.
        // Example: max = 13862, min = 13625,p = 1000.
        while (max / p == min / p && p > 1) {
            p /= 10;
        }
        p *= 10;

        // Now we iterate max step's digits and try replace it by 0 or 5 and replace the others by 0.
        // If the result number is between min and max, then it is the result.
        // Example: max = 13862, min = 13625. Numbers: 13000, 13500, 13800. Result: 13800
        int step = 0;
        while (p > 0) {
            step = max / p * p;
            if (step >= min && step < max) {
                return step;
            }
            p /= 10;
            step += 5 * p;
            if (step >= min && step < max) {
                return step;
            }
        }

        return step;
    }

    private void endAnimators() {
        if (currentGrid != null) {
            Utils.endAnimator(currentGrid.animator);
        }
        for (Grid grid : disappearingGrids) {
            Utils.endAnimator(grid.animator);
        }
    }

    private void removeCurrentGridWithAnimation() {
        if (currentGrid == null)
            return;

        final Grid grid = currentGrid;
        currentGrid = null;

        Utils.cancelAnimator(grid.animator);
        disappearingGrids.add(grid);

        int duration = grid.alpha * ChartAnimator.ANIM_DURATION / 255;
        grid.animator = ValueAnimator.ofInt(grid.alpha, 0);
        grid.animator.setDuration(duration);
        grid.animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                grid.alpha = (int) animation.getAnimatedValue();
                view.invalidate();
            }
        });
        grid.animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                disappearingGrids.remove(grid);
                gridPool.release(grid);
            }
        });
        grid.animator.start();
    }

    static class Grid {

        private ValueAnimator animator;
        private YAxisRenderer yAxisRenderer;
        private int alpha;
        private float[] lineBuffer;
        private float[] rows = new float[LINE_COUNT];

        void init(
            YAxisRenderer yAxisRenderer,
            float step,
            int alpha
        ) {
            this.yAxisRenderer = yAxisRenderer;
            this.alpha = alpha;
            for (int i = 0; i < LINE_COUNT; i++) {
                float y = (i + 1) * step;
                rows[i] = y;
            }
        }

        void prepareBuffer() {
            lineBuffer = yAxisRenderer.lineBufferPool.acquire();
            int j = 0;
            for (int i = 0; i < LINE_COUNT; i++) {
                float y = rows[i];
                lineBuffer[j++] = yAxisRenderer.chartState.getSelectionStartValue();
                lineBuffer[j++] = y;
                lineBuffer[j++] = yAxisRenderer.chartState.getSelectionEndValue();
                lineBuffer[j++] = y;
            }
            yAxisRenderer.transformer.transformPointsToPixels(lineBuffer);
        }

        void releaseBuffer() {
            yAxisRenderer.lineBufferPool.release(lineBuffer);
        }

        void drawZeroLine(Canvas canvas) {
            yAxisRenderer.linePaint.setAlpha(255);
            float y = yAxisRenderer.chartState.getBounds().bottom;
            canvas.drawLine(lineBuffer[0], y, lineBuffer[2], y, yAxisRenderer.linePaint);
        }

        void drawLines(Canvas canvas) {
            yAxisRenderer.linePaint.setAlpha(alpha);
            canvas.drawLines(lineBuffer, yAxisRenderer.linePaint);
        }

        void drawZeroNumber(Canvas canvas) {
            yAxisRenderer.textPaint.setAlpha(255);
            float x = lineBuffer[0];
            float y = yAxisRenderer.chartState.getBounds().bottom - yAxisRenderer.textBottomPadding;
            canvas.drawText("0", x, y, yAxisRenderer.textPaint);
        }

        void drawNumbers(Canvas canvas) {
            yAxisRenderer.textPaint.setAlpha(alpha);
            for (int i = 0; i < rows.length; i++) {
                float x = lineBuffer[i * 4];
                float y = lineBuffer[i * 4 + 1] - yAxisRenderer.textBottomPadding;
                canvas.drawText(yAxisRenderer.formatter.getLabel(rows[i]), x, y, yAxisRenderer.textPaint);
            }
        }
    }

    private static class GridPool extends ObjectPool<Grid> {

        private static WeakSingletonHolder<GridPool> provider =
            new WeakSingletonHolder<GridPool>() {
                @Override
                public GridPool create() {
                    return new GridPool();
                }
            };

        static GridPool getInstance() {
            return provider.getInstance();
        }


        Grid acquire(YAxisRenderer yAxisRenderer, float step, int alpha) {
            Grid grid = acquire();
            grid.init(yAxisRenderer, step, alpha);
            return grid;
        }

        @Override
        protected Grid create() {
            return new Grid();
        }
    }

    // We can use same line buffers for different charts.
    // At draw start we acquire buffer and release it at the end
    public static class LineBufferPool extends ObjectPool<float[]> {

        private static WeakSingletonHolder<LineBufferPool> provider =
            new WeakSingletonHolder<LineBufferPool>() {
                @Override
                public LineBufferPool create() {
                    return new LineBufferPool();
                }
            };

        static LineBufferPool getInstance() {
            return provider.getInstance();
        }

        @Override
        protected float[] create() {
            return new float[LINE_COUNT * 4];
        }
    }
}

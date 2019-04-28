package ru.luckycactus.telegramcontest.chartview.state;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;

import java.util.Map;
import java.util.WeakHashMap;

import ru.luckycactus.telegramcontest.chartview.common.Utils;
import ru.luckycactus.telegramcontest.chartview.model.LineData;

import static ru.luckycactus.telegramcontest.chartview.common.Utils.cancelAnimator;

public class ChartAnimator {

    public static final int ANIM_DURATION = 300;

    private final View view;
    private final ChartState chartState;
    private final ChartTransformer chartTransformer;
    private ValueAnimator chartHeightAnimator;
    private WeakHashMap<LineData, LineBundle> lineBundles = new WeakHashMap<>();
    private int runningAnimationsCount;
    private AnimationsListener animationsListener;
    private float dstChartHeight;

    public ChartAnimator(View view, ChartState chartState, ChartTransformer chartTransformer) {
        this.view = view;
        this.chartState = chartState;
        this.chartTransformer = chartTransformer;
    }

    public void init() {
        lineBundles.clear();
        for (LineState lineState : chartState.getLineStates()) {
            lineBundles.put(lineState.lineData, new LineBundle(lineState));
        }
        dstChartHeight = chartState.getMaxVisibleEntry();
    }

    public boolean animateChartSize() {
        float newChartHeight = chartState.getMaxVisibleEntry();

        if (dstChartHeight == newChartHeight)
            return false;
        dstChartHeight = newChartHeight;

        Utils.cancelAnimator(chartHeightAnimator);

        chartHeightAnimator =
            ValueAnimator.ofFloat(
                chartState.getChartHeight(),
                newChartHeight
            );
        chartHeightAnimator.setDuration(ANIM_DURATION);
        chartHeightAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                chartState.setChartHeight((float) animation.getAnimatedValue());
                chartTransformer.prepareMatrix();
                view.invalidate();
            }
        });
        chartHeightAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                chartTransformer.prepareMatrix();
                view.invalidate();
                decrementRunningAnimations();
            }
        });

        incrementRunningAnimations();
        chartHeightAnimator.start();
        return true;
    }

    public void animateLineVisibility(LineData lineData) {
        final LineBundle bundle = lineBundles.get(lineData);

        int dstAlpha = lineData.isChecked() ? 255 : 0;
        if (bundle.dstAlpha == dstAlpha)
            return;

        bundle.dstAlpha = dstAlpha;
        cancelAnimator(bundle.animator);

        bundle.animator = ValueAnimator.ofInt(bundle.lineState.getAlpha(), dstAlpha);
        bundle.animator.setDuration(ChartAnimator.ANIM_DURATION);
        bundle.animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                bundle.lineState.setAlpha((int) animation.getAnimatedValue());
                view.invalidate();
            }
        });
        bundle.animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                decrementRunningAnimations();
            }
        });
        incrementRunningAnimations();
        bundle.animator.start();
    }

    public void setAnimationsListener(AnimationsListener animationsListener) {
        this.animationsListener = animationsListener;
    }

    private void endAlphaAnimators() {
        for (Map.Entry<LineData, LineBundle> entry : lineBundles.entrySet()) {
            Utils.endAnimator(entry.getValue().animator);
        }
    }

    private void incrementRunningAnimations() {
        if (runningAnimationsCount == 0) {
            if (animationsListener != null) {
                animationsListener.onAnimationsStart();
            }
        }
        runningAnimationsCount++;
    }

    private void decrementRunningAnimations() {
        runningAnimationsCount--;
        if (runningAnimationsCount == 0) {
            if (animationsListener != null) {
                animationsListener.onAnimationsEnd();
            }
        }
    }

    public void jumpToTargetState() {
        Utils.endAnimator(chartHeightAnimator);
        endAlphaAnimators();
    }

    private static class LineBundle {
        final LineState lineState;
        ValueAnimator animator;
        int dstAlpha;

        LineBundle(LineState lineState) {
            this.lineState = lineState;
            this.dstAlpha = lineState.getAlpha();
        }
    }

    public interface AnimationsListener {
        void onAnimationsStart();

        void onAnimationsEnd();
    }
}

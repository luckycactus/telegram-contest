package ru.luckycactus.telegramcontest.chartview.renderer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextPaint;
import android.view.View;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import ru.luckycactus.telegramcontest.chartview.common.ChartTheme;
import ru.luckycactus.telegramcontest.chartview.common.Utils;
import ru.luckycactus.telegramcontest.chartview.common.ValueFormatter;
import ru.luckycactus.telegramcontest.chartview.state.ChartAnimator;
import ru.luckycactus.telegramcontest.chartview.state.ChartState;
import ru.luckycactus.telegramcontest.chartview.state.ChartTransformer;

import static ru.luckycactus.telegramcontest.chartview.common.Utils.sp;

// The first label is left-aligned, the last one is right-aligned and the others are center-aligned

// At the initialization we do pre-processing:
// 1) Find the number of labels, that fit on screen on full selection
// 2) Find the array step for that labels' count
// 3) Calculate the chart width at which there will be enough free space to fit
// middle label between the first two
// 4) Keep the step and the calculated width as the min width for that step (Let's call it a Cluster).
// 5) Divide step by two, return to step 3 and repeat after we get step = 1 (at which all labels are visible)
// In this way with a decrease of selection the old labels will remain in their places,
// and new ones will appear between them.

// Every cluster has its own alpha.
// During draw we iterate clusters from the biggest to the current.
// Keep processed array indexes to skip them later during processing of next clusters with smaller step.
// Recalculate cluster index on selection change. If it is not equal to the current one,
// then animate (dis)appearance of new (or excess) clusters
public class XAxisRenderer {

    private static final float textSize = sp(13f);
    private final float extraLabelSpacing = Utils.dpF(8f);

    private final View view;
    private final ChartState chartState;
    private final ChartTransformer chartTransformer;
    private final TextPaint textPaint = new TextPaint();
    private final float[] pointBuffer = new float[2];
    private final float textHeight;
    private final float textBottom;
    private ValueFormatter formatter = ValueFormatter.DEFAULT;

    private List<Cluster> clusters = new ArrayList<>();
    private float labelChartWidth; // width of label in terms of chart values
    private float extraChartSpacing; // min space between labels in terms of chart values
    private int currentClusterIndex;
    private int visibleClusterIndex; // for animating of disappearing clusters

    // BitSet to keeping whether the label has already been drawn
    private BitSet bitSet = new BitSet();

    public XAxisRenderer(View view, ChartState chartState, ChartTransformer chartTransformer) {
        this.view = view;
        this.chartState = chartState;
        this.chartTransformer = chartTransformer;
        updateTheme();
        textPaint.setTextSize(textSize);

        Paint.FontMetrics fm = textPaint.getFontMetrics();
        textHeight = fm.bottom - fm.top + fm.leading;
        textBottom = fm.bottom;
    }

    public void init() {
        finishAnimators();
        measure();
    }

    public void updateTheme() {
        textPaint.setColor(ChartTheme.labelColor);
    }

    public void notifySizeChanged() {
        measure();
    }

    private void measure() {
        if (!chartState.isInitialized())
            return;

        float[] xValues = chartState.chartData.xValues;
        if (xValues == null)
            return;

        if (chartState.getContentWidth() == 0)
            return;

        if (!clusters.isEmpty()) {
            finishAnimators();
            clusters.clear();
        }

        // how many x axis point in one pixel
        float xFactor = chartState.getXValuesRange() / chartState.getContentWidth();

        int xCount = xValues.length;
        String sampleLabel = "Nov 17"; //todo

        float labelWidth = textPaint.measureText(sampleLabel);
        labelChartWidth = xFactor * labelWidth;
        extraChartSpacing = xFactor * extraLabelSpacing;

        // calculate the number of labels, that fit on screen on full selection
        int initialLabelCount = 2;
        float step;
        float firstLabelSpace;
        do {
            initialLabelCount++;
            // calculate array step
            step = (xCount - 1) / (initialLabelCount - 1f);
            // calculate the distance between two first points corresponding to the current step
            firstLabelSpace = xValues[Math.round(step)] - xValues[0];
            // keep increase the label count until we get lower the min distance.
            // There should be enough space between the points to fit: the entire first label
            // (because it is left-aligned), half of the second label (it is center-aligned)
            // and extra label spacing.
        } while (firstLabelSpace >= labelChartWidth * 1.5f + extraChartSpacing);
        initialLabelCount--;
        step = (xCount - 1) / (initialLabelCount - 1f);


        boolean clusterIndexFound = false; // during calculating clusters at the same time we can find the current one
        // l & r - indexes of labels, between which we will insert new one
        int l = 0;
        int r = Math.round(step);
        while (step >= 1f) {
            float nextStep = step > 1f ? Math.max(1f, step / 2) : 0;
            float minChartWidth;
            if (step > 1) {
                int m = l + Math.round(nextStep); // index of middle element between l & r


                // To insert the middle label we need it to not overlap the labels with indices l and r.
                // Calculate minimum distances between points xValues[l] and xValues[m], xValues[m] and xValues[r].
                // We can insert the label when chart width <= the smallest of them.

                float minWidthFromLtoM = (l == 0 ? labelWidth : (labelWidth / 2)) + extraLabelSpacing + labelWidth / 2;
                float minWidthFromMtoR = labelWidth / 2 + extraLabelSpacing + labelWidth / 2;
                minChartWidth = Math.min(
                    chartState.getContentWidth() * (xValues[m] - xValues[l]) / minWidthFromLtoM,
                    chartState.getContentWidth() * (xValues[r] - xValues[m]) / minWidthFromMtoR
                );

                r = m;
                // if next step > 1 but there is no more labels between l and r,
                // then we should find next window
                if (r <= l + 1 && nextStep > 1f) {
                    int p = 2;
                    while (r <= l + 1) {
                        l = r;
                        r = Math.round(nextStep * p++);
                    }
                }
            } else {
                minChartWidth = 0f;
            }

            Cluster cluster;
            if (clusters.isEmpty() || clusters.get(clusters.size() - 1).minChartWidth > minChartWidth) {
                cluster = new Cluster(step, minChartWidth);

                clusters.add(cluster);
                cluster.alpha = cluster.dstAlpha = clusterIndexFound ? 0 : 255;
                if (chartState.getChartWidth() >= minChartWidth && !clusterIndexFound) {
                    currentClusterIndex = clusters.size() - 1;
                    visibleClusterIndex = clusters.size() - 1;
                    clusterIndexFound = true;
                }
            }

            step = nextStep;
        }
    }

    public void animateChanges() {
        if (clusters.isEmpty())
            return;

        int newClusterIndex = findClusterIndex();

        if (newClusterIndex == currentClusterIndex)
            return;

        if (newClusterIndex < currentClusterIndex) {
            for (int i = newClusterIndex + 1; i <= currentClusterIndex; i++) {
                animateCluster(i, false);
            }
        } else {
            for (int i = currentClusterIndex + 1; i <= newClusterIndex; i++) {
                animateCluster(i, true);
            }
            this.visibleClusterIndex = Math.max(newClusterIndex, visibleClusterIndex);
        }

        this.currentClusterIndex = newClusterIndex;
    }

    public void setFormatter(ValueFormatter formatter) {
        this.formatter = formatter;
    }

    public float getTextHeight() {
        return textHeight;
    }

    public void draw(Canvas canvas) {
        if (clusters.isEmpty())
            return;

        float[] xValues = chartState.chartData.xValues;
        int xCount = xValues.length;

        bitSet.clear();

        float from = chartState.getFirstVisibleValue() - (labelChartWidth + extraChartSpacing) / 2f;
        float to = chartState.getLastVisibleValue() + (labelChartWidth + extraChartSpacing) / 2f;

        for (int i = 0; i <= visibleClusterIndex; i++) {
            Cluster cluster = clusters.get(i);
            textPaint.setAlpha(cluster.alpha);

            int p = 0;
            int index = 0;
            while (index < xValues.length && (xValues[index] <= to || index == xCount - 1)) {
                if (!bitSet.get(index)) {
                    if (xValues[index] >= from || index == 0) {
                        pointBuffer[0] = xValues[index];
                        chartTransformer.transformPointsToPixels(pointBuffer);
                        float x = pointBuffer[0];
                        String label = formatter.getLabel(xValues[index]);

                        if (index == 0) {
                            textPaint.setTextAlign(Paint.Align.LEFT);
                        } else if (index == xCount - 1) {
                            textPaint.setTextAlign(Paint.Align.RIGHT);
                        } else {
                            textPaint.setTextAlign(Paint.Align.CENTER);
                        }
                        canvas.drawText(label, x, view.getHeight() - textBottom, textPaint);
                    }
                    bitSet.set(index);
                }

                p++;
                index = Math.round(cluster.step * p);
            }
        }
    }

    private void finishAnimators() {
        if (!clusters.isEmpty()) {
            for (Cluster cluster : clusters) {
                Utils.endAnimator(cluster.animator);
            }
        }
    }

    private void animateCluster(final int clusterIndex, final boolean on) {
        final Cluster cluster = clusters.get(clusterIndex);

        int newAlpha = on ? 255 : 0;
        if (cluster.dstAlpha == newAlpha)
            return;

        cluster.dstAlpha = newAlpha;

        Utils.cancelAnimator(cluster.animator);
        cluster.animator = ValueAnimator.ofInt(cluster.alpha, newAlpha);
        cluster.animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int alpha = (int) animation.getAnimatedValue();
                // additionally we decrease alpha depending on chart width when the cluster disappears
                // to avoid too many labels on screen on fast selection increase
                if (!on && clusterIndex >= 1) {
                    float minWidth = clusters.get(clusterIndex - 1).minChartWidth;
                    float maxWidth = clusterIndex >= 2 ? clusters.get(clusterIndex - 2).minChartWidth : chartState.getXValuesRange();
                    alpha = (int) (Math.max(0, maxWidth - chartState.getChartWidth()) * alpha /
                        (maxWidth - minWidth));
                    if (alpha == 0) {
                        cluster.animator.cancel();
                    }
                }
                cluster.alpha = alpha;
                view.invalidate();
            }
        });
        if (!on) {
            cluster.animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    visibleClusterIndex--;
                }
            });
        }
        int duration = Math.abs(cluster.dstAlpha - cluster.alpha) * ChartAnimator.ANIM_DURATION / 255;
        cluster.animator.setDuration(duration);
        cluster.animator.start();
    }

    private int findClusterIndex() {
        for (int i = 0; i < clusters.size(); i++) {
            if (chartState.getChartWidth() >= clusters.get(i).minChartWidth) {
                return i;
            }
        }
        return -1;
    }

    private static class Cluster {

        final float step;
        final float minChartWidth;
        int alpha = 255;
        int dstAlpha = 255;
        ValueAnimator animator;

        private Cluster(float step, float minChartWidth) {
            this.step = step;
            this.minChartWidth = minChartWidth;
        }
    }
}

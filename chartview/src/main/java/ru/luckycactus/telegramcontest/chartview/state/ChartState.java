package ru.luckycactus.telegramcontest.chartview.state;

import android.graphics.RectF;

import java.util.ArrayList;
import java.util.List;

import ru.luckycactus.telegramcontest.chartview.model.ChartData;
import ru.luckycactus.telegramcontest.chartview.model.LineData;
import ru.luckycactus.telegramcontest.chartview.rmq.BruteForceRMQStrategy;
import ru.luckycactus.telegramcontest.chartview.rmq.RMQStrategy;

public class ChartState {

    public ChartData chartData;
    private final RMQStrategy rmqStrategy = new BruteForceRMQStrategy();
    private float xValuesRange;
    private List<LineState> lineStates;
    private final RectF bounds = new RectF();
    private float windowWidth; // including horizontal padding
    private float windowHeight;
    private float contentWidth; // without horizontal padding
    private float chartHeight; // current chart height (in terms of chart values)
    private float chartWidth; // same for width
    private float horizontalPaddingPx;

    private float selectionStart = 0f; // selection start (from 0 to 1)
    private float selectionEnd = 0f; // same for selection end
    private float selectionStartValue;
    private float selectionEndValue;

    private int firstIndexInsideSelection;
    private int lastIndexInsideSelection;
    private int firstVisibleIndex; // including padding
    private int lastVisibleIndex;

    private float maxVisibleEntry;

    private int initialized;
    private static final int INIT_PART_CHART_DATA = 1;
    private static final int INIT_PART_BOUNDS = 2;
    private static final int INIT_FULL = INIT_PART_CHART_DATA | INIT_PART_BOUNDS;

    // *********************

    public void setChartData(ChartData chartData) {
        if (this.chartData == chartData) {
            return;
        }
        initialized |= INIT_PART_CHART_DATA;

        this.chartData = chartData;
        rmqStrategy.setChartData(chartData);
        xValuesRange = getMaxXValue() - getMinXValue();

        if (lineStates == null) {
            lineStates = new ArrayList<>(chartData.lines.size());
        } else {
            lineStates.clear();
        }
        for (LineData lineData : chartData.lines) {
            lineStates.add(new LineState(lineData));
        }

        updateSizes();
    }

    public void setBounds(float left, float top, float right, float bottom) {
        initialized |= INIT_PART_BOUNDS;
        if (bounds.left != left || bounds.top != top ||
            bounds.right != right || bounds.bottom != bottom
        ) {
            bounds.set(left, top, right, bottom);
            updateSizes();
        }
    }

    void setChartHeight(float chartHeight) {
        this.chartHeight = chartHeight;
    }

    public void setHorizontalPadding(float padding) {
        if (this.horizontalPaddingPx != padding) {
            this.horizontalPaddingPx = padding;
            updateSizes();
        }
    }

    public void setSelection(float from, float to) {
        if (this.selectionStart == from && this.selectionEnd == to)
            return;

        this.selectionStart = from;
        this.selectionEnd = to;

        updateChartValues();
    }

    public void updateMaxVisibleEntry() {
        float newMaxVisibleEntry = rmqStrategy.getMaxEntry(firstVisibleIndex, lastVisibleIndex);
        if (newMaxVisibleEntry > 0) {
            maxVisibleEntry = newMaxVisibleEntry;
        }
    }

    public int findIndexOfXValueInsideSelection(float targetXValue, Rounding type) {
        int index = findIndexOfXValue(targetXValue, type);
        index = Math.max(firstIndexInsideSelection, index);
        index = Math.min(lastIndexInsideSelection, index);
        return index;
    }

    public void restoreChartHeight(float chartHeight) {
        this.maxVisibleEntry = this.chartHeight = chartHeight;
    }

    // *********************

    public float getWindowWidth() {
        return windowWidth;
    }

    public float getWindowHeight() {
        return windowHeight;
    }

    public float getContentWidth() {
        return contentWidth;
    }

    public RectF getBounds() {
        return bounds;
    }

    public float getHorizontalPaddingPx() {
        return horizontalPaddingPx;
    }


    public int getXValuesCount() {
        return chartData.xValues.length;
    }

    public float getMinXValue() {
        return chartData.xValues[0];
    }

    public float getMaxXValue() {
        return chartData.xValues[chartData.xValues.length - 1];
    }

    public float getXValuesRange() {
        return xValuesRange;
    }


    public float getChartWidth() {
        return chartWidth;
    }

    public float getChartHeight() {
        return chartHeight;
    }

    public float getSelectionStartValue() {
        return selectionStartValue;
    }

    public float getSelectionEndValue() {
        return selectionEndValue;
    }

    public float getFirstVisibleValue() {
        return chartData.xValues[firstVisibleIndex];
    }

    public float getLastVisibleValue() {
        return chartData.xValues[lastVisibleIndex];
    }

    public int getFirstVisibleIndex() {
        return firstVisibleIndex;
    }

    public int getLastVisibleIndex() {
        return lastVisibleIndex;
    }

    public float getMaxVisibleEntry() {
        return maxVisibleEntry;
    }


    public List<LineState> getLineStates() {
        return lineStates;
    }

    public boolean isInitialized() {
        return initialized == INIT_FULL;
    }

    // *********************

    private void updateSizes() {
        windowHeight = bounds.height();
        windowWidth = bounds.width();
        contentWidth = windowWidth - 2 * horizontalPaddingPx;

        if (chartData != null) {
            updateChartValues();
            this.chartHeight = maxVisibleEntry;
        }
    }

    private void updateChartValues() {
        if (chartData == null)
            return;

        selectionStartValue = getMinXValue() + selectionStart * xValuesRange;
        selectionEndValue = getMinXValue() + selectionEnd * xValuesRange;

        chartWidth = (selectionEnd - selectionStart) * xValuesRange;

        firstIndexInsideSelection = findIndexOfXValue(selectionStartValue, Rounding.UP);
        lastIndexInsideSelection = findIndexOfXValue(selectionEndValue, Rounding.DOWN);

        float horizontalPadding = (chartWidth / windowWidth) * horizontalPaddingPx;
        float firstVisibleValue = selectionStartValue - horizontalPadding;
        float lastVisibleValue = selectionEndValue + horizontalPadding;
        firstVisibleIndex = Math.max(
            0,
            findIndexOfXValue(firstVisibleValue, Rounding.UP, 0, firstIndexInsideSelection)
        );
        lastVisibleIndex = Math.min(
            getXValuesCount() - 1,
            findIndexOfXValue(lastVisibleValue, Rounding.DOWN, lastIndexInsideSelection,
                getXValuesCount() - 1)
        );

        updateMaxVisibleEntry();
    }

    private int findIndexOfXValue(float targetXValue, Rounding type) {
        return findIndexOfXValue(targetXValue, type, 0, getXValuesCount() - 1);
    }

    private int findIndexOfXValue(float targetXValue, Rounding type, int l, int r) {
        while (l <= r) {
            int m = (l + r) / 2;

            float xValue = chartData.xValues[m];
            if (xValue > targetXValue) {
                r = m - 1;
            } else if (xValue < targetXValue) {
                l = m + 1;
            } else {
                return m;
            }
        }

        switch (type) {
            case UP:
                return Math.min(l, getXValuesCount() - 1);
            case DOWN:
                return Math.max(r, 0);
            default:
                if (r < 0) {
                    return 0;
                }
                if (l > getXValuesCount() - 1) {
                    return getXValuesCount() - 1;
                }
                if (Math.abs(targetXValue - chartData.xValues[r]) < Math.abs(targetXValue - chartData.xValues[l])) {
                    return r;
                } else {
                    return l;
                }
        }
    }

    public enum Rounding {
        UP,
        DOWN,
        Closest
    }
}

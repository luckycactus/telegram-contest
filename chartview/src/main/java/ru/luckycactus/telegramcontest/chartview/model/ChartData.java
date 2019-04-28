package ru.luckycactus.telegramcontest.chartview.model;

import java.util.List;

public final class ChartData {

    public final float[] xValues;
    public final List<LineData> lines;
    public final String name;

    public ChartData(float[] xValues, String name, List<LineData> lines) {
        this.xValues = xValues;
        this.lines = lines;
        this.name = name;
    }
}

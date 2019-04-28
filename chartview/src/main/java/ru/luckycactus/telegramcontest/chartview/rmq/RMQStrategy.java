package ru.luckycactus.telegramcontest.chartview.rmq;

import ru.luckycactus.telegramcontest.chartview.model.ChartData;

public abstract class RMQStrategy {

    protected ChartData chartData;

    public void setChartData(ChartData chartData) {
        this.chartData = chartData;
    }

    public abstract float getMaxEntry(int start, int end);
}

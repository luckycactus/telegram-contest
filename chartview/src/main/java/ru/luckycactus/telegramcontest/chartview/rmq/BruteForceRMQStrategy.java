package ru.luckycactus.telegramcontest.chartview.rmq;

import ru.luckycactus.telegramcontest.chartview.model.LineData;

public class BruteForceRMQStrategy extends RMQStrategy {

    @Override
    public float getMaxEntry(int start, int end) {
        float maxValue = 0;
        for (LineData line : chartData.lines) {
            if (line.isChecked()) {
                for (int i = start; i <= end; i++) {
                    maxValue = Math.max(maxValue, line.entries[i]);
                }
            }
        }
        return maxValue;
    }
}

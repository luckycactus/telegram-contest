package ru.luckycactus.telegramcontest.chartview.state;

import ru.luckycactus.telegramcontest.chartview.model.LineData;

public class LineState {

    final LineData lineData;
    private int alpha;

    LineState(LineData lineData) {
        this.lineData = lineData;
        this.alpha = lineData.isChecked() ? 255 : 0;
    }

    public LineData getLineData() {
        return lineData;
    }

    public int getAlpha() {
        return alpha;
    }

    public void setAlpha(int alpha) {
        this.alpha = alpha;
    }
}

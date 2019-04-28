package ru.luckycactus.telegramcontest.chartview.model;

public final class LineData {

    public final float[] entries;
    public final String label;
    public final int color;

    private boolean checked = true;

    public LineData(float[] entries, String label, int color) {
        this.entries = entries;
        this.label = label;
        this.color = color;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        if (this.checked != checked) {
            this.checked = checked;
        }
    }
}

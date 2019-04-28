package ru.luckycactus.telegramcontest.chartview.common;

public abstract class ValueFormatter {

    public static ValueFormatter DEFAULT = new ValueFormatter() {
        @Override
        public String getLabel(float value) {
            return String.valueOf(value);
        }
    };

    abstract public String getLabel(float value);
}

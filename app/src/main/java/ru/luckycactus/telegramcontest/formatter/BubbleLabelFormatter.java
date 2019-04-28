package ru.luckycactus.telegramcontest.formatter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ru.luckycactus.telegramcontest.chartview.common.ValueFormatter;

public class BubbleLabelFormatter extends ValueFormatter {

    private final SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d", Locale.ENGLISH);

    @Override
    public String getLabel(float value) {
        return sdf.format(new Date(1000 * (long) value));
    }
}

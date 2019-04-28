package ru.luckycactus.telegramcontest.formatter;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import ru.luckycactus.telegramcontest.chartview.common.ValueFormatter;

public class YAxisFormatter extends ValueFormatter {

    private final DecimalFormat df;

    public YAxisFormatter() {
        df = (DecimalFormat) NumberFormat.getInstance(Locale.US);
        df.setMaximumFractionDigits(2);
        DecimalFormatSymbols symbols = df.getDecimalFormatSymbols();
        symbols.setGroupingSeparator(' ');
        df.setDecimalFormatSymbols(symbols);
    }

    @Override
    public String getLabel(float value) {
        return df.format(value);
    }
}

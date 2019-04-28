package ru.luckycactus.telegramcontest.formatter;

import android.util.LruCache;

import ru.luckycactus.telegramcontest.chartview.common.ValueFormatter;

/**
 * Caching decorator for ValueFormatter
 */
public class CachingValueFormatter extends ValueFormatter {

    private final LruCache<Float, String> cache;
    private final ValueFormatter formatter;

    public CachingValueFormatter(ValueFormatter formatter) {
        this(formatter, 30);
    }

    public CachingValueFormatter(ValueFormatter formatter, int cacheSize) {
        cache = new LruCache<>(cacheSize);
        this.formatter = formatter;
    }

    @Override
    public final String getLabel(float value) {
        String label = cache.get(value);
        if (label == null) {
            label = formatter.getLabel(value);
            cache.put(value, label);
        }
        return label;
    }
}

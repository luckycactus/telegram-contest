package ru.luckycactus.telegramcontest.chartview.common;

import android.content.Context;

import ru.luckycactus.telegramcontest.chartview.R;

public class ChartTheme {

    public static int chartBackgroundColor;
    public static int yAxisLineColor;
    public static int labelColor;
    public static int scrollbarOverlayColor;
    public static int scrollbarFrameColor;
    public static int chartTitleColor;
    public static int dividerColor;
    public static int bubbleColor;
    public static int bubbleTitleColor;
    public static int bubbleLineColor;
    public static int textColor;
    public static int checkColor;

    private static boolean initialized;

    public static void update(Context context) {
        chartBackgroundColor = Utils.getThemeColorOrThrow(context, R.attr.themeChartBackgroundColor);
        yAxisLineColor = Utils.getThemeColorOrThrow(context, R.attr.themeYAxisLineColor);
        labelColor = Utils.getThemeColorOrThrow(context, R.attr.themeLabelColor);
        scrollbarOverlayColor = Utils.getThemeColorOrThrow(context, R.attr.themeScrollbarOverlayColor);
        scrollbarFrameColor = Utils.getThemeColorOrThrow(context, R.attr.themeScrollbarFrameColor);
        chartTitleColor = Utils.getThemeColorOrThrow(context, R.attr.themeChartTitleColor);
        dividerColor = Utils.getThemeColorOrThrow(context, R.attr.themeDividerColor);
        bubbleColor = Utils.getThemeColorOrThrow(context, R.attr.themeBubbleColor);
        bubbleTitleColor = Utils.getThemeColorOrThrow(context, R.attr.themeBubbleTitleColor);
        bubbleLineColor = Utils.getThemeColorOrThrow(context, R.attr.themeBubbleLineColor);
        textColor = Utils.getThemeColorOrThrow(context, android.R.attr.textColor);
        checkColor = Utils.getThemeColorOrThrow(context, R.attr.themeCheckColor);

        initialized = true;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}

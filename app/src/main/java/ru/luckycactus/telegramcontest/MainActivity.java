package ru.luckycactus.telegramcontest;

import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import java.util.List;

import ru.luckycactus.telegramcontest.chartview.common.ChartTheme;
import ru.luckycactus.telegramcontest.chartview.common.Utils;
import ru.luckycactus.telegramcontest.chartview.model.ChartData;
import ru.luckycactus.telegramcontest.chartview.view.ChartLayout;
import ru.luckycactus.telegramcontest.formatter.BubbleLabelFormatter;
import ru.luckycactus.telegramcontest.formatter.CachingValueFormatter;
import ru.luckycactus.telegramcontest.formatter.XAxisFormatter;
import ru.luckycactus.telegramcontest.formatter.YAxisFormatter;

import static ru.luckycactus.telegramcontest.chartview.common.Utils.getThemeColorOrThrow;

public class MainActivity extends Activity {

    private ThemeManager themeManager;
    private LinearLayout chartsContainer;
    private ChartLayout[] chartLayouts;
    private ActionBar actionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        themeManager = new ThemeManager(this);
        themeManager.applyCurrentTheme(this);


        setContentView(R.layout.activity_main);
        actionBar = findViewById(R.id.actionBar);
        actionBar.setTitle(R.string.statistics);
        actionBar.setOnThemeClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                themeManager.toggleTheme();
                themeManager.applyCurrentTheme(MainActivity.this);
                ChartTheme.update(MainActivity.this);
                updateTheme();
            }
        });

        updateColors();

        final List<ChartData> chartDataList = ViewModel.INSTANCE.getData(this);
        int[] ids = null;
        if (savedInstanceState != null) {
            ids = savedInstanceState.getIntArray("chartlayout_ids");
        }
        if (chartDataList != null) {
            chartsContainer = findViewById(R.id.chartsContainer);
            chartLayouts = new ChartLayout[chartDataList.size()];
            for (int i = 0; i < chartDataList.size(); i++) {
                ChartData chartData = chartDataList.get(i);
                final ChartLayout chartLayout = new ChartLayout(this);
                chartLayouts[i] = chartLayout;
                int id = (ids == null || i >= ids.length) ? Utils.generateViewId() : ids[i];
                chartLayout.setId(id);
                Utils.setElevation(chartLayout, Utils.dpF(2));
                chartLayout.setData(chartData);
                chartLayout.setYAxisFormatter(new CachingValueFormatter(new YAxisFormatter()));
                chartLayout.setXAxisFormatter(new CachingValueFormatter(new XAxisFormatter()));
                chartLayout.setBubbleFormatter(new CachingValueFormatter(new BubbleLabelFormatter()));
                chartLayout.setSelection(0.7f, 1f);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                );
                lp.setMargins(0, 0, 0, Utils.dp(16));
                chartsContainer.addView(
                    chartLayout,
                    lp
                );
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!isChangingConfigurations()) {
            ViewModel.INSTANCE.clear();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        int[] ids = new int[chartsContainer.getChildCount()];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = chartsContainer.getChildAt(i).getId();
        }
        outState.putIntArray("chartlayout_ids", ids);
    }

    private void updateTheme() {
        updateColors();
        for (ChartLayout chartLayout : chartLayouts) {
            chartLayout.updateTheme();
        }
    }

    private void updateColors() {
        Window window = getWindow();
        window.getDecorView().setBackgroundColor(
            getThemeColorOrThrow(this, R.attr.themeWindowBackgroundColor)
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getThemeColorOrThrow(this, R.attr.themePrimaryDarkColor));
        }

        actionBar.setBackgroundColor(getThemeColorOrThrow(this, R.attr.themePrimaryColor));
    }
}

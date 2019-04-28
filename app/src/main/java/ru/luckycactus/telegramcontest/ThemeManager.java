package ru.luckycactus.telegramcontest;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.SparseArray;

public class ThemeManager {

    private SharedPreferences prefs;
    private static String PREF_THEME = "theme";


    public ThemeManager(Context context) {
        this.prefs = context.getSharedPreferences("theme_manager", Context.MODE_PRIVATE);
    }

    public void applyCurrentTheme(Activity activity) {
        activity.setTheme(getCurrentTheme().themeId);
    }

    public void toggleTheme() {
        Theme theme;
        if (getCurrentTheme() == Theme.Day) {
            theme = Theme.Night;
        } else {
            theme = Theme.Day;
        }
        setTheme(theme);
    }

    public void setTheme(Theme theme) {
        prefs.edit().putInt(PREF_THEME, theme.ordinal()).apply();
    }

    public Theme getCurrentTheme() {
        return Theme.from(prefs.getInt(PREF_THEME, Theme.Day.ordinal()));
    }

    public enum Theme {
        Day(R.style.AppTheme_Day),
        Night(R.style.AppTheme_Night);

        final int themeId;
        private static SparseArray<Theme> entriesMap;

        static {
            entriesMap = new SparseArray<>();
            for (Theme value : Theme.values()) {
                entriesMap.put(value.ordinal(), value);
            }
        }

        Theme(int themeId) {
            this.themeId = themeId;
        }

        public static Theme from(int ordinal) {
            return entriesMap.get(ordinal);
        }
    }
}

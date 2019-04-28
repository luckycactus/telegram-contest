package ru.luckycactus.telegramcontest;

import android.content.Context;
import android.graphics.Color;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import ru.luckycactus.telegramcontest.chartview.model.ChartData;
import ru.luckycactus.telegramcontest.chartview.model.LineData;

public enum  ViewModel {
    INSTANCE;

    private List<ChartData> data;

    public List<ChartData> getData(Context context) {
        if (data == null) {
            data = parseJson(loadJson(context, "chart_data.json"));
        }
        return data;
    }

    public void clear() {
        data = null;
    }

    private List<ChartData> parseJson(String json) {
        try {
            JSONArray chartsJsonArray = new JSONArray(json);
            List<ChartData> chartDataList = new ArrayList<>(chartsJsonArray.length());
            for (int chartIter = 0; chartIter < chartsJsonArray.length(); chartIter++) {
                JSONObject chartJsonObject = chartsJsonArray.getJSONObject(chartIter);
                JSONObject typesJsonObject = chartJsonObject.getJSONObject("types");
                JSONObject namesJsonObject = chartJsonObject.getJSONObject("names");
                JSONObject colorsJsonObject = chartJsonObject.getJSONObject("colors");
                JSONArray columnsJsonArray = chartJsonObject.getJSONArray("columns");

                float[] xAxis = null;
                List<LineData> lines = new ArrayList<>(columnsJsonArray.length() - 1);

                for (int columnIter = 0; columnIter < columnsJsonArray.length(); columnIter++) {
                    JSONArray columnJsonArray = columnsJsonArray.getJSONArray(columnIter);
                    String name = columnJsonArray.getString(0);
                    float[] entries = new float[columnJsonArray.length() - 1];
                    boolean isXAxis = typesJsonObject.getString(name).equals("x");
                    for (int pointIter = 1; pointIter < columnJsonArray.length(); pointIter++) {
                        long value = columnJsonArray.getLong(pointIter);
                        if (isXAxis) {
                            value = value / 1000;
                        }
                        entries[pointIter - 1] = ((float) (value));
                    }
                    if (isXAxis) {
                        xAxis = entries;
                    } else {
                        String label = namesJsonObject.getString(name);
                        int color = Color.parseColor(colorsJsonObject.getString(name));
                        LineData lineData = new LineData(entries, label, color);
                        lines.add(lineData);
                    }
                }

                ChartData chartData = new ChartData(xAxis, "Chart #" + chartIter, lines);
                chartDataList.add(chartData);
            }
            return chartDataList;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String loadJson(Context context, String assetName) {
        String json = null;
        try {
            InputStream is = context.getAssets().open(assetName);
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return json;
    }
}

package ru.luckycactus.telegramcontest.chartview.rmq;

import android.util.Log;

import ru.luckycactus.telegramcontest.chartview.model.ChartData;
import ru.luckycactus.telegramcontest.chartview.model.LineData;

public class SparseTableRMQStrategy extends RMQStrategy {

    private int[][][] sparseTable;

    @Override
    public void setChartData(ChartData chartData) {
        if (this.chartData == chartData)
            return;

        super.setChartData(chartData);
        preprocess();
    }

    @Override
    public float getMaxEntry(int low, int high) {
        int k = floorLog2(high - low + 1);

        float chartMaxEntry = 0;
        for (int i = 0; i < chartData.lines.size(); i++) {
            LineData lineData = chartData.lines.get(i);
            if (!lineData.isChecked())
                continue;

            int[][] sparse = sparseTable[i];
            float lineMaxEntry = Math.max(
                lineData.entries[sparse[low][k]],
                lineData.entries[sparse[high - (1 << k) + 1][k]]
            );
            chartMaxEntry = Math.max(lineMaxEntry, chartMaxEntry);
        }
        return chartMaxEntry;

    }

    private void preprocess() {
        int n = chartData.xValues.length;
        sparseTable = new int[chartData.lines.size()][n][floorLog2(n) + 1];
        for (int i = 0; i < chartData.lines.size(); i++) {
            preprocessLine(chartData.lines.get(i).entries, sparseTable[i]);
        }
    }

    private static void preprocessLine(float[] entries, int[][] sparse) {
        int n = entries.length;

        for (int i = 0; i < n; i++) {
            sparse[i][0] = i;
        }

        for (int j = 1; (1 << j) <= n; j++) {
            for (int i = 0; (i + (1 << j) - 1) < n; i++) {
                if (entries[sparse[i][j - 1]] >= entries[sparse[i + (1 << (j - 1))][j - 1]]) {
                    sparse[i][j] = sparse[i][j - 1];
                } else {
                    sparse[i][j] = sparse[i + (1 << (j - 1))][j - 1];
                }
            }
        }
    }

    private static int floorLog2(int n) {
        return 31 - Integer.numberOfLeadingZeros(n);
    }
}

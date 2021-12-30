package com.netease.iot.rule.proxy.metric;

import com.google.common.annotations.VisibleForTesting;
import com.netease.iot.rule.proxy.util.Constants;

import java.util.*;

public class MetricUtil {
    public static long alignMetricTS(long ts) {
        return alignMetricTS(ts, Constants.METRIC_TIME_UNIT);
    }

    private static long alignMetricTS(long ts, long unit) {
        return ts / unit * unit;
    }

    public static long advanceMetricTS(long ts, int step) {
        return alignMetricTS(ts) + step * Constants.METRIC_TIME_UNIT;
    }

    @VisibleForTesting
    static List<Long> getSampleMetricTSHelper(long center, long bound, long sampleCount) {
        long sampleStep = bound / sampleCount;
        if (sampleStep < Constants.METRIC_TIME_UNIT) {
            sampleStep = Constants.METRIC_TIME_UNIT;
        }
        List<Long> samplePoints = new ArrayList<>();
        long nextTS = center - bound / 2;
        long endTS = center + bound / 2;
        do {
            samplePoints.add(alignMetricTS(nextTS + sampleStep));
            nextTS += sampleStep;
        } while (nextTS < endTS);
        return samplePoints;
    }

    public static List<Map.Entry<Long, List<Long>>> getSampleMetricTS(long startTime, long endTime, long pointNum) {
        if (endTime - startTime <= 3 * 3600000) {
            List<Map.Entry<Long, List<Long>>> points = new LinkedList<>();
            final long second10 = 10 * 1000;
            long i = startTime;
            while (i <= endTime) {
                i = alignMetricTS(i, second10);
                List<Long> samplePoints = MetricUtil.getSampleMetricTSHelper(i, second10, 3);
                points.add(new AbstractMap.SimpleEntry<Long, List<Long>>(i, samplePoints));
                i += second10;
            }
            return points;
        } else {
            return getSampleMetricTSUnstable(startTime, endTime, pointNum);
        }
    }

    public static List<Map.Entry<Long, List<Long>>> getSampleMetricTSUnstable(long startTime, long endTime, long pointNum) {
        long step = Math.floorDiv(endTime - startTime, pointNum);
        if (step < Constants.METRIC_TIME_UNIT) {
            step = Constants.METRIC_TIME_UNIT;
        }
        List<Map.Entry<Long, List<Long>>> points = new LinkedList<>();
        startTime = alignMetricTS(startTime);
        endTime = alignMetricTS(endTime);
        long i = startTime;
        while (points.size() < pointNum && i <= endTime) {
            List<Long> samplePoints = MetricUtil.getSampleMetricTSHelper(i, step, 5);
            points.add(new AbstractMap.SimpleEntry<Long, List<Long>>(i, samplePoints));
            i += step;
        }
        return points;
    }

    /**
     * Get row key according to metricType
     *
     * @param jobId      flink job id
     * @param ts         metric ts
     * @param metricType metric type
     * @return rowkey
     */
    public static String getRowKey(String jobId, long ts, String metricType) {
        StringBuilder builder = new StringBuilder(jobId.length() + 40);
        return builder.append(ts).append(":").append(jobId).toString();
    }
}

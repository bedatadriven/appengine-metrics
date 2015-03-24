package com.bedatadriven.appengine.metrics;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Identifies a distinct timeseries by the combination of labels
 */
public class TimeSeriesKey {
    private final Map<String, String> labels;

    public TimeSeriesKey(ImmutableMap<String, String> labels) {
        this.labels = labels;
    }
    public TimeSeriesKey(Map<String, String> labels) {
        this.labels = ImmutableMap.copyOf(labels);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TimeSeriesKey that = (TimeSeriesKey) o;

        if (!labels.equals(that.labels)) return false;

        return true;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    @Override
    public int hashCode() {
        return labels.hashCode();
    }

    public static TimeSeriesKey label(String labelKey, String labelValue) {
        return new TimeSeriesKey(ImmutableMap.of(labelKey, labelValue));
    }
}

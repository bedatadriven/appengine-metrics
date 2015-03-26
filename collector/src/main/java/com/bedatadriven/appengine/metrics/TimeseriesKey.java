package com.bedatadriven.appengine.metrics;

import com.google.api.services.cloudmonitoring.model.TimeseriesDescriptor;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * Identifies a distinct time series by its name and the combination of labels
 */
public class TimeseriesKey {
    private final String metricName;
    private final Map<String, String> labels;
    private final String cacheKey;
    private TimeseriesDescriptor descriptor;
    
    public TimeseriesKey(@Nonnull String metricName, @Nonnull ImmutableMap<String, String> labels) {
        this.metricName = metricName;
        Preconditions.checkNotNull(labels, "labels");

        this.labels = labels;
        this.cacheKey = buildCacheKey();
        this.descriptor = new TimeseriesDescriptor()
                .setMetric(metricName)
                .setLabels(labels);
    }

    public TimeseriesKey(@Nonnull String metricName, @Nonnull Map<String, String> labels) {
        this(metricName, ImmutableMap.copyOf(labels));
    }

    public TimeseriesKey(String metricName) {
        this(metricName, ImmutableMap.<String, String>of());
    }

    private String buildCacheKey() {
        StringBuilder sb = new StringBuilder(metricName);
        for (Map.Entry<String, String> label : labels.entrySet()) {
            String key = MetricNames.stripBaseName(label.getKey());
            sb.append('[').append(key).append('=').append(label.getValue()).append(']');
        }
        return sb.toString();
    }

    public String getMetricName() {
        return metricName;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public String getCacheKey() {
        return cacheKey;
    }

    public TimeseriesDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TimeseriesKey key = (TimeseriesKey) o;

        if (!labels.equals(key.labels)) return false;
        if (!metricName.equals(key.metricName)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = metricName.hashCode();
        result = 31 * result + labels.hashCode();
        return result;
    }

}

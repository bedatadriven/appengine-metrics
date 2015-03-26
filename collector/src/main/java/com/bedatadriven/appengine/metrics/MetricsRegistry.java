
package com.bedatadriven.appengine.metrics;

import com.google.appengine.api.utils.SystemProperty;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

/**
 * Central, per-instance registry of metrics.
 *
 * <p>Each new AppEngine instance will have its own set of records that can be 
 * safely updated by concurrent request threads. The MetricRequestFilter will periodically
 * flush the in-memory results to Memcache</p>
 * 
 * <p>Every 60 seconds, the /metrics con job will flush the aggregated metrics out to Google Cloud
 * Metrics through the restful API</p>
 */
public final class MetricsRegistry {

    private static final Logger LOGGER = Logger.getLogger(MetricsRegistry.class.getName());

    public static final MetricsRegistry INSTANCE = new MetricsRegistry();

    private final ConcurrentMap<TimeseriesKey, Timeseries> timeseriesMap = new ConcurrentHashMap<TimeseriesKey, Timeseries>();

    public Meter metric(String name, String label) {
        TimeseriesKey key = key(name, label);
        Meter meter = (Meter) timeseriesMap.get(key);
        if(meter != null) {
            return meter;
        }
        return insert(key, new Meter(key));
    }

    public RequestTimer timer(String name, String label) {
        TimeseriesKey key = key(name, label);
        RequestTimer timer = (RequestTimer) timeseriesMap.get(key);
        if(timer != null) {
            return timer;
        }

        return insert(key, new RequestTimer(key));
    }


    private TimeseriesKey key(String name, String label) {
        return new TimeseriesKey(name, ImmutableMap.of(MetricNames.KIND_LABEL, label));
    }

    private <T extends Timeseries> T insert(TimeseriesKey key, T newTimeseries) {
        T existingTimeseries = (T) timeseriesMap.putIfAbsent(key, newTimeseries);
        if (existingTimeseries == null) {
            // we successfully set the new series
            return newTimeseries;
        } else {

            // someone beat us to it
            return existingTimeseries;
        }
    }

    public String getProjectId() {
        return SystemProperty.Environment.applicationId.get();
    }

    public Set<Map.Entry<TimeseriesKey, Timeseries>> getTimeseries() {
        return timeseriesMap.entrySet();
    }

    public Iterable<Meter> meters() {
        return Iterables.filter(timeseriesMap.values(), Meter.class);
    }

    public Iterable<RequestTimer> timers() {
        return Iterables.filter(timeseriesMap.values(), RequestTimer.class);
    }
}
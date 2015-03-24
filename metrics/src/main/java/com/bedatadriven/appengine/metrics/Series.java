package com.bedatadriven.appengine.metrics;


import com.google.api.services.cloudmonitoring.model.MetricDescriptor;
import com.google.api.services.cloudmonitoring.model.TimeseriesDescriptor;

public class Series<R extends Recorder> {
    private TimeseriesDescriptor descriptor;
    private R recorder;

    public Series(MetricDescriptor metric, TimeSeriesKey key, R recorder) {
        this.recorder = recorder;
        this.descriptor = new TimeseriesDescriptor()
                .setProject(metric.getProject())
                .setMetric(metric.getName())
                .setLabels(key.getLabels());
    }

    public TimeseriesDescriptor getDescriptor() {
        return descriptor;
    }

    public R getRecorder() {
        return recorder;
    }
}

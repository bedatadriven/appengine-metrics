package com.bedatadriven.appengine.metrics;

import com.google.api.client.util.DateTime;
import com.google.api.services.cloudmonitoring.model.MetricDescriptor;
import com.google.api.services.cloudmonitoring.model.TimeseriesPoint;

import java.util.List;

public interface Metric {

    MetricDescriptor getDescriptor();

    void drainTo(DateTime startTime, DateTime endTime, List<TimeseriesPoint> points);
    
}

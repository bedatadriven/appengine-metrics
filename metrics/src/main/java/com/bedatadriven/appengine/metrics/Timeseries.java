package com.bedatadriven.appengine.metrics;

import com.google.api.services.cloudmonitoring.model.MetricDescriptorTypeDescriptor;

/**
 * Class responsible for recording and computing metric values.
 */
public interface Timeseries {
    
    TimeseriesKey getKey();

    MetricDescriptorTypeDescriptor getTypeDescriptor();

}

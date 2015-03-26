package com.bedatadriven.appengine.metrics;

import com.google.api.services.cloudmonitoring.model.MetricDescriptorTypeDescriptor;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Measures the rate at which an event occurs
 */
public class Meter implements Timeseries {


    private final TimeseriesKey key;
    private final AtomicLong count = new AtomicLong(0);

    public Meter(TimeseriesKey key) {
        this.key = key;
    }

    public void mark() {
        count.incrementAndGet();
    }
    
    /**
     * Resets the counter's value and return the previous value
     * 
     * @return the current value of the counter
     */
    public long drain() {
        return count.getAndSet(0L);
    }


    @Override
    public TimeseriesKey getKey() {
        return key;
    }
    
    public String getCacheKey() {
        return key.getCacheKey();
    }

    @Override
    public MetricDescriptorTypeDescriptor getTypeDescriptor() {
        MetricDescriptorTypeDescriptor type = new MetricDescriptorTypeDescriptor();
        type.setMetricType("gauge");
        type.setValueType("double");
        return type;
    }

}

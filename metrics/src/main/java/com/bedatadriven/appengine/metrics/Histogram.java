package com.bedatadriven.appengine.metrics;
import com.google.api.client.util.DateTime;

import com.google.api.services.cloudmonitoring.model.*;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
 * A metric which calculates the distribution of a value.
 */
public class Histogram implements Metric {


    public static class Builder {
        private final MetricDescriptor descriptor;
        private FixedBinning binning;

        public Builder(String name) {
            descriptor = new MetricDescriptor();
            descriptor.setName(name);
            descriptor.setLabels(new ArrayList<MetricDescriptorLabelDescriptor>());


            MetricDescriptorTypeDescriptor typeDescriptor = new MetricDescriptorTypeDescriptor();
            typeDescriptor.setMetricType("gauge");
            typeDescriptor.setValueType("distribution");
            descriptor.setTypeDescriptor(typeDescriptor);
        }

        public Builder defineLabel(String key, String description) {
            descriptor.getLabels().add(new MetricDescriptorLabelDescriptor()
                    .setKey(key)
                    .setDescription(description));
            return this;
        }


        public Builder useFixedBins(double min, double max, int binCount) {
            this.binning = new FixedBinning(min, max, binCount);
            return this;
        }

        public void build() {
            Preconditions.checkState(binning != null, "Binning has not been set");
            
            Histogram histogram = new Histogram(descriptor, binning);
            MetricsRegistry.INSTANCE.register(histogram);
        }
    }
    
    public static Builder newHistogram(String name) {
        Preconditions.checkArgument(name.startsWith(MetricNames.CUSTOM_METRICS_BASE_NAME), 
                "Custom metric names must start with " + MetricNames.CUSTOM_METRICS_BASE_NAME);
        
        return new Builder(name);
    }
    
    private final MetricDescriptor descriptor;
    private final FixedBinning binning;
    private final ConcurrentMap<TimeSeriesKey, Series<HistogramRecorder>> series;
    
    public Histogram(MetricDescriptor descriptor, FixedBinning binning) {
        this.descriptor = descriptor;
        this.binning = binning;
        this.series = Maps.newConcurrentMap();
    }
    
    public HistogramRecorder get(TimeSeriesKey key) {
        Series<HistogramRecorder> series = this.series.get(key);
        if(series == null) {
            series = new Series<HistogramRecorder>(descriptor, key, binning.newSeries());
            Series<HistogramRecorder> first = this.series.putIfAbsent(key, series);
            if(first != null) {
                // another thread beat us to it
                series = first;
            }
        }
        return series.getRecorder();
    }
    
    public Histogram setDescription(String description) {
        descriptor.setDescription(description);
        return this;
    }
    

    @Override
    public MetricDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public void drainTo(DateTime startTime, DateTime endTime, List<TimeseriesPoint> points) {
        for (Series series : this.series.values()) {
            TimeseriesPoint point = new TimeseriesPoint();
            point.setTimeseriesDesc(series.getDescriptor());
            point.setPoint(series.getRecorder().drainPoint(startTime, endTime));
            points.add(point);
        }
    }
}
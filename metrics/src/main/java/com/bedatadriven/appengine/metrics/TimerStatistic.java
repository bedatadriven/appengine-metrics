package com.bedatadriven.appengine.metrics;


import com.google.api.services.cloudmonitoring.model.TimeseriesDescriptor;

public enum TimerStatistic {
    
    PCTL95("95% Percentile") {
        @Override
        public double compute(RequestTimer timer, long[] counts) {
            return lowerPercentile(timer, counts, 95);
        }
    },
    PCTL5("5% Percentile") {
        @Override
        public double compute(RequestTimer timer, long[] counts) {
            return upperPercentile(timer, counts, 5);
        }
    },
    MEAN("Average") {
        @Override
        public double compute(RequestTimer timer, long[] counts) {
            return mean(timer, counts);
        }
    },
    MEDIAN("Median") {
        @Override
        public double compute(RequestTimer timer, long[] counts) {
            return lowerPercentile(timer, counts, 50);
        }
    };
    
    private String description;

    TimerStatistic(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public abstract double compute(RequestTimer timer, long[] counts);
    
    
    private static double lowerPercentile(RequestTimer timer, long[] counts, int percentile) {
        long maxCount = threshold(counts, percentile);
        long cumulative = 0;
        for(int i=0;i<counts.length;++i) {
            cumulative += counts[i];
            if(cumulative >= maxCount) {
                return timer.getUpperBound(i);
            }
        }
        throw new IllegalStateException();

    }
    
    private static double upperPercentile(RequestTimer timer, long[] counts, int percentile) {
        long maxCount = threshold(counts, percentile);
        long runningCount = 0;
        for(int i=(counts.length-1);i>=0;--i) {
            runningCount += counts[i];
            if(runningCount >= maxCount) {
                return timer.getUpperBound(i);
            }
        }
        throw new IllegalStateException();
    }
    

    private static long threshold(long[] counts, double percentile) {
        long totalCount = totalCount(counts);
        double alpha = percentile / 100d;
        long count = (long) Math.floor(((double) totalCount) * alpha);
        if(count < 1L) {
            return 1L;
        } else {
            return count;
        }
    }

    static double mean(RequestTimer timer, long counts[]) {
        double sum = 0;
        long count = 0;
        
        for(int i=0;i<counts.length;++i) {
            double midpoint = (timer.getLowerBound(i) + timer.getUpperBound(i)) / 2d;
            sum += (midpoint * ((double)counts[i]));
            count += counts[i];
        }
        
        return sum / ((double)count);
    }


    private static long totalCount(long[] counts) {
        long sum = 0;
        for(int i=0;i<counts.length;++i) {
            sum += counts[i];
        }
        return sum;
    }
    
    public TimeseriesDescriptor descriptor(TimeseriesKey key) {
        TimeseriesDescriptor descriptor = new TimeseriesDescriptor();
        descriptor.setMetric(metricName(key));
        descriptor.setLabels(key.getLabels());
        return descriptor;
    }

    public String metricName(TimeseriesKey key) {
        return key.getMetricName() + "/" + name().toLowerCase();
    }
}

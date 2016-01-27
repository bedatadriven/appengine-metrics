package com.bedatadriven.appengine.metrics;

import com.google.common.math.LongMath;

import java.math.RoundingMode;

/**
 * Concurrent online histogram, using the same breaks as the 
 * metrics seem to use for request_latency metrics... 
 */
public class TimingStatistic {
    

    /**
     * Number of buckets to use. We are limited to 20, including
     * overflow, so keep to 19.
     */
    private static final int BUCKET_COUNT = 19;

    private static final long[] UPPER_BOUNDS;
    private static final long[] LOWER_BOUNDS;

    static {
        UPPER_BOUNDS = new long[BUCKET_COUNT];
        LOWER_BOUNDS = new long[BUCKET_COUNT];
        UPPER_BOUNDS[0] = 2L;
        for(int i=1;i< UPPER_BOUNDS.length;++i) {
            LOWER_BOUNDS[i] = UPPER_BOUNDS[i-1];
            UPPER_BOUNDS[i] = UPPER_BOUNDS[i-1] * 2L;
        }
    }

    private final String key;
    private final long[] counts;
    private long overflowCount;
    private long overflowMax;
        ;
    public TimingStatistic(String key) {
        this.key = key;
        counts = new long[BUCKET_COUNT];
        overflowCount = 0;
        overflowMax = 0;
    }

    public void update(double value) {
        update((long)value);
    }
    
    public void update(long milliseconds) {
        int bucket = indexOf(milliseconds);
        if(bucket < BUCKET_COUNT) {
            counts[bucket]++;
        } else {
            overflowCount++;
        }
    }
    int indexOf(long milliseconds) {
        if(milliseconds <= 0) {
            return 0;
        } else {
            return LongMath.log2(milliseconds, RoundingMode.DOWN);
        }
    }

    public double upperPercentile(int percentile) {
        long maxCount = threshold(percentile);
        long runningCount = 0;
        for(int i=(counts.length-1);i>=0;--i) {
            runningCount += counts[i];
            if(runningCount >= maxCount) {
                return UPPER_BOUNDS[i];
            }
        }
        throw new IllegalStateException();
    }


    public long threshold(double percentile) {
        long totalCount = totalCount();
        double alpha = percentile / 100d;
        long count = (long) Math.floor(((double) totalCount) * alpha);
        if(count < 1L) {
            return 1L;
        } else {
            return count;
        }
    }

    public double mean() {
        double sum = 0;
        long count = 0;

        for(int i=0;i<counts.length;++i) {
            double midpoint = (LOWER_BOUNDS[i] + UPPER_BOUNDS[i]) / 2d;
            sum += (midpoint * ((double)counts[i]));
            count += counts[i];
        }

        return sum / ((double)count);
    }


    private long totalCount() {
        long sum = 0;
        for(int i=0;i<counts.length;++i) {
            sum += counts[i];
        }
        return sum;
    }

    public void reportTo(ReportBuilder builder) {
        builder.addGauge(key + ".mean", mean());
        builder.addGauge(key + ".median", upperPercentile(50));
        builder.addGauge(key + ".pctl95", upperPercentile(95));
    }
}

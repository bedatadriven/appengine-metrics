package com.bedatadriven.appengine.metrics;

import com.google.api.services.cloudmonitoring.model.MetricDescriptorTypeDescriptor;
import com.google.common.math.LongMath;

import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * Concurrent online histogram, using the same breaks as the 
 * metrics seem to use for request_latency metrics... 
 */
public class RequestTimer implements Timeseries {


    /**
     * Number of buckets to use. We are limited to 20, including
     * overflow, so keep to 19.
     */
    private static final int BUCKET_COUNT = 19;
    
    private final AtomicLongArray counts;
    private final AtomicLong overflowCount;
    private final AtomicLong overflowMax;
    private final long[] upperBounds;
    private final long[] lowerBounds;

    /**
     * The memcache keys for this instrument's value
     */
    private final String[] cacheKeys;
    private TimeseriesKey key;


    public RequestTimer(TimeseriesKey key) {
        this.key = key;
        counts = new AtomicLongArray(BUCKET_COUNT);
        overflowCount = new AtomicLong(0);
        overflowMax = new AtomicLong(0);
        
        upperBounds = new long[BUCKET_COUNT];
        lowerBounds = new long[BUCKET_COUNT];
        upperBounds[0] = 2L;
        for(int i=1;i<upperBounds.length;++i) {
            lowerBounds[i] = upperBounds[i-1];
            upperBounds[i] = upperBounds[i-1] * 2L;
        }
        
        // Pre-compute the keys used to store our histogram's
        // bucket values.
        cacheKeys = new String[BUCKET_COUNT];
        for(int i=0;i<BUCKET_COUNT;++i) {
            cacheKeys[i] = key.getCacheKey() + "[" + i + "]";
        }
    }

    @Override
    public TimeseriesKey getKey() {
        return key;
    }

    public void update(double value) {
        update((long)value);
    }
    
    public void update(long milliseconds) {
        int bucket = indexOf(milliseconds);
        if(bucket < BUCKET_COUNT) {
            counts.incrementAndGet(bucket);
        } else {
            overflowCount.incrementAndGet();
        }
    }

    int indexOf(long milliseconds) {
        if(milliseconds <= 0) {
            return 0;
        } else {
            return LongMath.log2(milliseconds, RoundingMode.DOWN);
        }
    }
    
    public String[] getCacheKeys() {
        return cacheKeys;
    }
    
    public long[] drain() {
        long[] snapshot = new long[BUCKET_COUNT];
        for(int i=0; i<BUCKET_COUNT;++i) {
            snapshot[i] = counts.getAndSet(i, 0L);
        }
        return snapshot;
    }

    @Override
    public MetricDescriptorTypeDescriptor getTypeDescriptor() {
        return new MetricDescriptorTypeDescriptor()
                .setMetricType("gauge")
                .setValueType("distribution");
    }

    public double getLowerBound(int i) {
        return lowerBounds[i];
    }
    
    public double getUpperBound(int i) {
        return upperBounds[i];
    }
}

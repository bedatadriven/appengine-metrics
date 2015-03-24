package com.bedatadriven.appengine.metrics;

import com.google.api.client.util.DateTime;
import com.google.api.services.cloudmonitoring.model.Point;
import com.google.api.services.cloudmonitoring.model.PointDistribution;
import com.google.api.services.cloudmonitoring.model.PointDistributionBucket;
import com.google.common.util.concurrent.AtomicDouble;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

public class FixedBinning {
    private final double min;
    private final int binCount;
    private final double binWidth;
    private final double max;

    public FixedBinning(double min, double max, int binCount) {
        this.min = min;
        this.max = max;
        this.binWidth = (max - min) / ((double)binCount);
        this.binCount = binCount;
    }
    
    public FixedBinRecorder newSeries() {
        return new FixedBinRecorder();
    }

    /**
     * A simple streaming histogram calculator that requires fixed range and 
     * number of bins. Useful for metrics which have intrinsic limits, such as
     * latency.
     */
    public class FixedBinRecorder implements HistogramRecorder {
        
        private AtomicLongArray counts;
        private AtomicLong overflowCount;
        private AtomicDouble overflowMax;
    
        public FixedBinRecorder() {
            this.counts = new AtomicLongArray(binCount);
            this.overflowCount = new AtomicLong(0);
            this.overflowMax = new AtomicDouble(0);
        }
    
        @Override
        public void update(double value) {
            if(value > max) {
                // Beyond the maximum range of our histogram
                overflowCount.getAndIncrement();
                double currentOverflowMax;
                while ( (currentOverflowMax=overflowMax.get()) < value ) {
                    overflowMax.compareAndSet(currentOverflowMax, value);
                }
            } else {
                // increment the bucket count
                int binIndex = bin(value);
                counts.incrementAndGet(binIndex);
            }
        }
    
        private int bin(double value) {
            return (int) Math.floor( (value - min)  / binWidth );
        }
        
        private long[] drain() {
            long[] snapshot = new long[binCount];
            for(int i=0;i!=binCount;++i) {
                snapshot[i] = counts.getAndSet(i, 0);
            }
            return snapshot;
        }
        
        @Override
        public Point drainPoint(DateTime startTime, DateTime endTime) {
            
            List<PointDistributionBucket> buckets = new ArrayList<PointDistributionBucket>();
            double lowerBound = min;
            for(int i=0;i<binCount;++i) {
                // Empty this bin
                long count = counts.getAndSet(i, 0);
                if(count > 0) {
                    // Add as point
                    buckets.add(new PointDistributionBucket()
                            .setLowerBound(lowerBound)
                            .setUpperBound(lowerBound + binWidth)
                            .setCount(count));

                }
                lowerBound += binWidth;
            }
            
            PointDistribution distribution = new PointDistribution()
                    .setBuckets(buckets);
                    
            Point point = new Point()
                    .setStart(endTime)
                    .setEnd(endTime)
                    .setDistributionValue(distribution);
            
            return point;
        }
    }
}

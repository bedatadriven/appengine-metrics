package com.bedatadriven.appengine.metrics;

import com.google.api.client.util.DateTime;
import com.google.api.services.cloudmonitoring.model.Point;
import com.google.api.services.cloudmonitoring.model.PointDistribution;
import com.google.api.services.cloudmonitoring.model.PointDistributionBucket;
import com.google.api.services.cloudmonitoring.model.TimeseriesPoint;
import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.common.collect.Lists;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;


public class Snapshot {

    /**
     * Memcache key used to store the beginning of this reporting interval
     */
    private static final String LAST_REPORT_TIME_KEY = "custom.cloudmonitoring.googleapis.com:lastReportTime";

    /**
     * The number of milliseconds between flushes
     */
    private static final long RESOLUTION = TimeUnit.MINUTES.toMillis(1);
    
    private static final Logger LOGGER = Logger.getLogger(Snapshot.class.getName());
    

    private final MemcacheService memcacheService;
    private final Iterable<Meter> meters;
    private final Iterable<RequestTimer> timers;
    private final DateTime time;

    private MemcacheService.IdentifiableValue periodStart;
    private long periodEnd;
    
    private Map<String, Long> counts = new HashMap<>();
    
    private List<TimeseriesPoint> points = new ArrayList<>();

    public Snapshot(MemcacheService memcacheService) {
        this.memcacheService = memcacheService;
        this.meters = MetricsRegistry.INSTANCE.meters();
        this.timers = MetricsRegistry.INSTANCE.timers();
        this.periodStart = memcacheService.getIdentifiable(LAST_REPORT_TIME_KEY);
        this.periodEnd = System.currentTimeMillis();
        this.time = new DateTime(periodEnd);

    }

    /**
     * Returns true if this is the absolute first time we're reporting
     */
    public boolean isInitial() {
        return periodStart == null;   
    }
    

    /**
     * Reset all buckets to zero and set the beginning of
     * the next interval to the current time
     */
    public static List<TimeseriesPoint> compute() {
        Snapshot snapshot = new Snapshot(MemcacheServiceFactory.getMemcacheService());
        if(snapshot.tick()) {
            snapshot.queryAndResetCounts();

            if (!snapshot.isInitial()) {
                snapshot.computeRates();
            }
            
            snapshot.computeTimerStatistics();
        }
        
        return snapshot.points;
    }

    private void queryAndResetCounts() {
        
        List<String> countKeys = new ArrayList<>();
        
        // Fetch counts for the meters
        for (Meter meter : MetricsRegistry.INSTANCE.meters()) {
            countKeys.add(meter.getCacheKey());
        }
        
        // Fetch bucket counts for histograms
        for (RequestTimer timer : MetricsRegistry.INSTANCE.timers()) {
            String[] keys = timer.getCacheKeys();
            for(int i=0;i<keys.length;++i) {
                countKeys.add(keys[i]);
            }
        }
        
        Map<String, Object> values = memcacheService.getAll(countKeys);
        
        Map<String, Long> decrements = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if(entry.getValue() instanceof Number) {
                long count = ((Number) entry.getValue()).longValue();
                if(count != 0) {
                    counts.put(entry.getKey(), count);
                    decrements.put(entry.getKey(), -count);
                }
            }
        }
        
        // Subtract the counts we're using for this reporting period 
        // from the buckets, any concurrent increments will count towards
        // the next period
        
        memcacheService.incrementAll(decrements);
    }


    private boolean tick() {
        
        if(periodStart == null) {
            return memcacheService.put(LAST_REPORT_TIME_KEY, System.currentTimeMillis(), 
                    Expiration.byDeltaSeconds((int)(RESOLUTION * 3)),
                    MemcacheService.SetPolicy.ADD_ONLY_IF_NOT_PRESENT);
        } else {
            return memcacheService.putIfUntouched(LAST_REPORT_TIME_KEY, periodStart, System.currentTimeMillis());
        }
    }
    
    private long getCount(String key) {
        Long count = counts.get(key);
        if(count == null) {
            return 0L;
        } else {
            return count;
        }
    }

    private void computeRates() {
        
        double periodInSeconds = periodInSeconds();
        
        for (Meter meter : meters) {
            double count = getCount(meter.getCacheKey());
            double rate = count / periodInSeconds;

            Point point = new Point();
            point.setStart(time);
            point.setEnd(time);
            point.setDoubleValue(rate);
            
            TimeseriesPoint timeseriesPoint = new TimeseriesPoint();
            timeseriesPoint.setPoint(point);
            timeseriesPoint.setTimeseriesDesc(meter.getKey().getDescriptor());
            timeseriesPoint.setPoint(point);
            
            points.add(timeseriesPoint);
        }
    }
    
    private void computeTimerStatistics() {

        // Doesn't seem to currently work

        for (RequestTimer timer : timers) {
            
            String keys[] = timer.getCacheKeys();
            long counts[] = new long[keys.length];
            
            long totalCount = 0;
            for(int i=0;i<keys.length;++i) {
                counts[i] = getCount(keys[i]);
                totalCount += counts[i];
            }

            LOGGER.fine(MetricNames.stripBaseName(timer.getKey().getMetricName()) + ": " + Arrays.toString(counts));
            
            if(totalCount > 0) {
                for (TimerStatistic statistic : TimerStatistic.values()) {

                    double estimate = statistic.compute(timer, counts);
                    
                    LOGGER.fine(MetricNames.stripBaseName(timer.getKey().getMetricName()) + 
                            ": " + statistic.name() + " = "  + estimate);

                    Point point = new Point();
                    point.setStart(time);
                    point.setEnd(time);
                    point.setDoubleValue(estimate);

                    TimeseriesPoint timeseriesPoint = new TimeseriesPoint();
                    timeseriesPoint.setPoint(point);
                    timeseriesPoint.setTimeseriesDesc(statistic.descriptor(timer.getKey()));
                    timeseriesPoint.setPoint(point);

                    points.add(timeseriesPoint);
                }
            }
        }
    }

    private PointDistribution computeDistribution(RequestTimer timer) {
        List<PointDistributionBucket> buckets = Lists.newArrayList();
        String[] keys = timer.getCacheKeys();
        for(int i=0;i!=keys.length;++i) {
            long count = getCount(keys[i]);
            if(count > 0) {
                PointDistributionBucket bucket = new PointDistributionBucket();
                bucket.setCount(count);
                bucket.setLowerBound(timer.getLowerBound(i));
                bucket.setUpperBound(timer.getUpperBound(i));
                buckets.add(bucket);
            }
        }

        PointDistribution distribution = new PointDistribution();
        distribution.setBuckets(buckets);
        return distribution;
    }


    private void computeDistributions() {

        // Doesn't seem to currently work
        
        for (RequestTimer timer : timers) {

            PointDistribution distribution = computeDistribution(timer);
            
            Point point = new Point();
            point.setStart(time);
            point.setEnd(time);
            point.setDistributionValue(distribution);

            TimeseriesPoint timeseriesPoint = new TimeseriesPoint();
            timeseriesPoint.setPoint(point);
            timeseriesPoint.setTimeseriesDesc(timer.getKey().getDescriptor());
            timeseriesPoint.setPoint(point);

            points.add(timeseriesPoint);
        }
    }

    private double periodInSeconds() {
        assert periodStart != null;
        long periodStartMillis = ((Number)periodStart.getValue()).longValue();
        long periodMillis = (periodEnd - periodStartMillis);
        return periodMillis / 1000d;
    }
}

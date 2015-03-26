package com.bedatadriven.appengine.metrics;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.modules.ModulesService;
import com.google.appengine.api.modules.ModulesServiceFactory;

import javax.servlet.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Periodically flushes the current metric values to Google Cloud Monitoring.
 */
public class MetricsRequestFilter implements Filter {

    private static final ModulesService MODULES = ModulesServiceFactory.getModulesService();

    private static final Logger LOGGER = Logger.getLogger(MetricsRequestFilter.class.getName());

    private final NonBlockingRateLimiter rateLimiter;
    
    private final MemcacheService memcache = MemcacheServiceFactory.getMemcacheService();

    public MetricsRequestFilter() {
        rateLimiter = new NonBlockingRateLimiter(10, TimeUnit.SECONDS);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        filterChain.doFilter(servletRequest, servletResponse);

        if(rateLimiter.tryAcquire()) {
            flushToMemcache();
        }
    }

    private void flushToMemcache() {
        try {
            LOGGER.info("Flushing metrics to memcache");
            
            Map<String, Long> increments = new HashMap<>();
            
            // Update the counts for meters
            for (Meter meter : MetricsRegistry.INSTANCE.meters()) {
                increments.put(meter.getCacheKey(), meter.drain());
            }
            
            // Update all the buckets for histograms
            for (RequestTimer timer : MetricsRegistry.INSTANCE.timers()) {
                String[] bucketKeys = timer.getCacheKeys();
                long[] bucketCounts = timer.drain();
                
                for(int i=0;i<bucketKeys.length;++i) {
                    long count = bucketCounts[i];
                    if(count > 0) {
                        increments.put(bucketKeys[i], count);
                    }
                }
            }
            
            long initialValueIfMissing = 0L;
            memcache.incrementAll(increments, initialValueIfMissing);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to flush metrics to memcache", e);
        
            // TODO: handle memcache write failures. maybe only reset the counters if the write succeeds?
        }
    }

    @Override
    public void destroy() {
        LOGGER.severe("MetricsRequestFilter.destroy() called");
        flushToMemcache();
    }
}

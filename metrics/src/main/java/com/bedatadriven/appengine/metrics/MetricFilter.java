package com.bedatadriven.appengine.metrics;

import com.google.appengine.api.urlfetch.HTTPResponse;

import javax.servlet.*;
import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Periodically flushes the current metric values to Google Cloud Monitoring.
 */
public class MetricFilter implements Filter {

    private static final Logger LOGGER = Logger.getLogger(MetricFilter.class.getName());

    private long interval;
    private final AtomicLong periodStart;
    private final TimeSeriesAsyncWriter writer;

    public MetricFilter() {
        this.periodStart = new AtomicLong();
        this.writer = new TimeSeriesAsyncWriter();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        periodStart.set(System.currentTimeMillis());
        interval = TimeUnit.SECONDS.toMillis(10);

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        Future<HTTPResponse> writeResponse = maybeReport();

        try {
            filterChain.doFilter(servletRequest, servletResponse);
        
        } finally {
            if(writeResponse != null) {
                HTTPResponse httpResponse;
                try {
                    httpResponse = writeResponse.get();
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Metric write request failed", e);
                    return;
                }
                if(httpResponse.getResponseCode() != 200) {
                    LOGGER.log(Level.WARNING, "Metric write request failed with status code " +
                            httpResponse.getResponseCode() + ": " + new String(httpResponse.getContent()));
                }
            }
        }
    }

    private Future<HTTPResponse> maybeReport() {
        final long oldTick = periodStart.get();
        final long newTick = System.currentTimeMillis();
        final long timeSinceLastExecution = newTick - oldTick;
        if (timeSinceLastExecution > interval) {
            if (periodStart.compareAndSet(oldTick, newTick)) {
                return writer.submit(MetricsRegistry.INSTANCE.drain(oldTick, newTick));
            }
        }
        return null;
    }

    @Override
    public void destroy() {
    }
}

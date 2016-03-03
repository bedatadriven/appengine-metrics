package com.bedatadriven.appengine.metrics;

import javax.servlet.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Periodically flushes the current metric values of this instance to the statsd module
 */
public class MetricsRequestFilter implements Filter {

    private static final Logger LOGGER = Logger.getLogger(MetricsRequestFilter.class.getName());

    private static final NonBlockingRateLimiter RATE_LIMITER = new NonBlockingRateLimiter(10, TimeUnit.SECONDS);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } catch (Error e) {
            throw e;
        }
        
        if(RATE_LIMITER.tryAcquire()) {
            MetricsRegistry.INSTANCE.flush();
        }
        
    }

    @Override
    public void destroy() {
        LOGGER.severe("MetricsRequestFilter.destroy() called");
    }
}

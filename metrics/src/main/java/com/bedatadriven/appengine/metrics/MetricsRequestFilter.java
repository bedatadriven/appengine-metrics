package com.bedatadriven.appengine.metrics;

import javax.servlet.*;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Periodically flushes the current metric values of this instance to Memcache, which stores 
 * the metric values aggregated across instances.
 */
public class MetricsRequestFilter implements Filter {

    private static final Logger LOGGER = Logger.getLogger(MetricsRequestFilter.class.getName());


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {
        LOGGER.severe("MetricsRequestFilter.destroy() called");
    }
}

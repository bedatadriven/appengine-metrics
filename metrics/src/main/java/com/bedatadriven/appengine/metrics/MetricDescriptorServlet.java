package com.bedatadriven.appengine.metrics;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Updates metric descriptors
 */
public class MetricDescriptorServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        MetricsRegistry.INSTANCE.updateDescriptors();
        
        resp.getWriter().println("Updated metric descriptors");
    }
}

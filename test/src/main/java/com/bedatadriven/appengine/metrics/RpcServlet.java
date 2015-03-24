package com.bedatadriven.appengine.metrics;


import com.bedatadriven.appengine.metrics.histogram.Histograms;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;
import com.google.common.base.Stopwatch;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public class RpcServlet extends HttpServlet {

    public static final String LATENCY_METRIC = MetricNames.customMetricName("rpc/time");
    public static final String COMMAND_LABEL = MetricNames.customLabel("command");

    private final DistributionMetric latency;

    public RpcServlet() {
        this.latency = MetricsRegistry.INSTANCE.distribution(LATENCY_METRIC, 
                Histograms.equalIntervals(0, TimeUnit.SECONDS.toMillis(5)));
    }

    @Override
    public void init() throws ServletException {
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String commandName = req.getRequestURI().substring("/rpc/".length());
        
        Distribution distribution = latency.get(TimeseriesKey.labeled(COMMAND_LABEL, commandName));
        
        Stopwatch stopwatch = Stopwatch.createStarted();

        URLFetchService service = URLFetchServiceFactory.getURLFetchService();
        if(commandName.equals("ai")) {
            service.fetch(new URL("http://www.activityinfo.org"));
        } else {
            service.fetch(new URL("http://bedatadriven.com"));
        }

        double elapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        distribution.update(elapsed);
        
        resp.getWriter().println("Command " + commandName + " finished in " + elapsed + " ms");
    }
}

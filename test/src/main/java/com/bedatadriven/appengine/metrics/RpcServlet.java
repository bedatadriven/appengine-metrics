package com.bedatadriven.appengine.metrics;


import com.bedatadriven.rebar.metrics.*;
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

    @Override
    public void init() throws ServletException {
        Histogram.newHistogram(LATENCY_METRIC)
                .defineLabel(COMMAND_LABEL, "RPC Command")
                .useFixedBins(0, 30, 19)
                .build();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String commandName = req.getRequestURI().substring("/rpc/".length());

        HistogramRecorder recorder = MetricsRegistry.INSTANCE
                .histogram(LATENCY_METRIC)
                .get(TimeSeriesKey.label(COMMAND_LABEL, commandName));
        
        Stopwatch stopwatch = Stopwatch.createStarted();
        

        URLFetchService service = URLFetchServiceFactory.getURLFetchService();
        if(commandName.equals("ai")) {
            service.fetch(new URL("http://www.activityinfo.org"));
        } else {
            service.fetch(new URL("http://bedatadriven.com"));
        }

        double elapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS) / 1000d;
        recorder.update(elapsed);
        
        resp.getWriter().println("Command " + commandName + " finished in " + elapsed + " ms");
    }
}

package com.bedatadriven.appengine.metrics;


import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.RealDistribution;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class RpcServlet extends HttpServlet {
    
    public static final String COUNT_METRIC = MetricNames.qualifyCustomMetricName("test/rpc/count");

    public static final String LATENCY_METRIC = MetricNames.qualifyCustomMetricName("test/rpc/time");

    private static final Logger LOGGER = Logger.getLogger(RpcServlet.class.getName());

    private final ImmutableMap<String, RealDistribution> latencyDistributions;

    public RpcServlet() {
        latencyDistributions = ImmutableMap.<String, RealDistribution>of(
                "blue", new NormalDistribution(ThreadLocalRandomGenerator.INSTANCE, 300, 25),
                "red", new NormalDistribution(ThreadLocalRandomGenerator.INSTANCE, 15000, 5000));
    }

    @Override
    public void init() throws ServletException {
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        Stopwatch stopwatch = Stopwatch.createStarted();
        
        String commandName = req.getRequestURI().substring("/rpc/".length());

        MetricsRegistry.INSTANCE.meter(COUNT_METRIC, commandName).mark();

        RealDistribution latencyDistribution = latencyDistributions.get(commandName);
        long latency = Math.round(latencyDistribution.sample());

        try {
            Thread.sleep(latency);
        } catch (InterruptedException e) {
            LOGGER.severe("Interrupted.");
        }


        long elapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        
        MetricsRegistry.INSTANCE.timer(LATENCY_METRIC, commandName).update(elapsed);
        
        resp.getWriter().println(String.format("Command %s latency: %d / %d ms", commandName,
                latency, elapsed));
    }


}

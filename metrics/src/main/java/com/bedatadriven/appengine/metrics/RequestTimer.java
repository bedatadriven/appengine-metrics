package com.bedatadriven.appengine.metrics;

/**
 * Concurrent online histogram, using the same breaks as the 
 * metrics seem to use for request_latency metrics... 
 */
public class RequestTimer {

    private final String key;

    public RequestTimer(String key) {
        this.key = key;
    }


    public void update(double value) {
        update((long)value);
    }

    public void update(long milliseconds) {
        MetricsRegistry.INSTANCE.sendMessage(key + ":" + milliseconds + "|ms");
    }
}

package com.bedatadriven.appengine.metrics;

/**
 * Measures the rate at which an event occurs
 */
public class Meter {
    
    private final String key;
    
    Meter(String key) {
        this.key = key;
    }

    public void mark() {
        MetricsRegistry.INSTANCE.sendMessage(key + ":1|c");
    }
}

package com.bedatadriven.appengine.metrics;


public final class MetricNames {
    
    public static final String CUSTOM_METRICS_BASE_NAME = "custom.cloudmonitoring.googleapis.com/";

    private MetricNames() {}

    
    public static String customMetricName(String name) {
        return CUSTOM_METRICS_BASE_NAME + name;
    }
    
    
    public static String customLabel(String name) {
        return CUSTOM_METRICS_BASE_NAME + name;
    }
}


package com.bedatadriven.appengine.metrics;

import com.google.api.client.googleapis.extensions.appengine.auth.oauth2.AppIdentityCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.cloudmonitoring.CloudMonitoring;
import com.google.api.services.cloudmonitoring.CloudMonitoringScopes;
import com.google.api.services.cloudmonitoring.model.TimeseriesPoint;
import com.google.api.services.cloudmonitoring.model.WriteTimeseriesRequest;
import com.google.appengine.api.utils.SystemProperty;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MetricsRegistry {
    
    
    private static final Logger LOGGER = Logger.getLogger(MetricsRegistry.class.getName());

    public static final MetricsRegistry INSTANCE = new MetricsRegistry();
    
    private final ConcurrentMap<String, Metric> metrics = new ConcurrentHashMap<String, Metric>();

    private CloudMonitoring client;

    private MetricsRegistry() {

    }

    private CloudMonitoring createClient() {
        if(client != null) {
            return client;
        }
        try {
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            JsonFactory jsonFactory = new JacksonFactory();
            HttpRequestInitializer requestInitializer =
                    new AppIdentityCredential(Arrays.asList(CloudMonitoringScopes.MONITORING));

            client = new CloudMonitoring.Builder(httpTransport, jsonFactory, requestInitializer)
                    .setApplicationName(SystemProperty.Environment.applicationId.get())
                    .build();
            
            return client;
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize CloudMonitoring client", e);
        }
    }


    public void register(Metric metric) {
        Metric existingMetric = metrics.putIfAbsent(metric.getDescriptor().getName(), metric);
        if(existingMetric != null) {
            throw new IllegalStateException("Metric with name '" + existingMetric.getDescriptor().getName() + "' " +
                    "is already registered.");
        }
    }

    public Histogram histogram(String name) {
        Metric metric = metrics.get(name);
        if(metric == null) {
            throw new IllegalStateException("Metric '" + name + "' is not registered.");
        }
        return (Histogram)metric;
    }

    public String getProjectId() {
        return SystemProperty.Environment.applicationId.get();
    }
    
    public void updateDescriptors() {
        LOGGER.info("Updating metric descriptors");

        CloudMonitoring client = createClient();

        for (Metric metric : metrics.values()) {
            LOGGER.info("Creating metric descriptor for " + metric.getDescriptor().getName());
            try {
                client.metricDescriptors().create(getProjectId(), metric.getDescriptor()).execute();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to create descriptor for metric " + metric.getDescriptor().getName(), e);
            }
        }
    }

    

    public WriteTimeseriesRequest drain(long periodStart, long periodEnd) {
        DateTime start = new DateTime(periodStart);
        DateTime end = new DateTime(periodEnd);

        List<TimeseriesPoint> points = Lists.newArrayList();
        for(Metric metric : metrics.values()) {
            metric.drainTo(start, end, points);
        }

        WriteTimeseriesRequest request = new WriteTimeseriesRequest();
        request.setTimeseries(points);
        return request;
    }

}
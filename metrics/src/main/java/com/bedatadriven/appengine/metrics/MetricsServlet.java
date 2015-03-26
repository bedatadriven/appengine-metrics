package com.bedatadriven.appengine.metrics;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.extensions.appengine.auth.oauth2.AppIdentityCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.client.util.DateTime;
import com.google.api.services.cloudmonitoring.CloudMonitoring;
import com.google.api.services.cloudmonitoring.CloudMonitoringScopes;
import com.google.api.services.cloudmonitoring.model.*;
import com.google.appengine.api.utils.SystemProperty;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reads the current, aggregated metric values from memcache once a minute and 
 * reports the results to the Cloud Monitoring Service's REST API.
 *
 */
public class MetricsServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(MetricsServlet.class.getName());

    private final JsonFactory jsonFactory = new JacksonFactory();

    /**
     * Local cache of metrics that we've already updated/created
     */
    private final ConcurrentHashMap<String, Boolean> registeredMetrics = new ConcurrentHashMap<String, Boolean>();


    public MetricsServlet() {
    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        if(!Strings.isNullOrEmpty(req.getParameter("query"))) {
            query(req.getParameter("query"), resp);

        } else {
            report();
        }
    }

    private void query(String query, HttpServletResponse resp) throws IOException {
        CloudMonitoring client = createClient();
        ListTimeseriesResponse response = client.timeseries().list(getProjectId(), query, new DateTime(System.currentTimeMillis()).toStringRfc3339()).execute();

        resp.setContentType("text/plain");
        resp.getWriter().write(jsonFactory.toPrettyString(response));
    }

    private void report() throws IOException {
        List<TimeseriesPoint> points = Snapshot.compute();
        if(points.isEmpty()) {
            LOGGER.info("No metrics to flush");

        } else {
            sendPoints(points);
        }
    }

    private void sendPoints(List<TimeseriesPoint> points) throws IOException {
        CloudMonitoring client = createClient();
        updateMetricDescriptors(client);

        writeTimeseries(client, points);
    }

    private CloudMonitoring createClient() throws IOException {
        HttpTransport transport = null;
        try {
            transport = GoogleNetHttpTransport.newTrustedTransport();
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Could not create HttpTransport", e);
        }

        GoogleCredential credential =
                new AppIdentityCredential.AppEngineCredentialWrapper(transport, jsonFactory)
                        .createScoped(CloudMonitoringScopes.all());

        return new CloudMonitoring.Builder(transport, jsonFactory, credential)
                .setApplicationName(getProjectId())
                .build();

    }


    public static String getProjectId() {
        return SystemProperty.applicationId.get();
    }


    private void updateMetricDescriptors(CloudMonitoring client) {
        for (Meter meter : MetricsRegistry.INSTANCE.meters()) {
            Boolean created = registeredMetrics.get(meter.getKey().getMetricName());
            if (created != Boolean.TRUE) {
                updateMeterDescriptors(client, meter);
            }
            registeredMetrics.put(meter.getKey().getMetricName(), true);

        }
        for (RequestTimer timer : MetricsRegistry.INSTANCE.timers()) {
            Boolean created = registeredMetrics.get(timer.getKey().getMetricName());
            if(created != Boolean.TRUE) {
                updateTimerDescriptors(client, timer);
            }
            registeredMetrics.put(timer.getKey().getMetricName(), true);
        }
    }


    private void updateMeterDescriptors(CloudMonitoring client, Meter meter) {

        MetricProperties properties = MetricProperties.get(meter.getKey().getMetricName());

        MetricDescriptor descriptor = new MetricDescriptor();
        descriptor.setName(meter.getKey().getMetricName());
        descriptor.setDescription(properties.getDescription());
        descriptor.setLabels(defaultLabels());
        descriptor.setTypeDescriptor(meter.getTypeDescriptor());

        updateDescriptor(client, descriptor);
    }

    private List<MetricDescriptorLabelDescriptor> defaultLabels() {
        return Arrays.asList(
                new MetricDescriptorLabelDescriptor()
                        .setKey(MetricNames.KIND_LABEL));
    }


    private void updateTimerDescriptors(CloudMonitoring client, RequestTimer timer) {

        MetricProperties properties = MetricProperties.get(timer.getKey());

        for(TimerStatistic statistic : TimerStatistic.values()) {
            MetricDescriptor descriptor = new MetricDescriptor();
            descriptor.setName(statistic.metricName(timer.getKey()));
            if(properties.getDescription() != null) {
                descriptor.setDescription(String.format("%s (%s)", properties.getDescription(), 
                        statistic.getDescription())); 
            }
            descriptor.setLabels(defaultLabels());
            descriptor.setTypeDescriptor(new MetricDescriptorTypeDescriptor()
                    .setMetricType("gauge")
                    .setValueType("double"));

            updateDescriptor(client, descriptor);
        }
    }

    private void updateDescriptor(CloudMonitoring client, MetricDescriptor descriptor) {
        try {
            LOGGER.fine("Updating metric descriptor: " + jsonFactory.toPrettyString(descriptor));
        } catch (IOException ignored) {
        }

        try {
            MetricDescriptor response = client.metricDescriptors().create(getProjectId(), descriptor).execute();

            LOGGER.fine("Successfully updated metric descriptor for " + response.getName());


        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to create metric descriptor for metric " + descriptor.getName(), e);
        }
    }

    private void writeTimeseries(CloudMonitoring client, List<TimeseriesPoint> points) throws IOException {
        WriteTimeseriesRequest request = new WriteTimeseriesRequest();
        request.setTimeseries(points);

        if(LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Writing timeseries: " + jsonFactory.toPrettyString(request));
        }

        try {
            client.timeseries().write(getProjectId(), request).execute();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Writing timeseries failed", e);
        }
    }
}

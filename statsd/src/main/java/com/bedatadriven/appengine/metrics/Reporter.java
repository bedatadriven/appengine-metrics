package com.bedatadriven.appengine.metrics;


import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.extensions.appengine.auth.oauth2.AppIdentityCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.cloudmonitoring.CloudMonitoring;
import com.google.api.services.cloudmonitoring.CloudMonitoringScopes;
import com.google.api.services.cloudmonitoring.model.*;
import com.google.appengine.api.utils.SystemProperty;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Background thread which handles the queue of snapshots emitted by the {@link Aggregator}  and posts them
 * to the Google Cloud Monitoring API.
 */
class Reporter implements Runnable  {

  private static final Logger LOGGER = Logger.getLogger(Reporter.class.getName());
  
  public static final ArrayBlockingQueue<Snapshot> FLUSH_QUEUE = new ArrayBlockingQueue<>(100);
  

  /**
   * Local cache of metrics that we've already updated/created
   */
  private final Set<String> registeredMetrics = new HashSet<>();

  /**
   * For each metric, track the labels that have been registered
   */
  private final Multimap<String, String> registeredLabels = HashMultimap.create();


  @Override
  public void run() {
    while(true) {
      Snapshot snapshot;
      try {
        snapshot = FLUSH_QUEUE.take();
      } catch (InterruptedException e) {
        LOGGER.severe("Reporter thread interrupted.");
        return;
      }

      try {
        flush(snapshot);
      } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "Failed to flush snapshot: " + e.getMessage(), e);
      }
    }
  }

  void flush(Snapshot snapshot) throws IOException {
    
    LOGGER.info("Starting flush. Registered metrics: " + registeredMetrics);

    ReportBuilder builder = new ReportBuilder(new DateTime(snapshot.getTime()));
    for (TimingStatistic timer : snapshot.getTimers()) {
      timer.reportTo(builder);
    }
    for (CountStatistic count : snapshot.getCounts()) {
      count.reportTo(builder);
    }

    if(builder.isEmpty()) {
      LOGGER.info("No metrics to flush");
      return;
    }

    CloudMonitoring client = createClient();
    updateMetricDescriptors(client, builder);
    writeTimeseries(client, builder.points());
  }

  private CloudMonitoring createClient() throws IOException {
    HttpTransport transport = null;
    try {
      transport = GoogleNetHttpTransport.newTrustedTransport();
    } catch (GeneralSecurityException e) {
      throw new RuntimeException("Could not create HttpTransport", e);
    }

    JsonFactory jsonFactory = new JacksonFactory();
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


  private void updateMetricDescriptors(CloudMonitoring client, ReportBuilder builder) {

    LOGGER.info("Metrics: " + builder.gaugeMetrics());
    
    for (String metricName : builder.gaugeMetrics()) {
      if(!registeredMetrics.contains(metricName) ||
          !registeredLabels.get(metricName).containsAll(builder.labels(metricName))) {

        updateGaugeDescriptor(client, metricName, builder);
      }
    }

    for (String metricName : builder.countMetrics()) {
      if(!registeredMetrics.contains(metricName) ||
         !registeredLabels.get(metricName).containsAll(builder.labels(metricName))) {
        
        updateCountMetric(client, metricName, builder);
      }
    }
  }


  private void updateGaugeDescriptor(CloudMonitoring client, String metricName, ReportBuilder builder) {
    
    LOGGER.fine("Updating gauge descriptor " + metricName + " with labels " + builder.labels(metricName));
    
    MetricDescriptor descriptor = new MetricDescriptor();
    descriptor.setName(metricName);
    descriptor.setLabels(labelDescriptors(builder.labels(metricName)));
    descriptor.setTypeDescriptor(new MetricDescriptorTypeDescriptor()
        .setMetricType("gauge")
        .setValueType("double"));

    updateDescriptor(client, descriptor);

    registeredMetrics.add(metricName);
    registeredLabels.putAll(metricName, builder.labels(metricName));
  
  }


  private void updateCountMetric(CloudMonitoring client, String metricName, ReportBuilder builder) {
    
    LOGGER.fine("Updating count descriptor " + metricName + " with labels " + builder.labels(metricName));


    MetricDescriptor descriptor = new MetricDescriptor();
    descriptor.setName(metricName);
    descriptor.setLabels(labelDescriptors(builder.labels(metricName)));
    descriptor.setTypeDescriptor(new MetricDescriptorTypeDescriptor()
        .setMetricType("gauge")
        .setValueType("double"));

    updateDescriptor(client, descriptor);
    
    registeredMetrics.add(metricName);
    registeredLabels.putAll(metricName, builder.labels(metricName));
  }

  private List<MetricDescriptorLabelDescriptor> labelDescriptors(Collection<String> labelKeys) {
    List<MetricDescriptorLabelDescriptor> descriptors = Lists.newArrayList();
    for (String labelKey : labelKeys) {
      descriptors.add(new MetricDescriptorLabelDescriptor().setKey(labelKey));
    }
    return descriptors;
  }

  private void updateDescriptor(CloudMonitoring client, MetricDescriptor descriptor) {
    LOGGER.fine("Updating metric descriptor: " + descriptor.getName());

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

//    if(LOGGER.isLoggable(Level.FINE)) {
//      LOGGER.log(Level.FINE, "Writing timeseries: " + jsonFactory.toPrettyString(request));
//    }

    try {
      client.timeseries().write(getProjectId(), request).execute();
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Writing timeseries failed", e);
    }
  }
}

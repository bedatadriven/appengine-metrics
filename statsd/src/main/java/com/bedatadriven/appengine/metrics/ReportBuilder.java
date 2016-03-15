package com.bedatadriven.appengine.metrics;

import com.google.api.client.util.DateTime;
import com.google.api.services.cloudmonitoring.model.Point;
import com.google.api.services.cloudmonitoring.model.TimeseriesDescriptor;
import com.google.api.services.cloudmonitoring.model.TimeseriesPoint;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import java.util.*;

/**
 * Creates a list of TimeSeriesPoints
 */
class ReportBuilder {


  private static final String CUSTOM_METRIC_PREFIX = "custom.cloudmonitoring.googleapis.com";
  
  private DateTime startTime;
  private DateTime endTime;
  private List<TimeseriesPoint> points = Lists.newArrayList();

  private Set<String> gaugeMetrics = new HashSet<>();
  private Set<String> countMetrics = new HashSet<>();

  private Multimap<String, String> metricLabels = HashMultimap.create();
  
  public ReportBuilder(DateTime startTime, DateTime endTime) {
    this.startTime = startTime;
    this.endTime = endTime;
  }

  public ReportBuilder(DateTime dateTime) {
    this(dateTime, dateTime);
  }


  private TimeseriesDescriptor keyToDescriptor(String key) {

    // Existing clients have already included the custom metrix prefix
    // Strip it off
    if(key.startsWith(CUSTOM_METRIC_PREFIX)) {
      key = key.substring(CUSTOM_METRIC_PREFIX.length());
    }

    String[] path = key.split("\\.");

    StringBuilder metricName = new StringBuilder(CUSTOM_METRIC_PREFIX);
    Map<String, String> labels = new HashMap<>();

    for(String component : path) {
      if(component.indexOf('=') != -1) {
        String[] keyValue = component.split("=");
        String labelKey = keyValue[0];
        String labelValue = keyValue[1];
        
        if(!labelKey.startsWith(CUSTOM_METRIC_PREFIX)) {
          labelKey = CUSTOM_METRIC_PREFIX + "/" + labelKey;
        }
        
        labels.put(labelKey, labelValue);
      } else {
        if(metricName.length() > 0 && !component.startsWith("/")) {
          metricName.append("/");
        }
        metricName.append(component);
      }
    }

    TimeseriesDescriptor descriptor = new TimeseriesDescriptor();
    descriptor.setMetric(metricName.toString());
    descriptor.setLabels(labels);
    return descriptor;
  }
  
  public void addGauge(String key, double value) {

    Point point = new Point();
    point.setStart(startTime);
    point.setEnd(endTime);
    point.setDoubleValue(value);

    TimeseriesDescriptor descriptor = keyToDescriptor(key);

    TimeseriesPoint timeseriesPoint = new TimeseriesPoint();
    timeseriesPoint.setTimeseriesDesc(descriptor);
    timeseriesPoint.setPoint(point);

    gaugeMetrics.add(descriptor.getMetric());
    metricLabels.putAll(descriptor.getMetric(), descriptor.getLabels().keySet());
    
    points.add(timeseriesPoint);
  }


  /**
   * Adds a count as a metered value in counts / second.
   * @param key
   * @param count
   */
  public void addCount(String key, long count) {

    Point point = new Point();
    point.setStart(startTime);
    point.setEnd(endTime);
    point.setDoubleValue((double) count / 60.d);

    TimeseriesDescriptor descriptor = keyToDescriptor(key);

    TimeseriesPoint timeseriesPoint = new TimeseriesPoint();
    timeseriesPoint.setTimeseriesDesc(descriptor);
    timeseriesPoint.setPoint(point);

    countMetrics.add(descriptor.getMetric());
    metricLabels.putAll(descriptor.getMetric(), descriptor.getLabels().keySet());

    points.add(timeseriesPoint);
  }

  public boolean isEmpty() {
    return points.isEmpty();
  }

  public List<TimeseriesPoint> points() {
    return points;
  }

  public Set<String> gaugeMetrics() {
    return gaugeMetrics;
  }
  
  public Set<String> countMetrics() {
    return countMetrics;
  }
  
  public Collection<String> labels(String metricName) {
    return metricLabels.get(metricName);
  }

}

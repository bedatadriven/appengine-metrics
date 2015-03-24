package com.bedatadriven.appengine.metrics;


import com.google.api.client.util.DateTime;
import com.google.api.services.cloudmonitoring.model.Point;

public interface Recorder {
    Point drainPoint(DateTime startTime, DateTime endTime);
}

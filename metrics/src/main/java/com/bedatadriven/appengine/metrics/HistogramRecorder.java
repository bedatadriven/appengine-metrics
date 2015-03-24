package com.bedatadriven.appengine.metrics;


/**
 * Records measurements of a value to be summarized in a histogram
 */
public interface HistogramRecorder extends Recorder {
    
    void update(double value);
    
}

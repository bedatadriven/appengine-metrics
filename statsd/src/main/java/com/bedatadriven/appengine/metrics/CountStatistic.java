package com.bedatadriven.appengine.metrics;

/**
 * Tracks count statistic
 */
public class CountStatistic {
  
  private final String key;
  
  private long count = 0;
  
  public CountStatistic(String key) {
    this.key = key;
  }
  
  public void add(long count) {
    this.count += count;
  }


  public void reportTo(ReportBuilder builder) {
    builder.addCount(key, count);
  }
}

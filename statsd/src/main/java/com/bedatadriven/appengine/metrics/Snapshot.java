package com.bedatadriven.appengine.metrics;


import java.util.Collection;

/**
 * Immutable snapshot of the state of the timers for a given interval
 */
public class Snapshot {

  private final long time;
  private final Collection<TimingStatistic> timers;
  private final Collection<CountStatistic> counts;

  public Snapshot(Collection<TimingStatistic> timers, Collection<CountStatistic> counts) {
    this.time = System.currentTimeMillis();
    this.timers = timers;
    this.counts = counts;
  }
  public long getTime() {
    return time;
  }

  public Iterable<TimingStatistic> getTimers() {
    return timers;
  }

  public Collection<CountStatistic> getCounts() {
    return counts;
  }
}

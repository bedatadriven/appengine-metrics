package com.bedatadriven.appengine.metrics;

import org.junit.Test;

import static com.bedatadriven.appengine.metrics.TimerStatistic.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.closeTo;
import static org.junit.Assert.assertThat;

public class TimerStatisticTest {

    
    @Test
    public void test() {
        
        RequestTimer timer = new RequestTimer(new TimeseriesKey("rpc"));
        timer.update(1L);
        timer.update(500L);
        timer.update(950L);
        timer.update(5000L);
        
        long counts[] = timer.drain();
        
        assertThat(MEDIAN.name(), (int) MEDIAN.compute(timer, counts), equalTo(512));
        assertThat(PCTL95.name(), (int) PCTL95.compute(timer, counts), equalTo(1024));
        assertThat(PCTL5.name(), (int) PCTL5.compute(timer, counts), equalTo(8192));
        assertThat(MEAN.name(), MEAN.compute(timer, counts), closeTo(1824, 10));
    }

    @Test
    public void oneDataPoint() {

        RequestTimer timer = new RequestTimer(new TimeseriesKey("rpc"));
        timer.update(42L);

        long counts[] = timer.drain();

        assertThat(MEDIAN.name(), (int) MEDIAN.compute(timer, counts), equalTo(64));
        assertThat(PCTL95.name(), (int) PCTL95.compute(timer, counts), equalTo(64));
        assertThat(PCTL5.name(), (int) PCTL5.compute(timer, counts), equalTo(64));
        assertThat(MEAN.name(), MEAN.compute(timer, counts), closeTo(48, 1));
    }
    

}
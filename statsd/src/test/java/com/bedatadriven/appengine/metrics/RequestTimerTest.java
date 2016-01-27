package com.bedatadriven.appengine.metrics;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class RequestTimerTest {
    
    @Test
    public void bucketIndexTest() {
        TimingStatistic histogram = new TimingStatistic("rpc");

        // Bucket bins are based pow of 2
        // 0 = [0, 2)
        // 1 = [2, 4)
        // 2 = [4, 8)
        // 3 = [8, 16)
        // 4 = [16, 32)
        // 5 = [32, 64)
        // 6 = [64, 128)
        // 7 = [128, 256)
        // 8 = [256, 512)
        // 9 = [512, 1024)
        // 10 = [1024, 2048)
        // 11 = [2048, 4096)
        // 12 = [4096, 8192)
        // 13 = [8192, 16384)
        // 14 = [16384, 32768)
        // 15 = [32768, 65536)
        // 16 = [65536, 131072)
        // 17 = [131072, 262144)
        
        assertThat(histogram.indexOf(0), equalTo(0));
        assertThat(histogram.indexOf(1), equalTo(0));
        assertThat(histogram.indexOf(2), equalTo(1));
        assertThat(histogram.indexOf(3), equalTo(1));
        assertThat(histogram.indexOf(4), equalTo(2));
        assertThat(histogram.indexOf(5), equalTo(2));
        assertThat(histogram.indexOf(32), equalTo(5));
        assertThat(histogram.indexOf(64), equalTo(6));
        assertThat(histogram.indexOf(127), equalTo(6));
        assertThat(histogram.indexOf(128), equalTo(7));
        assertThat(histogram.indexOf(65536), equalTo(16));
        assertThat(histogram.indexOf(65537), equalTo(16));
    }

}
package com.bedatadriven.appengine.metrics;

import com.google.common.util.concurrent.RateLimiter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MeterIT {
    
    
    public static void main(String[] args) {

        int blueRatePerSecond = 15;
        int redRatePerSecond = 30;
        RateLimiter blueRate = RateLimiter.create(blueRatePerSecond);
        RateLimiter redRate = RateLimiter.create(redRatePerSecond);
        
        ExecutorService executor = Executors.newCachedThreadPool();
//        for(int i=0;i<blueRatePerSecond;++i) {
//            executor.submit(new RpcInvoker("blue", blueRate));
//        }
        for(int i=0;i<(redRatePerSecond*5);++i) {
            executor.submit(new RpcInvoker("red", redRate));
        }
        
    }
}

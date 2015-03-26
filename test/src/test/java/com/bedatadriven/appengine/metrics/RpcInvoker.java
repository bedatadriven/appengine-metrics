package com.bedatadriven.appengine.metrics;


import com.google.common.io.ByteStreams;
import com.google.common.util.concurrent.RateLimiter;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class RpcInvoker implements Runnable {

    private final URL url;
    private RateLimiter rateLimiter;

    public RpcInvoker(String commandName, RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
        try {
            this.url = new URL("https://metrics-dot-ai-capacity-test.appspot.com/rpc/" + commandName);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        while(true) {
            rateLimiter.acquire();
            invoke();
        }
    }

    private void invoke() {
        try {
            InputStream in = url.openStream();
            byte[] body = ByteStreams.toByteArray(in);
            System.out.println(new String(body));
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

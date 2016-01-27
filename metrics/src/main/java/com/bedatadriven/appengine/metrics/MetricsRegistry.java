
package com.bedatadriven.appengine.metrics;

import com.google.appengine.api.modules.ModulesService;
import com.google.appengine.api.modules.ModulesServiceFactory;
import com.google.appengine.api.urlfetch.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central, per-instance registry of metrics.
 *
 * <p>Each new AppEngine instance will have its own set of records that can be 
 * safely updated by concurrent request threads. The MetricRequestFilter will periodically
 * flush the in-memory results to Memcache</p>
 * 
 * <p>Every 60 seconds, the /metrics con job will flush the aggregated metrics out to Google Cloud
 * Metrics through the restful API</p>
 */
public final class MetricsRegistry {

    private static final Logger LOGGER = Logger.getLogger(MetricsRegistry.class.getName());

    public static final MetricsRegistry INSTANCE = new MetricsRegistry();
    
    private final URL reportUrl = reportUrl();

    private final URLFetchService fetchService  = URLFetchServiceFactory.getURLFetchService();
    
    private static URL reportUrl() {
        ModulesService modulesService = ModulesServiceFactory.getModulesService();
        try {
            return new URL("http://" + modulesService.getVersionHostname("statsd", null) + "/report");
        } catch (MalformedURLException e) {
            LOGGER.severe("Unexpected problem: " + e.getMessage());
            return null;
        }
    }
    
    private String key(String name, String kind) {
        return name + ".kind=" + kind;
    }
    
    public Meter meter(String name, String label) {
        return new Meter(key(name, label));
    }

    public RequestTimer timer(String name, String label) {
        return new RequestTimer(key(name, label));
    }
    
    public void sendMessage(String message) {
        HTTPRequest request = new HTTPRequest(reportUrl, HTTPMethod.POST);
        request.setPayload(message.getBytes(StandardCharsets.UTF_8));
        
        // Start async request but don't wait for result
        // This is as close as we can get to UDP-style "fire and forget"
        fetchService.fetchAsync(request);
    }

}
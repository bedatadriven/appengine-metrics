package com.bedatadriven.appengine.metrics;

import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.cloudmonitoring.CloudMonitoring;
import com.google.api.services.cloudmonitoring.CloudMonitoringScopes;
import com.google.api.services.cloudmonitoring.model.WriteTimeseriesRequest;
import com.google.appengine.api.appidentity.AppIdentityService;
import com.google.appengine.api.appidentity.AppIdentityServiceFactory;
import com.google.appengine.api.urlfetch.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Future;
import java.util.logging.Logger;

/**
 * Post time series asynchronously
 */
public class TimeSeriesAsyncWriter {
    
    private static final Logger LOGGER = Logger.getLogger(TimeSeriesAsyncWriter.class.getName());

    private final AppIdentityService appIdentityService = AppIdentityServiceFactory.getAppIdentityService();
    private final URLFetchService fetchService = URLFetchServiceFactory.getURLFetchService();
    private final URL endpoint;
    private JacksonFactory jacksonFactory = new JacksonFactory();

    private AppIdentityService.GetAccessTokenResult token;

    public TimeSeriesAsyncWriter() {
        this.endpoint = serviceUrl();
    }

    private static URL serviceUrl() {
        try {
            return new URL(CloudMonitoring.DEFAULT_BASE_URL + MetricsRegistry.INSTANCE.getProjectId() + "/timeseries:write");
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Cloud monitoring endpoint is malformed", e);
        }
    }

    private String accessToken() {
        if(token == null || token.getExpirationTime().getTime() > System.currentTimeMillis()) {
            token = appIdentityService.getAccessToken(CloudMonitoringScopes.all());
        }
        return token.getAccessToken();
    }

    public Future<HTTPResponse> submit(WriteTimeseriesRequest writeRequest) {

        LOGGER.fine("Flushing metrics to " + endpoint + "...");

        byte[] payload;
        try {
            payload = jacksonFactory.toByteArray(writeRequest);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        LOGGER.fine("Payload: " + new String(payload));
        
        HTTPRequest httpRequest = new HTTPRequest(endpoint, HTTPMethod.POST);
        httpRequest.setHeader(new HTTPHeader("Authorization", "Bearer " + accessToken()));
        httpRequest.setHeader(new HTTPHeader("Content-Type", "application/json"));
        httpRequest.setPayload(payload);
        
        return fetchService.fetchAsync(httpRequest);
    }
}

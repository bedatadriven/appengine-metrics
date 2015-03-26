package com.bedatadriven.appengine.metrics;

import com.google.api.client.repackaged.com.google.common.base.Joiner;

import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

import static com.google.common.collect.Lists.reverse;
import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.newInputStreamSupplier;
import static java.util.Arrays.asList;

/**
 * Reads properties of metrics from a .properties file
 */
public class MetricProperties {
    
    public static final Logger LOGGER = Logger.getLogger(MetricProperties.class.getName());
    private Properties properties;

    private MetricProperties(Properties properties) {
        this.properties = properties;
    }
    
    public String getDescription() {
        return properties.getProperty("description");
    }


    public static MetricProperties get(String metricName) {
        String hostAndPath[] = metricName.split("/", 2);
        String host = reverseHostname(hostAndPath[0]);
        String path = hostAndPath[1];
        String propertiesResource = host + "/" + path + ".properties";

        Properties properties = new Properties();
        try(InputStream in  = newInputStreamSupplier(getResource(propertiesResource)).getInput()) {
            properties.load(in);
        } catch (Exception e) {
            LOGGER.warning(String.format("Cannot read metric properties from '%s': %s", propertiesResource, e.getMessage()));
        }

        return new MetricProperties(properties);
    }

    private static String reverseHostname(String s) {
        return Joiner.on('/').join(reverse(asList(s.split("\\."))));
    }


    public static MetricProperties get(TimeseriesKey key) {
        return get(key.getMetricName());
    }
}

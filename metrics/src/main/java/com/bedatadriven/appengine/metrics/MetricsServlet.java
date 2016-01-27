package com.bedatadriven.appengine.metrics;

import javax.servlet.http.HttpServlet;

/**
 * Reads the current, aggregated metric values from memcache once a minute and 
 * reports the results to the Cloud Monitoring Service's REST API.
 *
 */
@Deprecated
public class MetricsServlet extends HttpServlet {

}

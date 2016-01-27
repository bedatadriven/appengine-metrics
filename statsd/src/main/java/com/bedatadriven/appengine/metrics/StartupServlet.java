package com.bedatadriven.appengine.metrics;

import com.google.appengine.api.ThreadManager;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Handles the initial /_ah/start request by starting the background aggregator thread.
 */
public class StartupServlet extends HttpServlet {
    
    private static final Logger LOGGER = Logger.getLogger(StartupServlet.class.getName()); 

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Thread aggregatorThread = ThreadManager.createBackgroundThread(new Aggregator());
        aggregatorThread.start();

        LOGGER.info("Background aggregation thread started.");

        Thread reporterThread = ThreadManager.createBackgroundThread(new Reporter());
        reporterThread.start();

        LOGGER.info("Background reporter thread started.");
    }
}

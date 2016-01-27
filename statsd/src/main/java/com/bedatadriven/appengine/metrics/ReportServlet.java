package com.bedatadriven.appengine.metrics;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles metrics reported to /report
 */
public class ReportServlet extends HttpServlet {

  private static final Logger LOGGER = Logger.getLogger(ReportServlet.class.getName());

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

    // Do not spend anytime processing, put immediately to the queue and allow the request to complete
    String message = req.getParameter("m");

    LOGGER.info("Received message: " + message);

    Aggregator.MESSAGE_QUEUE.add(message);
    resp.setStatus(HttpServletResponse.SC_OK);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String message = CharStreams.toString(new InputStreamReader(req.getInputStream(), Charsets.UTF_8));

    Aggregator.MESSAGE_QUEUE.add(message);
    resp.setStatus(HttpServletResponse.SC_OK);
  }
}

package com.bedatadriven.appengine.metrics;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class QueueStatsServlet extends HttpServlet {

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    resp.setStatus(HttpServletResponse.SC_OK);
    PrintWriter writer = resp.getWriter();
    writer.println("Aggregator.MESSAGE_QUEUE.size() = " + Aggregator.MESSAGE_QUEUE.size());
    writer.close();
  }
}

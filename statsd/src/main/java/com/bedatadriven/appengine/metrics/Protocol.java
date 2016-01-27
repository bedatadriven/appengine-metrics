package com.bedatadriven.appengine.metrics;

/**
 * Parses the statsd protocal message
 */
class Protocol {
  
  public static final String GAUGE = "g";
  public static final String TIMING = "ms";
  public static final String SET = "s";
  public static final String COUNT = "c";
  

  public static String[] parse(String message) {
    int namespaceEnd = message.indexOf(':');
    int valueEnd = message.indexOf('|');
    
    return new String[] { message.substring(0, namespaceEnd),
                          message.substring(namespaceEnd+1, valueEnd),
                          message.substring(valueEnd+1) };
    
  }
}

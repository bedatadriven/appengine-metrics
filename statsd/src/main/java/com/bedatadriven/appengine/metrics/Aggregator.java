package com.bedatadriven.appengine.metrics;

import com.google.common.collect.Maps;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Background thread which process the queue of incoming messages 
 * sequentially, updating the statistics as it goes.
 *
 */
class Aggregator implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(Aggregator.class.getName());

    public static final ArrayBlockingQueue<String> MESSAGE_QUEUE = new ArrayBlockingQueue<>(50000);

    private Map<String, TimingStatistic> timers = Maps.newHashMap();
    private Map<String, CountStatistic> counts = Maps.newHashMap();

    private long lastFlushTime = 0;

    private long flushInterval = TimeUnit.MINUTES.toMillis(1);

    @Override
    public void run() {
        
        List<String> toProcess = new ArrayList<>();
        
        while(true) {
            String message;
            try {
                message = MESSAGE_QUEUE.poll(timeUntilNextFlush(), TimeUnit.MILLISECONDS);
                
                if(message != null) {
                    processMessage(message);

                    // Handle any backlog in the queue immediately
                    try {
                        MESSAGE_QUEUE.drainTo(toProcess);
                        for (String i : toProcess) {
                            processMessage(i);
                        }
                    } finally {
                        toProcess.clear();
                    }
                }

                long timeSinceLastFlush = System.currentTimeMillis() - lastFlushTime;
                if(timeSinceLastFlush > flushInterval) {
                    flush();
                }
                
            } catch (InterruptedException e) {
                LOGGER.severe("Aggregator interrupted.");
                return;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Exception processing queue: " + e.getMessage(), e);
            } 
        }
    }

    private long timeUntilNextFlush() {
        long timeSinceLastFlush = System.currentTimeMillis() - lastFlushTime;
        if(timeSinceLastFlush > flushInterval) {
            return 0;
        }

        return flushInterval - timeSinceLastFlush;
    }

    void processMessage(String message) {

        String messageParts[];
        try {
            messageParts = Protocol.parse(message);
        } catch (Exception e) {
            LOGGER.severe("Error parsing message '" + message + "'");
            return;
        }

        String type = messageParts[2];
        if(type.equals(Protocol.TIMING)) {
            addTiming(messageParts[0], messageParts[1]);
        } else if(type.equals(Protocol.COUNT)) {
            addCount(messageParts[0], messageParts[1]);

        } else {
            LOGGER.severe("Unknown message type:" + type);
        }
    }


    private void addTiming(String namespace, String millisecondsString) {
        int ms = Integer.parseInt(millisecondsString);

        TimingStatistic histogram = timers.get(namespace);
        if(histogram == null) {
            histogram = new TimingStatistic(namespace);
            timers.put(namespace, histogram);
        }
        histogram.update(ms);
    }


    private void addCount(String namespace, String countString) {

        CountStatistic count = counts.get(namespace);
        if(count == null) {
            count = new CountStatistic(namespace);
            counts.put(namespace, count);
        }

        count.add(Long.parseLong(countString));
    }



    void flush() {
        // Create a new snapshot object with the current state
        // of all our statistics
        Snapshot snapshot = new Snapshot(timers.values(), counts.values());

        // Reset our statistics
        this.timers = new HashMap<>();
        this.counts = new HashMap<>();

        lastFlushTime = System.currentTimeMillis();

        // Now hand of the snapshot to the reporting thread
        Reporter.FLUSH_QUEUE.add(snapshot);
    }
}

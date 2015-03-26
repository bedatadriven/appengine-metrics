package com.bedatadriven.appengine.metrics;

import com.google.common.base.Preconditions;

/**
 * Validates metric and label names
 */
class MetricNames {
    
    public static final String CUSTOM_BASE = "custom.cloudmonitoring.googleapis.com/";
    
    public static final String KIND_LABEL = CUSTOM_BASE + "kind";

    private static final String ALLOWED_CHARACTERS = "/\\$-_.+!*'()%";
    
    public static String qualifyCustomMetricName(String name) {
        Preconditions.checkNotNull(name, "name");
        if(name.startsWith(CUSTOM_BASE)) {
            return validateCustomMetricName(name);
        } else {
            return validateCustomMetricName(CUSTOM_BASE + name);
        }
    }
    
    public static String qualifyCustomLabelKey(String key) {
        Preconditions.checkNotNull(key);
        if(isQualified(key)) {
           return validateCustomMetricName(key); 
        } else {
            return validateCustomMetricName(CUSTOM_BASE + key);
        }
    }

    private static boolean isQualified(String key) {
        return key.matches("^[a-z\\.]+\\.googleapis.com/.+");
    }

    private static String validateCustomMetricName(String name) {
        if(name.length() > 100) {
            throw new IllegalArgumentException(
                    String.format("Invalid custom metric name '%s': names must be 100 characters or less.", name));
        }
        for(int i=0;i<name.length();++i) {
            if(!isValidChar(name.charAt(i))) {
                throw new IllegalArgumentException(
                        String.format("Invalid custom metric name '%s': " +
                                "names can include letters, numbers, and: / \\ $ - _ . + ! * ' () %", name));
            }
        }
        return name;
    }

    private static boolean isValidChar(char c) {
        return Character.isDigit(c) || Character.isLetter(c) || (ALLOWED_CHARACTERS.indexOf(c) != -1);
    }

    public static String stripBaseName(String key) {
        Preconditions.checkArgument(key.startsWith(CUSTOM_BASE));
        return key.substring(CUSTOM_BASE.length());
    }
}

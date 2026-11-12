package com.globaltechradar.collector;

/** A feed could not be fetched or parsed. The message is shown on the News Sources page. */
public class FeedException extends Exception {
    public FeedException(String message) { super(message); }

    public FeedException(String message, Throwable cause) { super(message, cause); }
}

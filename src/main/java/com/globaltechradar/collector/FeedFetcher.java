package com.globaltechradar.collector;

/** Downloads raw feed bytes. Separate from parsing so tests can substitute canned feeds. */
public interface FeedFetcher {
    byte[] fetch(String url) throws FeedException;
}

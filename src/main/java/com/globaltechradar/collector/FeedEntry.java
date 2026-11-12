package com.globaltechradar.collector;

import java.time.Instant;

/** One raw item parsed from an RSS or Atom feed, before classification. */
public record FeedEntry(String title, String link, String summary, Instant publishedAt) { }

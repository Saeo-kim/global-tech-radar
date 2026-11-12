package com.globaltechradar.web;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Template helpers, used as {@code ${@fmt.time(x)}} in Thymeleaf. */
@Component("fmt")
public class ViewFormat {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());

    /** Local date-time, or "never" when null. */
    public String time(Instant t) {
        return t == null ? "never" : TIME.format(t);
    }

    public String date(Instant t) {
        return t == null ? "date not provided" : DATE.format(t);
    }

    /** "5 min ago", "3 h ago", "2 d ago". */
    public String ago(Instant t) {
        if (t == null) return "never";
        Duration d = Duration.between(t, Instant.now());
        if (d.isNegative() || d.toMinutes() < 1) return "just now";
        if (d.toHours() < 1) return d.toMinutes() + " min ago";
        if (d.toDays() < 1) return d.toHours() + " h ago";
        return d.toDays() + " d ago";
    }
}

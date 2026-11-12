package com.globaltechradar.service;

import com.globaltechradar.domain.TrendPoint;

import java.time.LocalDate;
import java.util.*;

/** Chart-ready shape: shared x-axis periods and one value array per series key. */
public record TrendSeries(List<String> periods, List<Series> series, String unit, String dataStatus,
                          String sourceName, LocalDate publishedOn) {

    public record Series(String key, List<Double> values) { }

    public static TrendSeries of(List<TrendPoint> points) {
        SortedSet<String> periods = new TreeSet<>();
        Map<String, Map<String, Double>> byKey = new LinkedHashMap<>();
        String unit = null;
        String status = "SAMPLE";
        String source = null;
        LocalDate published = null;
        for (TrendPoint p : points) {
            periods.add(p.getPeriod());
            byKey.computeIfAbsent(p.getSeriesKey(), k -> new HashMap<>()).put(p.getPeriod(), p.getValue());
            unit = p.getUnit();
            if (p.getProvenance() != null) {
                status = p.getProvenance().getDataStatus().name();
                source = p.getProvenance().getSourceName();
                published = p.getProvenance().getPublishedOn();
            }
        }
        List<String> axis = new ArrayList<>(periods);
        List<Series> series = byKey.entrySet().stream()
            .map(e -> new Series(e.getKey(), axis.stream().map(x -> e.getValue().get(x)).toList()))
            .toList();
        return new TrendSeries(axis, series, unit, status, source, published);
    }
}

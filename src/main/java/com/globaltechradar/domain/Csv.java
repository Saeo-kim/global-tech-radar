package com.globaltechradar.domain;

import java.util.Arrays;
import java.util.List;

/** Small helper for the comma / pipe separated list columns used by the MVP schema. */
public final class Csv {

    private Csv() { }

    public static List<String> split(String value) {
        return split(value, ",");
    }

    public static List<String> pipes(String value) {
        return split(value, "\\|");
    }

    private static List<String> split(String value, String regex) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split(regex)).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}

package com.lms.common.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class TimeConverter {

    private TimeConverter() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated.");
    }

    public static LocalDateTime toLocalDateTime (Instant instant, ZoneId zoneId) {
        if (instant == null || zoneId == null)
            return null;

        return LocalDateTime.ofInstant(instant, zoneId);
    }

    public static LocalDateTime toLocalDateTime (Instant instant) {
        return toLocalDateTime (instant, ZoneId.systemDefault());
    }

    public static LocalDate toLocalDate (Instant instant, ZoneId zoneId) {
        if (instant == null || zoneId == null)
            return null;

        return LocalDate.ofInstant(instant, zoneId);
    }

    public static LocalDate toLocalDate (Instant instant) {
        return toLocalDate (instant, ZoneId.systemDefault());
    }
}

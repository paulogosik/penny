package com.penny.support;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

public class MutableClock extends Clock {

    public static final Instant DEFAULT_INSTANT = Instant.parse("2026-10-13T15:00:00Z");

    private final AtomicReference<Instant> current;
    private final ZoneId zone;

    public MutableClock() {
        this(new AtomicReference<>(DEFAULT_INSTANT), ZoneOffset.UTC);
    }

    private MutableClock(AtomicReference<Instant> current, ZoneId zone) {
        this.current = current;
        this.zone = zone;
    }

    public void set(Instant instant) {
        current.set(instant);
    }

    public void reset() {
        current.set(DEFAULT_INSTANT);
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new MutableClock(current, zone);
    }

    @Override
    public Instant instant() {
        return current.get();
    }
}

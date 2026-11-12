package dev.war.sentinel.managers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

public final class AttemptTracker {

    private record Entry(int count, long lastFailure, long lockedUntil) {
    }

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final long lockoutMillis;
    private final LongSupplier clock;

    public AttemptTracker(int maxAttempts, long lockoutMillis, LongSupplier clock) {
        this.maxAttempts = maxAttempts;
        this.lockoutMillis = lockoutMillis;
        this.clock = clock;
    }

    public boolean isEnabled() {
        return maxAttempts > 0 && lockoutMillis > 0;
    }

    public boolean isLocked(String key) {
        return remainingLockMillis(key) > 0;
    }

    public long remainingLockMillis(String key) {
        if (!isEnabled() || key == null) {
            return 0;
        }
        Entry entry = entries.get(key);
        if (entry == null) {
            return 0;
        }
        return Math.max(0, entry.lockedUntil() - clock.getAsLong());
    }

    public boolean recordFailure(String key) {
        if (!isEnabled() || key == null) {
            return false;
        }
        boolean[] triggered = {false};
        long now = clock.getAsLong();
        entries.compute(key, (k, old) -> {
            if (old != null && old.lockedUntil() > now) {
                return old;
            }
            int count = 1;
            if (old != null && old.lockedUntil() == 0 && now - old.lastFailure() <= lockoutMillis) {
                count = old.count() + 1;
            }
            if (count >= maxAttempts) {
                triggered[0] = true;
                return new Entry(0, now, now + lockoutMillis);
            }
            return new Entry(count, now, 0);
        });
        return triggered[0];
    }

    public void reset(String key) {
        if (key != null) {
            entries.remove(key);
        }
    }

    public void cleanup() {
        long now = clock.getAsLong();
        entries.entrySet().removeIf(e -> {
            Entry v = e.getValue();
            return v.lockedUntil() > 0 ? v.lockedUntil() <= now : now - v.lastFailure() > lockoutMillis;
        });
    }

    public int size() {
        return entries.size();
    }
}

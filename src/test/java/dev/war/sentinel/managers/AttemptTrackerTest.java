package dev.war.sentinel.managers;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class AttemptTrackerTest {

    private final AtomicLong now = new AtomicLong(0);

    private AttemptTracker tracker(int max) {
        return new AttemptTracker(max, 60_000L, now::get);
    }

    @Test
    void locksAfterMaxAttempts() {
        AttemptTracker t = tracker(3);
        assertFalse(t.recordFailure("a"));
        assertFalse(t.recordFailure("a"));
        assertFalse(t.isLocked("a"));
        assertTrue(t.recordFailure("a"));
        assertTrue(t.isLocked("a"));
        assertEquals(60_000L, t.remainingLockMillis("a"));
    }

    @Test
    void lockoutExpires() {
        AttemptTracker t = tracker(2);
        t.recordFailure("a");
        t.recordFailure("a");
        now.addAndGet(59_999);
        assertTrue(t.isLocked("a"));
        now.addAndGet(2);
        assertFalse(t.isLocked("a"));
        assertFalse(t.recordFailure("a"));
    }

    @Test
    void keysAreIndependent() {
        AttemptTracker t = tracker(2);
        t.recordFailure("a");
        t.recordFailure("a");
        assertTrue(t.isLocked("a"));
        assertFalse(t.isLocked("b"));
    }

    @Test
    void successResetsCounter() {
        AttemptTracker t = tracker(3);
        t.recordFailure("a");
        t.recordFailure("a");
        t.reset("a");
        assertFalse(t.recordFailure("a"));
        assertFalse(t.recordFailure("a"));
        assertTrue(t.recordFailure("a"));
    }

    @Test
    void oldFailuresDecay() {
        AttemptTracker t = tracker(3);
        t.recordFailure("a");
        t.recordFailure("a");
        now.addAndGet(60_001);
        assertFalse(t.recordFailure("a"));
        assertFalse(t.isLocked("a"));
    }

    @Test
    void zeroDisablesTracking() {
        AttemptTracker t = tracker(0);
        for (int i = 0; i < 20; i++) assertFalse(t.recordFailure("a"));
        assertFalse(t.isLocked("a"));
        assertFalse(t.recordFailure(null));
    }

    @Test
    void cleanupDropsExpiredEntries() {
        AttemptTracker t = tracker(2);
        t.recordFailure("a");
        t.recordFailure("a");
        t.recordFailure("b");
        now.addAndGet(120_000);
        t.cleanup();
        assertEquals(0, t.size());
    }
}

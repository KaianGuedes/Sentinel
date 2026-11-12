package dev.war.sentinel.managers;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class SessionManagerTest {

    private final AtomicLong now = new AtomicLong(1_000_000);
    private final UUID uuid = UUID.randomUUID();

    private SessionManager manager(boolean enabled) {
        return new SessionManager(enabled, 5 * 60_000L, now::get);
    }

    @Test
    void disabledNeverIssuesOrResumes() {
        SessionManager m = manager(false);
        assertNull(m.recordLogout(uuid, "1.2.3.4", "vanilla", false));
        assertFalse(m.tryResume(uuid, "1.2.3.4", "vanilla", false));
        assertEquals(0, m.size());
    }

    @Test
    void resumesOnceForSameIpAndBrand() {
        SessionManager m = manager(true);
        assertNotNull(m.recordLogout(uuid, "1.2.3.4", "vanilla", false));
        assertTrue(m.tryResume(uuid, "1.2.3.4", "vanilla", false));
        assertFalse(m.tryResume(uuid, "1.2.3.4", "vanilla", false));
    }

    @Test
    void tokensAreRandomAndLong() {
        SessionManager m = manager(true);
        String a = m.recordLogout(uuid, "1.2.3.4", null, false);
        String b = m.recordLogout(UUID.randomUUID(), "1.2.3.4", null, false);
        assertEquals(64, a.length());
        assertNotEquals(a, b);
        assertEquals(a, m.tokenOf(uuid));
    }

    @Test
    void differentIpDoesNotResumeAndBurnsTheSession() {
        SessionManager m = manager(true);
        m.recordLogout(uuid, "1.2.3.4", "vanilla", false);
        assertFalse(m.tryResume(uuid, "5.6.7.8", "vanilla", false));
        assertFalse(m.tryResume(uuid, "1.2.3.4", "vanilla", false));
    }

    @Test
    void brandMismatchRefusesButUnknownBrandIsTolerated() {
        SessionManager m = manager(true);
        m.recordLogout(uuid, "1.2.3.4", "vanilla", false);
        assertFalse(m.tryResume(uuid, "1.2.3.4", "fabric", false));

        m.recordLogout(uuid, "1.2.3.4", "vanilla", false);
        assertTrue(m.tryResume(uuid, "1.2.3.4", null, false));
    }

    @Test
    void expiresAfterConfiguredDuration() {
        SessionManager m = manager(true);
        m.recordLogout(uuid, "1.2.3.4", null, false);
        now.addAndGet(5 * 60_000L + 1);
        assertFalse(m.tryResume(uuid, "1.2.3.4", null, false));

        m.recordLogout(uuid, "1.2.3.4", null, false);
        now.addAndGet(5 * 60_000L);
        assertTrue(m.tryResume(uuid, "1.2.3.4", null, false));
    }

    @Test
    void bedrockPlayersAreNeverAutoLoggedIn() {
        SessionManager m = manager(true);
        assertNull(m.recordLogout(uuid, "1.2.3.4", null, true));
        m.recordLogout(uuid, "1.2.3.4", null, false);
        assertFalse(m.tryResume(uuid, "1.2.3.4", null, true));
    }

    @Test
    void missingIpNeverResumes() {
        SessionManager m = manager(true);
        assertNull(m.recordLogout(uuid, null, null, false));
        assertFalse(m.tryResume(uuid, null, null, false));
    }

    @Test
    void invalidateAndCleanupRemoveSessions() {
        SessionManager m = manager(true);
        m.recordLogout(uuid, "1.2.3.4", null, false);
        m.invalidate(uuid);
        assertFalse(m.hasSession(uuid));

        m.recordLogout(uuid, "1.2.3.4", null, false);
        m.recordLogout(UUID.randomUUID(), "1.2.3.4", null, false);
        now.addAndGet(6 * 60_000L);
        m.cleanup();
        assertEquals(0, m.size());
    }
}

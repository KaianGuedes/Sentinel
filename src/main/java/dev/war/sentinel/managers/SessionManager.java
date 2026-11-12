package dev.war.sentinel.managers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

public final class SessionManager {

    private record Session(String token, String ip, String brand, long expiresAt) {
    }

    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final boolean enabled;
    private final long expiryMillis;
    private final LongSupplier clock;

    public SessionManager(boolean enabled, long expiryMillis, LongSupplier clock) {
        this.enabled = enabled;
        this.expiryMillis = expiryMillis;
        this.clock = clock;
    }

    public boolean isEnabled() {
        return enabled && expiryMillis > 0;
    }

    public String recordLogout(UUID uuid, String ip, String brand, boolean bedrock) {
        if (!isEnabled() || bedrock || ip == null || uuid == null) {
            return null;
        }
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = HexFormat.of().formatHex(bytes);
        sessions.put(uuid, new Session(token, ip, brand, clock.getAsLong() + expiryMillis));
        return token;
    }

    public boolean tryResume(UUID uuid, String ip, String brand, boolean bedrock) {
        if (!isEnabled() || bedrock || ip == null || uuid == null) {
            return false;
        }
        Session session = sessions.remove(uuid);
        if (session == null) {
            return false;
        }
        if (clock.getAsLong() > session.expiresAt()) {
            return false;
        }
        if (!constantTimeEquals(session.ip(), ip)) {
            return false;
        }
        return session.brand() == null || brand == null || constantTimeEquals(session.brand(), brand);
    }

    public boolean hasSession(UUID uuid) {
        return sessions.containsKey(uuid);
    }

    public String tokenOf(UUID uuid) {
        Session session = sessions.get(uuid);
        return session == null ? null : session.token();
    }

    public void invalidate(UUID uuid) {
        sessions.remove(uuid);
    }

    public void cleanup() {
        long now = clock.getAsLong();
        sessions.values().removeIf(s -> now > s.expiresAt());
    }

    public int size() {
        return sessions.size();
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}

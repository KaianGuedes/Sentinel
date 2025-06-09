package dev.war.sentinel.managers;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class SessionManager {
    private final Map<UUID, Map<String, Long>> ipLogouts = new ConcurrentHashMap<>();
    private static final long EXPIRY_MILLIS = TimeUnit.MINUTES.toMillis(15);

    public void recordLogout(UUID uuid, String ip) {
        ipLogouts.computeIfAbsent(uuid, u -> new ConcurrentHashMap<>()).put(ip, System.currentTimeMillis());
    }

    public boolean canAutoLogin(UUID uuid, String ip) {
        Map<String, Long> ips = ipLogouts.get(uuid);
        if (ips == null) return false;
        Long lastLogout = ips.get(ip);
        if (lastLogout == null) return false;

        long elapsed = System.currentTimeMillis() - lastLogout;
        if (elapsed <= EXPIRY_MILLIS) {
            return true;
        }

        ips.remove(ip);
        if (ips.isEmpty()) {
            ipLogouts.remove(uuid);
        }
        return false;
    }

    public void clear(UUID uuid, String ip) {
        Map<String, Long> ips = ipLogouts.get(uuid);
        if (ips != null) {
            ips.remove(ip);
            if (ips.isEmpty()) {
                ipLogouts.remove(uuid);
            }
        }
    }

    public void clearAll(UUID uuid) {
        ipLogouts.remove(uuid);
    }
}
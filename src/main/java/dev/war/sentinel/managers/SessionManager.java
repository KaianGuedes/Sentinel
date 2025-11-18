package dev.war.sentinel.managers;

import dev.war.sentinel.Sentinel;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class SessionManager {
    private final Map<UUID, Map<String, Long>> ipLogouts = new ConcurrentHashMap<>();
    private final long expiryMillis;

    public SessionManager(Sentinel plugin) {
        FileConfiguration config = plugin.getConfig();
        long duration = config.getLong("session.duration-minutes", 15);
        this.expiryMillis = TimeUnit.MINUTES.toMillis(duration);
    }

    public void recordLogout(UUID uuid, String ip) {
        ipLogouts.computeIfAbsent(uuid, u -> new ConcurrentHashMap<>()).put(ip, System.currentTimeMillis());
    }

    public boolean canAutoLogin(UUID uuid, String ip) {
        Map<String, Long> ips = ipLogouts.get(uuid);
        if (ips == null) return false;
        Long lastLogout = ips.get(ip);
        if (lastLogout == null) return false;

        long elapsed = System.currentTimeMillis() - lastLogout;
        if (elapsed <= expiryMillis) {
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
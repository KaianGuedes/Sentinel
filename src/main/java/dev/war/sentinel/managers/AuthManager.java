package dev.war.sentinel.managers;

import dev.war.sentinel.utils.IPUtils;
import dev.war.sentinel.utils.uuid.UUIDUtils;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AuthManager {
    private final DatabaseManager db;
    private final SessionManager sessionManager;
    private final Set<UUID> loggedIn = new HashSet<>();

    public AuthManager(DatabaseManager db, SessionManager sessionManager) {
        this.db = db;
        this.sessionManager = sessionManager;
    }

    public boolean isRegistered(UUID uuid) {
        return db.isRegistered(uuid);
    }

    public boolean validatePassword(UUID uuid, String password) {
        return db.validatePassword(uuid, password);
    }

    public boolean login(UUID uuid, String password, String ip) {
        if (!db.validatePassword(uuid, password)) {
            return false;
        }

        setLoggedIn(uuid, true);
        sessionManager.clear(uuid, ip);
        return true;
    }

    public void register(UUID uuid, String password, String ip) {
        db.register(uuid, password, ip);
    }

    public void unregister(UUID uuid) {
        db.unregister(uuid);
        sessionManager.clearAll(uuid);
        setLoggedIn(uuid, false);
    }

    public void changePassword(UUID uuid, String newPassword) {
        db.changePassword(uuid, newPassword);
    }

    public boolean isLoggedIn(UUID uuid) {
        return loggedIn.contains(uuid);
    }

    public void setLoggedIn(UUID uuid, boolean status) {
        if (status) loggedIn.add(uuid);
        else loggedIn.remove(uuid);
    }

    public boolean autoLogin(Player player) {
        UUID uuid = UUIDUtils.getCorrectUUID(player.getName());
        String ip = IPUtils.getIP(player);

        if (!isRegistered(uuid)) return false;

        if (sessionManager.canAutoLogin(uuid, ip)) {
            setLoggedIn(uuid, true);
            sessionManager.clear(uuid, ip);
            return true;
        }

        return false;
    }
}

package dev.war.sentinel.managers;

import dev.war.sentinel.utils.PasswordHasher;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthManager {
    private final DatabaseManager db;
    private final SessionManager sessionManager;
    private final Set<UUID> loggedIn = ConcurrentHashMap.newKeySet();

    public AuthManager(DatabaseManager db, SessionManager sessionManager) {
        this.db = db;
        this.sessionManager = sessionManager;
    }

    public boolean isRegistered(UUID uuid) {
        return db.isRegistered(uuid);
    }

    public boolean verifyPassword(UUID uuid, String password) {
        String hash = db.getHash(uuid);
        return hash != null && PasswordHasher.check(password, hash);
    }

    public boolean isNameTakenByOther(String nameKey, UUID uuid) {
        return db.isNameKeyTakenByOther(nameKey, uuid);
    }

    public UUID findByNameKey(String nameKey) {
        return db.findUuidByNameKey(nameKey);
    }

    public void rememberName(UUID uuid, String nameKey) {
        db.fillNameKeyIfMissing(uuid, nameKey);
    }

    public boolean hasStateReset(UUID uuid) {
        return db.hasStateReset(uuid);
    }

    public void clearStateReset(UUID uuid) {
        db.clearStateReset(uuid);
    }

    public boolean register(UUID uuid, String nameKey, String password, String ip, int cost) {
        return db.register(uuid, nameKey, PasswordHasher.hash(password, cost), ip);
    }

    public boolean unregister(UUID uuid) {
        boolean removed = db.unregister(uuid);
        if (removed) {
            sessionManager.invalidate(uuid);
            setLoggedIn(uuid, false);
        }
        return removed;
    }

    public boolean changePassword(UUID uuid, String newPassword, int cost) {
        boolean changed = db.changePassword(uuid, PasswordHasher.hash(newPassword, cost));
        if (changed) {
            sessionManager.invalidate(uuid);
        }
        return changed;
    }

    public void logout(UUID uuid) {
        sessionManager.invalidate(uuid);
        setLoggedIn(uuid, false);
    }

    public boolean isLoggedIn(UUID uuid) {
        return loggedIn.contains(uuid);
    }

    public void setLoggedIn(UUID uuid, boolean status) {
        if (status) loggedIn.add(uuid);
        else loggedIn.remove(uuid);
    }
}

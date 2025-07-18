package dev.war.sentinel;

import dev.war.sentinel.commands.*;
import dev.war.sentinel.compat.Compat;
import dev.war.sentinel.listeners.PlayerListener;
import dev.war.sentinel.managers.AuthManager;
import dev.war.sentinel.managers.DatabaseManager;
import dev.war.sentinel.managers.PlayerStateManager;
import dev.war.sentinel.managers.SessionManager;
import dev.war.sentinel.utils.AnsiColor;
import dev.war.sentinel.utils.Messages;
import dev.war.sentinel.utils.uuid.UUIDUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Sentinel extends JavaPlugin {

    private static Sentinel instance;
    private SessionManager sessionManager;
    private DatabaseManager databaseManager;
    private AuthManager authManager;
    private PlayerStateManager playerStateManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        UUIDUtils.loadFromConfig(getConfig());

        Messages.init(this);

        this.databaseManager = new DatabaseManager();
        this.databaseManager.connect();
        this.sessionManager = new SessionManager();
        this.authManager = new AuthManager(databaseManager, sessionManager);
        this.playerStateManager = new PlayerStateManager(this);

        Commands.register(this, authManager, playerStateManager);
        Commands.setupCommandLoggingFilter();

        getServer().getPluginManager().registerEvents(new PlayerListener(authManager, playerStateManager, sessionManager), this);

        getLogger().info(AnsiColor.LIGHT_PURPLE + "Sentinel is watching" + AnsiColor.RESET);
    }

    @Override
    public void onDisable() {
        this.databaseManager.close();
        PlayerStateManager stateManager = this.playerStateManager;

        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                stateManager.saveIfNotRestricted(player);
            } catch (Exception ex) {
                getLogger().warning(Messages.get("server.disable.saving_error")
                        .replace("%player%", player.getName())
                        .replace("%error%", ex.getMessage()));
            }
        }
    }

    public static Sentinel getInstance() {
        return instance;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public AuthManager getAuthManager() {
        return authManager;
    }

    public PlayerStateManager getPlayerStateManager() {
        return playerStateManager;
    }
}

package dev.war.sentinel;

import dev.war.sentinel.commands.AuthCommands;
import dev.war.sentinel.commands.Commands;
import dev.war.sentinel.compat.Compat;
import dev.war.sentinel.listeners.PlayerListener;
import dev.war.sentinel.managers.AttemptTracker;
import dev.war.sentinel.managers.AuthManager;
import dev.war.sentinel.managers.AuthService;
import dev.war.sentinel.managers.DatabaseManager;
import dev.war.sentinel.managers.PlayerStateManager;
import dev.war.sentinel.managers.SessionManager;
import dev.war.sentinel.utils.AnsiColor;
import dev.war.sentinel.utils.CommandMatcher;
import dev.war.sentinel.utils.FloodgateHook;
import dev.war.sentinel.utils.Messages;
import dev.war.sentinel.utils.uuid.UUIDUtils;
import org.apache.logging.log4j.core.Filter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class Sentinel extends JavaPlugin {

    private static Sentinel instance;
    private SessionManager sessionManager;
    private DatabaseManager databaseManager;
    private AuthManager authManager;
    private PlayerStateManager playerStateManager;
    private AuthService authService;
    private Filter commandLogFilter;
    private BukkitTask cleanupTask;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        UUIDUtils.loadFromConfig(getConfig());

        Messages.init(this);

        Settings settings = Settings.from(getConfig());

        this.databaseManager = new DatabaseManager(new File(getDataFolder(), "auth.db"), getLogger());
        this.databaseManager.connect();
        this.sessionManager = new SessionManager(settings.sessionEnabled(),
                TimeUnit.MINUTES.toMillis(settings.sessionMinutes()), System::currentTimeMillis);
        this.authManager = new AuthManager(databaseManager, sessionManager);
        this.playerStateManager = new PlayerStateManager(this);

        long lockoutMillis = TimeUnit.SECONDS.toMillis(settings.lockoutSeconds());
        AttemptTracker playerAttempts = new AttemptTracker(settings.maxAttemptsPerPlayer(), lockoutMillis, System::currentTimeMillis);
        AttemptTracker ipAttempts = new AttemptTracker(settings.maxAttemptsPerIp(), lockoutMillis, System::currentTimeMillis);
        FloodgateHook floodgate = FloodgateHook.create();
        if (settings.floodgateSkipAuth() && !floodgate.isAvailable()) {
            getLogger().warning(Messages.get("server.floodgate_missing"));
        }

        this.authService = new AuthService(this, settings, authManager, playerStateManager, sessionManager,
                floodgate, playerAttempts, ipAttempts);
        AuthCommands authCommands = new AuthCommands(authService);
        CommandMatcher matcher = new CommandMatcher(settings.loginAliases(), settings.registerAliases(), settings.changePasswordAliases());

        Commands.register(this, authService, authCommands);
        this.commandLogFilter = Commands.installCommandLoggingFilter(matcher.logPattern());

        getServer().getPluginManager().registerEvents(new PlayerListener(authManager, authService, authCommands, matcher), this);

        this.cleanupTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            sessionManager.cleanup();
            playerAttempts.cleanup();
            ipAttempts.cleanup();
        }, 20L * 60, 20L * 60);

        Compat.getAdapter();

        for (Player player : Bukkit.getOnlinePlayers()) {
            authService.handleJoin(player);
        }

        getLogger().info(AnsiColor.LIGHT_PURPLE + "Sentinel is watching" + AnsiColor.RESET);
    }

    @Override
    public void onDisable() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }
        if (authService != null) {
            authService.shutdown();
        }

        if (authManager != null && playerStateManager != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                try {
                    if (authManager.isLoggedIn(UUIDUtils.getCorrectUUID(player))) {
                        playerStateManager.saveCurrentState(player);
                    }
                } catch (Exception ex) {
                    getLogger().warning(Messages.get("server.disable.saving_error")
                            .replace("%player%", player.getName())
                            .replace("%error%", String.valueOf(ex.getMessage())));
                }
            }
        }

        Commands.removeCommandLoggingFilter(commandLogFilter);

        if (databaseManager != null) {
            databaseManager.close();
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

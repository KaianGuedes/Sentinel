package dev.war.sentinel.managers;

import dev.war.sentinel.Sentinel;
import dev.war.sentinel.Settings;
import dev.war.sentinel.utils.AnsiColor;
import dev.war.sentinel.utils.FloodgateHook;
import dev.war.sentinel.utils.IPUtils;
import dev.war.sentinel.utils.Messages;
import dev.war.sentinel.utils.PasswordRules;
import dev.war.sentinel.utils.uuid.UUIDUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthService {

    private enum LoginOutcome {
        DB_ERROR,
        NOT_REGISTERED,
        VALID,
        INVALID
    }

    private enum RegisterOutcome {
        DB_ERROR,
        ALREADY_REGISTERED,
        NAME_CONFLICT,
        FAILED,
        OK
    }

    private final Sentinel plugin;
    private final Settings settings;
    private final AuthManager authManager;
    private final PlayerStateManager stateManager;
    private final SessionManager sessionManager;
    private final FloodgateHook floodgate;
    private final AttemptTracker playerAttempts;
    private final AttemptTracker ipAttempts;
    private final Set<UUID> busy = ConcurrentHashMap.newKeySet();
    private final Map<UUID, BukkitTask> timeoutTasks = new ConcurrentHashMap<>();

    public AuthService(Sentinel plugin, Settings settings, AuthManager authManager, PlayerStateManager stateManager,
                       SessionManager sessionManager, FloodgateHook floodgate,
                       AttemptTracker playerAttempts, AttemptTracker ipAttempts) {
        this.plugin = plugin;
        this.settings = settings;
        this.authManager = authManager;
        this.stateManager = stateManager;
        this.sessionManager = sessionManager;
        this.floodgate = floodgate;
        this.playerAttempts = playerAttempts;
        this.ipAttempts = ipAttempts;
    }

    private void async(Runnable task) {
        if (plugin.isEnabled()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

    private void sync(Runnable task) {
        if (plugin.isEnabled()) {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    private void info(String message) {
        plugin.getLogger().info(AnsiColor.LIGHT_PURPLE + message + AnsiColor.RESET);
    }

    private String brandOf(Player player) {
        try {
            return player.getClientBrandName();
        } catch (Throwable t) {
            return null;
        }
    }

    public long lockRemainingSeconds(UUID uuid, String ip) {
        long millis = Math.max(playerAttempts.remainingLockMillis(uuid.toString()), ipAttempts.remainingLockMillis(ip));
        return millis <= 0 ? 0 : (millis + 999) / 1000;
    }

    private void kickDatabaseError(Player player) {
        player.kick(Messages.getComponent(player, "auth.database_error"));
    }

    public void handleJoin(Player player) {
        UUID uuid = UUIDUtils.getCorrectUUID(player);
        String ip = IPUtils.getIP(player);
        boolean bedrock = floodgate.isBedrock(player.getUniqueId());

        if (bedrock && settings.floodgateSkipAuth()) {
            authManager.setLoggedIn(uuid, true);
            info(Messages.get("server.floodgate_skip").replace("%player%", player.getName()).replace("%ip%", IPUtils.display(ip)));
            return;
        }

        async(() -> {
            boolean registered;
            boolean reset;
            try {
                registered = authManager.isRegistered(uuid);
                reset = authManager.hasStateReset(uuid);
            } catch (DatabaseException e) {
                sync(() -> {
                    if (player.isOnline()) kickDatabaseError(player);
                });
                return;
            }
            sync(() -> applyJoin(player, uuid, ip, bedrock, registered, reset));
        });
    }

    private void applyJoin(Player player, UUID uuid, String ip, boolean bedrock, boolean registered, boolean reset) {
        if (!player.isOnline() || authManager.isLoggedIn(uuid)) {
            return;
        }

        if (registered && sessionManager.tryResume(uuid, ip, brandOf(player), bedrock)) {
            authManager.setLoggedIn(uuid, true);
            info(Messages.get("server.session_resumed")
                    .replace("%player%", player.getName())
                    .replace("%ip%", IPUtils.display(ip)));
            player.sendMessage(Messages.getComponent(player, "auth.auto_login_success"));
            return;
        }

        stateManager.restrict(player, reset);
        if (reset) {
            async(() -> authManager.clearStateReset(uuid));
        }
        startTimeout(player, uuid);

        if (registered) {
            player.sendMessage(Messages.getComponent(player, "auth.prompt_login"));
        } else {
            player.sendMessage(Messages.getComponent(player, "auth.prompt_register"));
        }
    }

    private void startTimeout(Player player, UUID uuid) {
        cancelTimeout(uuid);
        int seconds = settings.loginTimeoutSeconds();
        if (seconds <= 0) {
            return;
        }
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            timeoutTasks.remove(uuid);
            if (player.isOnline() && !authManager.isLoggedIn(uuid)) {
                player.kick(Messages.getComponent(player, "auth.timeout_kick"));
            }
        }, seconds * 20L);
        timeoutTasks.put(uuid, task);
    }

    public void cancelTimeout(UUID uuid) {
        BukkitTask task = timeoutTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    public void handleQuit(Player player) {
        UUID uuid = UUIDUtils.getCorrectUUID(player);
        if (authManager.isLoggedIn(uuid)) {
            stateManager.saveCurrentState(player);
            authManager.setLoggedIn(uuid, false);
            sessionManager.recordLogout(uuid, IPUtils.getIP(player), brandOf(player), floodgate.isBedrock(player.getUniqueId()));
        }
        stateManager.release(uuid);
        cancelTimeout(uuid);
        busy.remove(uuid);
    }

    private void completeLogin(Player player, UUID uuid) {
        cancelTimeout(uuid);
        sessionManager.invalidate(uuid);
        authManager.setLoggedIn(uuid, true);
        stateManager.restoreState(player);
    }

    public void login(Player player, String[] args) {
        if (args.length != 1) {
            player.sendMessage(Messages.getComponent(player, "login.usage"));
            return;
        }

        UUID uuid = UUIDUtils.getCorrectUUID(player);
        String ip = IPUtils.getIP(player);

        if (authManager.isLoggedIn(uuid)) {
            player.sendMessage(Messages.getComponent(player, "login.already_logged_in"));
            return;
        }

        long locked = lockRemainingSeconds(uuid, ip);
        if (locked > 0) {
            player.sendMessage(Messages.getComponent(player, "login.locked_out", "%seconds%", String.valueOf(locked)));
            return;
        }

        if (!busy.add(uuid)) {
            player.sendMessage(Messages.getComponent(player, "auth.busy"));
            return;
        }

        String password = args[0];
        String nameKey = UUIDUtils.nameKey(player.getName());
        async(() -> {
            LoginOutcome outcome;
            try {
                if (!authManager.isRegistered(uuid)) {
                    outcome = LoginOutcome.NOT_REGISTERED;
                } else if (authManager.verifyPassword(uuid, password)) {
                    authManager.rememberName(uuid, nameKey);
                    outcome = LoginOutcome.VALID;
                } else {
                    outcome = LoginOutcome.INVALID;
                }
            } catch (DatabaseException e) {
                outcome = LoginOutcome.DB_ERROR;
            }
            LoginOutcome result = outcome;
            sync(() -> {
                try {
                    finishLogin(player, uuid, ip, result);
                } finally {
                    busy.remove(uuid);
                }
            });
        });
    }

    private void finishLogin(Player player, UUID uuid, String ip, LoginOutcome outcome) {
        if (!player.isOnline() || authManager.isLoggedIn(uuid)) {
            return;
        }
        switch (outcome) {
            case DB_ERROR -> kickDatabaseError(player);
            case NOT_REGISTERED -> player.sendMessage(Messages.getComponent(player, "login.not_registered"));
            case VALID -> {
                playerAttempts.reset(uuid.toString());
                completeLogin(player, uuid);
                info(Messages.get("server.login_success")
                        .replace("%player%", player.getName())
                        .replace("%ip%", IPUtils.display(ip)));
                player.sendMessage(Messages.getComponent(player, "login.success"));
            }
            case INVALID -> {
                info(Messages.get("server.login_failed")
                        .replace("%player%", player.getName())
                        .replace("%ip%", IPUtils.display(ip)));
                registerFailure(player, uuid, ip, "login.wrong_password");
            }
        }
    }

    private void registerFailure(Player player, UUID uuid, String ip, String wrongMessageKey) {
        boolean playerLocked = playerAttempts.recordFailure(uuid.toString());
        boolean ipLocked = ipAttempts.recordFailure(ip);
        if (playerLocked || ipLocked) {
            plugin.getLogger().warning(Messages.get("server.login_locked")
                    .replace("%player%", player.getName())
                    .replace("%ip%", IPUtils.display(ip)));
            player.kick(Messages.getComponent(player, "auth.too_many_attempts"));
        } else {
            player.sendMessage(Messages.getComponent(player, wrongMessageKey));
        }
    }

    private boolean rejectPassword(Player player, String password) {
        PasswordRules.Result result = PasswordRules.validate(password, settings.minPasswordLength(), settings.maxPasswordLength());
        switch (result) {
            case TOO_SHORT -> player.sendMessage(Messages.getComponent(player, "password.too_short",
                    "%min%", String.valueOf(settings.minPasswordLength())));
            case TOO_LONG -> player.sendMessage(Messages.getComponent(player, "password.too_long",
                    "%max%", String.valueOf(settings.maxPasswordLength())));
            case TOO_LONG_BYTES -> player.sendMessage(Messages.getComponent(player, "password.too_long_bytes",
                    "%bytes%", String.valueOf(PasswordRules.BCRYPT_MAX_BYTES)));
            default -> {
                return false;
            }
        }
        return true;
    }

    public void register(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage(Messages.getComponent(player, "register.usage"));
            return;
        }
        if (!args[0].equals(args[1])) {
            player.sendMessage(Messages.getComponent(player, "register.mismatch"));
            return;
        }
        if (rejectPassword(player, args[0])) {
            return;
        }

        UUID uuid = UUIDUtils.getCorrectUUID(player);
        String ip = IPUtils.getIP(player);

        if (authManager.isLoggedIn(uuid)) {
            player.sendMessage(Messages.getComponent(player, "register.already_registered"));
            return;
        }
        if (!busy.add(uuid)) {
            player.sendMessage(Messages.getComponent(player, "auth.busy"));
            return;
        }

        String password = args[0];
        String nameKey = UUIDUtils.nameKey(player.getName());
        async(() -> {
            RegisterOutcome outcome;
            try {
                if (authManager.isRegistered(uuid)) {
                    outcome = RegisterOutcome.ALREADY_REGISTERED;
                } else if (authManager.isNameTakenByOther(nameKey, uuid)) {
                    outcome = RegisterOutcome.NAME_CONFLICT;
                } else if (authManager.register(uuid, nameKey, password, ip, settings.bcryptCost())) {
                    outcome = RegisterOutcome.OK;
                } else {
                    outcome = RegisterOutcome.FAILED;
                }
            } catch (DatabaseException e) {
                outcome = RegisterOutcome.DB_ERROR;
            }
            RegisterOutcome result = outcome;
            sync(() -> {
                try {
                    finishRegister(player, uuid, ip, result);
                } finally {
                    busy.remove(uuid);
                }
            });
        });
    }

    private void finishRegister(Player player, UUID uuid, String ip, RegisterOutcome outcome) {
        if (!player.isOnline()) {
            return;
        }
        switch (outcome) {
            case DB_ERROR -> kickDatabaseError(player);
            case ALREADY_REGISTERED -> player.sendMessage(Messages.getComponent(player, "register.already_registered"));
            case NAME_CONFLICT -> player.sendMessage(Messages.getComponent(player, "register.name_conflict"));
            case FAILED -> player.sendMessage(Messages.getComponent(player, "register.failed"));
            case OK -> {
                completeLogin(player, uuid);
                info(Messages.get("server.register_success")
                        .replace("%player%", player.getName())
                        .replace("%ip%", IPUtils.display(ip)));
                player.sendMessage(Messages.getComponent(player, "register.success"));
            }
        }
    }

    public void changePassword(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage(Messages.getComponent(player, "changepassword.usage"));
            return;
        }

        UUID uuid = UUIDUtils.getCorrectUUID(player);
        String ip = IPUtils.getIP(player);

        if (!authManager.isLoggedIn(uuid)) {
            player.sendMessage(Messages.getComponent(player, "changepassword.not_logged_in"));
            return;
        }
        if (rejectPassword(player, args[1])) {
            return;
        }
        if (!busy.add(uuid)) {
            player.sendMessage(Messages.getComponent(player, "auth.busy"));
            return;
        }

        String oldPassword = args[0];
        String newPassword = args[1];
        async(() -> {
            int state;
            try {
                if (!authManager.verifyPassword(uuid, oldPassword)) {
                    state = 1;
                } else {
                    state = authManager.changePassword(uuid, newPassword, settings.bcryptCost()) ? 0 : 2;
                }
            } catch (DatabaseException e) {
                state = 3;
            }
            int result = state;
            sync(() -> {
                try {
                    finishChangePassword(player, uuid, ip, result);
                } finally {
                    busy.remove(uuid);
                }
            });
        });
    }

    private void finishChangePassword(Player player, UUID uuid, String ip, int state) {
        if (!player.isOnline()) {
            return;
        }
        switch (state) {
            case 0 -> {
                info(Messages.get("server.password_changed")
                        .replace("%player%", player.getName())
                        .replace("%ip%", IPUtils.display(ip)));
                player.sendMessage(Messages.getComponent(player, "changepassword.success"));
            }
            case 1 -> registerFailure(player, uuid, ip, "changepassword.wrong_old_password");
            case 2 -> player.sendMessage(Messages.getComponent(player, "changepassword.failed"));
            default -> kickDatabaseError(player);
        }
    }

    public void logout(Player player) {
        UUID uuid = UUIDUtils.getCorrectUUID(player);
        if (!authManager.isLoggedIn(uuid)) {
            player.sendMessage(Messages.getComponent(player, "auth.must_authenticate"));
            return;
        }
        stateManager.saveCurrentState(player);
        authManager.logout(uuid);
        player.kick(Messages.getComponent(player, "logout.kicked"));
    }

    public void unregister(CommandSender sender, String target) {
        async(() -> {
            UUID resolved = null;
            boolean dbError = false;
            boolean removed = false;
            try {
                UUID uuid = UUIDUtils.getCorrectUUID(target);
                if (authManager.isRegistered(uuid)) {
                    resolved = uuid;
                } else {
                    resolved = authManager.findByNameKey(UUIDUtils.nameKey(target));
                }
                if (resolved != null) {
                    removed = authManager.unregister(resolved);
                }
            } catch (DatabaseException e) {
                dbError = true;
            }
            UUID finalUuid = resolved;
            boolean finalRemoved = removed;
            boolean finalError = dbError;
            sync(() -> finishUnregister(sender, target, finalUuid, finalRemoved, finalError));
        });
    }

    private void finishUnregister(CommandSender sender, String target, UUID uuid, boolean removed, boolean dbError) {
        if (dbError || (uuid != null && !removed)) {
            sender.sendMessage(Messages.getComponent(sender, "unregister.failed"));
            return;
        }
        if (uuid == null) {
            sender.sendMessage(Messages.getComponent(sender, "unregister.not_registered"));
            return;
        }

        Component message = Messages.getComponent(sender, "unregister.success").replaceText(
                TextReplacementConfig.builder().matchLiteral("%player%").replacement(target).build());
        if (sender instanceof Player) {
            sender.sendMessage(message);
        } else {
            sender.sendMessage(Component.text("[Sentinel] ").append(message));
        }

        Player targetPlayer = Bukkit.getPlayerExact(target);
        if (targetPlayer != null) {
            stateManager.clearStoredData(targetPlayer);
            cancelTimeout(uuid);
            targetPlayer.kick(Messages.getComponent(sender, "unregister.kicked"));
        }
    }

    public void shutdown() {
        for (BukkitTask task : timeoutTasks.values()) {
            task.cancel();
        }
        timeoutTasks.clear();
    }
}

package dev.war.sentinel.listeners;

import dev.war.sentinel.Sentinel;
import dev.war.sentinel.managers.AuthManager;
import dev.war.sentinel.managers.PlayerStateManager;
import dev.war.sentinel.managers.SessionManager;
import dev.war.sentinel.utils.AnsiColor;
import dev.war.sentinel.utils.IPUtils;
import dev.war.sentinel.utils.Messages;
import dev.war.sentinel.utils.uuid.UUIDUtils;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;

import java.util.UUID;

public class PlayerListener implements Listener {

    private final AuthManager authManager;
    private final PlayerStateManager stateManager;
    private final SessionManager sessionManager;

    public PlayerListener(AuthManager authManager, PlayerStateManager stateManager, SessionManager sessionManager) {
        this.authManager = authManager;
        this.stateManager = stateManager;
        this.sessionManager = sessionManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(PlayerLoginEvent e) {
        String playerName = e.getPlayer().getName();
        UUID uuid = UUIDUtils.getCorrectUUID(playerName);

        if (authManager.isLoggedIn(uuid)) {
            e.disallow(PlayerLoginEvent.Result.KICK_OTHER, Messages.getComponent(e.getPlayer(), "auth.already_logged_in"));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        UUID uuid = UUIDUtils.getCorrectUUID(p.getName());

        if (authManager.autoLogin(p)) {
            Sentinel.getInstance().getLogger().info(AnsiColor.LIGHT_PURPLE + Messages.get("server.session_resumed")
                    .replace("%player%", p.getName())
                    .replace("%ip%", IPUtils.getIP(p)) + AnsiColor.RESET);

            p.sendMessage(Messages.getComponent(p, "auth.auto_login_success"));
            stateManager.restoreState(p);
        } else {
            stateManager.restrict(p);
            if (authManager.isRegistered(uuid)) {
                p.sendMessage(Messages.getComponent(p, "auth.prompt_login"));
            } else {
                p.sendMessage(Messages.getComponent(p, "auth.prompt_register"));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        UUID uuid = UUIDUtils.getCorrectUUID(p.getName());
        String ip = IPUtils.getIP(p);

        if (authManager.isLoggedIn(uuid)) {
            authManager.setLoggedIn(uuid, false);
            sessionManager.recordLogout(uuid, ip);
        }

        stateManager.saveIfNotRestricted(p);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMove(PlayerMoveEvent e) {
        UUID uuid = UUIDUtils.getCorrectUUID(e.getPlayer().getName());

        if (!authManager.isLoggedIn(uuid)) {
            e.setTo(e.getFrom());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        UUID uuid = UUIDUtils.getCorrectUUID(p.getName());

        if (!authManager.isLoggedIn(uuid)) {
            String msg = e.getMessage().toLowerCase();
            if (!(msg.startsWith("/login") || msg.startsWith("/register") ||
                    msg.startsWith("/l ") || msg.startsWith("/r "))) {
                p.sendMessage(Messages.getComponent(p, "auth.must_authenticate"));
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSpectatorInteract(PlayerInteractEntityEvent e) {
        Player p = e.getPlayer();
        UUID uuid = UUIDUtils.getCorrectUUID(p.getName());

        if (p.getGameMode() == GameMode.SPECTATOR && stateManager.isRestricted(uuid)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSpectatorTeleport(PlayerTeleportEvent e) {
        Player p = e.getPlayer();
        UUID uuid = UUIDUtils.getCorrectUUID(p.getName());

        if (e.getCause() == PlayerTeleportEvent.TeleportCause.SPECTATE &&
                stateManager.isRestricted(uuid)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryOpen(InventoryOpenEvent e) {
        if (e.getPlayer() instanceof Player p) {
            UUID uuid = UUIDUtils.getCorrectUUID(p.getName());

            if (p.getGameMode() == GameMode.SPECTATOR && stateManager.isRestricted(uuid)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent e) {
        Player p = e.getPlayer();
        UUID uuid = UUIDUtils.getCorrectUUID(p.getName());

        if (stateManager.isRestricted(uuid)) {
            e.setCancelled(true);
            p.sendMessage(Messages.getComponent(p, "auth.must_authenticate"));
        }
    }
}

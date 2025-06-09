package dev.war.sentinel.listeners;

import dev.war.sentinel.Sentinel;
import dev.war.sentinel.managers.AuthManager;
import dev.war.sentinel.managers.PlayerStateManager;
import dev.war.sentinel.managers.SessionManager;
import dev.war.sentinel.utils.IPUtils;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.entity.Player;

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

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();

        if (authManager.autoLogin(p)) {
            Sentinel.getInstance().getLogger().info("\u001B[95mSessão para o jogador '"
                    + p.getName() + "' foi resumida no IP " + IPUtils.getIP(p) + "\u001B[0m");
            p.sendMessage(Component.text("Autologado com sucesso!", NamedTextColor.LIGHT_PURPLE));
            stateManager.restoreState(p);
        } else {
            stateManager.restrict(p);
            if (authManager.isRegistered(uuid)) {
                p.sendMessage(Component.text("Use /login <senha>", NamedTextColor.LIGHT_PURPLE));
            } else {
                p.sendMessage(Component.text("Use /register <senha>", NamedTextColor.LIGHT_PURPLE));
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();
        String ip = IPUtils.getIP(p);

        if (authManager.isLoggedIn(uuid)) {
            authManager.setLoggedIn(uuid, false);
            sessionManager.recordLogout(uuid, ip);
        }

        stateManager.saveIfNotRestricted(p);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (!authManager.isLoggedIn(e.getPlayer().getUniqueId())) {
            e.setTo(e.getFrom());
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();

        if (!authManager.isLoggedIn(p.getUniqueId())) {
            String msg = e.getMessage().toLowerCase();
            if (!(msg.startsWith("/login") || msg.startsWith("/register"))) {
                p.sendMessage(Component.text("Você deve se autenticar primeiro!", NamedTextColor.RED));
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onSpectatorInteract(PlayerInteractEntityEvent e) {
        Player p = e.getPlayer();
        if (p.getGameMode() == GameMode.SPECTATOR && stateManager.isRestricted(p.getUniqueId())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onSpectatorTeleport(PlayerTeleportEvent e) {
        if (e.getCause() == PlayerTeleportEvent.TeleportCause.SPECTATE &&
                stateManager.isRestricted(e.getPlayer().getUniqueId())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent e) {
        if (e.getPlayer() instanceof Player p) {
            if (p.getGameMode() == GameMode.SPECTATOR && stateManager.isRestricted(p.getUniqueId())) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onChat(AsyncChatEvent e) {
        if (stateManager.isRestricted(e.getPlayer().getUniqueId())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(Component.text("Você deve se autenticar primeiro!", NamedTextColor.RED));
        }
    }
}

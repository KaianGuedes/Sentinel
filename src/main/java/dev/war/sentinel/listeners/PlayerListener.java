package dev.war.sentinel.listeners;

import dev.war.sentinel.commands.AuthCommands;
import dev.war.sentinel.managers.AuthManager;
import dev.war.sentinel.managers.AuthService;
import dev.war.sentinel.utils.CommandMatcher;
import dev.war.sentinel.utils.Messages;
import dev.war.sentinel.utils.uuid.UUIDUtils;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.projectiles.ProjectileSource;

import java.util.UUID;

public class PlayerListener implements Listener {

    private final AuthManager authManager;
    private final AuthService authService;
    private final AuthCommands authCommands;
    private final CommandMatcher matcher;

    public PlayerListener(AuthManager authManager, AuthService authService, AuthCommands authCommands, CommandMatcher matcher) {
        this.authManager = authManager;
        this.authService = authService;
        this.authCommands = authCommands;
        this.matcher = matcher;
    }

    private boolean unauthenticated(Entity entity) {
        return entity instanceof Player p && !authManager.isLoggedIn(UUIDUtils.getCorrectUUID(p));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent e) {
        UUID uuid = UUIDUtils.resolve(e.getName(), e.getUniqueId());
        long seconds = authService.lockRemainingSeconds(uuid, e.getAddress().getHostAddress());
        if (seconds > 0) {
            e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    Messages.getComponent("auth.locked_out_join", "%seconds%", String.valueOf(seconds)));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(PlayerLoginEvent e) {
        UUID uuid = UUIDUtils.getCorrectUUID(e.getPlayer());

        if (authManager.isLoggedIn(uuid)) {
            e.disallow(PlayerLoginEvent.Result.KICK_OTHER, Messages.getComponent(e.getPlayer(), "auth.already_logged_in"));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        authService.handleJoin(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        authService.handleQuit(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMove(PlayerMoveEvent e) {
        if (unauthenticated(e.getPlayer())) {
            e.setTo(e.getFrom());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        String raw = e.getMessage();
        CommandMatcher.Type type = matcher.match(raw);

        if (type != CommandMatcher.Type.NONE) {
            String[] args = CommandMatcher.arguments(raw);
            e.setCancelled(true);
            e.setMessage("/" + CommandMatcher.firstToken(raw) + " [hidden]");
            authCommands.dispatch(type, p, args);
            return;
        }

        if (unauthenticated(p)) {
            p.sendMessage(Messages.getComponent(p, "auth.must_authenticate"));
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent e) {
        Player p = e.getPlayer();
        if (unauthenticated(p)) {
            e.setCancelled(true);
            p.sendMessage(Messages.getComponent(p, "auth.must_authenticate"));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSpectatorTeleport(PlayerTeleportEvent e) {
        if (e.getCause() == PlayerTeleportEvent.TeleportCause.SPECTATE && unauthenticated(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        if (unauthenticated(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent e) {
        if (unauthenticated(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent e) {
        if (unauthenticated(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent e) {
        if (unauthenticated(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        if (unauthenticated(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBucketFill(PlayerBucketFillEvent e) {
        if (unauthenticated(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrop(PlayerDropItemEvent e) {
        if (unauthenticated(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPickup(EntityPickupItemEvent e) {
        if (unauthenticated(e.getEntity())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onConsume(PlayerItemConsumeEvent e) {
        if (unauthenticated(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSwapHands(PlayerSwapHandItemsEvent e) {
        if (unauthenticated(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamaged(EntityDamageEvent e) {
        if (unauthenticated(e.getEntity())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamageDealt(EntityDamageByEntityEvent e) {
        Entity damager = e.getDamager();
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Entity source) {
                damager = source;
            }
        }
        if (unauthenticated(damager)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onHunger(FoodLevelChangeEvent e) {
        if (unauthenticated(e.getEntity())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onTarget(EntityTargetEvent e) {
        if (unauthenticated(e.getTarget())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryOpen(InventoryOpenEvent e) {
        if (unauthenticated(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent e) {
        if (unauthenticated(e.getWhoClicked())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryDrag(InventoryDragEvent e) {
        if (unauthenticated(e.getWhoClicked())) e.setCancelled(true);
    }
}

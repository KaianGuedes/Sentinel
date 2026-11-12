package dev.war.sentinel.managers;

import dev.war.sentinel.Sentinel;
import dev.war.sentinel.utils.Messages;
import dev.war.sentinel.utils.uuid.UUIDUtils;
import dev.war.sentinel.utils.pdc.PlayerPersistentData;
import dev.war.sentinel.utils.pdc.PlayerPersistentData.PlayerStoredData;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerStateManager {

    private final Sentinel plugin;
    private final Set<UUID> restrictedPlayers = new HashSet<>();
    private final Map<UUID, BukkitTask> timeoutTasks = new ConcurrentHashMap<>();

    public PlayerStateManager(Sentinel plugin) {
        this.plugin = plugin;
    }

    public void restrict(Player player) {
        UUID uuid = UUIDUtils.getCorrectUUID(player.getName());

        if (restrictedPlayers.contains(uuid)) return;

        PlayerPersistentData pdc = new PlayerPersistentData(plugin, player);
        if (pdc.getAllStoredData() == null) {
            pdc.saveAll(
                    player.getActivePotionEffects(),
                    player.getGameMode(),
                    player.isOp(),
                    player.getLocation()
            );
        }

        restrictedPlayers.add(uuid);

        player.setGameMode(GameMode.SPECTATOR);
        player.setOp(false);
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, false, false));

        long timeoutSeconds = plugin.getConfig().getLong("auth.timeout-seconds", 30);
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && isRestricted(player)) {
                player.kick(Messages.getComponent(player, "auth.timeout_kick"));
            }
        }, timeoutSeconds * 20L);

        timeoutTasks.put(uuid, task);
    }

    public void restoreState(Player player) {
        UUID uuid = UUIDUtils.getCorrectUUID(player.getName());
        if (!restrictedPlayers.contains(uuid)) return;

        cancelTimeoutTask(uuid);

        PlayerPersistentData pdc = new PlayerPersistentData(plugin, player);
        PlayerStoredData data = pdc.getAllStoredData();

        if (data == null || data.gameMode() == null) return;

        player.setGameMode(data.gameMode());

        if (data.wasOp()) {
            player.setOp(true);
        }

        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        for (PotionEffect effect : data.potionEffects()) {
            player.addPotionEffect(effect);
        }

        if (data.location() != null) {
            player.teleport(data.location());
        }

        restrictedPlayers.remove(uuid);
    }

    public void cancelTimeoutTask(UUID uuid) {
        BukkitTask task = timeoutTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    public void saveIfNotRestricted(Player player) {
        if (!isRestricted(player)) {
            PlayerPersistentData pdc = new PlayerPersistentData(plugin, player);
            pdc.saveAll(
                    player.getActivePotionEffects(),
                    player.getGameMode(),
                    player.isOp(),
                    player.getLocation()
            );
        }
    }

    public boolean isRestricted(UUID uuid) {
        return restrictedPlayers.contains(uuid);
    }

    public boolean isRestricted(Player player) {
        return isRestricted(UUIDUtils.getCorrectUUID(player.getName()));
    }
}

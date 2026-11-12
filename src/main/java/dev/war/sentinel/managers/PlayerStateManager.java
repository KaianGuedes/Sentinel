package dev.war.sentinel.managers;

import dev.war.sentinel.Sentinel;
import dev.war.sentinel.utils.uuid.UUIDUtils;
import dev.war.sentinel.utils.pdc.PlayerPersistentData;
import dev.war.sentinel.utils.pdc.PlayerPersistentData.PlayerStoredData;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerStateManager {

    private final Sentinel plugin;
    private final Set<UUID> restrictedPlayers = ConcurrentHashMap.newKeySet();

    public PlayerStateManager(Sentinel plugin) {
        this.plugin = plugin;
    }

    public void restrict(Player player, boolean discardStoredState) {
        UUID uuid = UUIDUtils.getCorrectUUID(player);

        if (restrictedPlayers.contains(uuid)) return;

        PlayerPersistentData pdc = new PlayerPersistentData(plugin, player);
        if (discardStoredState) {
            pdc.clearAll();
            pdc.saveAll(List.of(), Bukkit.getDefaultGameMode(), false, player.getLocation());
        } else if (!pdc.hasStoredData()) {
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
    }

    public void restoreState(Player player) {
        UUID uuid = UUIDUtils.getCorrectUUID(player);
        if (!restrictedPlayers.contains(uuid)) return;

        PlayerPersistentData pdc = new PlayerPersistentData(plugin, player);
        PlayerStoredData data = pdc.getAllStoredData();

        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        if (data == null || data.gameMode() == null) {
            player.setGameMode(Bukkit.getDefaultGameMode());
            restrictedPlayers.remove(uuid);
            return;
        }

        player.setGameMode(data.gameMode());

        if (data.wasOp()) {
            player.setOp(true);
        }

        for (PotionEffect effect : data.potionEffects()) {
            player.addPotionEffect(effect);
        }

        if (data.location() != null) {
            player.teleport(data.location());
        }

        restrictedPlayers.remove(uuid);
    }

    public void saveCurrentState(Player player) {
        PlayerPersistentData pdc = new PlayerPersistentData(plugin, player);
        pdc.saveAll(
                player.getActivePotionEffects(),
                player.getGameMode(),
                player.isOp(),
                player.getLocation()
        );
    }

    public void clearStoredData(Player player) {
        new PlayerPersistentData(plugin, player).clearAll();
    }

    public void release(UUID uuid) {
        restrictedPlayers.remove(uuid);
    }

    public boolean isRestricted(UUID uuid) {
        return restrictedPlayers.contains(uuid);
    }
}

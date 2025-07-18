package dev.war.sentinel.managers;

import dev.war.sentinel.Sentinel;
import dev.war.sentinel.utils.uuid.UUIDUtils;
import dev.war.sentinel.utils.pdc.PlayerPersistentData;
import dev.war.sentinel.utils.pdc.PlayerPersistentData.PlayerStoredData;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerStateManager {

    private final Sentinel plugin;
    private final Set<UUID> restrictedPlayers = new HashSet<>();

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
    }

    public void restoreState(Player player) {
        UUID uuid = UUIDUtils.getCorrectUUID(player.getName());
        if (!restrictedPlayers.contains(uuid)) return;

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

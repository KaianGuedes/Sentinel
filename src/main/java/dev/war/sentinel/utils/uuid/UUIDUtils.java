package dev.war.sentinel.utils.uuid;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class UUIDUtils {
    private static UUIDMode uuidMode = UUIDMode.ADAPT;

    public static void loadFromConfig(FileConfiguration config) {
        uuidMode = UUIDMode.fromString(config.getString("uuid-mode", "adapt"));
    }

    public static UUID getCorrectUUID(String name) {
        final UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
        return switch (uuidMode) {
            case ONLINE -> Bukkit.getOfflinePlayer(name).getUniqueId();
            case OFFLINE -> uuid;
            case ADAPT -> {
                if (Bukkit.getOnlineMode()) {
                    yield Bukkit.getOfflinePlayer(name).getUniqueId();
                } else {
                    yield uuid;
                }
            }
        };
    }

    public static UUID getCorrectUUID(Player player) {
        return getCorrectUUID(player.getName());
    }
}

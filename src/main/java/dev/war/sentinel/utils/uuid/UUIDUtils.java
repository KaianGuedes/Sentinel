package dev.war.sentinel.utils.uuid;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

public class UUIDUtils {
    private static UUIDMode uuidMode = UUIDMode.ADAPT;

    public static void loadFromConfig(FileConfiguration config) {
        uuidMode = UUIDMode.fromString(config.getString("uuid-mode", "adapt"));
    }

    public static String nameKey(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    public static UUID offlineUuid(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
    }

    private static boolean usesOnlineUuid() {
        return switch (uuidMode) {
            case ONLINE -> true;
            case OFFLINE -> false;
            case ADAPT -> Bukkit.getOnlineMode();
        };
    }

    public static UUID getCorrectUUID(String name) {
        if (usesOnlineUuid()) {
            return Bukkit.getOfflinePlayer(name).getUniqueId();
        }
        return offlineUuid(name);
    }

    public static UUID getCorrectUUID(Player player) {
        if (usesOnlineUuid()) {
            return player.getUniqueId();
        }
        return offlineUuid(player.getName());
    }

    public static UUID resolve(String name, UUID connectionUuid) {
        if (usesOnlineUuid() && connectionUuid != null) {
            return connectionUuid;
        }
        return offlineUuid(name);
    }
}

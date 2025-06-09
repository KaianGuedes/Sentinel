package dev.war.sentinel.utils;

import org.bukkit.entity.Player;

public class IPUtils {
    public static String getIP(Player player) {
        if (player.getAddress().getAddress() == null) return null;
        return player.getAddress().getAddress().getHostAddress();
    }
}

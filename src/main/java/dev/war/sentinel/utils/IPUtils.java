package dev.war.sentinel.utils;

import org.bukkit.entity.Player;

import java.net.InetSocketAddress;

public class IPUtils {
    public static String getIP(Player player) {
        InetSocketAddress address = player.getAddress();
        if (address == null || address.getAddress() == null) return null;
        return address.getAddress().getHostAddress();
    }

    public static String display(String ip) {
        return ip == null ? "unknown" : ip;
    }
}

package dev.war.sentinel.utils;

import org.bukkit.Bukkit;

import java.lang.reflect.Method;
import java.util.UUID;

public final class FloodgateHook {

    private final Object api;
    private final Method isFloodgatePlayer;

    private FloodgateHook(Object api, Method isFloodgatePlayer) {
        this.api = api;
        this.isFloodgatePlayer = isFloodgatePlayer;
    }

    public static FloodgateHook create() {
        if (Bukkit.getPluginManager().getPlugin("floodgate") == null) {
            return new FloodgateHook(null, null);
        }
        try {
            Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            Method method = apiClass.getMethod("isFloodgatePlayer", UUID.class);
            return new FloodgateHook(api, method);
        } catch (ReflectiveOperationException | LinkageError e) {
            return new FloodgateHook(null, null);
        }
    }

    public boolean isAvailable() {
        return api != null;
    }

    public boolean isBedrock(UUID uuid) {
        if (api == null || uuid == null) {
            return false;
        }
        try {
            return (boolean) isFloodgatePlayer.invoke(api, uuid);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }
}

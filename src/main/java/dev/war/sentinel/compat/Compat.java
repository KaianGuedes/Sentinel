package dev.war.sentinel.compat;

import org.bukkit.Bukkit;

public class Compat {
    private static final CompatAdapter ADAPTER = resolveAdapter();

    public static CompatAdapter getAdapter() {
        return ADAPTER;
    }

    private static CompatAdapter resolveAdapter() {
        String version = Bukkit.getBukkitVersion();

        if (isAtLeast(version, "1.21")) {
            return resolveAdapterFor_1_21();
        }

        if (isAtLeast(version, "1.20")) {
            return resolveAdapterFor_1_20();
        }

        throw new UnsupportedOperationException("Unsupported Minecraft version: " + version);
    }

    private static CompatAdapter resolveAdapterFor_1_21() {
        Class<?> registryClass = getRegistryClass();

        if (hasField(registryClass, "MOB_EFFECT")) {
            return new CompatAdapter_1_21_4();
        }

        if (hasField(registryClass, "EFFECT")) {
            return new CompatAdapter_1_20_3();
        }

        return new CompatAdapter_1_20();
    }

    private static CompatAdapter resolveAdapterFor_1_20() {
        Class<?> registryClass = getRegistryClass();

        if (hasField(registryClass, "EFFECT")) {
            return new CompatAdapter_1_20_3();
        }

        return new CompatAdapter_1_20();
    }

    private static boolean isAtLeast(String currentVersion, String prefix) {
        return currentVersion.startsWith(prefix);
    }

    private static boolean hasField(Class<?> clazz, String fieldName) {
        try {
            clazz.getDeclaredField(fieldName);
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    private static Class<?> getRegistryClass() {
        try {
            return Class.forName("org.bukkit.Registry");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load org.bukkit.Registry", e);
        }
    }
}

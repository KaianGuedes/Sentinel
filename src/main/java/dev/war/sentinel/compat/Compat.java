package dev.war.sentinel.compat;

import org.bukkit.Bukkit;

public class Compat {
    private static final CompatAdapter ADAPTER = resolveAdapter();

    public static CompatAdapter getAdapter() {
        return ADAPTER;
    }

    private static CompatAdapter resolveAdapter() {
        try {
            MinecraftVersion version = MinecraftVersion.parse(Bukkit.getBukkitVersion());
            return chooseAdapter(version);
        } catch (Throwable t) {
            return new CompatAdapter_Fallback();
        }
    }

    static CompatAdapter chooseAdapter(MinecraftVersion version) {
        if (version == null || version.isAtLeast(1, 21)) {
            return resolveAdapterFor_1_21();
        }
        return resolveAdapterFor_1_20();
    }

    private static CompatAdapter resolveAdapterFor_1_21() {
        Class<?> registryClass = getRegistryClass();

        if (hasField(registryClass, "MOB_EFFECT")) {
            return new CompatAdapter_1_21_4();
        }

        if (hasField(registryClass, "EFFECT")) {
            return new CompatAdapter_1_20_3();
        }

        return hasField(registryClass, "POTION_EFFECT_TYPE") ? new CompatAdapter_1_20() : new CompatAdapter_Fallback();
    }

    private static CompatAdapter resolveAdapterFor_1_20() {
        Class<?> registryClass = getRegistryClass();

        if (hasField(registryClass, "EFFECT")) {
            return new CompatAdapter_1_20_3();
        }

        return hasField(registryClass, "POTION_EFFECT_TYPE") ? new CompatAdapter_1_20() : new CompatAdapter_Fallback();
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
            throw new IllegalStateException("Failed to load org.bukkit.Registry", e);
        }
    }
}

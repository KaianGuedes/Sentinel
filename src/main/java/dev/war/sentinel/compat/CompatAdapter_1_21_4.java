package dev.war.sentinel.compat;

import org.bukkit.NamespacedKey;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Registry;

import java.lang.reflect.Field;

public class CompatAdapter_1_21_4 implements CompatAdapter {
    private final Registry<PotionEffectType> mobEffectRegistry;

    @SuppressWarnings("unchecked")
    public CompatAdapter_1_21_4() {
        try {
            Class<?> registryClass = Class.forName("org.bukkit.Registry");
            Field field = registryClass.getDeclaredField("MOB_EFFECT");
            this.mobEffectRegistry = (Registry<PotionEffectType>) field.get(null);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Registry.MOB_EFFECT is not available. This adapter requires Paper 1.21.4+.", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to access Registry.MOB_EFFECT via reflection.", e);
        }
    }

    @Override
    public PotionEffectType getMobEffect(NamespacedKey key) {
        return mobEffectRegistry.get(key);
    }
}

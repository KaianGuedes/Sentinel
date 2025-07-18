package dev.war.sentinel.compat;

import org.bukkit.NamespacedKey;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Registry;

import java.lang.reflect.Field;

public class CompatAdapter_1_20_3 implements CompatAdapter {
    private final Registry<PotionEffectType> effectRegistry;

    @SuppressWarnings("unchecked")
    public CompatAdapter_1_20_3() {
        try {
            Class<?> registryClass = Class.forName("org.bukkit.Registry");
            Field field = registryClass.getDeclaredField("EFFECT");
            this.effectRegistry = (Registry<PotionEffectType>) field.get(null);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Registry.EFFECT is not available in this version.", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to access Registry.EFFECT via reflection.", e);
        }
    }

    @Override
    public PotionEffectType getMobEffect(NamespacedKey key) {
        return effectRegistry.get(key);
    }
}

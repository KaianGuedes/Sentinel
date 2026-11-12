package dev.war.sentinel.compat;

import org.bukkit.NamespacedKey;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Registry;

import java.lang.reflect.Field;

public class CompatAdapter_Fallback implements CompatAdapter {
    private static final String[] REGISTRY_FIELDS = {"MOB_EFFECT", "EFFECT", "POTION_EFFECT_TYPE"};

    @Override
    @SuppressWarnings("unchecked")
    public PotionEffectType getMobEffect(NamespacedKey key) {
        for (String name : REGISTRY_FIELDS) {
            try {
                Field field = Registry.class.getDeclaredField(name);
                Registry<PotionEffectType> registry = (Registry<PotionEffectType>) field.get(null);
                if (registry != null) {
                    return registry.get(key);
                }
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            }
        }
        return null;
    }
}

package dev.war.sentinel.compat;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.potion.PotionEffectType;

public class CompatAdapter_1_20 implements CompatAdapter {
    @Override
    public PotionEffectType getMobEffect(NamespacedKey key) {
        return Registry.POTION_EFFECT_TYPE.get(key);
    }
}

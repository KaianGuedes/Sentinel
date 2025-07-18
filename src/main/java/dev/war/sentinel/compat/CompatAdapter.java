package dev.war.sentinel.compat;

import org.bukkit.NamespacedKey;
import org.bukkit.potion.PotionEffectType;

public interface CompatAdapter {
    PotionEffectType getMobEffect(NamespacedKey key);
}

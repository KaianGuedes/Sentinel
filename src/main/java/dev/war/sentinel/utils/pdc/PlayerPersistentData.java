package dev.war.sentinel.utils.pdc;

import dev.war.sentinel.compat.Compat;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class PlayerPersistentData extends AbstractPersistentData {
    private final Player player;

    public PlayerPersistentData(Plugin plugin, Player player) {
        super(plugin);
        this.player = player;
    }

    @Override
    protected PersistentDataContainer getContainer() {
        return player.getPersistentDataContainer();
    }

    public void saveAll(Collection<PotionEffect> effects, GameMode mode, boolean wasOp, Location loc) {
        savePotionEffects(effects);
        setOriginalGameMode(mode);
        setWasOp(wasOp);
        saveLocation(loc);
    }

    public Collection<PotionEffect> getStoredPotionEffects() {
        return getSavedPotionEffects();
    }

    public boolean wasOpStored() {
        Byte value = get(key("was_op"), PersistentDataType.BYTE);
        return value != null && value == 1;
    }

    public GameMode getStoredGameMode() {
        String name = get(key("original_gamemode"), PersistentDataType.STRING);
        try {
            return name != null ? GameMode.valueOf(name) : GameMode.SURVIVAL;
        } catch (IllegalArgumentException ex) {
            return GameMode.SURVIVAL;
        }
    }

    public Location getStoredLocation() {
        return getSavedLocation();
    }

    public PlayerStoredData getAllStoredData() {
        return new PlayerStoredData(
                getStoredPotionEffects(),
                getStoredGameMode(),
                wasOpStored(),
                getStoredLocation()
        );
    }

    private void savePotionEffects(Collection<PotionEffect> effects) {
        StringBuilder sb = new StringBuilder();
        for (PotionEffect effect : effects) {
            sb.append(effect.getType().getKey().getKey())
                    .append(",")
                    .append(effect.getDuration())
                    .append(",")
                    .append(effect.getAmplifier())
                    .append(";");
        }
        if (!sb.isEmpty()) sb.setLength(sb.length() - 1);
        set(key("potion_effects"), PersistentDataType.STRING, sb.toString());
    }

    private List<PotionEffect> getSavedPotionEffects() {
        String raw = get(key("potion_effects"), PersistentDataType.STRING);
        List<PotionEffect> effects = new ArrayList<>();
        if (raw == null || raw.isEmpty()) return effects;

        for (String part : raw.split(";")) {
            String[] data = part.split(",");
            if (data.length != 3) continue;

            String effectName = data[0].toLowerCase(Locale.ROOT);
            NamespacedKey key = NamespacedKey.fromString("minecraft:" + effectName);

            PotionEffectType type = null;
            if (key != null) {
                type = Compat.getAdapter().getMobEffect(key);
            }

            if (type == null) continue;

            try {
                int duration = Integer.parseInt(data[1]);
                int amplifier = Integer.parseInt(data[2]);
                effects.add(new PotionEffect(type, duration, amplifier, false, false));
            } catch (NumberFormatException ignored) {}
        }
        return effects;
    }

    private void setOriginalGameMode(GameMode mode) {
        set(key("original_gamemode"), PersistentDataType.STRING, mode.name());
    }

    private void setWasOp(boolean value) {
        set(key("was_op"), PersistentDataType.BYTE, (byte) (value ? 1 : 0));
    }

    private void saveLocation(Location loc) {
        set(key("loc_world"), PersistentDataType.STRING, loc.getWorld().getName());
        set(key("loc_x"), PersistentDataType.DOUBLE, loc.getX());
        set(key("loc_y"), PersistentDataType.DOUBLE, loc.getY());
        set(key("loc_z"), PersistentDataType.DOUBLE, loc.getZ());
        set(key("loc_yaw"), PersistentDataType.FLOAT, loc.getYaw());
        set(key("loc_pitch"), PersistentDataType.FLOAT, loc.getPitch());
    }

    private Location getSavedLocation() {
        String world = get(key("loc_world"), PersistentDataType.STRING);
        if (world == null) return null;

        Double x = get(key("loc_x"), PersistentDataType.DOUBLE);
        Double y = get(key("loc_y"), PersistentDataType.DOUBLE);
        Double z = get(key("loc_z"), PersistentDataType.DOUBLE);
        Float yaw = get(key("loc_yaw"), PersistentDataType.FLOAT);
        Float pitch = get(key("loc_pitch"), PersistentDataType.FLOAT);

        if (x == null || y == null || z == null || yaw == null || pitch == null) return null;

        World w = Bukkit.getWorld(world);
        return (w != null) ? new Location(w, x, y, z, yaw, pitch) : null;
    }

    public record PlayerStoredData(
            Collection<PotionEffect> potionEffects,
            GameMode gameMode,
            boolean wasOp,
            Location location
    ) {}
}

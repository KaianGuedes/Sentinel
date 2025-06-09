package dev.war.sentinel.utils.pdc;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public abstract class AbstractPersistentData implements IPersistentData {

    protected final Plugin plugin;

    protected AbstractPersistentData(Plugin plugin) {
        this.plugin = plugin;
    }

    protected abstract PersistentDataContainer getContainer();

    @Override
    public <T, Z> void set(NamespacedKey key, PersistentDataType<T, Z> type, Z value) {
        getContainer().set(key, type, value);
    }

    @Override
    public <T, Z> Z get(NamespacedKey key, PersistentDataType<T, Z> type) {
        return getContainer().get(key, type);
    }

    @Override
    public void remove(NamespacedKey key) {
        getContainer().remove(key);
    }

    @Override
    public boolean has(NamespacedKey key, PersistentDataType<?, ?> type) {
        return getContainer().has(key, type);
    }

    protected NamespacedKey key(String id) {
        return new NamespacedKey(plugin, id);
    }
}

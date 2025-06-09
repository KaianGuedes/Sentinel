package dev.war.sentinel.utils.pdc;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

public interface IPersistentData {

    <T, Z> void set(NamespacedKey key, PersistentDataType<T, Z> type, Z value);

    <T, Z> Z get(NamespacedKey key, PersistentDataType<T, Z> type);

    void remove(NamespacedKey key);

    boolean has(NamespacedKey key, PersistentDataType<?, ?> type);
}

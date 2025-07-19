package dev.war.sentinel.utils;

import dev.war.sentinel.Sentinel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Messages {

    private static final Map<String, YamlConfiguration> languages = new HashMap<>();
    private static String serverLanguage = "en-US";

    public static void init(Sentinel instance) {
        File langFolder = new File(instance.getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();

            instance.saveResource("lang/en-US.yml", false);
            instance.saveResource("lang/en-GB.yml", false);
            instance.saveResource("lang/pt-BR.yml", false);
            instance.saveResource("lang/pt-PT.yml", false);
            instance.saveResource("lang/es-ES.yml", false);
            instance.saveResource("lang/de-DE.yml", false);
            instance.saveResource("lang/ru-RU.yml", false);
        }

        for (File file : langFolder.listFiles()) {
            if (file.getName().endsWith(".yml")) {
                String localeKey = file.getName().replace(".yml", "");
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                languages.put(localeKey, config);
            }
        }

        loadServerLanguage(instance);
    }

    public static void loadServerLanguage(Sentinel instance) {
        String configuredLang = instance.getConfig().getString("language", "en-US");
        if (languages.containsKey(configuredLang)) {
            serverLanguage = configuredLang;
        } else {
            instance.getLogger().warning("[!] Language '" + configuredLang + "' not found. Using default 'en-US'.");
            serverLanguage = "en-US";
        }
    }

    public static void reloadServerLanguage(Sentinel instance) {
        instance.reloadConfig();
        loadServerLanguage(instance);
    }

    public static String get(Player player, String path) {
        String langTag = player.locale().toLanguageTag();
        YamlConfiguration config = languages.getOrDefault(langTag, languages.get("en-US"));
        return config != null ? config.getString(path, "[!] Message not found: " + path) : "[!] No language file loaded.";
    }

    public static Component getComponent(Player player, String path) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(get(player, path));
    }

    public static String get(String path) {
        YamlConfiguration config = languages.getOrDefault(serverLanguage, languages.get("en-US"));
        return config != null ? config.getString(path, "[!] Message not found: " + path) : "[!] No language file loaded.";
    }

    public static Component getComponent(String path) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(get(path));
    }

    public static Component getComponent(CommandSender sender, String path) {
        if (sender instanceof Player player) {
            return getComponent(player, path);
        } else {
            return getComponent(path);
        }
    }

    public static String getServerLanguage() {
        return serverLanguage;
    }
}

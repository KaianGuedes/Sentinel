package dev.war.sentinel.utils;

import dev.war.sentinel.Sentinel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
                InputStream bundled = instance.getResource("lang/" + file.getName());
                if (bundled != null) {
                    config.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(bundled, StandardCharsets.UTF_8)));
                }
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

    private static String lookup(YamlConfiguration config, String path) {
        String value = config != null ? config.getString(path) : null;
        if (value == null) {
            YamlConfiguration fallback = languages.get("en-US");
            value = fallback != null ? fallback.getString(path) : null;
        }
        return value != null ? value : "[!] Message not found: " + path;
    }

    private static String apply(String text, String... replacements) {
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            text = text.replace(replacements[i], replacements[i + 1]);
        }
        return text;
    }

    public static String get(Player player, String path, String... replacements) {
        String langTag = player.locale().toLanguageTag();
        YamlConfiguration config = languages.getOrDefault(langTag, languages.get("en-US"));
        return apply(lookup(config, path), replacements);
    }

    public static Component getComponent(Player player, String path, String... replacements) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(get(player, path, replacements));
    }

    public static String get(String path, String... replacements) {
        YamlConfiguration config = languages.getOrDefault(serverLanguage, languages.get("en-US"));
        return apply(lookup(config, path), replacements);
    }

    public static Component getComponent(String path, String... replacements) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(get(path, replacements));
    }

    public static Component getComponent(CommandSender sender, String path, String... replacements) {
        if (sender instanceof Player player) {
            return getComponent(player, path, replacements);
        } else {
            return getComponent(path, replacements);
        }
    }

    public static String getServerLanguage() {
        return serverLanguage;
    }
}

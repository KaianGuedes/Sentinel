package dev.war.sentinel;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public record Settings(
        boolean sessionEnabled,
        long sessionMinutes,
        int loginTimeoutSeconds,
        int maxAttemptsPerPlayer,
        int maxAttemptsPerIp,
        int lockoutSeconds,
        int minPasswordLength,
        int maxPasswordLength,
        int bcryptCost,
        boolean floodgateSkipAuth,
        List<String> loginAliases,
        List<String> registerAliases,
        List<String> changePasswordAliases
) {

    public static Settings from(FileConfiguration c) {
        int timeout;
        if (!c.isSet("login.timeout-seconds") && c.isSet("session.timeout-seconds")) {
            timeout = c.getInt("session.timeout-seconds", 60);
        } else {
            timeout = c.getInt("login.timeout-seconds", 60);
        }
        int min = Math.max(1, c.getInt("password.min-length", 8));
        int max = Math.max(min, c.getInt("password.max-length", 64));
        int cost = Math.min(20, Math.max(4, c.getInt("password.bcrypt-cost", 12)));
        return new Settings(
                c.getBoolean("session.enabled", false),
                c.getLong("session.duration-minutes", 5),
                timeout,
                c.getInt("login.max-attempts-per-player", 5),
                c.getInt("login.max-attempts-per-ip", 10),
                c.getInt("login.lockout-seconds", 300),
                min,
                max,
                cost,
                c.getBoolean("floodgate.skip-auth", false),
                aliases(c, "login", List.of("l")),
                aliases(c, "register", List.of()),
                aliases(c, "changepassword", List.of())
        );
    }

    private static List<String> aliases(FileConfiguration c, String name, List<String> def) {
        String path = "commands.aliases." + name;
        return c.isList(path) ? c.getStringList(path) : def;
    }
}

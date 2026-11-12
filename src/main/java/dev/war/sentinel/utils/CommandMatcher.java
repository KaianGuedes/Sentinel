package dev.war.sentinel.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

public final class CommandMatcher {

    public enum Type {
        LOGIN,
        REGISTER,
        CHANGEPASSWORD,
        NONE
    }

    private static final String NAMESPACE = "sentinel:";

    private final Map<String, Type> byName = new LinkedHashMap<>();

    public CommandMatcher(Collection<String> loginAliases, Collection<String> registerAliases, Collection<String> changePasswordAliases) {
        add(Type.LOGIN, "login", loginAliases);
        add(Type.REGISTER, "register", registerAliases);
        add(Type.CHANGEPASSWORD, "changepassword", changePasswordAliases);
    }

    private void add(Type type, String base, Collection<String> aliases) {
        byName.putIfAbsent(base, type);
        if (aliases == null) {
            return;
        }
        for (String alias : aliases) {
            String name = sanitize(alias);
            if (name != null) {
                byName.putIfAbsent(name, type);
            }
        }
    }

    private static String sanitize(String raw) {
        if (raw == null) {
            return null;
        }
        String name = raw.trim().toLowerCase(Locale.ROOT);
        while (name.startsWith("/")) {
            name = name.substring(1);
        }
        if (name.isEmpty() || name.chars().anyMatch(Character::isWhitespace) || name.contains("/")) {
            return null;
        }
        return name;
    }

    public static String firstToken(String raw) {
        if (raw == null) {
            return "";
        }
        String text = raw.trim();
        while (text.startsWith("/")) {
            text = text.substring(1);
        }
        int end = 0;
        while (end < text.length() && !Character.isWhitespace(text.charAt(end))) {
            end++;
        }
        String token = text.substring(0, end).toLowerCase(Locale.ROOT);
        if (token.startsWith(NAMESPACE)) {
            token = token.substring(NAMESPACE.length());
        }
        return token;
    }

    public static String[] arguments(String raw) {
        if (raw == null) {
            return new String[0];
        }
        String[] parts = raw.trim().split("\\s+");
        if (parts.length <= 1) {
            return new String[0];
        }
        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);
        return args;
    }

    public Type match(String raw) {
        return byName.getOrDefault(firstToken(raw), Type.NONE);
    }

    public boolean isAuthCommand(String raw) {
        return match(raw) != Type.NONE;
    }

    public Set<String> names() {
        return new LinkedHashSet<>(byName.keySet());
    }

    public Pattern logPattern() {
        List<String> quoted = new ArrayList<>();
        for (String name : new TreeSet<>(byName.keySet())) {
            quoted.add(Pattern.quote(name));
        }
        String regex = ".*issued server command: /(?:" + Pattern.quote(NAMESPACE) + ")?(?:" + String.join("|", quoted) + ")(?:\\s.*|$)";
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    }
}

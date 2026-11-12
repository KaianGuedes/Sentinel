package dev.war.sentinel.compat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record MinecraftVersion(int major, int minor, int patch) implements Comparable<MinecraftVersion> {

    private static final Pattern PATTERN = Pattern.compile("^\\s*(\\d+)\\.(\\d+)(?:\\.(\\d+))?");

    public static MinecraftVersion parse(String text) {
        if (text == null) return null;
        Matcher m = PATTERN.matcher(text);
        if (!m.find()) return null;
        try {
            int major = Integer.parseInt(m.group(1));
            int minor = Integer.parseInt(m.group(2));
            int patch = m.group(3) != null ? Integer.parseInt(m.group(3)) : 0;
            return new MinecraftVersion(major, minor, patch);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public boolean isAtLeast(int major, int minor) {
        return compareTo(new MinecraftVersion(major, minor, 0)) >= 0;
    }

    @Override
    public int compareTo(MinecraftVersion other) {
        int c = Integer.compare(major, other.major);
        if (c != 0) return c;
        c = Integer.compare(minor, other.minor);
        if (c != 0) return c;
        return Integer.compare(patch, other.patch);
    }
}

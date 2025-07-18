package dev.war.sentinel.utils;

public enum AnsiColor {
    RESET("\u001B[0m"),
    LIGHT_PURPLE("\u001B[95m");

    private final String code;

    AnsiColor(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return code;
    }
}
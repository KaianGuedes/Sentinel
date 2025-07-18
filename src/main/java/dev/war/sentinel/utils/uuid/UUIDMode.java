package dev.war.sentinel.utils.uuid;

public enum UUIDMode {
    ONLINE, OFFLINE, ADAPT;

    public static UUIDMode fromString(String mode) {
        return switch (mode.toLowerCase()) {
            case "online" -> ONLINE;
            case "offline" -> OFFLINE;
            default -> ADAPT;
        };
    }
}

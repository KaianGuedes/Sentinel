package dev.war.sentinel.utils;

import java.nio.charset.StandardCharsets;

public final class PasswordRules {

    public static final int BCRYPT_MAX_BYTES = 72;

    public enum Result {
        OK,
        TOO_SHORT,
        TOO_LONG,
        TOO_LONG_BYTES
    }

    private PasswordRules() {
    }

    public static Result validate(String password, int minLength, int maxLength) {
        if (password == null || password.length() < minLength) {
            return Result.TOO_SHORT;
        }
        if (password.length() > maxLength) {
            return Result.TOO_LONG;
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            return Result.TOO_LONG_BYTES;
        }
        return Result.OK;
    }
}

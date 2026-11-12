package dev.war.sentinel.utils;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordHasher {

    private PasswordHasher() {
    }

    public static String hash(String password, int cost) {
        return BCrypt.hashpw(password, BCrypt.gensalt(cost));
    }

    public static boolean check(String password, String hash) {
        if (password == null || hash == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(password, hash);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}

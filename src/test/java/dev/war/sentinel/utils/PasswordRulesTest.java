package dev.war.sentinel.utils;

import dev.war.sentinel.utils.PasswordRules.Result;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import static org.junit.jupiter.api.Assertions.*;

class PasswordRulesTest {

    @Test
    void acceptsPasswordsWithinBounds() {
        assertEquals(Result.OK, PasswordRules.validate("12345678", 8, 64));
        assertEquals(Result.OK, PasswordRules.validate("a".repeat(64), 8, 64));
    }

    @Test
    void rejectsTooShortAndTooLong() {
        assertEquals(Result.TOO_SHORT, PasswordRules.validate("1234567", 8, 64));
        assertEquals(Result.TOO_SHORT, PasswordRules.validate("", 8, 64));
        assertEquals(Result.TOO_SHORT, PasswordRules.validate(null, 8, 64));
        assertEquals(Result.TOO_LONG, PasswordRules.validate("a".repeat(65), 8, 64));
    }

    @Test
    void rejectsMoreThan72Bytes() {
        assertEquals(Result.OK, PasswordRules.validate("a".repeat(72), 8, 100));
        assertEquals(Result.TOO_LONG_BYTES, PasswordRules.validate("a".repeat(73), 8, 100));
        assertEquals(Result.TOO_LONG_BYTES, PasswordRules.validate("é".repeat(40), 8, 100));
    }

    @Test
    void hasherRoundTripsAndKeepsOldHashesWorking() {
        String hash = PasswordHasher.hash("correct horse", 4);
        assertTrue(PasswordHasher.check("correct horse", hash));
        assertFalse(PasswordHasher.check("wrong", hash));

        String legacy = BCrypt.hashpw("legacy-pass", BCrypt.gensalt());
        assertTrue(PasswordHasher.check("legacy-pass", legacy));
        assertFalse(PasswordHasher.check("x", "not-a-hash"));
    }
}

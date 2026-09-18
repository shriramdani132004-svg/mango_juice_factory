package com.smartfactory;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;

public class GenerateHashTest {

    private static final String ADMIN_PASSWORD = "admin123";
    private static final String ADMIN_HASH = "$2a$10$WhBGYdFrB1jsf9hErmqAkuDGANy8b0zsRRmzA7VFH3YiOEMMkmsca";

    @Test
    void generatedHashIsValidBCrypt() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode(ADMIN_PASSWORD);
        System.out.println("BCRYPT_HASH=" + hash);
        System.out.println("BCRYPT_LENGTH=" + hash.length());
        assertTrue(encoder.matches(ADMIN_PASSWORD, hash), "Fresh hash must match admin123");
        assertEquals(60, hash.length(), "BCrypt hash must be 60 characters");
        assertTrue(hash.startsWith("$2"), "BCrypt hash must start with $2");
    }

    @Test
    void v2MigrationHashMatchesAdmin123() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        assertEquals(60, ADMIN_HASH.length(), "V2 hash must be 60 characters");
        assertTrue(ADMIN_HASH.startsWith("$2"), "V2 hash must start with $2");
        assertFalse(ADMIN_HASH.contains("\\"), "V2 hash must not contain backslashes");
        assertTrue(encoder.matches(ADMIN_PASSWORD, ADMIN_HASH),
            "V2 migration hash must match password 'admin123'");
    }

    @Test
    void v7RepairHashMatchesAdmin123() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String v7Hash = "$2a$10$WhBGYdFrB1jsf9hErmqAkuDGANy8b0zsRRmzA7VFH3YiOEMMkmsca";
        assertEquals(60, v7Hash.length(), "V7 hash must be 60 characters");
        assertTrue(encoder.matches(ADMIN_PASSWORD, v7Hash),
            "V7 repair hash must match password 'admin123'");
        assertEquals(ADMIN_HASH, v7Hash, "V2 and V7 hashes must be identical");
    }
}

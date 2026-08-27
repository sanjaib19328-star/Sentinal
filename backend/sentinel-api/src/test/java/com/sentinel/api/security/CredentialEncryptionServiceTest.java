package com.sentinel.api.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CredentialEncryptionServiceTest {

    private CredentialEncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new CredentialEncryptionService("test-secret-key-32-bytes-long!!");
    }

    @Test
    void testEncryptAndDecryptSuccess() {
        String original = "super-secret-customer-bearer-token-12345!@#";
        String encrypted = encryptionService.encrypt(original);

        assertNotNull(encrypted);
        assertNotEquals(original, encrypted);

        String decrypted = encryptionService.decrypt(encrypted);
        assertEquals(original, decrypted);
    }

    @Test
    void testNullAndEmptyHandling() {
        assertNull(encryptionService.encrypt(null));
        assertNull(encryptionService.encrypt(""));
        assertNull(encryptionService.decrypt(null));
        assertNull(encryptionService.decrypt(""));
    }

    @Test
    void testDifferentIVsProduceDifferentCiphertexts() {
        String secret = "same-secret";
        String cipher1 = encryptionService.encrypt(secret);
        String cipher2 = encryptionService.encrypt(secret);

        assertNotEquals(cipher1, cipher2);
        assertEquals(secret, encryptionService.decrypt(cipher1));
        assertEquals(secret, encryptionService.decrypt(cipher2));
    }
}

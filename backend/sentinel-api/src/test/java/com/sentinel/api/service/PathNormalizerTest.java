package com.sentinel.api.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PathNormalizerTest {

    @Test
    void testNullAndEmptyPath() {
        assertEquals("/", PathNormalizer.normalize(null));
        assertEquals("/", PathNormalizer.normalize(""));
        assertEquals("/", PathNormalizer.normalize("   "));
        assertEquals("/", PathNormalizer.normalize("/"));
    }

    @Test
    void testStaticPaths() {
        assertEquals("/users", PathNormalizer.normalize("/users"));
        assertEquals("/api/v1/products", PathNormalizer.normalize("/api/v1/products"));
        assertEquals("/orders/checkout", PathNormalizer.normalize("orders/checkout"));
        assertEquals("/users", PathNormalizer.normalize("/users/"));
    }

    @Test
    void testQueryParametersStripping() {
        assertEquals("/users", PathNormalizer.normalize("/users?page=1&limit=10"));
        assertEquals("/products/{id}", PathNormalizer.normalize("/products/12345?fields=name,price"));
        assertEquals("/search", PathNormalizer.normalize("/search?q=test%20query"));
    }

    @Test
    void testNumericIdNormalization() {
        assertEquals("/users/{id}", PathNormalizer.normalize("/users/123"));
        assertEquals("/users/{id}", PathNormalizer.normalize("/users/0"));
        assertEquals("/orders/{id}/items/{id}", PathNormalizer.normalize("/orders/987654/items/42"));
    }

    @Test
    void testUuidNormalization() {
        String uuid = "c9a646d3-9c61-4cc7-b352-19e49e29f9e5";
        assertEquals("/users/{id}", PathNormalizer.normalize("/users/" + uuid));
        assertEquals("/organizations/{id}/members/{id}", PathNormalizer.normalize("/organizations/" + uuid + "/members/456"));
    }

    @Test
    void testHexAndHashNormalization() {
        // 24-character MongoDB ObjectId
        assertEquals("/items/{id}", PathNormalizer.normalize("/items/507f1f77bcf86cd799439011"));
        // 32-character MD5 hash
        assertEquals("/files/{id}", PathNormalizer.normalize("/files/5d41402abc4b2a76b9719d911017c592"));
        // 64-character SHA-256 hash
        assertEquals("/hashes/{id}", PathNormalizer.normalize("/hashes/e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"));
    }

    @Test
    void testPrefixedIdNormalization() {
        assertEquals("/customers/{id}", PathNormalizer.normalize("/customers/cus_123456789"));
        assertEquals("/charges/{id}", PathNormalizer.normalize("/charges/ch_9876543210"));
    }

    @Test
    void testDistinctNonIdSegmentsRemainIntact() {
        assertEquals("/users/profile", PathNormalizer.normalize("/users/profile"));
        assertEquals("/users/settings/security", PathNormalizer.normalize("/users/settings/security"));
        assertEquals("/reports/daily-summary", PathNormalizer.normalize("/reports/daily-summary"));
    }
}

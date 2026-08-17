package it.unibo.piscina.service.security;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Hash delle password con PBKDF2-HMAC-SHA256 e salt per account. */
public final class PasswordHasher {

    public static final int DEFAULT_ITERATIONS = 210_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    public PasswordHash hash(final char[] password) {
        final byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        final byte[] hash = derive(password, salt, DEFAULT_ITERATIONS);
        return new PasswordHash(
            Base64.getEncoder().encodeToString(hash),
            Base64.getEncoder().encodeToString(salt),
            DEFAULT_ITERATIONS
        );
    }

    public boolean verify(
            final char[] password,
            final String expectedHash,
            final String encodedSalt,
            final int iterations) {

        try {
            final byte[] salt = Base64.getDecoder().decode(encodedSalt);
            final byte[] expected = Base64.getDecoder().decode(expectedHash);
            final byte[] actual = derive(password, salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private byte[] derive(
            final char[] password,
            final byte[] salt,
            final int iterations) {

        final PBEKeySpec specification = new PBEKeySpec(
            password,
            salt,
            iterations,
            HASH_BITS
        );
        try {
            return SecretKeyFactory
                .getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(specification)
                .getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                "Algoritmo di protezione password non disponibile",
                exception
            );
        } finally {
            specification.clearPassword();
        }
    }

    public record PasswordHash(
        String hash,
        String salt,
        int iterations
    ) {
    }
}

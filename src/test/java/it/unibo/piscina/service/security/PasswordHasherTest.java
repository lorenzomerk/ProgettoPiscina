package it.unibo.piscina.service.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unibo.piscina.service.security.PasswordHasher.PasswordHash;
import org.junit.jupiter.api.Test;

class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    void verificaPasswordCorrettaERifiutaQuellaErrata() {
        final PasswordHash protectedPassword =
            hasher.hash("Cliente123!".toCharArray());

        assertTrue(hasher.verify(
            "Cliente123!".toCharArray(),
            protectedPassword.hash(),
            protectedPassword.salt(),
            protectedPassword.iterations()
        ));
        assertFalse(hasher.verify(
            "PasswordErrata1".toCharArray(),
            protectedPassword.hash(),
            protectedPassword.salt(),
            protectedPassword.iterations()
        ));
    }

    @Test
    void generaSaltDiversiPerLaStessaPassword() {
        final PasswordHash first =
            hasher.hash("Cliente123!".toCharArray());
        final PasswordHash second =
            hasher.hash("Cliente123!".toCharArray());

        assertNotEquals(first.salt(), second.salt());
        assertNotEquals(first.hash(), second.hash());
    }
}

package com.example.myapplication;

import com.example.myapplication.utils.PasswordHasher;
import org.junit.Test;
import static org.junit.Assert.*;

public final class PasswordHasherTest {
    @Test public void saltsAreUniqueAndOnlyCorrectPasswordMatches() {
        PasswordHasher passwords = new PasswordHasher();
        String first = passwords.hash("sandi-kuat");
        String second = passwords.hash("sandi-kuat");
        assertNotEquals(first, second);
        assertFalse(first.contains("sandi-kuat"));
        assertTrue(passwords.matches("sandi-kuat", first));
        assertFalse(passwords.matches("sandi-salah", first));
    }
}

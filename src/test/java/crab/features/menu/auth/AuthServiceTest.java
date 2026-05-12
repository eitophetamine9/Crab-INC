package crab.features.menu.auth;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AuthServiceTest {
    @Test
    void signInAcceptsMatchingPasswordHash() {
        PasswordHasher hasher = new PasswordHasher();
        FakeUserCredentialsRepository users = new FakeUserCredentialsRepository();
        users.put("player", hasher.hash("secret".toCharArray()));

        AuthService auth = new AuthService(users, hasher);

        assertTrue(auth.signIn("player", "secret"));
    }

    @Test
    void signInRejectsWrongPassword() {
        PasswordHasher hasher = new PasswordHasher();
        FakeUserCredentialsRepository users = new FakeUserCredentialsRepository();
        users.put("player", hasher.hash("secret".toCharArray()));

        AuthService auth = new AuthService(users, hasher);

        assertFalse(auth.signIn("player", "wrong"));
    }

    @Test
    void signInRejectsBlankInput() {
        AuthService auth = new AuthService(new FakeUserCredentialsRepository(), new PasswordHasher());

        assertFalse(auth.signIn("", "secret"));
        assertFalse(auth.signIn("player", ""));
    }

    @Test
    void signInRejectsMalformedStoredHash() {
        FakeUserCredentialsRepository users = new FakeUserCredentialsRepository();
        users.put("player", "pbkdf2_sha256$65536$***$***");

        AuthService auth = new AuthService(users, new PasswordHasher());

        assertFalse(auth.signIn("player", "secret"));
    }

    @Test
    void signUpCreatesUserWhoCanSignIn() {
        AuthService auth = new AuthService(new FakeUserCredentialsRepository(), new PasswordHasher());

        SignUpResult result = auth.signUp(" player ", "secret", "secret");

        assertEquals(SignUpResult.CREATED, result);
        assertTrue(auth.signIn("player", "secret"));
    }

    @Test
    void signUpRejectsDuplicateUsername() {
        AuthService auth = new AuthService(new FakeUserCredentialsRepository(), new PasswordHasher());
        auth.signUp("player", "secret", "secret");

        SignUpResult result = auth.signUp("player", "other", "other");

        assertEquals(SignUpResult.USERNAME_TAKEN, result);
    }

    @Test
    void signUpRejectsBlankInput() {
        AuthService auth = new AuthService(new FakeUserCredentialsRepository(), new PasswordHasher());

        assertEquals(SignUpResult.BLANK_INPUT, auth.signUp("", "secret", "secret"));
        assertEquals(SignUpResult.BLANK_INPUT, auth.signUp("player", "", ""));
    }

    @Test
    void signUpRejectsPasswordMismatch() {
        AuthService auth = new AuthService(new FakeUserCredentialsRepository(), new PasswordHasher());

        SignUpResult result = auth.signUp("player", "secret", "different");

        assertEquals(SignUpResult.PASSWORD_MISMATCH, result);
    }

    @Test
    void devFallbackAllowsDemoSignInWhenPrimaryRepositoryHasNoUser() {
        PasswordHasher hasher = new PasswordHasher();
        UserCredentialsRepository users = new DevFallbackUserCredentialsRepository(
                new FakeUserCredentialsRepository(),
                hasher
        );
        AuthService auth = new AuthService(users, hasher);

        assertTrue(auth.signIn("demo", "demo"));
        assertFalse(auth.signIn("demo", "wrong"));
    }

    private static final class FakeUserCredentialsRepository implements UserCredentialsRepository {
        private final Map<String, String> hashesByUsername = new HashMap<>();

        private void put(String username, String passwordHash) {
            hashesByUsername.put(username, passwordHash);
        }

        @Override
        public Optional<String> findPasswordHash(String username) {
            return Optional.ofNullable(hashesByUsername.get(username));
        }

        @Override
        public boolean createUser(String username, String passwordHash) {
            if (hashesByUsername.containsKey(username)) {
                return false;
            }
            hashesByUsername.put(username, passwordHash);
            return true;
        }
    }
}

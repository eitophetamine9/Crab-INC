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
    void crabUserDemoHasDefaultFallbackValues() {
        CrabUser demo = CrabUser.demo();

        assertEquals(0, demo.id());
        assertEquals("demo", demo.username());
        assertEquals("Demo Crab", demo.displayName());
    }

    @Test
    void signInAcceptsMatchingPasswordHash() {
        PasswordHasher hasher = new PasswordHasher();
        FakeUserCredentialsRepository users = new FakeUserCredentialsRepository();
        users.put(CrabUser.create(42, "player", "Player"), hasher.hash("secret".toCharArray()));

        AuthService auth = new AuthService(users, hasher);

        assertTrue(auth.signIn("player", "secret"));
        assertEquals(42, auth.signInUser("player", "secret").orElseThrow().id());
    }

    @Test
    void signInRejectsWrongPassword() {
        PasswordHasher hasher = new PasswordHasher();
        FakeUserCredentialsRepository users = new FakeUserCredentialsRepository();
        users.put(CrabUser.create(42, "player", "Player"), hasher.hash("secret".toCharArray()));

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
        users.put(CrabUser.create(42, "player", "Player"), "pbkdf2_sha256$65536$***$***");

        AuthService auth = new AuthService(users, new PasswordHasher());

        assertFalse(auth.signIn("player", "secret"));
    }

    @Test
    void signUpCreatesUserWhoCanSignIn() {
        AuthService auth = new AuthService(new FakeUserCredentialsRepository(), new PasswordHasher());

        SignUpResult result = auth.signUp(" player ", "secret", "secret");

        assertEquals(SignUpResult.CREATED, result);
        CrabUser createdUser = auth.signInUser("player", "secret").orElseThrow();
        assertTrue(createdUser.id() > 0);
        assertEquals("player", createdUser.username());
        assertEquals("player", createdUser.displayName());
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
        assertEquals(CrabUser.demo(), auth.signInUser("demo", "demo").orElseThrow());
    }

    private static final class FakeUserCredentialsRepository implements UserCredentialsRepository {
        private final Map<String, UserCredentials> credentialsByUsername = new HashMap<>();
        private long nextId = 1;

        private void put(CrabUser user, String passwordHash) {
            credentialsByUsername.put(user.username(), new UserCredentials(user, passwordHash));
        }

        @Override
        public Optional<UserCredentials> findCredentials(String username) {
            return Optional.ofNullable(credentialsByUsername.get(username));
        }

        @Override
        public boolean createUser(String username, String passwordHash) {
            if (credentialsByUsername.containsKey(username)) {
                return false;
            }
            put(CrabUser.create(nextId++, username, username), passwordHash);
            return true;
        }
    }
}

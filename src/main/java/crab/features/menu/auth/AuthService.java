package crab.features.menu.auth;

import java.util.Objects;
import java.util.Optional;

public final class AuthService {
    private final UserCredentialsRepository users;
    private final PasswordHasher passwordHasher;

    public AuthService(UserCredentialsRepository users, PasswordHasher passwordHasher) {
        this.users = Objects.requireNonNull(users, "users");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher");
    }

    public boolean signIn(String username, String password) {
        return signInUser(username, password).isPresent();
    }

    public Optional<CrabUser> signInUser(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Optional.empty();
        }

        String normalizedUsername = username.trim();
        return users.findCredentials(normalizedUsername)
                .filter(credentials -> passwordHasher.verify(password.toCharArray(), credentials.passwordHash()))
                .map(UserCredentials::user);
    }

    public SignUpResult signUp(String username, String password, String confirmPassword) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return SignUpResult.BLANK_INPUT;
        }
        if (!password.equals(confirmPassword)) {
            return SignUpResult.PASSWORD_MISMATCH;
        }

        String normalizedUsername = username.trim();
        String passwordHash = passwordHasher.hash(password.toCharArray());
        return users.createUser(normalizedUsername, passwordHash)
                ? SignUpResult.CREATED
                : SignUpResult.USERNAME_TAKEN;
    }
}

package crab.features.menu.auth;

import java.util.Objects;
import java.util.Optional;

public final class DevFallbackUserCredentialsRepository implements UserCredentialsRepository {
    private static final String DEV_USERNAME = "demo";
    private static final String DEV_PASSWORD = "demo";

    private final UserCredentialsRepository primary;
    private final String devPasswordHash;

    public DevFallbackUserCredentialsRepository(UserCredentialsRepository primary, PasswordHasher passwordHasher) {
        this.primary = Objects.requireNonNull(primary, "primary");
        Objects.requireNonNull(passwordHasher, "passwordHasher");
        this.devPasswordHash = passwordHasher.hash(DEV_PASSWORD.toCharArray());
    }

    @Override
    public Optional<String> findPasswordHash(String username) {
        Optional<String> primaryHash = primary.findPasswordHash(username);
        if (primaryHash.isPresent()) {
            return primaryHash;
        }

        // Offline mode only: keep demo/demo usable while Docker MySQL is unavailable during development.
        if (DEV_USERNAME.equals(username)) {
            return Optional.of(devPasswordHash);
        }
        return Optional.empty();
    }

    @Override
    public boolean createUser(String username, String passwordHash) {
        return primary.createUser(username, passwordHash);
    }
}

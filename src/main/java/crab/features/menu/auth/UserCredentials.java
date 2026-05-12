package crab.features.menu.auth;

import java.util.Objects;

public record UserCredentials(CrabUser user, String passwordHash) {
    public UserCredentials {
        Objects.requireNonNull(user, "user");
        passwordHash = Objects.requireNonNull(passwordHash, "passwordHash").trim();
        if (passwordHash.isBlank()) {
            throw new IllegalArgumentException("Password hash cannot be blank");
        }
    }
}

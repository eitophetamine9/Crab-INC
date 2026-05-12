package crab.features.menu.auth;

import java.util.Objects;

public record CrabUser(long id, String username, String displayName) {
    public CrabUser {
        username = Objects.requireNonNull(username, "username").trim();
        displayName = Objects.requireNonNull(displayName, "displayName").trim();
        if (id < 0) {
            throw new IllegalArgumentException("User id cannot be negative");
        }
        if (username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be blank");
        }
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("Display name cannot be blank");
        }
    }

    public static CrabUser create(long id, String username, String displayName) {
        return new CrabUser(id, username, displayName);
    }

    public static CrabUser demo() {
        return new CrabUser(0, "demo", "Demo Crab");
    }
}

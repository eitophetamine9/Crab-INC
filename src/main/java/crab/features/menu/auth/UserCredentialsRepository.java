package crab.features.menu.auth;

import java.util.Optional;

public interface UserCredentialsRepository {
    Optional<String> findPasswordHash(String username);

    boolean createUser(String username, String passwordHash);
}

package crab.features.menu.auth;

import java.util.Optional;

public interface UserCredentialsRepository {
    Optional<UserCredentials> findCredentials(String username);

    default Optional<String> findPasswordHash(String username) {
        return findCredentials(username).map(UserCredentials::passwordHash);
    }

    boolean createUser(String username, String passwordHash);
}

package crab.appcore.db;

import crab.features.menu.auth.UserCredentialsRepository;
import crab.features.menu.auth.UserCredentials;

import java.util.Optional;

public final class DatabaseUserCredentialsRepository implements UserCredentialsRepository {
    @Override
    public Optional<UserCredentials> findCredentials(String username) {
        return DatabaseManager.findCredentials(username);
    }

    @Override
    public boolean createUser(String username, String passwordHash) {
        return DatabaseManager.createUser(username, passwordHash);
    }
}

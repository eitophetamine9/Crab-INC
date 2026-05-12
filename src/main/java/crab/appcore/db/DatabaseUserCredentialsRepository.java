package crab.appcore.db;

import crab.features.menu.auth.UserCredentialsRepository;

import java.util.Optional;

public final class DatabaseUserCredentialsRepository implements UserCredentialsRepository {
    @Override
    public Optional<String> findPasswordHash(String username) {
        return DatabaseManager.findPasswordHash(username);
    }

    @Override
    public boolean createUser(String username, String passwordHash) {
        return DatabaseManager.createUser(username, passwordHash);
    }
}

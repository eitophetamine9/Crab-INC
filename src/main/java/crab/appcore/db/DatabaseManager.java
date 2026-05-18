package crab.appcore.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Optional;

import crab.features.menu.auth.CrabUser;
import crab.features.menu.auth.PasswordHasher;
import crab.features.menu.auth.UserCredentials;

public class DatabaseManager {
    private static final String URL = env("CRABINC_DB_URL",
            "jdbc:mysql://localhost:3306/crabinc");
    private static final String USER = env("CRABINC_DB_USER", "root");
    private static final String PASS = env("CRABINC_DB_PASSWORD", "crabinc-root");

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static boolean initialized = false;
    public static synchronized void initDB() {
        if (initialized) return;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
                 Statement stmt = conn.createStatement()) {
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS crab_user (
                            id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
                            username VARCHAR(80) NOT NULL UNIQUE,
                            display_name VARCHAR(120) NOT NULL,
                            password_hash VARCHAR(255) NOT NULL,
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                        )
                        """);
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS users (
                            id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
                            username VARCHAR(80) NOT NULL UNIQUE,
                            password_hash VARCHAR(255) NOT NULL,
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                        )
                        """);
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS player_stats (
                            id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
                            username VARCHAR(80) NOT NULL,
                            score INT NOT NULL DEFAULT 0,
                            games_played INT NOT NULL DEFAULT 0
                        )
                        """);
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS app_user (
                            username VARCHAR(80) PRIMARY KEY,
                            password_hash VARCHAR(255) NOT NULL,
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                        )
                        """);
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS saves (
                            username VARCHAR(255) PRIMARY KEY,
                            crab_user_id BIGINT UNSIGNED NULL,
                            filepath VARCHAR(255),
                            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                        )
                        """);
            }
            migrateLegacyAppUsers();
            ensureSavesCrabUserColumn();
            ensureDevelopmentUser();
            initialized = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void migrateLegacyAppUsers() {
        String sql = """
                INSERT IGNORE INTO crab_user(username, display_name, password_hash)
                SELECT username, username, password_hash FROM app_user
                """;
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void ensureSavesCrabUserColumn() {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             ResultSet columns = conn.getMetaData().getColumns(null, null, "saves", "crab_user_id")) {
            if (!columns.next()) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("ALTER TABLE saves ADD COLUMN crab_user_id BIGINT UNSIGNED NULL AFTER username");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void ensureDevelopmentUser() {
        String username = env("CRABINC_DEV_USERNAME", "demo");
        String password = env("CRABINC_DEV_PASSWORD", "demo");
        String hash = new PasswordHasher().hash(password.toCharArray());
        String sql = """
                INSERT INTO users(username, password_hash)
                VALUES(?, ?)
                ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash)
                """;
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, hash);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Optional<UserCredentials> findCredentials(String username) {
        initDB();
        String sql = "SELECT id, username, password_hash FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    CrabUser user = CrabUser.create(
                            rs.getLong("id"),
                            rs.getString("username"),
                            rs.getString("username") // Use username as display_name for backwards compatibility
                    );
                    return Optional.of(new UserCredentials(user, rs.getString("password_hash")));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public static Optional<String> findPasswordHash(String username) {
        return findCredentials(username).map(UserCredentials::passwordHash);
    }

    public static Optional<CrabUser> findCrabUser(String username) {
        return findCredentials(username).map(UserCredentials::user);
    }

    private static Optional<Long> findCrabUserId(String username) {
        initDB();
        String sql = "SELECT id FROM crab_user WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getLong("id"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public static boolean createUser(String username, String passwordHash) {
        initDB();
        String sql = "INSERT INTO users(username, password_hash) VALUES(?, ?)";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, passwordHash);
            pstmt.executeUpdate();
            return true;
        } catch (java.sql.SQLIntegrityConstraintViolationException e) {
            // This is a real duplicate found in the database
            System.err.println("Signup failed: Username '" + username + "' already exists.");
            return false;
        } catch (Exception e) {
            System.err.println("Database error during signup:");
            e.printStackTrace();
            throw new RuntimeException("Database error during signup", e);
        }
    }

    public static void registerSave(String username, String filepath) {
        initDB();
        Optional<Long> crabUserId = findCrabUserId(username);
        String sql = "INSERT INTO saves(username, crab_user_id, filepath) VALUES(?, ?, ?) ON DUPLICATE KEY UPDATE crab_user_id = ?, filepath = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            if (crabUserId.isPresent()) {
                pstmt.setLong(2, crabUserId.orElseThrow());
                pstmt.setLong(4, crabUserId.orElseThrow());
            } else {
                pstmt.setNull(2, java.sql.Types.BIGINT);
                pstmt.setNull(4, java.sql.Types.BIGINT);
            }
            pstmt.setString(3, filepath);
            pstmt.setString(5, filepath);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getSaveForUser(String username) {
        initDB();
        String sql = "SELECT filepath FROM saves WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("filepath");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static record PlayerCareerStats(int highScore, int gamesPlayed) {}

    public static void recordPlayerStats(String username, int score) {
        if (username == null || username.isBlank() || "guest".equalsIgnoreCase(username)) {
            return;
        }
        initDB();
        String selectSql = "SELECT games_played, score FROM player_stats WHERE username = ?";
        String insertSql = "INSERT INTO player_stats(username, score, games_played) VALUES(?, ?, 1)";
        String updateSql = "UPDATE player_stats SET score = ?, games_played = ? WHERE username = ?";
        
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            int currentGames = 0;
            int maxScore = 0;
            boolean exists = false;
            
            try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
                pstmt.setString(1, username);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        currentGames = rs.getInt("games_played");
                        maxScore = rs.getInt("score");
                        exists = true;
                    }
                }
            }
            
            if (exists) {
                try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                    pstmt.setInt(1, Math.max(maxScore, score));
                    pstmt.setInt(2, currentGames + 1);
                    pstmt.setString(3, username);
                    pstmt.executeUpdate();
                }
            } else {
                try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                    pstmt.setString(1, username);
                    pstmt.setInt(2, score);
                    pstmt.executeUpdate();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static PlayerCareerStats getPlayerStats(String username) {
        if (username == null || username.isBlank() || "guest".equalsIgnoreCase(username)) {
            return new PlayerCareerStats(0, 0);
        }
        initDB();
        String sql = "SELECT score, games_played FROM player_stats WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new PlayerCareerStats(rs.getInt("score"), rs.getInt("games_played"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new PlayerCareerStats(0, 0);
    }
}

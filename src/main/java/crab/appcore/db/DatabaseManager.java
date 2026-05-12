package crab.appcore.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Optional;

import crab.features.menu.auth.PasswordHasher;

public class DatabaseManager {
    private static final String URL = env("CRABINC_DB_URL",
            "jdbc:mysql://localhost:3306/crabinc?serverTimezone=UTC");
    private static final String USER = env("CRABINC_DB_USER", "crabinc");
    private static final String PASS = env("CRABINC_DB_PASSWORD", "crabinc");

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    public static void initDB() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
                 Statement stmt = conn.createStatement()) {
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS app_user (
                            username VARCHAR(80) PRIMARY KEY,
                            password_hash VARCHAR(255) NOT NULL,
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                        )
                        """);
                stmt.execute("CREATE TABLE IF NOT EXISTS saves (username VARCHAR(255) PRIMARY KEY, filepath VARCHAR(255))");
            }
            ensureDevelopmentUser();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void ensureDevelopmentUser() {
        String username = env("CRABINC_DEV_USERNAME", "demo");
        String password = env("CRABINC_DEV_PASSWORD", "demo");
        String hash = new PasswordHasher().hash(password.toCharArray());
        String sql = "INSERT IGNORE INTO app_user(username, password_hash) VALUES(?, ?)";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, hash);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Optional<String> findPasswordHash(String username) {
        initDB();
        String sql = "SELECT password_hash FROM app_user WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getString("password_hash"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public static boolean createUser(String username, String passwordHash) {
        initDB();
        String sql = "INSERT INTO app_user(username, password_hash) VALUES(?, ?)";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, passwordHash);
            pstmt.executeUpdate();
            return true;
        } catch (java.sql.SQLIntegrityConstraintViolationException e) {
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void registerSave(String username, String filepath) {
        initDB();
        String sql = "INSERT INTO saves(username, filepath) VALUES(?, ?) ON DUPLICATE KEY UPDATE filepath = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, filepath);
            pstmt.setString(3, filepath);
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
}

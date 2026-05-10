package crab.appcore.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class DatabaseManager {
    private static final String URL = "jdbc:mysql://localhost:3306/crabinc?createDatabaseIfNotExist=true&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASS = "";

    public static void initDB() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
                 Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS saves (username VARCHAR(255) PRIMARY KEY, filepath VARCHAR(255))");
            }
        } catch (Exception e) {
            e.printStackTrace();
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

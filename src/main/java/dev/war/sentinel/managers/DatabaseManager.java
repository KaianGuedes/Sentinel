package dev.war.sentinel.managers;

import java.io.File;
import java.sql.*;
import java.util.UUID;
import java.util.logging.Level;

import dev.war.sentinel.Sentinel;
import dev.war.sentinel.utils.Messages;
import org.mindrot.jbcrypt.BCrypt;

public class DatabaseManager {

    private Connection connection;

    public void connect() {
        try {
            File dbDir = new File("plugins/Sentinel");
            if (!dbDir.exists()) dbDir.mkdirs();

            connection = DriverManager.getConnection("jdbc:sqlite:plugins/Sentinel/auth.db");

            PreparedStatement stmt = connection.prepareStatement("""
                CREATE TABLE IF NOT EXISTS auth (
                    uuid TEXT PRIMARY KEY,
                    password TEXT NOT NULL,
                    ip TEXT NOT NULL
                )
            """);
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            Sentinel.getInstance().getLogger().log(Level.SEVERE, Messages.get("server.database.connect_error"), e);
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            Sentinel.getInstance().getLogger().log(Level.SEVERE, Messages.get("server.database.close_error"), e);
        }
    }

    public boolean isRegistered(UUID uuid) {
        try {
            PreparedStatement stmt = connection.prepareStatement("SELECT 1 FROM auth WHERE uuid = ?");
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            boolean exists = rs.next();
            rs.close();
            stmt.close();
            return exists;
        } catch (SQLException e) {
            Sentinel.getInstance().getLogger().log(Level.SEVERE, Messages.get("server.database.access_error"), e);
            return false;
        }
    }

    public boolean validatePassword(UUID uuid, String password) {
        try {
            PreparedStatement stmt = connection.prepareStatement("SELECT password FROM auth WHERE uuid = ?");
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();

            boolean valid = false;
            if (rs.next()) {
                String hash = rs.getString("password");
                valid = BCrypt.checkpw(password, hash);
            }

            rs.close();
            stmt.close();
            return valid;
        } catch (SQLException e) {
            Sentinel.getInstance().getLogger().log(Level.SEVERE, Messages.get("server.database.access_error"), e);
            return false;
        }
    }

    public void register(UUID uuid, String password, String ip) {
        try {
            String hash = BCrypt.hashpw(password, BCrypt.gensalt());
            PreparedStatement stmt = connection.prepareStatement("INSERT INTO auth (uuid, password, ip) VALUES (?, ?, ?)");
            stmt.setString(1, uuid.toString());
            stmt.setString(2, hash);
            stmt.setString(3, ip);
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            Sentinel.getInstance().getLogger().log(Level.SEVERE, Messages.get("server.database.update_error"), e);
        }
    }

    public void unregister(UUID uuid) {
        try {
            PreparedStatement stmt = connection.prepareStatement("DELETE FROM auth WHERE uuid = ?");
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            Sentinel.getInstance().getLogger().log(Level.SEVERE, Messages.get("server.database.update_error"), e);
        }
    }

    public void changePassword(UUID uuid, String newPassword) {
        try {
            String hash = BCrypt.hashpw(newPassword, BCrypt.gensalt());
            PreparedStatement stmt = connection.prepareStatement("UPDATE auth SET password = ? WHERE uuid = ?");
            stmt.setString(1, hash);
            stmt.setString(2, uuid.toString());
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            Sentinel.getInstance().getLogger().log(Level.SEVERE, Messages.get("server.database.update_error"), e);
        }
    }

    public String getIP(UUID uuid) {
        try {
            PreparedStatement stmt = connection.prepareStatement("SELECT ip FROM auth WHERE uuid = ?");
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();

            String ip = rs.next() ? rs.getString("ip") : null;

            rs.close();
            stmt.close();
            return ip;
        } catch (SQLException e) {
            Sentinel.getInstance().getLogger().log(Level.SEVERE, Messages.get("server.database.access_error"), e);
            return null;
        }
    }
}

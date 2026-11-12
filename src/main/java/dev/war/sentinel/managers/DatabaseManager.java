package dev.war.sentinel.managers;

import dev.war.sentinel.utils.Messages;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseManager {

    private final File file;
    private final Logger logger;
    private Connection connection;

    public DatabaseManager(File file, Logger logger) {
        this.file = file;
        this.logger = logger;
    }

    public synchronized void connect() {
        try {
            conn();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, Messages.get("server.database.connect_error"), e);
        }
    }

    public synchronized void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, Messages.get("server.database.close_error"), e);
        } finally {
            connection = null;
        }
    }

    private Connection conn() throws SQLException {
        if (connection == null || connection.isClosed() || !connection.isValid(2)) {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ignored) {
                }
                logger.warning(Messages.get("server.database.reconnecting"));
            }
            open();
        }
        return connection;
    }

    private void open() throws SQLException {
        File parent = file.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        Connection created = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
        try (Statement st = created.createStatement()) {
            st.execute("PRAGMA busy_timeout=5000");
            st.execute("PRAGMA journal_mode=WAL");
            st.execute("PRAGMA synchronous=NORMAL");
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS auth (
                    uuid TEXT PRIMARY KEY,
                    password TEXT NOT NULL,
                    ip TEXT NOT NULL
                )
            """);
            boolean hasNameKey = false;
            try (ResultSet rs = st.executeQuery("PRAGMA table_info(auth)")) {
                while (rs.next()) {
                    if ("name_key".equalsIgnoreCase(rs.getString("name"))) hasNameKey = true;
                }
            }
            if (!hasNameKey) {
                st.executeUpdate("ALTER TABLE auth ADD COLUMN name_key TEXT");
            }
            st.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS idx_auth_name_key ON auth(name_key) WHERE name_key IS NOT NULL");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS state_reset (uuid TEXT PRIMARY KEY)");
        } catch (SQLException e) {
            created.close();
            throw e;
        }
        connection = created;
    }

    private DatabaseException fail(SQLException e) {
        logger.log(Level.SEVERE, Messages.get("server.database.access_error"), e);
        return new DatabaseException("Database access failed", e);
    }

    public synchronized boolean isRegistered(UUID uuid) {
        try (PreparedStatement stmt = conn().prepareStatement("SELECT 1 FROM auth WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw fail(e);
        }
    }

    public synchronized String getHash(UUID uuid) {
        try (PreparedStatement stmt = conn().prepareStatement("SELECT password FROM auth WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString("password") : null;
            }
        } catch (SQLException e) {
            throw fail(e);
        }
    }

    public synchronized boolean isNameKeyTakenByOther(String nameKey, UUID uuid) {
        try (PreparedStatement stmt = conn().prepareStatement("SELECT 1 FROM auth WHERE name_key = ? AND uuid <> ?")) {
            stmt.setString(1, nameKey);
            stmt.setString(2, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw fail(e);
        }
    }

    public synchronized UUID findUuidByNameKey(String nameKey) {
        try (PreparedStatement stmt = conn().prepareStatement("SELECT uuid FROM auth WHERE name_key = ?")) {
            stmt.setString(1, nameKey);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? UUID.fromString(rs.getString("uuid")) : null;
            }
        } catch (SQLException e) {
            throw fail(e);
        }
    }

    public synchronized void fillNameKeyIfMissing(UUID uuid, String nameKey) {
        try (PreparedStatement stmt = conn().prepareStatement("UPDATE auth SET name_key = ? WHERE uuid = ? AND name_key IS NULL")) {
            stmt.setString(1, nameKey);
            stmt.setString(2, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.WARNING, Messages.get("server.database.update_error"), e);
        }
    }

    public synchronized boolean register(UUID uuid, String nameKey, String hash, String ip) {
        try (PreparedStatement stmt = conn().prepareStatement("INSERT INTO auth (uuid, password, ip, name_key) VALUES (?, ?, ?, ?)")) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, hash);
            stmt.setString(3, ip == null ? "unknown" : ip);
            stmt.setString(4, nameKey);
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, Messages.get("server.database.update_error"), e);
            return false;
        }
    }

    public synchronized boolean unregister(UUID uuid) {
        try {
            Connection c = conn();
            c.setAutoCommit(false);
            try (PreparedStatement del = c.prepareStatement("DELETE FROM auth WHERE uuid = ?");
                 PreparedStatement mark = c.prepareStatement("INSERT OR IGNORE INTO state_reset (uuid) VALUES (?)")) {
                del.setString(1, uuid.toString());
                del.executeUpdate();
                mark.setString(1, uuid.toString());
                mark.executeUpdate();
                c.commit();
                return true;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, Messages.get("server.database.update_error"), e);
            return false;
        }
    }

    public synchronized boolean changePassword(UUID uuid, String hash) {
        try (PreparedStatement stmt = conn().prepareStatement("UPDATE auth SET password = ? WHERE uuid = ?")) {
            stmt.setString(1, hash);
            stmt.setString(2, uuid.toString());
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, Messages.get("server.database.update_error"), e);
            return false;
        }
    }

    public synchronized boolean hasStateReset(UUID uuid) {
        try (PreparedStatement stmt = conn().prepareStatement("SELECT 1 FROM state_reset WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw fail(e);
        }
    }

    public synchronized void clearStateReset(UUID uuid) {
        try (PreparedStatement stmt = conn().prepareStatement("DELETE FROM state_reset WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.WARNING, Messages.get("server.database.update_error"), e);
        }
    }

    public synchronized String getIP(UUID uuid) {
        try (PreparedStatement stmt = conn().prepareStatement("SELECT ip FROM auth WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString("ip") : null;
            }
        } catch (SQLException e) {
            throw fail(e);
        }
    }
}

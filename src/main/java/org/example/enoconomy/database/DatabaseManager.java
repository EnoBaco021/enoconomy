package org.example.enoconomy.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.example.enoconomy.Enoconomy;
import org.example.enoconomy.models.PlayerAccount;
import org.example.enoconomy.models.Transaction;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DatabaseManager {

    private final Enoconomy plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(Enoconomy plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        String dbType = plugin.getConfig().getString("database.type", "sqlite");

        HikariConfig config = new HikariConfig();

        if (dbType.equalsIgnoreCase("mysql")) {
            String host = plugin.getConfig().getString("database.mysql.host", "localhost");
            int port = plugin.getConfig().getInt("database.mysql.port", 3306);
            String database = plugin.getConfig().getString("database.mysql.database", "enoconomy");
            String username = plugin.getConfig().getString("database.mysql.username", "root");
            String password = plugin.getConfig().getString("database.mysql.password", "");

            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true");
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        } else {
            File dbFile = new File(plugin.getDataFolder(), "economy.db");
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            config.setDriverClassName("org.sqlite.JDBC");
        }

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(300000);
        config.setConnectionTimeout(10000);
        config.setMaxLifetime(600000);

        dataSource = new HikariDataSource(config);

        createTables();
    }

    private void createTables() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Oyuncu hesapları tablosu
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS player_accounts (
                    uuid VARCHAR(36) PRIMARY KEY,
                    username VARCHAR(16) NOT NULL,
                    balance DOUBLE DEFAULT 0.0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // İşlem geçmişi tablosu
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    sender_uuid VARCHAR(36),
                    receiver_uuid VARCHAR(36),
                    amount DOUBLE NOT NULL,
                    type VARCHAR(32) NOT NULL,
                    description TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // Web panel admin kullanıcıları tablosu
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS web_admins (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username VARCHAR(32) UNIQUE NOT NULL,
                    password_hash VARCHAR(256) NOT NULL,
                    api_key VARCHAR(64) UNIQUE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            plugin.getLogger().info("Veritabanı tabloları oluşturuldu!");
        } catch (SQLException e) {
            plugin.getLogger().severe("Tablolar oluşturulurken hata: " + e.getMessage());
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    // ==================== Player Account Methods ====================

    public PlayerAccount getPlayerAccount(UUID uuid) {
        String sql = "SELECT * FROM player_accounts WHERE uuid = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new PlayerAccount(
                        UUID.fromString(rs.getString("uuid")),
                        rs.getString("username"),
                        rs.getDouble("balance"),
                        rs.getTimestamp("created_at"),
                        rs.getTimestamp("last_seen")
                );
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Oyuncu hesabı alınırken hata: " + e.getMessage());
        }
        return null;
    }

    public PlayerAccount getPlayerAccountByName(String username) {
        String sql = "SELECT * FROM player_accounts WHERE LOWER(username) = LOWER(?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new PlayerAccount(
                        UUID.fromString(rs.getString("uuid")),
                        rs.getString("username"),
                        rs.getDouble("balance"),
                        rs.getTimestamp("created_at"),
                        rs.getTimestamp("last_seen")
                );
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Oyuncu hesabı alınırken hata: " + e.getMessage());
        }
        return null;
    }

    public void createPlayerAccount(UUID uuid, String username) {
        String sql = "INSERT OR IGNORE INTO player_accounts (uuid, username, balance) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, username);
            stmt.setDouble(3, plugin.getConfig().getDouble("economy.starting-balance", 100.0));
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Oyuncu hesabı oluşturulurken hata: " + e.getMessage());
        }
    }

    public void updatePlayerBalance(UUID uuid, double balance) {
        String sql = "UPDATE player_accounts SET balance = ? WHERE uuid = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, balance);
            stmt.setString(2, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Bakiye güncellenirken hata: " + e.getMessage());
        }
    }

    public void updateLastSeen(UUID uuid) {
        String sql = "UPDATE player_accounts SET last_seen = CURRENT_TIMESTAMP WHERE uuid = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Son görülme güncellenirken hata: " + e.getMessage());
        }
    }

    public List<PlayerAccount> getTopBalances(int limit) {
        List<PlayerAccount> accounts = new ArrayList<>();
        String sql = "SELECT * FROM player_accounts ORDER BY balance DESC LIMIT ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                accounts.add(new PlayerAccount(
                        UUID.fromString(rs.getString("uuid")),
                        rs.getString("username"),
                        rs.getDouble("balance"),
                        rs.getTimestamp("created_at"),
                        rs.getTimestamp("last_seen")
                ));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Top bakiyeler alınırken hata: " + e.getMessage());
        }
        return accounts;
    }

    public List<PlayerAccount> getAllPlayerAccounts() {
        List<PlayerAccount> accounts = new ArrayList<>();
        String sql = "SELECT * FROM player_accounts ORDER BY balance DESC";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                accounts.add(new PlayerAccount(
                        UUID.fromString(rs.getString("uuid")),
                        rs.getString("username"),
                        rs.getDouble("balance"),
                        rs.getTimestamp("created_at"),
                        rs.getTimestamp("last_seen")
                ));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Tüm hesaplar alınırken hata: " + e.getMessage());
        }
        return accounts;
    }

    // ==================== Transaction Methods ====================

    public void logTransaction(UUID sender, UUID receiver, double amount, String type, String description) {
        String sql = "INSERT INTO transactions (sender_uuid, receiver_uuid, amount, type, description) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, sender != null ? sender.toString() : null);
            stmt.setString(2, receiver != null ? receiver.toString() : null);
            stmt.setDouble(3, amount);
            stmt.setString(4, type);
            stmt.setString(5, description);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("İşlem kaydedilirken hata: " + e.getMessage());
        }
    }

    public List<Transaction> getPlayerTransactions(UUID playerUuid, int limit) {
        List<Transaction> transactions = new ArrayList<>();
        String sql = """
            SELECT * FROM transactions 
            WHERE sender_uuid = ? OR receiver_uuid = ?
            ORDER BY created_at DESC
            LIMIT ?
        """;
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, playerUuid.toString());
            stmt.setInt(3, limit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                transactions.add(new Transaction(
                        rs.getInt("id"),
                        rs.getString("sender_uuid") != null ? UUID.fromString(rs.getString("sender_uuid")) : null,
                        rs.getString("receiver_uuid") != null ? UUID.fromString(rs.getString("receiver_uuid")) : null,
                        rs.getDouble("amount"),
                        rs.getString("type"),
                        rs.getString("description"),
                        rs.getTimestamp("created_at")
                ));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("İşlem geçmişi alınırken hata: " + e.getMessage());
        }
        return transactions;
    }

    public List<Transaction> getAllTransactions(int limit) {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM transactions ORDER BY created_at DESC LIMIT ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                transactions.add(new Transaction(
                        rs.getInt("id"),
                        rs.getString("sender_uuid") != null ? UUID.fromString(rs.getString("sender_uuid")) : null,
                        rs.getString("receiver_uuid") != null ? UUID.fromString(rs.getString("receiver_uuid")) : null,
                        rs.getDouble("amount"),
                        rs.getString("type"),
                        rs.getString("description"),
                        rs.getTimestamp("created_at")
                ));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Tüm işlemler alınırken hata: " + e.getMessage());
        }
        return transactions;
    }

    // ==================== Web Admin Methods ====================

    public boolean validateWebAdmin(String username, String passwordHash) {
        String sql = "SELECT 1 FROM web_admins WHERE username = ? AND password_hash = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, passwordHash);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            plugin.getLogger().severe("Admin doğrulaması yapılırken hata: " + e.getMessage());
        }
        return false;
    }

    public boolean validateApiKey(String apiKey) {
        String sql = "SELECT 1 FROM web_admins WHERE api_key = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, apiKey);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            plugin.getLogger().severe("API anahtarı doğrulaması yapılırken hata: " + e.getMessage());
        }
        return false;
    }

    public void createWebAdmin(String username, String passwordHash, String apiKey) {
        String sql = "INSERT INTO web_admins (username, password_hash, api_key) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, passwordHash);
            stmt.setString(3, apiKey);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Web admin oluşturulurken hata: " + e.getMessage());
        }
    }

    public String getApiKeyForUser(String username) {
        String sql = "SELECT api_key FROM web_admins WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("api_key");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("API anahtarı alınırken hata: " + e.getMessage());
        }
        return null;
    }

    // ==================== Statistics Methods ====================

    public double getTotalServerMoney() {
        String sql = "SELECT SUM(balance) as total FROM player_accounts";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("total");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Toplam para hesaplanırken hata: " + e.getMessage());
        }
        return 0;
    }

    public int getTotalPlayerCount() {
        String sql = "SELECT COUNT(*) as count FROM player_accounts";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Toplam oyuncu sayısı alınırken hata: " + e.getMessage());
        }
        return 0;
    }

    public int getTotalTransactionCount() {
        String sql = "SELECT COUNT(*) as count FROM transactions";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Toplam işlem sayısı alınırken hata: " + e.getMessage());
        }
        return 0;
    }
}


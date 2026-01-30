package org.example.enoconomy.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.example.enoconomy.Enoconomy;
import org.example.enoconomy.models.PlayerAccount;
import org.example.enoconomy.models.Transaction;
import spark.Spark;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

import static spark.Spark.*;

public class WebServer {

    private final Enoconomy plugin;
    private final Gson gson;
    private final int port;
    private final Set<String> activeSessions = new HashSet<>();

    public WebServer(Enoconomy plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.port = plugin.getConfig().getInt("web-panel.port", 3000);
    }

    public void start() {
        port(port);

        // Static dosyalar
        staticFiles.location("/web");

        // CORS ayarları
        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.header("Access-Control-Allow-Headers", "Content-Type, Authorization, X-API-Key");
        });

        options("/*", (request, response) -> {
            return "OK";
        });

        // Ana sayfa
        get("/", (req, res) -> {
            res.redirect("/index.html");
            return null;
        });

        // ==================== AUTH ROUTES ====================

        post("/api/auth/login", (req, res) -> {
            res.type("application/json");
            Map<String, String> body = gson.fromJson(req.body(), Map.class);
            String username = body.get("username");
            String password = body.get("password");

            if (username == null || password == null) {
                res.status(400);
                return gson.toJson(Map.of("error", "Kullanıcı adı ve şifre gerekli"));
            }

            String passwordHash = hashPassword(password);
            if (plugin.getDatabaseManager().validateWebAdmin(username, passwordHash)) {
                String sessionToken = UUID.randomUUID().toString();
                activeSessions.add(sessionToken);
                String apiKey = plugin.getDatabaseManager().getApiKeyForUser(username);
                return gson.toJson(Map.of(
                    "success", true,
                    "token", sessionToken,
                    "apiKey", apiKey,
                    "username", username
                ));
            } else {
                res.status(401);
                return gson.toJson(Map.of("error", "Geçersiz kullanıcı adı veya şifre"));
            }
        });

        post("/api/auth/logout", (req, res) -> {
            res.type("application/json");
            String token = req.headers("Authorization");
            if (token != null) {
                token = token.replace("Bearer ", "");
                activeSessions.remove(token);
            }
            return gson.toJson(Map.of("success", true));
        });

        // ==================== API ROUTES ====================

        // Dashboard istatistikleri
        get("/api/stats", (req, res) -> {
            res.type("application/json");
            if (!isAuthorized(req)) {
                res.status(401);
                return gson.toJson(Map.of("error", "Yetkisiz erişim"));
            }

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalPlayers", plugin.getDatabaseManager().getTotalPlayerCount());
            stats.put("totalMoney", plugin.getDatabaseManager().getTotalServerMoney());
            stats.put("totalTransactions", plugin.getDatabaseManager().getTotalTransactionCount());
            stats.put("onlinePlayers", plugin.getServer().getOnlinePlayers().size());
            stats.put("currencySymbol", plugin.getConfig().getString("economy.currency-symbol", "$"));
            stats.put("serverName", plugin.getServer().getName());

            return gson.toJson(stats);
        });

        // Tüm oyuncular
        get("/api/players", (req, res) -> {
            res.type("application/json");
            if (!isAuthorized(req)) {
                res.status(401);
                return gson.toJson(Map.of("error", "Yetkisiz erişim"));
            }

            List<PlayerAccount> accounts = plugin.getDatabaseManager().getAllPlayerAccounts();
            List<Map<String, Object>> players = accounts.stream().map(acc -> {
                Map<String, Object> player = new HashMap<>();
                player.put("uuid", acc.getUuid().toString());
                player.put("username", acc.getUsername());
                player.put("balance", acc.getBalance());
                player.put("createdAt", acc.getCreatedAt() != null ? acc.getCreatedAt().getTime() : 0);
                player.put("lastSeen", acc.getLastSeen() != null ? acc.getLastSeen().getTime() : 0);
                player.put("online", plugin.getServer().getPlayer(acc.getUuid()) != null);
                return player;
            }).collect(Collectors.toList());

            return gson.toJson(Map.of("players", players, "total", players.size()));
        });

        // Tek oyuncu bilgisi
        get("/api/players/:uuid", (req, res) -> {
            res.type("application/json");
            if (!isAuthorized(req)) {
                res.status(401);
                return gson.toJson(Map.of("error", "Yetkisiz erişim"));
            }

            String uuidStr = req.params(":uuid");
            try {
                UUID uuid = UUID.fromString(uuidStr);
                PlayerAccount account = plugin.getDatabaseManager().getPlayerAccount(uuid);
                if (account == null) {
                    res.status(404);
                    return gson.toJson(Map.of("error", "Oyuncu bulunamadı"));
                }

                Map<String, Object> player = new HashMap<>();
                player.put("uuid", account.getUuid().toString());
                player.put("username", account.getUsername());
                player.put("balance", account.getBalance());
                player.put("createdAt", account.getCreatedAt() != null ? account.getCreatedAt().getTime() : 0);
                player.put("lastSeen", account.getLastSeen() != null ? account.getLastSeen().getTime() : 0);
                player.put("online", plugin.getServer().getPlayer(uuid) != null);

                // İşlem geçmişi
                List<Transaction> transactions = plugin.getDatabaseManager().getPlayerTransactions(uuid, 50);
                player.put("transactions", transactions.stream().map(this::transactionToMap).collect(Collectors.toList()));

                return gson.toJson(player);
            } catch (IllegalArgumentException e) {
                res.status(400);
                return gson.toJson(Map.of("error", "Geçersiz UUID"));
            }
        });

        // Bakiye güncelleme
        put("/api/players/:uuid/balance", (req, res) -> {
            res.type("application/json");
            if (!isAuthorized(req)) {
                res.status(401);
                return gson.toJson(Map.of("error", "Yetkisiz erişim"));
            }

            String uuidStr = req.params(":uuid");
            Map<String, Object> body = gson.fromJson(req.body(), Map.class);

            try {
                UUID uuid = UUID.fromString(uuidStr);
                PlayerAccount account = plugin.getDatabaseManager().getPlayerAccount(uuid);
                if (account == null) {
                    res.status(404);
                    return gson.toJson(Map.of("error", "Oyuncu bulunamadı"));
                }

                String action = (String) body.get("action");
                double amount = ((Number) body.get("amount")).doubleValue();

                boolean success = false;
                switch (action) {
                    case "set" -> success = plugin.getEconomyManager().setBalance(uuid, amount);
                    case "add" -> success = plugin.getEconomyManager().deposit(uuid, amount);
                    case "remove" -> success = plugin.getEconomyManager().withdraw(uuid, amount);
                }

                if (success) {
                    // İşlemi kaydet
                    plugin.getDatabaseManager().logTransaction(null, uuid, amount, "WEB_ADMIN_" + action.toUpperCase(),
                            "Web panel üzerinden " + action);

                    PlayerAccount updated = plugin.getDatabaseManager().getPlayerAccount(uuid);
                    return gson.toJson(Map.of(
                        "success", true,
                        "newBalance", updated.getBalance(),
                        "message", "Bakiye güncellendi"
                    ));
                } else {
                    res.status(400);
                    return gson.toJson(Map.of("error", "İşlem başarısız"));
                }
            } catch (Exception e) {
                res.status(400);
                return gson.toJson(Map.of("error", e.getMessage()));
            }
        });

        // İşlem geçmişi
        get("/api/transactions", (req, res) -> {
            res.type("application/json");
            if (!isAuthorized(req)) {
                res.status(401);
                return gson.toJson(Map.of("error", "Yetkisiz erişim"));
            }

            int limit = 100;
            String limitParam = req.queryParams("limit");
            if (limitParam != null) {
                try {
                    limit = Integer.parseInt(limitParam);
                } catch (NumberFormatException ignored) {}
            }

            List<Transaction> transactions = plugin.getDatabaseManager().getAllTransactions(limit);
            List<Map<String, Object>> transactionList = transactions.stream()
                    .map(this::transactionToMap)
                    .collect(Collectors.toList());

            return gson.toJson(Map.of("transactions", transactionList, "total", transactionList.size()));
        });

        // Top bakiyeler
        get("/api/leaderboard", (req, res) -> {
            res.type("application/json");
            if (!isAuthorized(req)) {
                res.status(401);
                return gson.toJson(Map.of("error", "Yetkisiz erişim"));
            }

            int limit = 10;
            String limitParam = req.queryParams("limit");
            if (limitParam != null) {
                try {
                    limit = Integer.parseInt(limitParam);
                } catch (NumberFormatException ignored) {}
            }

            List<PlayerAccount> topPlayers = plugin.getDatabaseManager().getTopBalances(limit);
            List<Map<String, Object>> leaderboard = new ArrayList<>();

            int rank = 1;
            for (PlayerAccount acc : topPlayers) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("rank", rank++);
                entry.put("uuid", acc.getUuid().toString());
                entry.put("username", acc.getUsername());
                entry.put("balance", acc.getBalance());
                entry.put("online", plugin.getServer().getPlayer(acc.getUuid()) != null);
                leaderboard.add(entry);
            }

            return gson.toJson(Map.of("leaderboard", leaderboard));
        });


        // Sunucu bilgisi
        get("/api/server", (req, res) -> {
            res.type("application/json");
            if (!isAuthorized(req)) {
                res.status(401);
                return gson.toJson(Map.of("error", "Yetkisiz erişim"));
            }

            Map<String, Object> server = new HashMap<>();
            server.put("name", plugin.getServer().getName());
            server.put("version", plugin.getServer().getVersion());
            server.put("onlinePlayers", plugin.getServer().getOnlinePlayers().size());
            server.put("maxPlayers", plugin.getServer().getMaxPlayers());
            server.put("pluginVersion", plugin.getDescription().getVersion());

            return gson.toJson(server);
        });

        // Konfigürasyon
        get("/api/config", (req, res) -> {
            res.type("application/json");
            if (!isAuthorized(req)) {
                res.status(401);
                return gson.toJson(Map.of("error", "Yetkisiz erişim"));
            }

            Map<String, Object> config = new HashMap<>();
            config.put("currencySymbol", plugin.getConfig().getString("economy.currency-symbol", "$"));
            config.put("currencyName", plugin.getConfig().getString("economy.currency-name", "Coin"));
            config.put("currencyNamePlural", plugin.getConfig().getString("economy.currency-name-plural", "Coins"));
            config.put("startingBalance", plugin.getConfig().getDouble("economy.starting-balance", 100.0));
            config.put("maxBalance", plugin.getConfig().getDouble("economy.max-balance", 1000000000));
            config.put("transferTax", plugin.getConfig().getDouble("economy.transfer-tax", 0));
            config.put("minTransfer", plugin.getConfig().getDouble("economy.min-transfer", 1.0));

            return gson.toJson(config);
        });

        // Konfigürasyon güncelleme
        put("/api/config", (req, res) -> {
            res.type("application/json");
            if (!isAuthorized(req)) {
                res.status(401);
                return gson.toJson(Map.of("error", "Yetkisiz erişim"));
            }

            try {
                Map<String, Object> body = gson.fromJson(req.body(), Map.class);

                // Ayarları güncelle
                if (body.containsKey("currencySymbol")) {
                    plugin.getConfig().set("economy.currency-symbol", body.get("currencySymbol"));
                }
                if (body.containsKey("currencyName")) {
                    plugin.getConfig().set("economy.currency-name", body.get("currencyName"));
                }
                if (body.containsKey("currencyNamePlural")) {
                    plugin.getConfig().set("economy.currency-name-plural", body.get("currencyNamePlural"));
                }
                if (body.containsKey("startingBalance")) {
                    plugin.getConfig().set("economy.starting-balance", ((Number) body.get("startingBalance")).doubleValue());
                }
                if (body.containsKey("maxBalance")) {
                    plugin.getConfig().set("economy.max-balance", ((Number) body.get("maxBalance")).doubleValue());
                }
                if (body.containsKey("transferTax")) {
                    plugin.getConfig().set("economy.transfer-tax", ((Number) body.get("transferTax")).doubleValue());
                }
                if (body.containsKey("minTransfer")) {
                    plugin.getConfig().set("economy.min-transfer", ((Number) body.get("minTransfer")).doubleValue());
                }

                // Config dosyasını kaydet
                plugin.saveConfig();

                // EconomyManager'ı yeniden yükle
                plugin.getEconomyManager().reloadConfig();

                return gson.toJson(Map.of(
                    "success", true,
                    "message", "Ayarlar başarıyla güncellendi!"
                ));
            } catch (Exception e) {
                res.status(400);
                return gson.toJson(Map.of("error", "Ayarlar güncellenirken hata: " + e.getMessage()));
            }
        });

        plugin.getLogger().info("Web panel başlatıldı: http://localhost:" + port);
    }

    public void stop() {
        Spark.stop();
        plugin.getLogger().info("Web panel durduruldu.");
    }

    private boolean isAuthorized(spark.Request req) {
        // Session token kontrolü
        String authHeader = req.headers("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (activeSessions.contains(token)) {
                return true;
            }
        }

        // API Key kontrolü
        String apiKey = req.headers("X-API-Key");
        if (apiKey != null) {
            return plugin.getDatabaseManager().validateApiKey(apiKey);
        }

        return false;
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return password;
        }
    }

    private Map<String, Object> transactionToMap(Transaction t) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", t.getId());
        map.put("senderUuid", t.getSenderUuid() != null ? t.getSenderUuid().toString() : null);
        map.put("receiverUuid", t.getReceiverUuid() != null ? t.getReceiverUuid().toString() : null);
        map.put("amount", t.getAmount());
        map.put("type", t.getType());
        map.put("description", t.getDescription());
        map.put("createdAt", t.getCreatedAt() != null ? t.getCreatedAt().getTime() : 0);

        // İsimleri ekle
        if (t.getSenderUuid() != null) {
            PlayerAccount sender = plugin.getDatabaseManager().getPlayerAccount(t.getSenderUuid());
            map.put("senderName", sender != null ? sender.getUsername() : "Sistem");
        } else {
            map.put("senderName", "Sistem");
        }

        if (t.getReceiverUuid() != null) {
            PlayerAccount receiver = plugin.getDatabaseManager().getPlayerAccount(t.getReceiverUuid());
            map.put("receiverName", receiver != null ? receiver.getUsername() : "Sistem");
        } else {
            map.put("receiverName", "Sistem");
        }

        return map;
    }
}

package org.example.enoconomy;

import org.bukkit.plugin.java.JavaPlugin;
import org.example.enoconomy.api.EconomyAPI;
import org.example.enoconomy.commands.*;
import org.example.enoconomy.database.DatabaseManager;
import org.example.enoconomy.listeners.PlayerListener;
import org.example.enoconomy.managers.EconomyManager;
import org.example.enoconomy.managers.TransactionManager;
import org.example.enoconomy.placeholders.EnoconomyPlaceholders;
import org.example.enoconomy.web.WebServer;

public class Enoconomy extends JavaPlugin {

    private static Enoconomy instance;
    private DatabaseManager databaseManager;
    private EconomyManager economyManager;
    private TransactionManager transactionManager;
    private WebServer webServer;
    private EconomyAPI economyAPI;

    @Override
    public void onEnable() {
        instance = this;

        // Config dosyasını kaydet
        saveDefaultConfig();

        // Veritabanı bağlantısını başlat
        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();

        // Manager'ları başlat
        economyManager = new EconomyManager(this);
        transactionManager = new TransactionManager(this);

        // API'yi başlat
        economyAPI = new EconomyAPI(this);

        // Komutları kaydet
        MainCommand mainCommand = new MainCommand(this);
        MoneyCommand moneyCommand = new MoneyCommand(this);
        PayCommand payCommand = new PayCommand(this);
        TopCommand topCommand = new TopCommand(this);

        // Ana komut
        getCommand("enoconomy").setExecutor(mainCommand);
        getCommand("enoconomy").setTabCompleter(mainCommand);

        // Oyuncu komutları
        getCommand("money").setExecutor(moneyCommand);
        getCommand("money").setTabCompleter(moneyCommand);

        getCommand("pay").setExecutor(payCommand);
        getCommand("pay").setTabCompleter(payCommand);

        getCommand("top").setExecutor(topCommand);
        getCommand("top").setTabCompleter(topCommand);

        // Listener'ları kaydet
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        // PlaceholderAPI entegrasyonu
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new EnoconomyPlaceholders(this).register();
            getLogger().info("PlaceholderAPI entegrasyonu aktif!");
        }

        // Web sunucusunu başlat
        if (getConfig().getBoolean("web-panel.enabled", true)) {
            webServer = new WebServer(this);
            webServer.start();
        }

        getLogger().info("Enoconomy başarıyla aktifleştirildi!");
    }

    @Override
    public void onDisable() {
        // Web sunucusunu durdur
        if (webServer != null) {
            webServer.stop();
        }

        // Veritabanı bağlantısını kapat
        if (databaseManager != null) {
            databaseManager.close();
        }

        getLogger().info("Enoconomy devre dışı bırakıldı!");
    }

    public static Enoconomy getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public TransactionManager getTransactionManager() {
        return transactionManager;
    }

    public EconomyAPI getEconomyAPI() {
        return economyAPI;
    }

    public WebServer getWebServer() {
        return webServer;
    }
}

package org.example.enoconomy.api;

import org.bukkit.OfflinePlayer;
import org.example.enoconomy.Enoconomy;
import org.example.enoconomy.models.PlayerAccount;

import java.util.UUID;

/**
 * Enoconomy API - Diğer eklentilerin ekonomi sistemine erişmesini sağlar
 */
public class EconomyAPI {

    private final Enoconomy plugin;

    public EconomyAPI(Enoconomy plugin) {
        this.plugin = plugin;
    }

    /**
     * Oyuncunun bakiyesini döndürür
     */
    public double getBalance(UUID uuid) {
        return plugin.getEconomyManager().getBalance(uuid);
    }

    /**
     * Oyuncunun bakiyesini döndürür
     */
    public double getBalance(OfflinePlayer player) {
        return plugin.getEconomyManager().getBalance(player);
    }

    /**
     * Oyuncunun belirtilen miktara sahip olup olmadığını kontrol eder
     */
    public boolean hasEnough(UUID uuid, double amount) {
        return plugin.getEconomyManager().hasEnough(uuid, amount);
    }

    /**
     * Oyuncunun belirtilen miktara sahip olup olmadığını kontrol eder
     */
    public boolean hasEnough(OfflinePlayer player, double amount) {
        return plugin.getEconomyManager().hasEnough(player, amount);
    }

    /**
     * Oyuncunun hesabına para ekler
     */
    public boolean deposit(UUID uuid, double amount) {
        return plugin.getEconomyManager().deposit(uuid, amount);
    }

    /**
     * Oyuncunun hesabına para ekler
     */
    public boolean deposit(OfflinePlayer player, double amount) {
        return plugin.getEconomyManager().deposit(player, amount);
    }

    /**
     * Oyuncunun hesabından para çeker
     */
    public boolean withdraw(UUID uuid, double amount) {
        return plugin.getEconomyManager().withdraw(uuid, amount);
    }

    /**
     * Oyuncunun hesabından para çeker
     */
    public boolean withdraw(OfflinePlayer player, double amount) {
        return plugin.getEconomyManager().withdraw(player, amount);
    }

    /**
     * Oyuncunun bakiyesini ayarlar
     */
    public boolean setBalance(UUID uuid, double amount) {
        return plugin.getEconomyManager().setBalance(uuid, amount);
    }

    /**
     * Oyuncunun bakiyesini ayarlar
     */
    public boolean setBalance(OfflinePlayer player, double amount) {
        return plugin.getEconomyManager().setBalance(player, amount);
    }

    /**
     * İki oyuncu arasında para transferi yapar
     */
    public boolean transfer(UUID from, UUID to, double amount) {
        return plugin.getEconomyManager().transfer(from, to, amount);
    }

    /**
     * İki oyuncu arasında para transferi yapar
     */
    public boolean transfer(OfflinePlayer from, OfflinePlayer to, double amount) {
        return plugin.getEconomyManager().transfer(from, to, amount);
    }

    /**
     * Oyuncunun hesabı olup olmadığını kontrol eder
     */
    public boolean hasAccount(UUID uuid) {
        return plugin.getEconomyManager().hasAccount(uuid);
    }

    /**
     * Oyuncunun hesabı olup olmadığını kontrol eder
     */
    public boolean hasAccount(OfflinePlayer player) {
        return plugin.getEconomyManager().hasAccount(player);
    }

    /**
     * Para miktarını formatlar
     */
    public String formatMoney(double amount) {
        return plugin.getEconomyManager().formatMoney(amount);
    }

    /**
     * Para birimi adını döndürür
     */
    public String getCurrencyName(boolean plural) {
        return plugin.getEconomyManager().getCurrencyName(plural);
    }
}

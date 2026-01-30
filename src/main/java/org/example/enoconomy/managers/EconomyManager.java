package org.example.enoconomy.managers;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.example.enoconomy.Enoconomy;
import org.example.enoconomy.models.PlayerAccount;

import java.text.DecimalFormat;
import java.util.UUID;

public class EconomyManager {

    private final Enoconomy plugin;
    private DecimalFormat decimalFormat;

    public EconomyManager(Enoconomy plugin) {
        this.plugin = plugin;
        this.decimalFormat = new DecimalFormat(plugin.getConfig().getString("economy.format", "#,##0.00"));
    }

    public void reloadConfig() {
        this.decimalFormat = new DecimalFormat(plugin.getConfig().getString("economy.format", "#,##0.00"));
    }

    public double getBalance(UUID uuid) {
        PlayerAccount account = plugin.getDatabaseManager().getPlayerAccount(uuid);
        return account != null ? account.getBalance() : 0.0;
    }

    public double getBalance(OfflinePlayer player) {
        return getBalance(player.getUniqueId());
    }

    public boolean hasEnough(UUID uuid, double amount) {
        return getBalance(uuid) >= amount;
    }

    public boolean hasEnough(OfflinePlayer player, double amount) {
        return hasEnough(player.getUniqueId(), amount);
    }

    public boolean deposit(UUID uuid, double amount) {
        if (amount <= 0) return false;

        PlayerAccount account = plugin.getDatabaseManager().getPlayerAccount(uuid);
        if (account == null) return false;

        double maxBalance = plugin.getConfig().getDouble("economy.max-balance", 1000000000);
        double newBalance = Math.min(account.getBalance() + amount, maxBalance);

        plugin.getDatabaseManager().updatePlayerBalance(uuid, newBalance);
        return true;
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        return deposit(player.getUniqueId(), amount);
    }

    public boolean withdraw(UUID uuid, double amount) {
        if (amount <= 0) return false;

        PlayerAccount account = plugin.getDatabaseManager().getPlayerAccount(uuid);
        if (account == null) return false;

        if (account.getBalance() < amount) return false;

        plugin.getDatabaseManager().updatePlayerBalance(uuid, account.getBalance() - amount);
        return true;
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        return withdraw(player.getUniqueId(), amount);
    }

    public boolean setBalance(UUID uuid, double amount) {
        if (amount < 0) return false;

        PlayerAccount account = plugin.getDatabaseManager().getPlayerAccount(uuid);
        if (account == null) return false;

        double maxBalance = plugin.getConfig().getDouble("economy.max-balance", 1000000000);
        plugin.getDatabaseManager().updatePlayerBalance(uuid, Math.min(amount, maxBalance));
        return true;
    }

    public boolean setBalance(OfflinePlayer player, double amount) {
        return setBalance(player.getUniqueId(), amount);
    }

    public boolean transfer(UUID from, UUID to, double amount) {
        if (amount <= 0) return false;
        if (!hasEnough(from, amount)) return false;

        double tax = plugin.getConfig().getDouble("economy.transfer-tax", 0);
        double taxAmount = amount * (tax / 100);
        double finalAmount = amount - taxAmount;

        if (withdraw(from, amount) && deposit(to, finalAmount)) {
            plugin.getDatabaseManager().logTransaction(from, to, amount, "TRANSFER",
                    "Para transferi" + (taxAmount > 0 ? " (Vergi: " + formatMoney(taxAmount) + ")" : ""));
            return true;
        }
        return false;
    }

    public boolean transfer(OfflinePlayer from, OfflinePlayer to, double amount) {
        return transfer(from.getUniqueId(), to.getUniqueId(), amount);
    }

    public String formatMoney(double amount) {
        String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");
        String symbolPosition = plugin.getConfig().getString("economy.symbol-position", "before");

        String formatted = decimalFormat.format(amount);

        if (symbolPosition.equalsIgnoreCase("before")) {
            return symbol + formatted;
        } else {
            return formatted + symbol;
        }
    }

    public String getCurrencyName(boolean plural) {
        if (plural) {
            return plugin.getConfig().getString("economy.currency-name-plural", "Coins");
        }
        return plugin.getConfig().getString("economy.currency-name", "Coin");
    }

    public void createAccount(Player player) {
        plugin.getDatabaseManager().createPlayerAccount(player.getUniqueId(), player.getName());
    }

    public boolean hasAccount(UUID uuid) {
        return plugin.getDatabaseManager().getPlayerAccount(uuid) != null;
    }

    public boolean hasAccount(OfflinePlayer player) {
        return hasAccount(player.getUniqueId());
    }

    public PlayerAccount getAccount(UUID uuid) {
        return plugin.getDatabaseManager().getPlayerAccount(uuid);
    }

    public PlayerAccount getAccountByName(String name) {
        return plugin.getDatabaseManager().getPlayerAccountByName(name);
    }
}

package org.example.enoconomy.managers;

import org.example.enoconomy.Enoconomy;
import org.example.enoconomy.models.Transaction;

import java.util.List;
import java.util.UUID;

public class TransactionManager {

    private final Enoconomy plugin;

    public TransactionManager(Enoconomy plugin) {
        this.plugin = plugin;
    }

    public void logTransaction(UUID sender, UUID receiver, double amount, String type, String description) {
        plugin.getDatabaseManager().logTransaction(sender, receiver, amount, type, description);
    }

    public List<Transaction> getPlayerTransactions(UUID playerUuid, int limit) {
        return plugin.getDatabaseManager().getPlayerTransactions(playerUuid, limit);
    }

    public List<Transaction> getAllTransactions(int limit) {
        return plugin.getDatabaseManager().getAllTransactions(limit);
    }

    public void logAdminGive(UUID admin, UUID target, double amount) {
        logTransaction(admin, target, amount, "ADMIN_GIVE", "Admin tarafından para verildi");
    }

    public void logAdminTake(UUID admin, UUID target, double amount) {
        logTransaction(target, admin, amount, "ADMIN_TAKE", "Admin tarafından para alındı");
    }

    public void logAdminSet(UUID admin, UUID target, double amount) {
        logTransaction(admin, target, amount, "ADMIN_SET", "Admin tarafından bakiye ayarlandı");
    }

    public void logPurchase(UUID player, double amount, String item) {
        logTransaction(player, null, amount, "PURCHASE", "Satın alma: " + item);
    }

    public void logSale(UUID player, double amount, String item) {
        logTransaction(null, player, amount, "SALE", "Satış: " + item);
    }
}


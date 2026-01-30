package org.example.enoconomy.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.example.enoconomy.Enoconomy;
import org.example.enoconomy.models.PlayerAccount;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EnoconomyPlaceholders extends PlaceholderExpansion {

    private final Enoconomy plugin;

    public EnoconomyPlaceholders(Enoconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "enoconomy";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Enoconomy";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        PlayerAccount account = plugin.getEconomyManager().getAccount(player.getUniqueId());

        // Kendi istatistikleri
        switch (params.toLowerCase()) {
            case "balance":
                return account != null ? plugin.getEconomyManager().formatMoney(account.getBalance()) : "0";
            case "balance_raw":
                return account != null ? String.valueOf(account.getBalance()) : "0";
            case "balance_formatted":
                return account != null ? formatLargeNumber(account.getBalance()) : "0";
            case "currency":
                return plugin.getEconomyManager().getCurrencyName(false);
            case "currency_plural":
                return plugin.getEconomyManager().getCurrencyName(true);
        }

        // Top bakiyeler
        if (params.startsWith("top_")) {
            String[] parts = params.split("_");
            if (parts.length >= 3) {
                try {
                    int position = Integer.parseInt(parts[1]);
                    String type = parts[2].toLowerCase();

                    List<PlayerAccount> topAccounts = plugin.getDatabaseManager().getTopBalances(position);
                    if (topAccounts.size() >= position) {
                        PlayerAccount topAccount = topAccounts.get(position - 1);
                        return switch (type) {
                            case "name" -> topAccount.getUsername();
                            case "balance" -> plugin.getEconomyManager().formatMoney(topAccount.getBalance());
                            case "balance_raw" -> String.valueOf(topAccount.getBalance());
                            default -> "";
                        };
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        // Sunucu istatistikleri
        switch (params.toLowerCase()) {
            case "server_total":
                return plugin.getEconomyManager().formatMoney(plugin.getDatabaseManager().getTotalServerMoney());
            case "server_players":
                return String.valueOf(plugin.getDatabaseManager().getTotalPlayerCount());
            case "server_transactions":
                return String.valueOf(plugin.getDatabaseManager().getTotalTransactionCount());
        }

        return null;
    }

    private String formatLargeNumber(double number) {
        if (number >= 1_000_000_000) {
            return String.format("%.1fB", number / 1_000_000_000);
        } else if (number >= 1_000_000) {
            return String.format("%.1fM", number / 1_000_000);
        } else if (number >= 1_000) {
            return String.format("%.1fK", number / 1_000);
        }
        return String.valueOf((int) number);
    }
}

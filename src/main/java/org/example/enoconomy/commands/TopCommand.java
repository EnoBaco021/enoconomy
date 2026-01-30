package org.example.enoconomy.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.example.enoconomy.Enoconomy;
import org.example.enoconomy.models.PlayerAccount;
import org.example.enoconomy.utils.MessageUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TopCommand implements CommandExecutor, TabCompleter {

    private final Enoconomy plugin;

    public TopCommand(Enoconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        int page = 1;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
                if (page < 1) page = 1;
            } catch (NumberFormatException e) {
                page = 1;
            }
        }

        int perPage = 10;
        List<PlayerAccount> allAccounts = plugin.getDatabaseManager().getTopBalances(100);

        int totalPages = (int) Math.ceil(allAccounts.size() / (double) perPage);
        if (page > totalPages && totalPages > 0) page = totalPages;

        int startIndex = (page - 1) * perPage;
        int endIndex = Math.min(startIndex + perPage, allAccounts.size());

        sender.sendMessage(MessageUtils.color("&8&m----------------------------------------"));
        sender.sendMessage(MessageUtils.color("&6&l  EN ZENGİN OYUNCULAR &7(Sayfa " + page + "/" + Math.max(1, totalPages) + ")"));
        sender.sendMessage(MessageUtils.color("&8&m----------------------------------------"));

        if (allAccounts.isEmpty()) {
            sender.sendMessage(MessageUtils.color("&7Henüz kayıtlı oyuncu yok."));
        } else {
            for (int i = startIndex; i < endIndex; i++) {
                PlayerAccount account = allAccounts.get(i);
                int rank = i + 1;
                String rankColor = getRankColor(rank);
                sender.sendMessage(MessageUtils.color(rankColor + " #" + rank + " &f" + account.getUsername() +
                        " &7- &a" + plugin.getEconomyManager().formatMoney(account.getBalance())));
            }
        }

        sender.sendMessage(MessageUtils.color("&8&m----------------------------------------"));

        if (totalPages > 1 && page < totalPages) {
            sender.sendMessage(MessageUtils.color("&7Sonraki sayfa için: &e/top " + (page + 1)));
        }

        return true;
    }

    private String getRankColor(int rank) {
        return switch (rank) {
            case 1 -> "&6&l";
            case 2 -> "&f&l";
            case 3 -> "&c&l";
            default -> "&7";
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.addAll(Arrays.asList("1", "2", "3"));
        }
        return completions;
    }
}


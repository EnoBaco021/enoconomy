package org.example.enoconomy.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.example.enoconomy.Enoconomy;
import org.example.enoconomy.models.PlayerAccount;
import org.example.enoconomy.utils.MessageUtils;

import java.util.ArrayList;
import java.util.List;

public class PayCommand implements CommandExecutor, TabCompleter {

    private final Enoconomy plugin;

    public PayCommand(Enoconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtils.color("&cBu komutu sadece oyuncular kullanabilir!"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(MessageUtils.color("&cKullanım: /pay <oyuncu> <miktar>"));
            return true;
        }

        String targetName = args[0];
        PlayerAccount targetAccount = plugin.getEconomyManager().getAccountByName(targetName);

        if (targetAccount == null) {
            sender.sendMessage(MessageUtils.color("&cOyuncu bulunamadı: &e" + targetName));
            return true;
        }

        if (targetAccount.getUuid().equals(player.getUniqueId())) {
            sender.sendMessage(MessageUtils.color("&cKendinize para gönderemezsiniz!"));
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageUtils.color("&cGeçersiz miktar: &e" + args[1]));
            return true;
        }

        if (amount <= 0) {
            sender.sendMessage(MessageUtils.color("&cMiktar 0'dan büyük olmalıdır!"));
            return true;
        }

        double minTransfer = plugin.getConfig().getDouble("economy.min-transfer", 1.0);
        if (amount < minTransfer) {
            sender.sendMessage(MessageUtils.color("&cMinimum transfer miktarı: &e" + plugin.getEconomyManager().formatMoney(minTransfer)));
            return true;
        }

        if (!plugin.getEconomyManager().hasEnough(player.getUniqueId(), amount)) {
            sender.sendMessage(MessageUtils.color("&cYeterli bakiyeniz yok!"));
            return true;
        }

        double tax = plugin.getConfig().getDouble("economy.transfer-tax", 0);
        double taxAmount = amount * (tax / 100);
        double finalAmount = amount - taxAmount;

        if (plugin.getEconomyManager().transfer(player.getUniqueId(), targetAccount.getUuid(), amount)) {
            sender.sendMessage(MessageUtils.color("&a" + targetAccount.getUsername() + " &7adlı oyuncuya &a" +
                    plugin.getEconomyManager().formatMoney(amount) + " &7gönderdiniz!"));

            if (taxAmount > 0) {
                sender.sendMessage(MessageUtils.color("&7Vergi: &c-" + plugin.getEconomyManager().formatMoney(taxAmount)));
            }

            // Alıcıya bildirim gönder
            Player target = Bukkit.getPlayer(targetAccount.getUuid());
            if (target != null && target.isOnline()) {
                target.sendMessage(MessageUtils.color("&a" + player.getName() + " &7adlı oyuncudan &a" +
                        plugin.getEconomyManager().formatMoney(finalAmount) + " &7aldınız!"));
            }
        } else {
            sender.sendMessage(MessageUtils.color("&cTransfer işlemi başarısız oldu!"));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            plugin.getServer().getOnlinePlayers().forEach(player -> {
                if (player.getName().toLowerCase().startsWith(input) && !player.getName().equals(sender.getName())) {
                    completions.add(player.getName());
                }
            });
        } else if (args.length == 2) {
            completions.add("100");
            completions.add("500");
            completions.add("1000");
        }
        return completions;
    }
}


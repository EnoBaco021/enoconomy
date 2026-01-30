package org.example.enoconomy.commands;

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

public class BalanceCommand implements CommandExecutor, TabCompleter {

    private final Enoconomy plugin;

    public BalanceCommand(Enoconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(MessageUtils.color("&cBu komutu sadece oyuncular kullanabilir!"));
                return true;
            }

            PlayerAccount account = plugin.getEconomyManager().getAccount(player.getUniqueId());
            if (account == null) {
                sender.sendMessage(MessageUtils.color("&cHesabınız bulunamadı!"));
                return true;
            }

            sendBalanceInfo(player, account);
        } else {
            if (!sender.hasPermission("enoconomy.balance.others")) {
                sender.sendMessage(MessageUtils.color("&cBu işlem için yetkiniz yok!"));
                return true;
            }

            String targetName = args[0];
            PlayerAccount account = plugin.getEconomyManager().getAccountByName(targetName);

            if (account == null) {
                sender.sendMessage(MessageUtils.color("&cOyuncu bulunamadı: &e" + targetName));
                return true;
            }

            sender.sendMessage(MessageUtils.color("&6&l" + account.getUsername() + " &7adlı oyuncunun bakiyesi:"));
            sender.sendMessage(MessageUtils.color("&7Bakiye: &a" + plugin.getEconomyManager().formatMoney(account.getBalance())));
        }

        return true;
    }

    private void sendBalanceInfo(Player player, PlayerAccount account) {
        player.sendMessage(MessageUtils.color("&8&m----------------------------------------"));
        player.sendMessage(MessageUtils.color("&6&l  BAKIYE BİLGİSİ"));
        player.sendMessage(MessageUtils.color("&8&m----------------------------------------"));
        player.sendMessage(MessageUtils.color("&7  Bakiye: &a" + plugin.getEconomyManager().formatMoney(account.getBalance())));
        player.sendMessage(MessageUtils.color("&8&m----------------------------------------"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1 && sender.hasPermission("enoconomy.balance.others")) {
            String input = args[0].toLowerCase();
            plugin.getServer().getOnlinePlayers().forEach(player -> {
                if (player.getName().toLowerCase().startsWith(input)) {
                    completions.add(player.getName());
                }
            });
        }
        return completions;
    }
}

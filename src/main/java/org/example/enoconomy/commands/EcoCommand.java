package org.example.enoconomy.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.example.enoconomy.Enoconomy;
import org.example.enoconomy.models.PlayerAccount;
import org.example.enoconomy.utils.MessageUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class EcoCommand implements CommandExecutor, TabCompleter {

    private final Enoconomy plugin;

    public EcoCommand(Enoconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("enoconomy.admin")) {
            sender.sendMessage(MessageUtils.color("&cBu işlem için yetkiniz yok!"));
            return true;
        }

        if (args.length < 1) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "give" -> handleGive(sender, args);
            case "take" -> handleTake(sender, args);
            case "set" -> handleSet(sender, args);
            case "reset" -> handleReset(sender, args);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MessageUtils.color("&cKullanım: /eco give <oyuncu> <miktar>"));
            return;
        }

        String targetName = args[1];
        PlayerAccount account = plugin.getEconomyManager().getAccountByName(targetName);

        if (account == null) {
            sender.sendMessage(MessageUtils.color("&cOyuncu bulunamadı: &e" + targetName));
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageUtils.color("&cGeçersiz miktar: &e" + args[2]));
            return;
        }

        if (amount <= 0) {
            sender.sendMessage(MessageUtils.color("&cMiktar 0'dan büyük olmalıdır!"));
            return;
        }

        if (plugin.getEconomyManager().deposit(account.getUuid(), amount)) {
            sender.sendMessage(MessageUtils.color("&a" + account.getUsername() + " &7adlı oyuncuya &a" +
                    plugin.getEconomyManager().formatMoney(amount) + " &7verildi!"));

            // İşlemi kaydet
            UUID adminUuid = sender instanceof Player p ? p.getUniqueId() : null;
            plugin.getTransactionManager().logAdminGive(adminUuid, account.getUuid(), amount);

            // Oyuncuya bildirim
            Player target = Bukkit.getPlayer(account.getUuid());
            if (target != null && target.isOnline()) {
                target.sendMessage(MessageUtils.color("&7Hesabınıza &a" + plugin.getEconomyManager().formatMoney(amount) + " &7eklendi!"));
            }
        } else {
            sender.sendMessage(MessageUtils.color("&cİşlem başarısız oldu!"));
        }
    }

    private void handleTake(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MessageUtils.color("&cKullanım: /eco take <oyuncu> <miktar>"));
            return;
        }

        String targetName = args[1];
        PlayerAccount account = plugin.getEconomyManager().getAccountByName(targetName);

        if (account == null) {
            sender.sendMessage(MessageUtils.color("&cOyuncu bulunamadı: &e" + targetName));
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageUtils.color("&cGeçersiz miktar: &e" + args[2]));
            return;
        }

        if (amount <= 0) {
            sender.sendMessage(MessageUtils.color("&cMiktar 0'dan büyük olmalıdır!"));
            return;
        }

        if (plugin.getEconomyManager().withdraw(account.getUuid(), amount)) {
            sender.sendMessage(MessageUtils.color("&a" + account.getUsername() + " &7adlı oyuncudan &c" +
                    plugin.getEconomyManager().formatMoney(amount) + " &7alındı!"));

            // İşlemi kaydet
            UUID adminUuid = sender instanceof Player p ? p.getUniqueId() : null;
            plugin.getTransactionManager().logAdminTake(adminUuid, account.getUuid(), amount);

            // Oyuncuya bildirim
            Player target = Bukkit.getPlayer(account.getUuid());
            if (target != null && target.isOnline()) {
                target.sendMessage(MessageUtils.color("&7Hesabınızdan &c" + plugin.getEconomyManager().formatMoney(amount) + " &7çekildi!"));
            }
        } else {
            sender.sendMessage(MessageUtils.color("&cİşlem başarısız oldu! (Yetersiz bakiye olabilir)"));
        }
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MessageUtils.color("&cKullanım: /eco set <oyuncu> <miktar>"));
            return;
        }

        String targetName = args[1];
        PlayerAccount account = plugin.getEconomyManager().getAccountByName(targetName);

        if (account == null) {
            sender.sendMessage(MessageUtils.color("&cOyuncu bulunamadı: &e" + targetName));
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageUtils.color("&cGeçersiz miktar: &e" + args[2]));
            return;
        }

        if (amount < 0) {
            sender.sendMessage(MessageUtils.color("&cMiktar negatif olamaz!"));
            return;
        }

        if (plugin.getEconomyManager().setBalance(account.getUuid(), amount)) {
            sender.sendMessage(MessageUtils.color("&a" + account.getUsername() + " &7adlı oyuncunun bakiyesi &e" +
                    plugin.getEconomyManager().formatMoney(amount) + " &7olarak ayarlandı!"));

            // İşlemi kaydet
            UUID adminUuid = sender instanceof Player p ? p.getUniqueId() : null;
            plugin.getTransactionManager().logAdminSet(adminUuid, account.getUuid(), amount);

            // Oyuncuya bildirim
            Player target = Bukkit.getPlayer(account.getUuid());
            if (target != null && target.isOnline()) {
                target.sendMessage(MessageUtils.color("&7Bakiyeniz &e" + plugin.getEconomyManager().formatMoney(amount) + " &7olarak ayarlandı!"));
            }
        } else {
            sender.sendMessage(MessageUtils.color("&cİşlem başarısız oldu!"));
        }
    }

    private void handleReset(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessageUtils.color("&cKullanım: /eco reset <oyuncu>"));
            return;
        }

        String targetName = args[1];
        PlayerAccount account = plugin.getEconomyManager().getAccountByName(targetName);

        if (account == null) {
            sender.sendMessage(MessageUtils.color("&cOyuncu bulunamadı: &e" + targetName));
            return;
        }

        double startingBalance = plugin.getConfig().getDouble("economy.starting-balance", 100.0);

        if (plugin.getEconomyManager().setBalance(account.getUuid(), startingBalance)) {
            sender.sendMessage(MessageUtils.color("&a" + account.getUsername() + " &7adlı oyuncunun bakiyesi sıfırlandı!"));
            sender.sendMessage(MessageUtils.color("&7Yeni bakiye: &e" + plugin.getEconomyManager().formatMoney(startingBalance)));

            // Oyuncuya bildirim
            Player target = Bukkit.getPlayer(account.getUuid());
            if (target != null && target.isOnline()) {
                target.sendMessage(MessageUtils.color("&7Bakiyeniz sıfırlandı! Yeni bakiye: &e" + plugin.getEconomyManager().formatMoney(startingBalance)));
            }
        } else {
            sender.sendMessage(MessageUtils.color("&cİşlem başarısız oldu!"));
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(MessageUtils.color("&8&m----------------------------------------"));
        sender.sendMessage(MessageUtils.color("&6&l  ENOCONOMY - Admin Komutları"));
        sender.sendMessage(MessageUtils.color("&8&m----------------------------------------"));
        sender.sendMessage(MessageUtils.color("&e/eco give <oyuncu> <miktar> &7- Para ver"));
        sender.sendMessage(MessageUtils.color("&e/eco take <oyuncu> <miktar> &7- Para al"));
        sender.sendMessage(MessageUtils.color("&e/eco set <oyuncu> <miktar> &7- Bakiye ayarla"));
        sender.sendMessage(MessageUtils.color("&e/eco reset <oyuncu> &7- Bakiyeyi sıfırla"));
        sender.sendMessage(MessageUtils.color("&8&m----------------------------------------"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!sender.hasPermission("enoconomy.admin")) {
            return completions;
        }

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("give", "take", "set", "reset");
            String input = args[0].toLowerCase();
            for (String sub : subCommands) {
                if (sub.startsWith(input)) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2) {
            String input = args[1].toLowerCase();
            plugin.getServer().getOnlinePlayers().forEach(player -> {
                if (player.getName().toLowerCase().startsWith(input)) {
                    completions.add(player.getName());
                }
            });
        } else if (args.length == 3 && !args[0].equalsIgnoreCase("reset")) {
            completions.addAll(Arrays.asList("100", "500", "1000", "5000", "10000"));
        }

        return completions;
    }
}


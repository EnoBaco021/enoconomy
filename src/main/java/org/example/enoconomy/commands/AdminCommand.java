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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class AdminCommand implements CommandExecutor, TabCompleter {

    private final Enoconomy plugin;

    public AdminCommand(Enoconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("enoconomy.admin")) {
            sender.sendMessage(MessageUtils.color("&cBu işlem için yetkiniz yok!"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (subCommand) {
            case "give", "ver" -> handleGive(sender, subArgs);
            case "take", "al" -> handleTake(sender, subArgs);
            case "set", "ayarla" -> handleSet(sender, subArgs);
            case "reset", "sifirla" -> handleReset(sender, subArgs);
            case "reload", "yenile" -> handleReload(sender);
            case "stats", "istatistik" -> handleStats(sender);
            case "webadmin" -> handleWebAdmin(sender, subArgs);
            case "webpanel" -> handleWebPanelInfo(sender);
            case "help", "yardim", "?" -> sendHelp(sender);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessageUtils.color("&cKullanım: /enoconomy give <oyuncu> <miktar>"));
            return;
        }

        PlayerAccount account = plugin.getEconomyManager().getAccountByName(args[0]);
        if (account == null) {
            sender.sendMessage(MessageUtils.color("&cOyuncu bulunamadı: &e" + args[0]));
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageUtils.color("&cGeçersiz miktar: &e" + args[1]));
            return;
        }

        if (amount <= 0) {
            sender.sendMessage(MessageUtils.color("&cMiktar 0'dan büyük olmalıdır!"));
            return;
        }

        if (plugin.getEconomyManager().deposit(account.getUuid(), amount)) {
            sender.sendMessage(MessageUtils.color("&a" + account.getUsername() + " &7adlı oyuncuya &a" +
                    plugin.getEconomyManager().formatMoney(amount) + " &7verildi!"));

            UUID adminUuid = sender instanceof Player p ? p.getUniqueId() : null;
            plugin.getTransactionManager().logAdminGive(adminUuid, account.getUuid(), amount);

            Player target = Bukkit.getPlayer(account.getUuid());
            if (target != null && target.isOnline()) {
                target.sendMessage(MessageUtils.color("&7Hesabınıza &a" + plugin.getEconomyManager().formatMoney(amount) + " &7eklendi!"));
            }
        } else {
            sender.sendMessage(MessageUtils.color("&cİşlem başarısız oldu!"));
        }
    }

    private void handleTake(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessageUtils.color("&cKullanım: /enoconomy take <oyuncu> <miktar>"));
            return;
        }

        PlayerAccount account = plugin.getEconomyManager().getAccountByName(args[0]);
        if (account == null) {
            sender.sendMessage(MessageUtils.color("&cOyuncu bulunamadı: &e" + args[0]));
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageUtils.color("&cGeçersiz miktar: &e" + args[1]));
            return;
        }

        if (amount <= 0) {
            sender.sendMessage(MessageUtils.color("&cMiktar 0'dan büyük olmalıdır!"));
            return;
        }

        if (plugin.getEconomyManager().withdraw(account.getUuid(), amount)) {
            sender.sendMessage(MessageUtils.color("&a" + account.getUsername() + " &7adlı oyuncudan &c" +
                    plugin.getEconomyManager().formatMoney(amount) + " &7alındı!"));

            UUID adminUuid = sender instanceof Player p ? p.getUniqueId() : null;
            plugin.getTransactionManager().logAdminTake(adminUuid, account.getUuid(), amount);

            Player target = Bukkit.getPlayer(account.getUuid());
            if (target != null && target.isOnline()) {
                target.sendMessage(MessageUtils.color("&7Hesabınızdan &c" + plugin.getEconomyManager().formatMoney(amount) + " &7çekildi!"));
            }
        } else {
            sender.sendMessage(MessageUtils.color("&cİşlem başarısız oldu! (Yetersiz bakiye olabilir)"));
        }
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessageUtils.color("&cKullanım: /enoconomy set <oyuncu> <miktar>"));
            return;
        }

        PlayerAccount account = plugin.getEconomyManager().getAccountByName(args[0]);
        if (account == null) {
            sender.sendMessage(MessageUtils.color("&cOyuncu bulunamadı: &e" + args[0]));
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageUtils.color("&cGeçersiz miktar: &e" + args[1]));
            return;
        }

        if (amount < 0) {
            sender.sendMessage(MessageUtils.color("&cMiktar negatif olamaz!"));
            return;
        }

        if (plugin.getEconomyManager().setBalance(account.getUuid(), amount)) {
            sender.sendMessage(MessageUtils.color("&a" + account.getUsername() + " &7adlı oyuncunun bakiyesi &e" +
                    plugin.getEconomyManager().formatMoney(amount) + " &7olarak ayarlandı!"));

            UUID adminUuid = sender instanceof Player p ? p.getUniqueId() : null;
            plugin.getTransactionManager().logAdminSet(adminUuid, account.getUuid(), amount);

            Player target = Bukkit.getPlayer(account.getUuid());
            if (target != null && target.isOnline()) {
                target.sendMessage(MessageUtils.color("&7Bakiyeniz &e" + plugin.getEconomyManager().formatMoney(amount) + " &7olarak ayarlandı!"));
            }
        } else {
            sender.sendMessage(MessageUtils.color("&cİşlem başarısız oldu!"));
        }
    }

    private void handleReset(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(MessageUtils.color("&cKullanım: /enoconomy reset <oyuncu>"));
            return;
        }

        PlayerAccount account = plugin.getEconomyManager().getAccountByName(args[0]);
        if (account == null) {
            sender.sendMessage(MessageUtils.color("&cOyuncu bulunamadı: &e" + args[0]));
            return;
        }

        double startingBalance = plugin.getConfig().getDouble("economy.starting-balance", 100.0);

        if (plugin.getEconomyManager().setBalance(account.getUuid(), startingBalance)) {
            sender.sendMessage(MessageUtils.color("&a" + account.getUsername() + " &7adlı oyuncunun bakiyesi sıfırlandı!"));
            sender.sendMessage(MessageUtils.color("&7Yeni bakiye: &e" + plugin.getEconomyManager().formatMoney(startingBalance)));

            Player target = Bukkit.getPlayer(account.getUuid());
            if (target != null && target.isOnline()) {
                target.sendMessage(MessageUtils.color("&7Bakiyeniz sıfırlandı! Yeni bakiye: &e" + plugin.getEconomyManager().formatMoney(startingBalance)));
            }
        } else {
            sender.sendMessage(MessageUtils.color("&cİşlem başarısız oldu!"));
        }
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        sender.sendMessage(MessageUtils.color("&aKonfigurasyon yeniden yüklendi!"));
    }

    private void handleStats(CommandSender sender) {
        double totalMoney = plugin.getDatabaseManager().getTotalServerMoney();
        int totalPlayers = plugin.getDatabaseManager().getTotalPlayerCount();
        int totalTransactions = plugin.getDatabaseManager().getTotalTransactionCount();

        sender.sendMessage(MessageUtils.color("&8&m----------------------------------------"));
        sender.sendMessage(MessageUtils.color("&6&l  SUNUCU EKONOMİ İSTATİSTİKLERİ"));
        sender.sendMessage(MessageUtils.color("&8&m----------------------------------------"));
        sender.sendMessage(MessageUtils.color("&7  Toplam Oyuncu: &e" + totalPlayers));
        sender.sendMessage(MessageUtils.color("&7  Toplam Para: &a" + plugin.getEconomyManager().formatMoney(totalMoney)));
        sender.sendMessage(MessageUtils.color("&7  Toplam İşlem: &b" + totalTransactions));
        sender.sendMessage(MessageUtils.color("&8&m----------------------------------------"));
    }

    private void handleWebAdmin(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MessageUtils.color("&cKullanım: /enoconomy webadmin create <kullanıcıadı> <şifre>"));
            return;
        }

        if (args[0].equalsIgnoreCase("create")) {
            String username = args[1];
            String password = args[2];
            String passwordHash = hashPassword(password);
            String apiKey = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

            plugin.getDatabaseManager().createWebAdmin(username, passwordHash, apiKey);

            sender.sendMessage(MessageUtils.color("&aWeb panel yöneticisi oluşturuldu!"));
            sender.sendMessage(MessageUtils.color("&7Kullanıcı: &e" + username));
            sender.sendMessage(MessageUtils.color("&7API Key: &e" + apiKey));
            sender.sendMessage(MessageUtils.color("&cBu bilgileri güvenli bir yerde saklayın!"));
        }
    }

    private void handleWebPanelInfo(CommandSender sender) {
        if (plugin.getWebServer() == null) {
            sender.sendMessage(MessageUtils.color("&cWeb panel aktif değil!"));
            return;
        }

        int port = plugin.getConfig().getInt("web-panel.port", 3000);
        sender.sendMessage(MessageUtils.color("&8&m----------------------------------------"));
        sender.sendMessage(MessageUtils.color("&6&l  WEB PANEL BİLGİSİ"));
        sender.sendMessage(MessageUtils.color("&8&m----------------------------------------"));
        sender.sendMessage(MessageUtils.color("&7  Durum: &aAktif"));
        sender.sendMessage(MessageUtils.color("&7  Adres: &ehttp://localhost:" + port));
        sender.sendMessage(MessageUtils.color("&7  API: &ehttp://localhost:" + port + "/api"));
        sender.sendMessage(MessageUtils.color("&8&m----------------------------------------"));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(MessageUtils.color("&8&m----------------------------------------"));
        sender.sendMessage(MessageUtils.color("&6&l  ENOCONOMY - Admin Komutları"));
        sender.sendMessage(MessageUtils.color("&8&m----------------------------------------"));
        sender.sendMessage(MessageUtils.color("&e/enoconomy give <oyuncu> <miktar> &7- Para ver"));
        sender.sendMessage(MessageUtils.color("&e/enoconomy take <oyuncu> <miktar> &7- Para al"));
        sender.sendMessage(MessageUtils.color("&e/enoconomy set <oyuncu> <miktar> &7- Bakiye ayarla"));
        sender.sendMessage(MessageUtils.color("&e/enoconomy reset <oyuncu> &7- Bakiyeyi sıfırla"));
        sender.sendMessage(MessageUtils.color("&e/enoconomy stats &7- İstatistikleri gör"));
        sender.sendMessage(MessageUtils.color("&e/enoconomy reload &7- Ayarları yeniden yükle"));
        sender.sendMessage(MessageUtils.color("&e/enoconomy webadmin create <user> <pass> &7- Web admin oluştur"));
        sender.sendMessage(MessageUtils.color("&e/enoconomy webpanel &7- Web panel bilgisi"));
        sender.sendMessage(MessageUtils.color("&8&m----------------------------------------"));
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!sender.hasPermission("enoconomy.admin")) {
            return completions;
        }

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("give", "take", "set", "reset", "reload", "stats", "webadmin", "webpanel", "help");
            String input = args[0].toLowerCase();
            for (String sub : subCommands) {
                if (sub.startsWith(input)) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            String input = args[1].toLowerCase();

            switch (subCommand) {
                case "give", "take", "set", "reset" -> {
                    plugin.getServer().getOnlinePlayers().forEach(player -> {
                        if (player.getName().toLowerCase().startsWith(input)) {
                            completions.add(player.getName());
                        }
                    });
                }
                case "webadmin" -> {
                    if ("create".startsWith(input)) {
                        completions.add("create");
                    }
                }
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("give") || subCommand.equals("take") || subCommand.equals("set")) {
                completions.addAll(Arrays.asList("100", "500", "1000", "5000", "10000"));
            }
        }

        return completions;
    }
}


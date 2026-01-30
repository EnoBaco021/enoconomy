package org.example.enoconomy.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.example.enoconomy.Enoconomy;
import org.example.enoconomy.utils.MessageUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class EconomyAdminCommand implements CommandExecutor, TabCompleter {

    private final Enoconomy plugin;

    public EconomyAdminCommand(Enoconomy plugin) {
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
            case "reload" -> handleReload(sender);
            case "stats" -> handleStats(sender);
            case "webadmin" -> handleWebAdmin(sender, args);
            case "webpanel" -> handleWebPanelInfo(sender);
            default -> sendHelp(sender);
        }

        return true;
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
        if (args.length < 4) {
            sender.sendMessage(MessageUtils.color("&cKullanım: /economy webadmin <create> <kullanıcıadı> <şifre>"));
            return;
        }

        String action = args[1].toLowerCase();
        String username = args[2];
        String password = args[3];

        if (action.equals("create")) {
            String passwordHash = hashPassword(password);
            String apiKey = generateApiKey();

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

        int port = plugin.getConfig().getInt("web-panel.port", 8080);
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
        sender.sendMessage(MessageUtils.color("&6&l  ENOCONOMY - Yönetim"));
        sender.sendMessage(MessageUtils.color("&8&m----------------------------------------"));
        sender.sendMessage(MessageUtils.color("&e/economy reload &7- Konfigürasyonu yeniden yükle"));
        sender.sendMessage(MessageUtils.color("&e/economy stats &7- Ekonomi istatistiklerini gör"));
        sender.sendMessage(MessageUtils.color("&e/economy webadmin create <kullanıcı> <şifre> &7- Web admin oluştur"));
        sender.sendMessage(MessageUtils.color("&e/economy webpanel &7- Web panel bilgisi"));
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

    private String generateApiKey() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!sender.hasPermission("enoconomy.admin")) {
            return completions;
        }

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("reload", "stats", "webadmin", "webpanel");
            String input = args[0].toLowerCase();
            for (String sub : subCommands) {
                if (sub.startsWith(input)) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("webadmin")) {
            completions.add("create");
        }

        return completions;
    }
}


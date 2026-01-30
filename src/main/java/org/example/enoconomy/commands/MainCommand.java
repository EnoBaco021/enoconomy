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

public class MainCommand implements CommandExecutor, TabCompleter {

    private final Enoconomy plugin;

    public MainCommand(Enoconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (subCommand) {
            case "balance", "bal", "bakiye" -> handleBalance(sender, subArgs);
            case "pay", "gonder", "transfer" -> handlePay(sender, subArgs);
            case "top", "baltop", "siralama" -> handleTop(sender, subArgs);
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

    // ==================== BALANCE ====================
    private void handleBalance(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(MessageUtils.color("&cBu komutu sadece oyuncular kullanabilir!"));
                return;
            }

            PlayerAccount account = plugin.getEconomyManager().getAccount(player.getUniqueId());
            if (account == null) {
                sender.sendMessage(MessageUtils.color("&cHesabınız bulunamadı!"));
                return;
            }

            sendBalanceInfo(player, account);
        } else {
            if (!sender.hasPermission("enoconomy.balance.others")) {
                sender.sendMessage(MessageUtils.color("&cBu işlem için yetkiniz yok!"));
                return;
            }

            String targetName = args[0];
            PlayerAccount account = plugin.getEconomyManager().getAccountByName(targetName);

            if (account == null) {
                sender.sendMessage(MessageUtils.color("&cOyuncu bulunamadı: &e" + targetName));
                return;
            }

            sender.sendMessage(MessageUtils.color("&6&l" + account.getUsername() + " &7adlı oyuncunun bakiyesi:"));
            sender.sendMessage(MessageUtils.color("&7Bakiye: &a" + plugin.getEconomyManager().formatMoney(account.getBalance())));
        }
    }

    private void sendBalanceInfo(Player player, PlayerAccount account) {
        player.sendMessage(MessageUtils.color("&8&m----------------------------------------"));
        player.sendMessage(MessageUtils.color("&6&l  BAKIYE BİLGİSİ"));
        player.sendMessage(MessageUtils.color("&8&m----------------------------------------"));
        player.sendMessage(MessageUtils.color("&7  Bakiye: &a" + plugin.getEconomyManager().formatMoney(account.getBalance())));
        player.sendMessage(MessageUtils.color("&8&m----------------------------------------"));
    }

    // ==================== PAY ====================
    private void handlePay(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtils.color("&cBu komutu sadece oyuncular kullanabilir!"));
            return;
        }

        if (!sender.hasPermission("enoconomy.pay")) {
            sender.sendMessage(MessageUtils.color("&cBu işlem için yetkiniz yok!"));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(MessageUtils.color("&cKullanım: /enoconomy pay <oyuncu> <miktar>"));
            return;
        }

        String targetName = args[0];
        PlayerAccount targetAccount = plugin.getEconomyManager().getAccountByName(targetName);

        if (targetAccount == null) {
            sender.sendMessage(MessageUtils.color("&cOyuncu bulunamadı: &e" + targetName));
            return;
        }

        if (targetAccount.getUuid().equals(player.getUniqueId())) {
            sender.sendMessage(MessageUtils.color("&cKendinize para gönderemezsiniz!"));
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

        double minTransfer = plugin.getConfig().getDouble("economy.min-transfer", 1.0);
        if (amount < minTransfer) {
            sender.sendMessage(MessageUtils.color("&cMinimum transfer miktarı: &e" + plugin.getEconomyManager().formatMoney(minTransfer)));
            return;
        }

        if (!plugin.getEconomyManager().hasEnough(player.getUniqueId(), amount)) {
            sender.sendMessage(MessageUtils.color("&cYeterli bakiyeniz yok!"));
            return;
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

            Player target = Bukkit.getPlayer(targetAccount.getUuid());
            if (target != null && target.isOnline()) {
                target.sendMessage(MessageUtils.color("&a" + player.getName() + " &7adlı oyuncudan &a" +
                        plugin.getEconomyManager().formatMoney(finalAmount) + " &7aldınız!"));
            }
        } else {
            sender.sendMessage(MessageUtils.color("&cTransfer işlemi başarısız oldu!"));
        }
    }

    // ==================== TOP ====================
    private void handleTop(CommandSender sender, String[] args) {
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
            sender.sendMessage(MessageUtils.color("&7Sonraki sayfa için: &e/enoconomy top " + (page + 1)));
        }
    }

    private String getRankColor(int rank) {
        return switch (rank) {
            case 1 -> "&6&l";
            case 2 -> "&f&l";
            case 3 -> "&c&l";
            default -> "&7";
        };
    }

    // ==================== ADMIN: GIVE ====================
    private void handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("enoconomy.admin")) {
            sender.sendMessage(MessageUtils.color("&cBu işlem için yetkiniz yok!"));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(MessageUtils.color("&cKullanım: /enoconomy give <oyuncu> <miktar>"));
            return;
        }

        String targetName = args[0];
        PlayerAccount account = plugin.getEconomyManager().getAccountByName(targetName);

        if (account == null) {
            sender.sendMessage(MessageUtils.color("&cOyuncu bulunamadı: &e" + targetName));
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

    // ==================== ADMIN: TAKE ====================
    private void handleTake(CommandSender sender, String[] args) {
        if (!sender.hasPermission("enoconomy.admin")) {
            sender.sendMessage(MessageUtils.color("&cBu işlem için yetkiniz yok!"));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(MessageUtils.color("&cKullanım: /enoconomy take <oyuncu> <miktar>"));
            return;
        }

        String targetName = args[0];
        PlayerAccount account = plugin.getEconomyManager().getAccountByName(targetName);

        if (account == null) {
            sender.sendMessage(MessageUtils.color("&cOyuncu bulunamadı: &e" + targetName));
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

    // ==================== ADMIN: SET ====================
    private void handleSet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("enoconomy.admin")) {
            sender.sendMessage(MessageUtils.color("&cBu işlem için yetkiniz yok!"));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(MessageUtils.color("&cKullanım: /enoconomy set <oyuncu> <miktar>"));
            return;
        }

        String targetName = args[0];
        PlayerAccount account = plugin.getEconomyManager().getAccountByName(targetName);

        if (account == null) {
            sender.sendMessage(MessageUtils.color("&cOyuncu bulunamadı: &e" + targetName));
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

    // ==================== ADMIN: RESET ====================
    private void handleReset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("enoconomy.admin")) {
            sender.sendMessage(MessageUtils.color("&cBu işlem için yetkiniz yok!"));
            return;
        }

        if (args.length < 1) {
            sender.sendMessage(MessageUtils.color("&cKullanım: /enoconomy reset <oyuncu>"));
            return;
        }

        String targetName = args[0];
        PlayerAccount account = plugin.getEconomyManager().getAccountByName(targetName);

        if (account == null) {
            sender.sendMessage(MessageUtils.color("&cOyuncu bulunamadı: &e" + targetName));
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

    // ==================== ADMIN: RELOAD ====================
    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("enoconomy.admin")) {
            sender.sendMessage(MessageUtils.color("&cBu işlem için yetkiniz yok!"));
            return;
        }

        plugin.reloadConfig();
        sender.sendMessage(MessageUtils.color("&aKonfigurasyon yeniden yüklendi!"));
    }

    // ==================== ADMIN: STATS ====================
    private void handleStats(CommandSender sender) {
        if (!sender.hasPermission("enoconomy.admin")) {
            sender.sendMessage(MessageUtils.color("&cBu işlem için yetkiniz yok!"));
            return;
        }

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

    // ==================== ADMIN: WEBADMIN ====================
    private void handleWebAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("enoconomy.admin")) {
            sender.sendMessage(MessageUtils.color("&cBu işlem için yetkiniz yok!"));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(MessageUtils.color("&cKullanım: /enoconomy webadmin create <kullanıcıadı> <şifre>"));
            return;
        }

        String action = args[0].toLowerCase();
        String username = args[1];
        String password = args[2];

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

    // ==================== ADMIN: WEBPANEL ====================
    private void handleWebPanelInfo(CommandSender sender) {
        if (!sender.hasPermission("enoconomy.admin")) {
            sender.sendMessage(MessageUtils.color("&cBu işlem için yetkiniz yok!"));
            return;
        }

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

    // ==================== HELP ====================
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(MessageUtils.color("&8&m----------------------------------------"));
        sender.sendMessage(MessageUtils.color("&6&l  ENOCONOMY - Komutlar"));
        sender.sendMessage(MessageUtils.color("&8&m----------------------------------------"));
        sender.sendMessage(MessageUtils.color("&e/enoconomy balance [oyuncu] &7- Bakiye görüntüle"));
        sender.sendMessage(MessageUtils.color("&e/enoconomy pay <oyuncu> <miktar> &7- Para gönder"));
        sender.sendMessage(MessageUtils.color("&e/enoconomy top [sayfa] &7- En zenginler listesi"));

        if (sender.hasPermission("enoconomy.admin")) {
            sender.sendMessage(MessageUtils.color("&8&m----------------------------------------"));
            sender.sendMessage(MessageUtils.color("&c&l  Admin Komutları:"));
            sender.sendMessage(MessageUtils.color("&e/enoconomy give <oyuncu> <miktar> &7- Para ver"));
            sender.sendMessage(MessageUtils.color("&e/enoconomy take <oyuncu> <miktar> &7- Para al"));
            sender.sendMessage(MessageUtils.color("&e/enoconomy set <oyuncu> <miktar> &7- Bakiye ayarla"));
            sender.sendMessage(MessageUtils.color("&e/enoconomy reset <oyuncu> &7- Bakiyeyi sıfırla"));
            sender.sendMessage(MessageUtils.color("&e/enoconomy stats &7- İstatistikleri gör"));
            sender.sendMessage(MessageUtils.color("&e/enoconomy reload &7- Ayarları yeniden yükle"));
            sender.sendMessage(MessageUtils.color("&e/enoconomy webadmin create <user> <pass> &7- Web admin oluştur"));
            sender.sendMessage(MessageUtils.color("&e/enoconomy webpanel &7- Web panel bilgisi"));
        }

        sender.sendMessage(MessageUtils.color("&8&m----------------------------------------"));
    }

    // ==================== UTILS ====================
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

    // ==================== TAB COMPLETE ====================
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>(Arrays.asList("balance", "pay", "top", "help"));
            if (sender.hasPermission("enoconomy.admin")) {
                subCommands.addAll(Arrays.asList("give", "take", "set", "reset", "reload", "stats", "webadmin", "webpanel"));
            }

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
                case "balance", "pay", "give", "take", "set", "reset" -> {
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
            if (subCommand.equals("pay") || subCommand.equals("give") || subCommand.equals("take") || subCommand.equals("set")) {
                completions.addAll(Arrays.asList("100", "500", "1000", "5000", "10000"));
            }
        }

        return completions;
    }
}


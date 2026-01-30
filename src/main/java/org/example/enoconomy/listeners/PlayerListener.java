package org.example.enoconomy.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.example.enoconomy.Enoconomy;

public class PlayerListener implements Listener {

    private final Enoconomy plugin;

    public PlayerListener(Enoconomy plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Hesap yoksa oluştur
        if (!plugin.getEconomyManager().hasAccount(player.getUniqueId())) {
            plugin.getEconomyManager().createAccount(player);

            if (plugin.getConfig().getBoolean("economy.welcome-message", true)) {
                double startingBalance = plugin.getConfig().getDouble("economy.starting-balance", 100.0);
                player.sendMessage("§a§lEnoconomy'ye hoş geldiniz!");
                player.sendMessage("§7Başlangıç bakiyeniz: §e" + plugin.getEconomyManager().formatMoney(startingBalance));
            }
        }

        // Son görülme zamanını güncelle
        plugin.getDatabaseManager().updateLastSeen(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Son görülme zamanını güncelle
        plugin.getDatabaseManager().updateLastSeen(player.getUniqueId());
    }
}


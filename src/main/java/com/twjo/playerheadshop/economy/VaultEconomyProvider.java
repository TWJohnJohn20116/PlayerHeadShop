package com.twjo.playerheadshop.economy;

import com.twjo.playerheadshop.PlayerHeadShop;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Vault 經濟服務的實際對接實作（僅在確定 Vault 存在時才會被 JVM 類別載入器加載）
 */
public class VaultEconomyProvider implements EconomyProvider {

    private final PlayerHeadShop plugin;
    private Economy economy = null;

    public VaultEconomyProvider(PlayerHeadShop plugin) {
        this.plugin = plugin;
        setupEconomy();
    }

    private boolean setupEconomy() {
        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }

        this.economy = rsp.getProvider();
        return this.economy != null;
    }

    @Override
    public boolean hasEconomy() {
        if (economy == null) {
            setupEconomy();
        }
        return economy != null;
    }

    @Override
    public double getBalance(Player player) {
        if (!hasEconomy() || player == null) {
            return 0.0;
        }
        return economy.getBalance(player);
    }

    @Override
    public boolean has(Player player, double amount) {
        if (!hasEconomy() || player == null) {
            return false;
        }
        return economy.has(player, amount);
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        if (!hasEconomy() || player == null) {
            return false;
        }
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return response.transactionSuccess();
    }

    @Override
    public boolean deposit(Player player, double amount) {
        if (!hasEconomy() || player == null) {
            return false;
        }
        EconomyResponse response = economy.depositPlayer(player, amount);
        return response.transactionSuccess();
    }

    @Override
    public boolean deposit(OfflinePlayer player, double amount) {
        if (!hasEconomy() || player == null) {
            return false;
        }
        try {
            EconomyResponse response = economy.depositPlayer(player, amount);
            return response.transactionSuccess();
        } catch (Throwable t) {
            plugin.getLogger().warning("向離線玩家發放貨幣失敗: " + t.getMessage());
            return false;
        }
    }

    @Override
    public String format(double amount) {
        if (hasEconomy()) {
            try {
                return economy.format(amount);
            } catch (Throwable ignored) {}
        }
        return String.format("%.2f", amount);
    }

    @Override
    public String getCurrencyName() {
        if (hasEconomy()) {
            try {
                String name = economy.currencyNamePlural();
                if (name != null && !name.isEmpty()) {
                    return name;
                }
            } catch (Throwable ignored) {}
        }
        return "Coins";
    }
}

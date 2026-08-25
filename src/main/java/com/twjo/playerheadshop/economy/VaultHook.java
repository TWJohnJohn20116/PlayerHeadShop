package com.twjo.playerheadshop.economy;

import com.twjo.playerheadshop.PlayerHeadShop;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * 管理 Vault 經濟插件的掛鉤與貨幣操作
 */
public class VaultHook {

    private final PlayerHeadShop plugin;
    private Economy economy = null;

    public VaultHook(PlayerHeadShop plugin) {
        this.plugin = plugin;
        setupEconomy();
    }

    /**
     * 嘗試檢測並掛鉤 Vault 經濟服務
     */
    public boolean setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }

        this.economy = rsp.getProvider();
        return this.economy != null;
    }

    /**
     * 是否已成功掛鉤 Vault 經濟服務
     */
    public boolean hasEconomy() {
        if (economy == null) {
            setupEconomy();
        }
        return economy != null;
    }

    /**
     * 獲取玩家目前的貨幣餘額
     */
    public double getBalance(Player player) {
        if (!hasEconomy() || player == null) {
            return 0.0;
        }
        return economy.getBalance(player);
    }

    /**
     * 檢查玩家是否有足夠的貨幣
     */
    public boolean has(Player player, double amount) {
        if (!hasEconomy() || player == null) {
            return false;
        }
        return economy.has(player, amount);
    }

    /**
     * 安全扣除玩家貨幣
     */
    public boolean withdraw(Player player, double amount) {
        if (!hasEconomy() || player == null) {
            return false;
        }
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return response.transactionSuccess();
    }

    /**
     * 格式化貨幣金額字串（如 $500.00 或 500 金幣）
     */
    public String format(double amount) {
        if (hasEconomy()) {
            try {
                return economy.format(amount);
            } catch (Throwable ignored) {}
        }
        return String.format("%.2f", amount);
    }

    /**
     * 獲取貨幣名稱
     */
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

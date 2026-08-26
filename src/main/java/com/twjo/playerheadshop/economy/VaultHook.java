package com.twjo.playerheadshop.economy;

import com.twjo.playerheadshop.PlayerHeadShop;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * 管理 Vault 經濟插件的防護掛鉤（採用安全橋接模式，未安裝 Vault 時絕不拋出 NoClassDefFoundError）
 */
public class VaultHook {

    private final EconomyProvider provider;

    public VaultHook(PlayerHeadShop plugin) {
        EconomyProvider loadedProvider = null;

        // 僅在伺服器已啟用 Vault 時，透過反射/隔離類別安全載入 VaultEconomyProvider
        if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            try {
                Class.forName("net.milkbowl.vault.economy.Economy");
                VaultEconomyProvider vaultProv = new VaultEconomyProvider(plugin);
                if (vaultProv.hasEconomy()) {
                    loadedProvider = vaultProv;
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("無法連接至 Vault 經濟服務: " + t.getMessage());
            }
        }

        this.provider = (loadedProvider != null) ? loadedProvider : new NullEconomyProvider();
    }

    /**
     * 是否已成功掛鉤 Vault 經濟服務
     */
    public boolean hasEconomy() {
        return provider.hasEconomy();
    }

    /**
     * 獲取玩家目前的貨幣餘額
     */
    public double getBalance(Player player) {
        return provider.getBalance(player);
    }

    /**
     * 檢查玩家是否有足夠的貨幣
     */
    public boolean has(Player player, double amount) {
        return provider.has(player, amount);
    }

    /**
     * 安全扣除玩家貨幣
     */
    public boolean withdraw(Player player, double amount) {
        return provider.withdraw(player, amount);
    }

    /**
     * 格式化貨幣金額字串（如 $500.00 或 500 金幣）
     */
    public String format(double amount) {
        return provider.format(amount);
    }

    /**
     * 獲取貨幣名稱
     */
    public String getCurrencyName() {
        return provider.getCurrencyName();
    }
}

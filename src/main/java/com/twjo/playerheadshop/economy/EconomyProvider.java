package com.twjo.playerheadshop.economy;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * 經濟服務提供者介面
 */
public interface EconomyProvider {

    boolean hasEconomy();

    double getBalance(Player player);

    boolean has(Player player, double amount);

    boolean withdraw(Player player, double amount);

    boolean deposit(Player player, double amount);

    /**
     * 給予離線玩家貨幣（用於市集賣家分潤，賣家不一定在線）
     */
    boolean deposit(OfflinePlayer player, double amount);

    String format(double amount);

    String getCurrencyName();
}

package com.twjo.playerheadshop.economy;

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

    String format(double amount);

    String getCurrencyName();
}

package com.twjo.playerheadshop.economy;

import org.bukkit.entity.Player;

/**
 * 當未安裝 Vault 或未啟用經濟插件時的預設空實作
 */
public class NullEconomyProvider implements EconomyProvider {

    @Override
    public boolean hasEconomy() {
        return false;
    }

    @Override
    public double getBalance(Player player) {
        return 0.0;
    }

    @Override
    public boolean has(Player player, double amount) {
        return false;
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        return false;
    }

    @Override
    public boolean deposit(Player player, double amount) {
        return false;
    }

    @Override
    public String format(double amount) {
        return String.format("%.2f", amount);
    }

    @Override
    public String getCurrencyName() {
        return "Coins";
    }
}

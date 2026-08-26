package com.twjo.playerheadshop.gui;

import com.twjo.playerheadshop.config.ShopOption;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * 用於安全識別 PlayerHeadShop GUI 介面的自訂 InventoryHolder
 */
public class HeadShopGuiHolder implements InventoryHolder {

    private final Player player;
    private final Map<Integer, ShopOption> options;
    private int marketSlot = -1;
    private Inventory inventory;

    public HeadShopGuiHolder(Player player, Map<Integer, ShopOption> options) {
        this.player = player;
        this.options = options != null ? Map.copyOf(options) : Map.of();
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public Player getPlayer() {
        return player;
    }

    public ShopOption getOption(int slot) {
        return options.get(slot);
    }

    public int getMarketSlot() {
        return marketSlot;
    }

    public void setMarketSlot(int marketSlot) {
        this.marketSlot = marketSlot;
    }
}

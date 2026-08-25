package com.twjo.playerheadshop.config;

import org.bukkit.Material;

import java.util.List;

/**
 * 代表一個頭顱兌換方案的設定資料模型
 */
public class ShopOption {

    private final int slot;
    private final String displayName;
    private final List<String> lore;
    private final Material costItem;
    private final int costAmount;
    private final int headAmount;

    public ShopOption(int slot, String displayName, List<String> lore, Material costItem, int costAmount, int headAmount) {
        this.slot = slot;
        this.displayName = displayName;
        this.lore = lore != null ? List.copyOf(lore) : List.of();
        this.costItem = costItem != null ? costItem : Material.DIAMOND;
        this.costAmount = Math.max(1, costAmount);
        this.headAmount = Math.max(1, headAmount);
    }

    public int getSlot() {
        return slot;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getLore() {
        return lore;
    }

    public Material getCostItem() {
        return costItem;
    }

    public int getCostAmount() {
        return costAmount;
    }

    public int getHeadAmount() {
        return headAmount;
    }
}

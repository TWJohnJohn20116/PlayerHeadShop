package com.twjo.playerheadshop.market;

import com.twjo.playerheadshop.config.ShopOption;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * 頭顱上架設定選單 (Publish Setup GUI) InventoryHolder 標記
 */
public class PublishGuiHolder implements InventoryHolder {

    public static final int SIZE = 27;
    public static final int MODE_SLOT = 10;
    public static final int PRICE_SLOT = 12;
    public static final int TITLE_SLOT = 14;
    public static final int PREVIEW_SLOT = 16;
    public static final int BACK_SLOT = 18;
    public static final int CONFIRM_SLOT = 22;

    private final Player player;
    private ShopOption.CostType costType = ShopOption.CostType.ITEM;
    private Material costItem = Material.DIAMOND;
    private double costAmount = 1.0;
    private int headAmount = 1;
    private String customTitle;
    private Inventory inventory;

    public PublishGuiHolder(Player player) {
        this.player = player;
        this.customTitle = player.getName() + " 的頭顱";
    }

    public Player getPlayer() {
        return player;
    }

    public ShopOption.CostType getCostType() {
        return costType;
    }

    public void setCostType(ShopOption.CostType costType) {
        this.costType = costType;
    }

    public Material getCostItem() {
        return costItem;
    }

    public void setCostItem(Material costItem) {
        this.costItem = costItem;
    }

    public double getCostAmount() {
        return costAmount;
    }

    public void setCostAmount(double costAmount) {
        this.costAmount = Math.max(1.0, costAmount);
    }

    public int getHeadAmount() {
        return headAmount;
    }

    public void setHeadAmount(int headAmount) {
        this.headAmount = Math.max(1, headAmount);
    }

    public String getCustomTitle() {
        return customTitle;
    }

    public void setCustomTitle(String customTitle) {
        if (customTitle != null && !customTitle.trim().isEmpty()) {
            this.customTitle = customTitle.trim();
        }
    }

    public void cycleCostType(boolean hasVault) {
        if (costType == ShopOption.CostType.ITEM && costItem == Material.DIAMOND) {
            costItem = Material.EMERALD;
        } else if (costType == ShopOption.CostType.ITEM && costItem == Material.EMERALD) {
            if (hasVault) {
                costType = ShopOption.CostType.VAULT;
                costItem = Material.GOLD_INGOT;
            } else {
                costType = ShopOption.CostType.EXP_LEVEL;
                costItem = Material.EXPERIENCE_BOTTLE;
            }
        } else if (costType == ShopOption.CostType.VAULT) {
            costType = ShopOption.CostType.EXP_LEVEL;
            costItem = Material.EXPERIENCE_BOTTLE;
        } else if (costType == ShopOption.CostType.EXP_LEVEL) {
            costType = ShopOption.CostType.EXP_POINTS;
            costItem = Material.EXPERIENCE_BOTTLE;
        } else {
            costType = ShopOption.CostType.ITEM;
            costItem = Material.DIAMOND;
        }
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}

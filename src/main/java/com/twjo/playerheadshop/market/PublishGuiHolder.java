package com.twjo.playerheadshop.market;

import com.twjo.playerheadshop.config.PluginConfig;
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
    private ShopOption.CostType costType;
    private Material costItem;
    private double costAmount;
    private int headAmount;
    private String customTitle;
    private Inventory inventory;

    /**
     * @param config     初始定價來源（market.default-price.*），服主改設定會立即反映在上架選單
     * @param titleFormat 預設頭顱名稱樣板，其中的 {@code <player>} 會替換為賣家名稱
     */
    public PublishGuiHolder(Player player, PluginConfig config, String titleFormat) {
        this.player = player;
        this.costType = config != null ? config.getMarketDefaultCostType() : ShopOption.CostType.ITEM;
        this.costItem = config != null ? config.getMarketDefaultCostItem() : Material.DIAMOND;
        this.costAmount = Math.max(1.0, config != null ? config.getMarketDefaultCostAmount() : 1.0);
        this.headAmount = Math.max(1, config != null ? config.getMarketDefaultHeadAmount() : 1);
        this.customTitle = (titleFormat != null ? titleFormat : "<player>")
                .replace("<player>", player.getName());
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

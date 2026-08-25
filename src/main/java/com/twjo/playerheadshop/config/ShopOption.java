package com.twjo.playerheadshop.config;

import org.bukkit.Material;

import java.util.List;

/**
 * 代表一個頭顱兌換方案的設定資料模型（支援物品、Vault 貨幣、經驗等級與經驗點數支付模式）
 */
public class ShopOption {

    public enum CostType {
        ITEM,
        VAULT,
        EXP_LEVEL,
        EXP_POINTS
    }

    private final int slot;
    private final CostType costType;
    private final String displayName;
    private final List<String> lore;
    private final Material costItem;
    private final double costAmount;
    private final int headAmount;

    public ShopOption(int slot, CostType costType, String displayName, List<String> lore, Material costItem, double costAmount, int headAmount) {
        this.slot = slot;
        this.costType = costType != null ? costType : CostType.ITEM;
        this.displayName = displayName;
        this.lore = lore != null ? List.copyOf(lore) : List.of();
        this.costItem = costItem != null ? costItem : Material.DIAMOND;
        this.costAmount = Math.max(1, costAmount);
        this.headAmount = Math.max(1, headAmount);
    }

    public ShopOption withSlot(int newSlot) {
        return new ShopOption(newSlot, this.costType, this.displayName, this.lore, this.costItem, this.costAmount, this.headAmount);
    }

    public int getSlot() {
        return slot;
    }

    public boolean isAutoSlot() {
        return slot < 0;
    }

    public CostType getCostType() {
        return costType;
    }

    public boolean isVault() {
        return costType == CostType.VAULT;
    }

    public boolean isExpLevel() {
        return costType == CostType.EXP_LEVEL;
    }

    public boolean isExpPoints() {
        return costType == CostType.EXP_POINTS;
    }

    public boolean isDirectPurchase() {
        return costType == CostType.VAULT || costType == CostType.EXP_LEVEL || costType == CostType.EXP_POINTS;
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

    public double getCostAmount() {
        return costAmount;
    }

    public int getCostAmountInt() {
        return (int) Math.round(costAmount);
    }

    public int getHeadAmount() {
        return headAmount;
    }
}

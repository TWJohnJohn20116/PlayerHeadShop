package com.twjo.playerheadshop.treasury;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * 收益金庫 GUI InventoryHolder 標記
 */
public class TreasuryGuiHolder implements InventoryHolder {

    public static final int SIZE = 54;
    public static final int ITEM_STORAGE_LIMIT = 45; // 0 ~ 44 為實體物品存放槽
    public static final int VAULT_SLOT = 48;         // 金幣提領按鈕
    public static final int EXP_SLOT = 50;           // 經驗提領按鈕
    public static final int LOGS_SLOT = 52;          // 稽核日誌按鈕
    public static final int REFRESH_SLOT = 53;       // 重新整理按鈕

    private final Player admin;
    private Inventory inventory;

    public TreasuryGuiHolder(Player admin) {
        this.admin = admin;
    }

    public Player getAdmin() {
        return admin;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public static boolean isItemStorageSlot(int slot) {
        return slot >= 0 && slot < ITEM_STORAGE_LIMIT;
    }
}

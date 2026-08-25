package com.twjo.playerheadshop.gui;

import com.twjo.playerheadshop.config.ShopOption;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * 玩家放置物品進行兌換的專屬 GUI Holder（支援多格放置區，解決單格無法超過 64 個物品的問題）
 */
public class DepositGuiHolder implements InventoryHolder {

    // 支援 6 個放置格（最多可容納 6 * 64 = 384 個物品）
    public static final Set<Integer> INPUT_SLOTS = Set.of(10, 11, 12, 19, 20, 21);
    public static final int CONFIRM_SLOT = 14;
    public static final int PREVIEW_SLOT = 16;
    public static final int BACK_SLOT = 18;

    private final Player player;
    private final ShopOption option;
    private Inventory inventory;
    private boolean isNavigatingBack = false;

    public DepositGuiHolder(Player player, ShopOption option) {
        this.player = player;
        this.option = option;
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

    public ShopOption getOption() {
        return option;
    }

    public boolean isNavigatingBack() {
        return isNavigatingBack;
    }

    public void setNavigatingBack(boolean navigatingBack) {
        isNavigatingBack = navigatingBack;
    }

    public static boolean isInputSlot(int slot) {
        return INPUT_SLOTS.contains(slot);
    }
}

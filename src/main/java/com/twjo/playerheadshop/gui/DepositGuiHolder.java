package com.twjo.playerheadshop.gui;

import com.twjo.playerheadshop.config.ShopOption;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * 玩家放置物品進行兌換的專屬 GUI Holder
 */
public class DepositGuiHolder implements InventoryHolder {

    public static final int INPUT_SLOT = 11;
    public static final int CONFIRM_SLOT = 13;
    public static final int PREVIEW_SLOT = 15;
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
}

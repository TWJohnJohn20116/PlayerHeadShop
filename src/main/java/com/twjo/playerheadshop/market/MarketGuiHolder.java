package com.twjo.playerheadshop.market;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * 社群頭顱市集 GUI InventoryHolder 標記
 */
public class MarketGuiHolder implements InventoryHolder {

    public static final int SIZE = 54;
    public static final int PAGE_SIZE = 45; // Slots 0 ~ 44
    public static final int PREV_SLOT = 45;
    public static final int SHARE_SLOT = 48;
    public static final int MY_LISTINGS_SLOT = 49;
    public static final int REFRESH_SLOT = 50;
    public static final int NEXT_SLOT = 53;

    private final Player player;
    private int page = 1;
    private boolean myListingsOnly = false;
    private final Map<Integer, SharedHeadRecord> slotMap = new HashMap<>();
    private Inventory inventory;

    public MarketGuiHolder(Player player, int page, boolean myListingsOnly) {
        this.player = player;
        this.page = page;
        this.myListingsOnly = myListingsOnly;
    }

    public Player getPlayer() {
        return player;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public boolean isMyListingsOnly() {
        return myListingsOnly;
    }

    public void setMyListingsOnly(boolean myListingsOnly) {
        this.myListingsOnly = myListingsOnly;
    }

    public Map<Integer, SharedHeadRecord> getSlotMap() {
        return slotMap;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}

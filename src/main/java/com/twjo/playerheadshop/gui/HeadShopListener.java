package com.twjo.playerheadshop.gui;

import com.twjo.playerheadshop.config.ShopOption;
import com.twjo.playerheadshop.service.HeadShopService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * 監聽 GUI 點擊與拖曳事件，防止物品被拿取並觸發交易邏輯
 */
public class HeadShopListener implements Listener {

    private final HeadShopService headShopService;

    public HeadShopListener(HeadShopService headShopService) {
        this.headShopService = headShopService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        // 僅處理屬於 HeadShopGuiHolder 的介面
        if (!(event.getInventory().getHolder() instanceof HeadShopGuiHolder holder)) {
            return;
        }

        // 強制取消任何點擊動作（防拿取、防 Shift 點擊、防熱鍵更換）
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // 確保點擊的是頂部 GUI 介面，而非玩家自身背包
        if (event.getClickedInventory() == null || !(event.getClickedInventory().getHolder() instanceof HeadShopGuiHolder)) {
            return;
        }

        int slot = event.getSlot();
        ShopOption option = holder.getOption(slot);

        if (option != null) {
            // 執行購買邏輯
            headShopService.processPurchase(player, option);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        // 防止在 GUI 中拖曳物品
        if (event.getInventory().getHolder() instanceof HeadShopGuiHolder) {
            event.setCancelled(true);
        }
    }
}

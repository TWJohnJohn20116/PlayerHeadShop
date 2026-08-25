package com.twjo.playerheadshop.gui;

import com.twjo.playerheadshop.config.ShopOption;
import com.twjo.playerheadshop.service.HeadShopService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

/**
 * 監聽 GUI 點擊、拖曳與關閉事件，支援物品放置兌換與 Vault 貨幣直接扣款
 */
public class HeadShopListener implements Listener {

    private final HeadShopService headShopService;
    private final HeadShopGui headShopGui;

    public HeadShopListener(HeadShopService headShopService, HeadShopGui headShopGui) {
        this.headShopService = headShopService;
        this.headShopGui = headShopGui;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // ==========================================
        // 情況 1: 主選單 GUI (HeadShopGuiHolder)
        // ==========================================
        if (event.getInventory().getHolder() instanceof HeadShopGuiHolder holder) {
            event.setCancelled(true);

            if (event.getClickedInventory() != null && event.getClickedInventory().getHolder() instanceof HeadShopGuiHolder) {
                int slot = event.getSlot();
                ShopOption option = holder.getOption(slot);
                if (option != null) {
                    if (option.isVault()) {
                        // Vault 貨幣方案：直接執行扣款購買
                        headShopService.processVaultPurchase(player, option);
                    } else {
                        // 實體物品方案：開啟放置兌換介面
                        headShopGui.openDepositGui(player, option);
                    }
                }
            }
            return;
        }

        // ==========================================
        // 情況 2: 放置兌換介面 (DepositGuiHolder)
        // ==========================================
        if (event.getInventory().getHolder() instanceof DepositGuiHolder holder) {
            if (event.getClickedInventory() != null && event.getClickedInventory().getHolder() instanceof DepositGuiHolder) {
                int slot = event.getSlot();

                if (DepositGuiHolder.isInputSlot(slot)) {
                    // 允許在放置區自由放置與拿取
                    return;
                }

                // 其他格子一律禁止拿取
                event.setCancelled(true);

                if (slot == DepositGuiHolder.CONFIRM_SLOT || slot == DepositGuiHolder.PREVIEW_SLOT) {
                    headShopService.processDepositPurchase(player, holder);
                } else if (slot == DepositGuiHolder.BACK_SLOT) {
                    returnInputItems(holder);
                    holder.setNavigatingBack(true);
                    headShopGui.open(player);
                }
            } else {
                if (event.isShiftClick()) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof HeadShopGuiHolder) {
            event.setCancelled(true);
            return;
        }

        if (event.getInventory().getHolder() instanceof DepositGuiHolder) {
            int topSize = event.getInventory().getSize();
            for (int rawSlot : event.getRawSlots()) {
                if (rawSlot < topSize && !DepositGuiHolder.isInputSlot(rawSlot)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof DepositGuiHolder holder) {
            if (!holder.isNavigatingBack()) {
                returnInputItems(holder);
            }
        }
    }

    /**
     * 安全將放置區內的所有物品歸還給玩家，背包滿溢時自動掉落於腳下
     */
    private void returnInputItems(DepositGuiHolder holder) {
        Player player = holder.getPlayer();
        if (!player.isOnline()) {
            return;
        }

        for (int slot : DepositGuiHolder.INPUT_SLOTS) {
            ItemStack inputItem = holder.getInventory().getItem(slot);
            if (inputItem != null && !inputItem.getType().isAir()) {
                HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(inputItem);
                if (!overflow.isEmpty()) {
                    for (ItemStack leftover : overflow.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                    }
                }
                holder.getInventory().setItem(slot, null);
            }
        }
    }
}

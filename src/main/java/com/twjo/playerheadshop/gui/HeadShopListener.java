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
 * 監聽 GUI 點擊、拖曳與關閉事件，支援主選單與放置兌換介面的交互邏輯
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
            // 全局取消，禁止拿取或移動任何物品
            event.setCancelled(true);

            // 若點擊頂部主選單
            if (event.getClickedInventory() != null && event.getClickedInventory().getHolder() instanceof HeadShopGuiHolder) {
                int slot = event.getSlot();
                ShopOption option = holder.getOption(slot);
                if (option != null) {
                    // 點擊方案後，開啟專屬放置兌換介面
                    headShopGui.openDepositGui(player, option);
                }
            }
            return;
        }

        // ==========================================
        // 情況 2: 放置兌換介面 (DepositGuiHolder)
        // ==========================================
        if (event.getInventory().getHolder() instanceof DepositGuiHolder holder) {
            // 若點擊頂部放置介面
            if (event.getClickedInventory() != null && event.getClickedInventory().getHolder() instanceof DepositGuiHolder) {
                int slot = event.getSlot();

                if (slot == DepositGuiHolder.INPUT_SLOT) {
                    // 允許在 Slot 11 (放置區) 正常放置與拿取物品
                    return;
                }

                // 其他格子一律禁止拿取
                event.setCancelled(true);

                if (slot == DepositGuiHolder.CONFIRM_SLOT || slot == DepositGuiHolder.PREVIEW_SLOT) {
                    // 點擊確認兌換或預覽頭顱
                    headShopService.processDepositPurchase(player, holder);
                } else if (slot == DepositGuiHolder.BACK_SLOT) {
                    // 點擊返回主選單：先返還放置區物品，再開啟主選單
                    returnInputItems(holder);
                    holder.setNavigatingBack(true);
                    headShopGui.open(player);
                }
            } else {
                // 點擊玩家自身背包：防止 Shift 快捷鍵將物品誤放入非放置區
                if (event.isShiftClick()) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        // 主選單禁止任何拖曳
        if (event.getInventory().getHolder() instanceof HeadShopGuiHolder) {
            event.setCancelled(true);
            return;
        }

        // 放置介面：僅允許拖曳至 Slot 11 或玩家自身背包
        if (event.getInventory().getHolder() instanceof DepositGuiHolder) {
            int topSize = event.getInventory().getSize();
            for (int rawSlot : event.getRawSlots()) {
                if (rawSlot < topSize && rawSlot != DepositGuiHolder.INPUT_SLOT) {
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
     * 安全將放置區 (Slot 11) 內的物品歸還給玩家，背包滿溢時自動掉落於腳下
     */
    private void returnInputItems(DepositGuiHolder holder) {
        ItemStack inputItem = holder.getInventory().getItem(DepositGuiHolder.INPUT_SLOT);
        if (inputItem != null && !inputItem.getType().isAir()) {
            Player player = holder.getPlayer();
            if (player.isOnline()) {
                HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(inputItem);
                if (!overflow.isEmpty()) {
                    for (ItemStack leftover : overflow.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                    }
                }
            }
            holder.getInventory().setItem(DepositGuiHolder.INPUT_SLOT, null);
        }
    }
}

package com.twjo.playerheadshop.gui;

import com.twjo.playerheadshop.PlayerHeadShop;
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
 * 監聽 GUI 點擊、拖曳與關閉事件，支援物品放置兌換、Vault 貨幣、經驗支付與社群市集入口
 */
public class HeadShopListener implements Listener {

    private final PlayerHeadShop plugin;
    private final HeadShopService headShopService;
    private final HeadShopGui headShopGui;

    public HeadShopListener(PlayerHeadShop plugin, HeadShopService headShopService, HeadShopGui headShopGui) {
        this.plugin = plugin;
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
        if (event.getView().getTopInventory().getHolder() instanceof HeadShopGuiHolder holder) {
            event.setCancelled(true);

            if (event.getClickedInventory() != null && event.getClickedInventory().getHolder() instanceof HeadShopGuiHolder) {
                int slot = event.getSlot();

                // 點擊社群市集入口按鈕
                if (slot == holder.getMarketSlot()) {
                    if (plugin.getMarketGui() != null) {
                        // 延後一 tick 才切換介面，避免在事件處理期間更換介面
                        plugin.getSchedulerAdapter().runForEntityLater(player,
                                () -> plugin.getMarketGui().open(player, 1, false), 1L);
                    }
                    return;
                }

                // 點擊方案商品
                ShopOption option = holder.getOption(slot);
                if (option != null) {
                    if (option.isDirectPurchase()) {
                        // Vault 貨幣、經驗等級、經驗點數：直接扣款購買
                        headShopService.processPurchase(player, option);
                    } else {
                        // 實體物品方案：開啟放置兌換介面（延後一 tick）
                        plugin.getSchedulerAdapter().runForEntityLater(player,
                                () -> headShopGui.openDepositGui(player, option), 1L);
                    }
                }
            }
            return;
        }

        // ==========================================
        // 情況 2: 放置兌換介面 (DepositGuiHolder)
        // ==========================================
        if (event.getView().getTopInventory().getHolder() instanceof DepositGuiHolder holder) {
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
                    // 放置區允許游標持有物品，因此絕不可在事件處理期間切換介面：
                    // 那會使游標物品進入未定義狀態（Bukkit 明確警告的複製 / 遺失路徑）。
                    holder.setNavigatingBack(true);
                    plugin.getSchedulerAdapter().runForEntityLater(player, () -> {
                        returnInputItems(holder);
                        headShopGui.open(player);
                    }, 1L);
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
        if (event.getView().getTopInventory().getHolder() instanceof HeadShopGuiHolder) {
            event.setCancelled(true);
            return;
        }

        if (event.getView().getTopInventory().getHolder() instanceof DepositGuiHolder) {
            int topSize = event.getView().getTopInventory().getSize();
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

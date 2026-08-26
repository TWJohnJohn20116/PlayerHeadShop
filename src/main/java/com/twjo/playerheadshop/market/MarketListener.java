package com.twjo.playerheadshop.market;

import com.twjo.playerheadshop.PlayerHeadShop;
import com.twjo.playerheadshop.config.PluginConfig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * 監聽社群頭顱市集 GUI 的點擊、購買、上架與下架事件
 */
public class MarketListener implements Listener {

    private final PlayerHeadShop plugin;
    private final MarketManager marketManager;
    private final MarketGui marketGui;
    private final PluginConfig config;

    public MarketListener(PlayerHeadShop plugin, MarketManager marketManager, MarketGui marketGui, PluginConfig config) {
        this.plugin = plugin;
        this.marketManager = marketManager;
        this.marketGui = marketGui;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getInventory().getHolder() instanceof MarketGuiHolder holder) {
            event.setCancelled(true);

            if (event.getClickedInventory() != null && event.getClickedInventory().getHolder() instanceof MarketGuiHolder) {
                int slot = event.getSlot();

                // 1. 點擊頭顱商品 (Slots 0 ~ 44)
                if (slot >= 0 && slot < MarketGuiHolder.PAGE_SIZE) {
                    SharedHeadRecord rec = holder.getSlotMap().get(slot);
                    if (rec != null) {
                        // 若為本人上架或管理員 -> 下架
                        if (rec.getSellerUuid().equals(player.getUniqueId()) || player.hasPermission("playerheadshop.admin")) {
                            marketManager.unlistHead(player, rec.getId(), player.hasPermission("playerheadshop.admin")).thenAccept(success -> {
                                if (success) {
                                    marketGui.open(player, holder.getPage(), holder.isMyListingsOnly());
                                }
                            });
                        }
                        // 若為買家 -> 購買
                        else {
                            if (marketManager.purchaseSharedHead(player, rec)) {
                                marketGui.open(player, holder.getPage(), holder.isMyListingsOnly());
                            }
                        }
                    }
                    return;
                }

                // 2. 上一頁 (Slot 45)
                if (slot == MarketGuiHolder.PREV_SLOT && holder.getPage() > 1) {
                    marketGui.open(player, holder.getPage() - 1, holder.isMyListingsOnly());
                }
                // 3. ◀ 返回個人頭顱商店 (Slot 46)
                else if (slot == MarketGui.BACK_TO_SHOP_SLOT) {
                    if (plugin.getHeadShopGui() != null) {
                        plugin.getHeadShopGui().open(player);
                    }
                }
                // 4. ➕ 上架我的頭顱設定選單 (Slot 48)
                else if (slot == MarketGuiHolder.SHARE_SLOT) {
                    if (plugin.getPublishGui() != null) {
                        plugin.getPublishGui().open(player, null);
                    }
                }
                // 5. 🎒 切換 全部/我的上架 (Slot 49)
                else if (slot == MarketGuiHolder.MY_LISTINGS_SLOT) {
                    marketGui.open(player, 1, !holder.isMyListingsOnly());
                }
                // 6. 🔄 重新整理 (Slot 50)
                else if (slot == MarketGuiHolder.REFRESH_SLOT) {
                    marketGui.open(player, holder.getPage(), holder.isMyListingsOnly());
                }
                // 7. 下一頁 (Slot 53)
                else if (slot == MarketGuiHolder.NEXT_SLOT) {
                    marketGui.open(player, holder.getPage() + 1, holder.isMyListingsOnly());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof MarketGuiHolder) {
            event.setCancelled(true);
        }
    }
}

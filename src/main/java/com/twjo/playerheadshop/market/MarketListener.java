package com.twjo.playerheadshop.market;

import com.twjo.playerheadshop.PlayerHeadShop;
import com.twjo.playerheadshop.config.PluginConfig;
import com.twjo.playerheadshop.lang.LanguageManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * 監聽社群頭顱市集 GUI 的點擊、購買、上架與下架事件
 *
 * <p>所有介面切換都延後至下一 tick 執行：在 InventoryClickEvent 處理期間更換介面會讓游標上的物品
 * 進入未定義狀態，是 Bukkit 明確警告過的複製 / 遺失路徑。</p>
 */
public class MarketListener implements Listener {

    private final PlayerHeadShop plugin;
    private final MarketManager marketManager;
    private final MarketGui marketGui;
    private final PluginConfig config;
    private final LanguageManager lang;

    public MarketListener(PlayerHeadShop plugin, MarketManager marketManager, MarketGui marketGui,
                          PluginConfig config, LanguageManager lang) {
        this.plugin = plugin;
        this.marketManager = marketManager;
        this.marketGui = marketGui;
        this.config = config;
        this.lang = lang;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!(event.getView().getTopInventory().getHolder() instanceof MarketGuiHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() == null
                || !(event.getClickedInventory().getHolder() instanceof MarketGuiHolder)) {
            return;
        }

        int slot = event.getSlot();

        // 1. 點擊頭顱商品 (Slots 0 ~ 44)
        if (slot >= 0 && slot < MarketGuiHolder.PAGE_SIZE) {
            SharedHeadRecord rec = holder.getSlotMap().get(slot);
            if (rec != null) {
                handleHeadClick(player, holder, rec);
            }
            return;
        }

        // 2. 上一頁 (Slot 45)
        if (slot == MarketGuiHolder.PREV_SLOT && holder.getPage() > 1) {
            reopen(player, holder.getPage() - 1, holder.isMyListingsOnly());
        }
        // 3. ◀ 返回個人頭顱商店 (Slot 46)
        else if (slot == MarketGui.BACK_TO_SHOP_SLOT) {
            if (plugin.getHeadShopGui() != null) {
                plugin.getSchedulerAdapter().runForEntityLater(player,
                        () -> plugin.getHeadShopGui().open(player), 1L);
            }
        }
        // 4. ➕ 上架我的頭顱設定選單 (Slot 48)
        else if (slot == MarketGuiHolder.SHARE_SLOT) {
            if (plugin.getPublishGui() != null) {
                plugin.getSchedulerAdapter().runForEntityLater(player,
                        () -> plugin.getPublishGui().open(player, null), 1L);
            }
        }
        // 5. 🎒 切換 全部/我的上架 (Slot 49)
        else if (slot == MarketGuiHolder.MY_LISTINGS_SLOT) {
            reopen(player, 1, !holder.isMyListingsOnly());
        }
        // 6. 🔄 重新整理 (Slot 50)
        else if (slot == MarketGuiHolder.REFRESH_SLOT) {
            reopen(player, holder.getPage(), holder.isMyListingsOnly());
        }
        // 7. 下一頁 (Slot 53)
        else if (slot == MarketGuiHolder.NEXT_SLOT) {
            reopen(player, holder.getPage() + 1, holder.isMyListingsOnly());
        }
    }

    /**
     * 處理商品點擊：下架或購買。購買前一律重新讀取最新狀態，避免買到已下架或已改價的商品。
     */
    private void handleHeadClick(Player player, MarketGuiHolder holder, SharedHeadRecord snapshot) {
        boolean isAdmin = player.hasPermission("playerheadshop.admin");

        // 若為本人上架或管理員 -> 下架
        if (snapshot.getSellerUuid().equals(player.getUniqueId()) || isAdmin) {
            marketManager.unlistHead(player, snapshot.getId(), isAdmin).thenAccept(success -> {
                if (success) {
                    reopen(player, holder.getPage(), holder.isMyListingsOnly());
                }
            });
            return;
        }

        // 買家 -> 以資料庫最新狀態購買（slotMap 為 GUI 開啟時的快照，可能已過期）
        plugin.getDatabaseManager().getActiveSharedHeadById(snapshot.getId()).thenAccept(fresh ->
                plugin.getSchedulerAdapter().runForEntity(player, () -> {
                    if (fresh == null) {
                        lang.sendMessage(player, "market-listing-gone");
                        reopen(player, holder.getPage(), holder.isMyListingsOnly());
                        return;
                    }
                    if (marketManager.purchaseSharedHead(player, fresh)) {
                        reopen(player, holder.getPage(), holder.isMyListingsOnly());
                    }
                }));
    }

    /**
     * 延後一 tick 後重新開啟市集頁面
     */
    private void reopen(Player player, int page, boolean myListingsOnly) {
        plugin.getSchedulerAdapter().runForEntityLater(player,
                () -> marketGui.open(player, page, myListingsOnly), 1L);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MarketGuiHolder) {
            event.setCancelled(true);
        }
    }
}

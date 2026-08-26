package com.twjo.playerheadshop.market;

import com.twjo.playerheadshop.PlayerHeadShop;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * 監聽頭顱上架設定選單 (Publish Setup GUI) 的點擊與告示牌輸入互動事件
 */
public class PublishListener implements Listener {

    private final PlayerHeadShop plugin;
    private final PublishGui publishGui;
    private final MarketManager marketManager;
    private final SignInputManager signInputManager;

    public PublishListener(PlayerHeadShop plugin, PublishGui publishGui, MarketManager marketManager, SignInputManager signInputManager) {
        this.plugin = plugin;
        this.publishGui = publishGui;
        this.marketManager = marketManager;
        this.signInputManager = signInputManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getInventory().getHolder() instanceof PublishGuiHolder holder) {
            event.setCancelled(true);

            if (event.getClickedInventory() != null && event.getClickedInventory().getHolder() instanceof PublishGuiHolder) {
                int slot = event.getSlot();

                // 1. 切換支付模式 (Slot 10)
                if (slot == PublishGuiHolder.MODE_SLOT) {
                    holder.cycleCostType();
                    publishGui.open(player, holder);
                }
                // 2. 設定售價金額 (Slot 12)
                else if (slot == PublishGuiHolder.PRICE_SLOT) {
                    ClickType click = event.getClick();
                    if (click == ClickType.RIGHT) {
                        holder.setCostAmount(holder.getCostAmount() + 1);
                        publishGui.open(player, holder);
                    } else if (click == ClickType.SHIFT_RIGHT) {
                        holder.setCostAmount(holder.getCostAmount() + 10);
                        publishGui.open(player, holder);
                    } else if (click == ClickType.SHIFT_LEFT) {
                        holder.setCostAmount(Math.max(1, holder.getCostAmount() - 1));
                        publishGui.open(player, holder);
                    } else {
                        // 左鍵點擊：開啟告示牌輸入
                        player.closeInventory();
                        signInputManager.openSignInput(player, new String[]{
                                String.valueOf((int) holder.getCostAmount()),
                                "^^^^^^^^^^^^^^^",
                                "請在第一行輸入",
                                "欲設定的售價金額"
                        }, input -> {
                            try {
                                double val = Double.parseDouble(input.replaceAll("[^0-9.]", ""));
                                if (val > 0) {
                                    holder.setCostAmount(val);
                                }
                            } catch (Exception ignored) {}
                            publishGui.open(player, holder);
                        });
                    }
                }
                // 3. 自訂頭顱標題 (Slot 14)
                else if (slot == PublishGuiHolder.TITLE_SLOT) {
                    player.closeInventory();
                    signInputManager.openSignInput(player, new String[]{
                            holder.getCustomTitle(),
                            "^^^^^^^^^^^^^^^",
                            "請在第一行輸入",
                            "自訂頭顱標題"
                    }, input -> {
                        if (input != null && !input.trim().isEmpty()) {
                            holder.setCustomTitle(input.trim());
                        }
                        publishGui.open(player, holder);
                    });
                }
                // 4. 返回市集 (Slot 18)
                else if (slot == PublishGuiHolder.BACK_SLOT) {
                    plugin.getMarketGui().open(player, 1, false);
                }
                // 5. 確認上架發布 (Slot 22)
                else if (slot == PublishGuiHolder.CONFIRM_SLOT) {
                    marketManager.publishHead(
                            player,
                            holder.getCustomTitle(),
                            holder.getCostType(),
                            holder.getCostItem().name(),
                            holder.getCostAmount(),
                            holder.getHeadAmount()
                    ).thenAccept(success -> {
                        if (success) {
                            plugin.getMarketGui().open(player, 1, false);
                        } else {
                            publishGui.open(player, holder);
                        }
                    });
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof PublishGuiHolder) {
            event.setCancelled(true);
        }
    }
}

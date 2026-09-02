package com.twjo.playerheadshop.market;

import com.twjo.playerheadshop.PlayerHeadShop;
import com.twjo.playerheadshop.lang.LanguageManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * 監聽頭顱上架設定選單 (Publish Setup GUI) 的點擊與告示牌輸入互動事件
 *
 * <p>所有介面切換都延後至下一 tick，避免在 InventoryClickEvent 處理期間更換介面導致游標物品異常。</p>
 */
public class PublishListener implements Listener {

    private final PlayerHeadShop plugin;
    private final PublishGui publishGui;
    private final MarketManager marketManager;
    private final SignInputManager signInputManager;
    private final LanguageManager lang;

    public PublishListener(PlayerHeadShop plugin, PublishGui publishGui, MarketManager marketManager,
                           SignInputManager signInputManager, LanguageManager lang) {
        this.plugin = plugin;
        this.publishGui = publishGui;
        this.marketManager = marketManager;
        this.signInputManager = signInputManager;
        this.lang = lang;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!(event.getView().getTopInventory().getHolder() instanceof PublishGuiHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() == null
                || !(event.getClickedInventory().getHolder() instanceof PublishGuiHolder)) {
            return;
        }

        int slot = event.getSlot();

        // 1. 切換支付模式 (Slot 10)
        if (slot == PublishGuiHolder.MODE_SLOT) {
            holder.cycleCostType(plugin.getVaultHook().hasEconomy());
            reopen(player, holder);
        }
        // 2. 設定售價金額 (Slot 12)
        else if (slot == PublishGuiHolder.PRICE_SLOT) {
            ClickType click = event.getClick();
            if (click == ClickType.RIGHT) {
                holder.setCostAmount(holder.getCostAmount() + 1);
                reopen(player, holder);
            } else if (click == ClickType.SHIFT_RIGHT) {
                holder.setCostAmount(holder.getCostAmount() + 10);
                reopen(player, holder);
            } else if (click == ClickType.SHIFT_LEFT) {
                holder.setCostAmount(Math.max(1, holder.getCostAmount() - 1));
                reopen(player, holder);
            } else {
                // 左鍵點擊：開啟告示牌輸入
                openSignInput(player, holder, new String[]{
                        String.valueOf((int) holder.getCostAmount()),
                        lang.getRaw(player, "sign.separator", "^^^^^^^^^^^^^^^"),
                        lang.getRaw(player, "sign.price-hint-1", "請在第一行輸入"),
                        lang.getRaw(player, "sign.price-hint-2", "欲設定的售價金額")
                }, input -> {
                    try {
                        double val = Double.parseDouble(input.replaceAll("[^0-9.]", ""));
                        if (val > 0) {
                            holder.setCostAmount(val);
                        }
                    } catch (Exception ignored) {}
                });
            }
        }
        // 3. 自訂頭顱標題 (Slot 14)
        else if (slot == PublishGuiHolder.TITLE_SLOT) {
            openSignInput(player, holder, new String[]{
                    holder.getCustomTitle(),
                    lang.getRaw(player, "sign.separator", "^^^^^^^^^^^^^^^"),
                    lang.getRaw(player, "sign.title-hint-1", "請在第一行輸入"),
                    lang.getRaw(player, "sign.title-hint-2", "自訂頭顱標題")
            }, input -> {
                if (input != null && !input.trim().isEmpty()) {
                    holder.setCustomTitle(input.trim());
                }
            });
        }
        // 4. 返回市集 (Slot 18)
        else if (slot == PublishGuiHolder.BACK_SLOT) {
            plugin.getSchedulerAdapter().runForEntityLater(player,
                    () -> plugin.getMarketGui().open(player, 1, false), 1L);
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
            ).thenAccept(success -> plugin.getSchedulerAdapter().runForEntity(player, () -> {
                if (success) {
                    plugin.getMarketGui().open(player, 1, false);
                } else {
                    publishGui.open(player, holder);
                }
            }));
        }
    }

    /**
     * 關閉介面並開啟告示牌輸入，完成後重新開啟上架設定選單
     */
    private void openSignInput(Player player, PublishGuiHolder holder, String[] lines,
                               java.util.function.Consumer<String> onInput) {
        plugin.getSchedulerAdapter().runForEntityLater(player, () -> {
            player.closeInventory();
            signInputManager.openSignInput(player, lines, input -> {
                onInput.accept(input);
                publishGui.open(player, holder);
            });
        }, 1L);
    }

    /**
     * 延後一 tick 後重新渲染上架設定選單
     */
    private void reopen(Player player, PublishGuiHolder holder) {
        plugin.getSchedulerAdapter().runForEntityLater(player, () -> publishGui.open(player, holder), 1L);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof PublishGuiHolder) {
            event.setCancelled(true);
        }
    }
}

package com.twjo.playerheadshop.treasury;

import com.twjo.playerheadshop.PlayerHeadShop;
import com.twjo.playerheadshop.lang.LanguageManager;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

/**
 * 監聽管理員收益金庫 GUI 的互動與提領事件（實作嚴格「僅限提領 (Take-Only)」保護，禁止放入物品）
 *
 * <p>所有提領一律取消原生點擊，改由 {@link TreasuryManager#withdrawItem} 即時扣減後再發給管理員。
 * 關閉 GUI 時不做任何回寫，因此不存在快照覆蓋造成的物品複製或憑空消失。</p>
 */
public class TreasuryListener implements Listener {

    private final PlayerHeadShop plugin;
    private final LanguageManager lang;
    private final TreasuryManager treasuryManager;
    private final TreasuryGui treasuryGui;

    public TreasuryListener(PlayerHeadShop plugin, LanguageManager lang, TreasuryManager treasuryManager, TreasuryGui treasuryGui) {
        this.plugin = plugin;
        this.lang = lang;
        this.treasuryManager = treasuryManager;
        this.treasuryGui = treasuryGui;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player admin)) {
            return;
        }

        Inventory topInventory = event.getView().getTopInventory();
        if (!(topInventory.getHolder() instanceof TreasuryGuiHolder holder)) {
            return;
        }

        Inventory clicked = event.getClickedInventory();

        // 1. 若點擊的是玩家自身的背包 (下方背包)：
        if (clicked == null || !(clicked.getHolder() instanceof TreasuryGuiHolder)) {
            // 禁止使用 Shift-Click 快速將物品送入金庫
            if (event.isShiftClick()) {
                event.setCancelled(true);
            }
            return;
        }

        // 2. 點擊金庫介面本身：一律取消原生行為，改由本插件全權控制
        event.setCancelled(true);

        int slot = event.getSlot();

        // 2-1. 底排控制列按鈕 (Slots 45 ~ 53)
        if (!TreasuryGuiHolder.isItemStorageSlot(slot)) {
            handleControlButtonClick(admin, holder, slot);
            return;
        }

        // 2-2. 物品存放槽 (Slots 0 ~ 44)：僅允許提領，禁止任何放入
        ItemStack cursorItem = event.getCursor();
        if (cursorItem != null && !cursorItem.getType().isAir()) {
            return;
        }

        if (event.getClick() == ClickType.NUMBER_KEY) {
            return;
        }

        handleItemWithdraw(admin, holder, slot, event.getClick());
    }

    /**
     * 即時提領單一物品槽：先從 TreasuryManager 扣減，成功後才發給管理員
     */
    private void handleItemWithdraw(Player admin, TreasuryGuiHolder holder, int slot, ClickType click) {
        Inventory inventory = holder.getInventory();
        if (inventory == null) return;

        ItemStack shown = inventory.getItem(slot);
        if (shown == null || shown.getType().isAir()) {
            return;
        }

        boolean halfOnly = (click == ClickType.RIGHT);
        ItemStack taken = treasuryManager.withdrawItem(admin, slot, shown, halfOnly);

        if (taken == null) {
            // 快照已過期（他人先取走、或期間有新物品存入）：重新渲染畫面並提示
            treasuryGui.render(admin, holder, inventory);
            lang.sendMessage(admin, "treasury-item-stale");
            playSound(admin, Sound.ENTITY_VILLAGER_NO, 1.0f);
            return;
        }

        // 發放給管理員，背包滿溢則掉落於腳下
        HashMap<Integer, ItemStack> overflow = admin.getInventory().addItem(taken);
        if (!overflow.isEmpty()) {
            for (ItemStack leftover : overflow.values()) {
                admin.getWorld().dropItemNaturally(admin.getLocation(), leftover);
            }
            lang.sendMessage(admin, "inventory-full");
        }

        // 以扣減後的真實狀態重繪畫面
        treasuryGui.render(admin, holder, inventory);
    }

    private void handleControlButtonClick(Player admin, TreasuryGuiHolder holder, int slot) {
        // Slot 48: 提領 Vault 金幣
        if (slot == TreasuryGuiHolder.VAULT_SLOT) {
            withdrawVault(admin, holder);
        }
        // Slot 50: 提領經驗點數
        else if (slot == TreasuryGuiHolder.EXP_SLOT) {
            withdrawExp(admin, holder);
        }
        // Slot 52: 查看稽核日誌
        else if (slot == TreasuryGuiHolder.LOGS_SLOT) {
            // 延後一 tick 執行：避免在點擊事件處理期間關閉介面
            plugin.getSchedulerAdapter().runForEntityLater(admin, () -> {
                admin.closeInventory();
                admin.performCommand("buyhead pool logs 1");
            }, 1L);
        }
        // Slot 53: 重新整理
        else if (slot == TreasuryGuiHolder.REFRESH_SLOT) {
            Inventory inventory = holder.getInventory();
            if (inventory != null) {
                treasuryGui.render(admin, holder, inventory);
            }
        }
    }

    private void withdrawVault(Player admin, TreasuryGuiHolder holder) {
        double balance = treasuryManager.getVaultBalance();
        if (balance <= 0.0) {
            lang.sendMessage(admin, "treasury-empty-vault");
            playSound(admin, Sound.ENTITY_VILLAGER_NO, 1.0f);
            return;
        }

        if (!treasuryManager.withdrawVault(admin, balance)) {
            return;
        }

        // 發放失敗必須回滾，否則金庫已扣、管理員沒拿到，金額直接蒸發
        if (!plugin.getVaultHook().deposit(admin, balance)) {
            treasuryManager.refundVault(balance);
            lang.sendMessage(admin, "treasury-withdraw-failed");
            playSound(admin, Sound.ENTITY_VILLAGER_NO, 1.0f);
            redraw(admin, holder);
            return;
        }

        lang.sendMessage(admin, "treasury-withdraw-vault-success",
                Placeholder.parsed("amount", plugin.getVaultHook().format(balance))
        );
        playSound(admin, Sound.ENTITY_PLAYER_LEVELUP, 1.2f);
        redraw(admin, holder);
    }

    private void withdrawExp(Player admin, TreasuryGuiHolder holder) {
        int exp = treasuryManager.getExpPoints();
        if (exp <= 0) {
            lang.sendMessage(admin, "treasury-empty-exp");
            playSound(admin, Sound.ENTITY_VILLAGER_NO, 1.0f);
            return;
        }

        if (!treasuryManager.withdrawExp(admin, exp)) {
            return;
        }

        lang.sendMessage(admin, "treasury-withdraw-exp-success",
                Placeholder.parsed("amount", lang.formatExpPoints(admin, exp))
        );
        playSound(admin, Sound.ENTITY_PLAYER_LEVELUP, 1.2f);
        redraw(admin, holder);
    }

    private void redraw(Player admin, TreasuryGuiHolder holder) {
        Inventory inventory = holder.getInventory();
        if (inventory != null) {
            treasuryGui.render(admin, holder, inventory);
        }
    }

    private void playSound(Player player, Sound sound, float pitch) {
        try {
            player.playSound(player.getLocation(), sound, 1.0f, pitch);
        } catch (Throwable ignored) {}
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof TreasuryGuiHolder) {
            int topSize = event.getView().getTopInventory().getSize();
            for (int rawSlot : event.getRawSlots()) {
                // 一律禁止拖曳放置物品至金庫上方任何格子
                if (rawSlot < topSize) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }
}

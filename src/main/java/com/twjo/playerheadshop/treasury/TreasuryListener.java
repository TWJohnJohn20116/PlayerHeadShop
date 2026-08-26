package com.twjo.playerheadshop.treasury;

import com.twjo.playerheadshop.PlayerHeadShop;
import com.twjo.playerheadshop.lang.LanguageManager;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 監聽管理員收益金庫 GUI 的互動、提領與關閉同步事件（實作嚴格「僅限提領 (Take-Only)」保護，禁止放入物品）
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

        if (event.getInventory().getHolder() instanceof TreasuryGuiHolder) {
            // 1. 若點擊的是玩家自身的背包 (下方背包)：
            if (event.getClickedInventory() != null && !(event.getClickedInventory().getHolder() instanceof TreasuryGuiHolder)) {
                // 禁止使用 Shift-Click 快速將物品送入金庫
                if (event.isShiftClick()) {
                    event.setCancelled(true);
                }
                return;
            }

            // 2. 若點擊的是上方金庫介面：
            if (event.getClickedInventory() != null && event.getClickedInventory().getHolder() instanceof TreasuryGuiHolder) {
                int slot = event.getSlot();

                // 2-1. 底排控制列按鈕 (Slots 45 ~ 53)
                if (!TreasuryGuiHolder.isItemStorageSlot(slot)) {
                    event.setCancelled(true);
                    handleControlButtonClick(admin, slot);
                    return;
                }

                // 2-2. 物品存放槽 (Slots 0 ~ 44) - 嚴格「僅限拿取 (Take-Only)」保護
                ItemStack cursorItem = event.getCursor();
                ItemStack currentItem = event.getCurrentItem();

                // 禁止游標持有任何物品時點擊金庫格子放入
                if (cursorItem != null && !cursorItem.getType().isAir()) {
                    event.setCancelled(true);
                    return;
                }

                // 禁止使用快捷鍵 1~9 將背包物品調換入金庫
                if (event.getClick() == ClickType.NUMBER_KEY ||
                    event.getAction() == InventoryAction.HOTBAR_SWAP ||
                    event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD) {
                    event.setCancelled(true);
                    return;
                }

                // 禁止任何帶有 PLACE 或 SWAP 的放置動作
                switch (event.getAction()) {
                    case PLACE_ALL, PLACE_SOME, PLACE_ONE, SWAP_WITH_CURSOR -> {
                        event.setCancelled(true);
                        return;
                    }
                    default -> {}
                }

                // 若點擊的是空槽位，直接取消點擊
                if (currentItem == null || currentItem.getType().isAir()) {
                    event.setCancelled(true);
                }

                // 純拿取動作 (PICKUP_ALL, PICKUP_HALF, PICKUP_ONE, Shift-Click 取出) 允許正常執行
            }
        }
    }

    private void handleControlButtonClick(Player admin, int slot) {
        // Slot 48: 提領 Vault 金幣
        if (slot == TreasuryGuiHolder.VAULT_SLOT) {
            double balance = treasuryManager.getVaultBalance();
            if (balance <= 0.0) {
                lang.sendMessage(admin, "treasury-empty-vault");
                try {
                    admin.playSound(admin.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                } catch (Throwable ignored) {}
                return;
            }

            if (treasuryManager.withdrawVault(admin, balance)) {
                plugin.getVaultHook().deposit(admin, balance);
                lang.sendMessage(admin, "treasury-withdraw-vault-success",
                        Placeholder.parsed("amount", plugin.getVaultHook().format(balance))
                );
                try {
                    admin.playSound(admin.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                } catch (Throwable ignored) {}
                treasuryGui.open(admin);
            }
        }
        // Slot 50: 提領經驗點數
        else if (slot == TreasuryGuiHolder.EXP_SLOT) {
            int exp = treasuryManager.getExpPoints();
            if (exp <= 0) {
                lang.sendMessage(admin, "treasury-empty-exp");
                try {
                    admin.playSound(admin.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                } catch (Throwable ignored) {}
                return;
            }

            if (treasuryManager.withdrawExp(admin, exp)) {
                lang.sendMessage(admin, "treasury-withdraw-exp-success",
                        Placeholder.parsed("amount", lang.formatExpPoints(admin, exp))
                );
                try {
                    admin.playSound(admin.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                } catch (Throwable ignored) {}
                treasuryGui.open(admin);
            }
        }
        // Slot 52: 查看稽核日誌
        else if (slot == TreasuryGuiHolder.LOGS_SLOT) {
            admin.closeInventory();
            Bukkit.dispatchCommand(admin, "buyhead pool logs 1");
        }
        // Slot 53: 重新整理
        else if (slot == TreasuryGuiHolder.REFRESH_SLOT) {
            treasuryGui.open(admin);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof TreasuryGuiHolder) {
            int topSize = event.getInventory().getSize();
            for (int rawSlot : event.getRawSlots()) {
                // 一律禁止拖曳放置物品至金庫上方任何格子
                if (rawSlot < topSize) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof TreasuryGuiHolder) {
            if (event.getPlayer() instanceof Player admin) {
                List<ItemStack> slots = new ArrayList<>();
                for (int i = 0; i < TreasuryGuiHolder.ITEM_STORAGE_LIMIT; i++) {
                    ItemStack stack = event.getInventory().getItem(i);
                    slots.add(stack != null ? stack.clone() : null);
                }
                treasuryManager.syncItemsFromGui(slots, admin);
            }
        }
    }
}

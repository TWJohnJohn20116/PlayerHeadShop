package com.twjo.playerheadshop.service;

import com.twjo.playerheadshop.PlayerHeadShop;
import com.twjo.playerheadshop.config.ShopOption;
import com.twjo.playerheadshop.database.DatabaseManager;
import com.twjo.playerheadshop.gui.DepositGuiHolder;
import com.twjo.playerheadshop.lang.LanguageManager;
import com.twjo.playerheadshop.util.ExperienceUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 處理玩家頭顱兌換、背包物品檢查與扣除、Vault 貨幣扣款、經驗等級/點數扣除、放置介面多格扣除、頭顱生成與日誌。
 */
public class HeadShopService {

    private final PlayerHeadShop plugin;
    private final LanguageManager lang;
    private final DatabaseManager databaseManager;

    public HeadShopService(PlayerHeadShop plugin, LanguageManager lang, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.lang = lang;
        this.databaseManager = databaseManager;
    }

    /**
     * 處理玩家在主選單點擊 Vault 貨幣方案的即時扣款購買
     */
    public boolean processVaultPurchase(Player player, ShopOption option) {
        if (player == null || !player.isOnline() || option == null) {
            return false;
        }

        if (!plugin.getVaultHook().hasEconomy()) {
            lang.sendMessage(player, "vault-disabled");
            try {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            } catch (Throwable ignored) {}
            return false;
        }

        double costAmount = option.getCostAmount();
        int headAmount = option.getHeadAmount();

        // 1. 檢查玩家 Vault 貨幣餘額
        if (!plugin.getVaultHook().has(player, costAmount)) {
            double current = plugin.getVaultHook().getBalance(player);
            double missing = costAmount - current;
            lang.sendMessage(player, "not-enough-money",
                    Placeholder.parsed("required", plugin.getVaultHook().format(costAmount)),
                    Placeholder.parsed("current", plugin.getVaultHook().format(current)),
                    Placeholder.parsed("missing", plugin.getVaultHook().format(missing))
            );
            try {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            } catch (Throwable ignored) {}
            return false;
        }

        // 2. 執行 Vault 扣款
        if (!plugin.getVaultHook().withdraw(player, costAmount)) {
            lang.sendMessage(player, "not-enough-money",
                    Placeholder.parsed("required", plugin.getVaultHook().format(costAmount)),
                    Placeholder.parsed("current", plugin.getVaultHook().format(plugin.getVaultHook().getBalance(player))),
                    Placeholder.parsed("missing", plugin.getVaultHook().format(costAmount))
            );
            return false;
        }

        // 3. 生成頭顱並發放
        giveHeads(player, headAmount, (int) Math.round(costAmount), "VAULT");

        // 4. 發送 Vault 專屬成功提示
        lang.sendMessage(player, "vault-success",
                Placeholder.parsed("cost_amount", plugin.getVaultHook().format(costAmount)),
                Placeholder.parsed("head_amount", lang.formatAmount(player, headAmount))
        );

        return true;
    }

    /**
     * 處理玩家點擊 EXP_LEVEL 經驗等級購買方案
     */
    public boolean processExpLevelPurchase(Player player, ShopOption option) {
        if (player == null || !player.isOnline() || option == null) {
            return false;
        }

        int requiredLevel = option.getCostAmountInt();
        int currentLevel = player.getLevel();
        int headAmount = option.getHeadAmount();

        if (currentLevel < requiredLevel) {
            int missingLevel = requiredLevel - currentLevel;
            lang.sendMessage(player, "not-enough-exp-level",
                    Placeholder.parsed("required", lang.formatExpLevel(player, requiredLevel)),
                    Placeholder.parsed("current", lang.formatExpLevel(player, currentLevel)),
                    Placeholder.parsed("missing", lang.formatExpLevel(player, missingLevel))
            );
            try {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            } catch (Throwable ignored) {}
            return false;
        }

        // 扣除等級
        player.setLevel(currentLevel - requiredLevel);

        // 發放頭顱
        giveHeads(player, headAmount, requiredLevel, "EXP_LEVEL");

        // 發送成功訊息
        lang.sendMessage(player, "exp-level-success",
                Placeholder.parsed("cost_amount", lang.formatExpLevel(player, requiredLevel)),
                Placeholder.parsed("head_amount", lang.formatAmount(player, headAmount))
        );

        return true;
    }

    /**
     * 處理玩家點擊 EXP_POINTS 經驗點數購買方案
     */
    public boolean processExpPointsPurchase(Player player, ShopOption option) {
        if (player == null || !player.isOnline() || option == null) {
            return false;
        }

        int requiredPoints = option.getCostAmountInt();
        int currentTotalPoints = ExperienceUtil.getPlayerTotalExp(player);
        int headAmount = option.getHeadAmount();

        if (currentTotalPoints < requiredPoints) {
            int missingPoints = requiredPoints - currentTotalPoints;
            lang.sendMessage(player, "not-enough-exp-points",
                    Placeholder.parsed("required", lang.formatExpPoints(player, requiredPoints)),
                    Placeholder.parsed("current", lang.formatExpPoints(player, currentTotalPoints)),
                    Placeholder.parsed("missing", lang.formatExpPoints(player, missingPoints))
            );
            try {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            } catch (Throwable ignored) {}
            return false;
        }

        // 安全扣除經驗點數並同步等級與經驗條
        ExperienceUtil.deductPlayerExp(player, requiredPoints);

        // 發放頭顱
        giveHeads(player, headAmount, requiredPoints, "EXP_POINTS");

        // 發送成功訊息
        lang.sendMessage(player, "exp-points-success",
                Placeholder.parsed("cost_amount", lang.formatExpPoints(player, requiredPoints)),
                Placeholder.parsed("head_amount", lang.formatAmount(player, headAmount))
        );

        return true;
    }

    /**
     * 處理玩家在主選單直接購買方案的請求
     */
    public boolean processPurchase(Player player, ShopOption option) {
        if (player == null || !player.isOnline() || option == null) {
            return false;
        }

        if (option.isVault()) {
            return processVaultPurchase(player, option);
        }

        if (option.isExpLevel()) {
            return processExpLevelPurchase(player, option);
        }

        if (option.isExpPoints()) {
            return processExpPointsPurchase(player, option);
        }

        Material costItem = option.getCostItem();
        int requiredAmount = option.getCostAmountInt();
        int headAmount = option.getHeadAmount();

        // 1. 檢查玩家背包中指定物品的持有總數
        int currentAmount = countItems(player, costItem);

        if (currentAmount < requiredAmount) {
            int missingAmount = requiredAmount - currentAmount;
            lang.sendMessage(player, "not-enough-items",
                    Placeholder.parsed("required", lang.formatAmount(player, requiredAmount)),
                    Placeholder.parsed("item", costItem.name()),
                    Placeholder.parsed("current", lang.formatAmount(player, currentAmount)),
                    Placeholder.parsed("missing", lang.formatAmount(player, missingAmount))
            );
            try {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            } catch (Throwable ignored) {}
            return false;
        }

        // 2. 安全扣除所需物品
        deductItems(player, costItem, requiredAmount);

        // 3. 生成印有該玩家皮膚的頭顱物品堆疊清單並發放
        giveHeads(player, headAmount, requiredAmount, costItem.name());

        // 發送物品購買成功提示
        lang.sendMessage(player, "success",
                Placeholder.parsed("cost_amount", lang.formatAmount(player, requiredAmount)),
                Placeholder.parsed("cost_item", costItem.name()),
                Placeholder.parsed("head_amount", lang.formatAmount(player, headAmount)),
                Placeholder.parsed("amount", String.valueOf(requiredAmount)),
                Placeholder.parsed("item", costItem.name())
        );

        return true;
    }

    /**
     * 處理玩家在放置兌換介面中點擊確認兌換的請求（支援多格累計計算，支援 > 64 個物品）
     */
    public boolean processDepositPurchase(Player player, DepositGuiHolder holder) {
        if (player == null || !player.isOnline() || holder == null) {
            return false;
        }

        ShopOption option = holder.getOption();
        if (option == null) {
            return false;
        }

        Material costItem = option.getCostItem();
        int requiredAmount = option.getCostAmountInt();
        int headAmount = option.getHeadAmount();

        // 1. 計算所有放置格 (Slots 10, 11, 12, 19, 20, 21) 內的目標物品總數
        int currentAmount = 0;
        for (int slot : DepositGuiHolder.INPUT_SLOTS) {
            ItemStack stack = holder.getInventory().getItem(slot);
            if (stack != null && stack.getType() == costItem) {
                currentAmount += stack.getAmount();
            }
        }

        if (currentAmount < requiredAmount) {
            int missingAmount = requiredAmount - currentAmount;
            lang.sendMessage(player, "not-enough-items",
                    Placeholder.parsed("required", lang.formatAmount(player, requiredAmount)),
                    Placeholder.parsed("item", costItem.name()),
                    Placeholder.parsed("current", lang.formatAmount(player, currentAmount)),
                    Placeholder.parsed("missing", lang.formatAmount(player, missingAmount))
            );
            try {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            } catch (Throwable ignored) {}
            return false;
        }

        // 2. 依序扣除放置格中的指定數量
        int remainingToDeduct = requiredAmount;
        for (int slot : DepositGuiHolder.INPUT_SLOTS) {
            ItemStack stack = holder.getInventory().getItem(slot);
            if (stack != null && stack.getType() == costItem) {
                int stackAmount = stack.getAmount();
                if (stackAmount <= remainingToDeduct) {
                    remainingToDeduct -= stackAmount;
                    holder.getInventory().setItem(slot, null);
                } else {
                    stack.setAmount(stackAmount - remainingToDeduct);
                    remainingToDeduct = 0;
                }

                if (remainingToDeduct <= 0) {
                    break;
                }
            }
        }

        // 3. 生成頭顱並發放
        giveHeads(player, headAmount, requiredAmount, costItem.name());

        // 發送物品購買成功提示
        lang.sendMessage(player, "success",
                Placeholder.parsed("cost_amount", lang.formatAmount(player, requiredAmount)),
                Placeholder.parsed("cost_item", costItem.name()),
                Placeholder.parsed("head_amount", lang.formatAmount(player, headAmount)),
                Placeholder.parsed("amount", String.valueOf(requiredAmount)),
                Placeholder.parsed("item", costItem.name())
        );

        return true;
    }

    /**
     * 生成頭顱並發放給玩家（處理背包溢出與非同步日誌記錄）
     */
    private void giveHeads(Player player, int headAmount, int costAmount, String costItemName) {
        List<ItemStack> headsToGive = createPlayerHeads(player, headAmount);

        boolean hasOverflow = false;
        for (ItemStack headStack : headsToGive) {
            HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(headStack);
            if (!overflow.isEmpty()) {
                hasOverflow = true;
                for (ItemStack leftover : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
            }
        }

        if (hasOverflow) {
            lang.sendMessage(player, "inventory-full");
        }

        try {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        } catch (Throwable ignored) {}

        // 非同步寫入交易歷史記錄至 SQLite
        if (databaseManager != null) {
            databaseManager.logTrade(player.getUniqueId(), player.getName(), costItemName, costAmount, headAmount);
        }
    }

    /**
     * 計算玩家背包 (Storage Contents) 中的指定 Material 總數
     */
    private int countItems(Player player, Material material) {
        int total = 0;
        ItemStack[] storage = player.getInventory().getStorageContents();
        for (ItemStack item : storage) {
            if (item != null && item.getType() == material) {
                total += item.getAmount();
            }
        }
        return total;
    }

    /**
     * 安全扣除玩家背包中的指定數量 Material
     */
    private void deductItems(Player player, Material material, int amountToDeduct) {
        ItemStack[] storage = player.getInventory().getStorageContents();
        int remaining = amountToDeduct;

        for (int i = 0; i < storage.length; i++) {
            ItemStack item = storage[i];
            if (item != null && item.getType() == material) {
                int stackAmount = item.getAmount();
                if (stackAmount <= remaining) {
                    remaining -= stackAmount;
                    storage[i] = null;
                } else {
                    item.setAmount(stackAmount - remaining);
                    remaining = 0;
                }

                if (remaining <= 0) {
                    break;
                }
            }
        }

        player.getInventory().setStorageContents(storage);
    }

    /**
     * 建立印有指定玩家皮膚外觀的頭顱物品列表（支援超過 64 個時自動拆分為多堆疊）
     */
    private List<ItemStack> createPlayerHeads(Player player, int totalAmount) {
        List<ItemStack> list = new ArrayList<>();
        int remaining = totalAmount;

        while (remaining > 0) {
            int currentStackSize = Math.min(64, remaining);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD, currentStackSize);
            if (head.getItemMeta() instanceof SkullMeta skullMeta) {
                try {
                    skullMeta.setPlayerProfile(player.getPlayerProfile());
                } catch (Throwable ignored) {
                    skullMeta.setOwningPlayer(player);
                }
                head.setItemMeta(skullMeta);
            }
            list.add(head);
            remaining -= currentStackSize;
        }

        return list;
    }
}

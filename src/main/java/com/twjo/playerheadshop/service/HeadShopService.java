package com.twjo.playerheadshop.service;

import com.twjo.playerheadshop.config.PluginConfig;
import com.twjo.playerheadshop.config.ShopOption;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 處理玩家頭顱兌換、背包物品檢查與扣除、頭顱生成與掉落邏輯。
 */
public class HeadShopService {

    private final PluginConfig config;

    public HeadShopService(PluginConfig config) {
        this.config = config;
    }

    /**
     * 處理玩家購買特定方案頭顱的請求
     *
     * @param player 執行購買的玩家
     * @param option 選擇的兌換方案
     * @return 購買成功返回 true，否則返回 false
     */
    public boolean processPurchase(Player player, ShopOption option) {
        if (player == null || !player.isOnline() || option == null) {
            return false;
        }

        Material costItem = option.getCostItem();
        int requiredAmount = option.getCostAmount();
        int headAmount = option.getHeadAmount();

        // 1. 檢查玩家背包中指定物品的持有總數
        int currentAmount = countItems(player, costItem);

        if (currentAmount < requiredAmount) {
            int missingAmount = requiredAmount - currentAmount;
            config.sendNotEnoughItems(player, requiredAmount, costItem.name(), currentAmount, missingAmount);
            return false;
        }

        // 2. 安全扣除所需物品
        deductItems(player, costItem, requiredAmount);

        // 3. 生成印有該玩家皮膚的頭顱物品堆疊清單
        List<ItemStack> headsToGive = createPlayerHeads(player, headAmount);

        // 4. 嘗試放入玩家背包，若背包已滿則安全掉落至玩家腳下
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
            config.sendInventoryFull(player);
        }

        // 5. 發送購買成功提示
        config.sendSuccess(player, requiredAmount, costItem.name(), headAmount);
        return true;
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
     * 建立印有指定玩家皮膚外觀的頭顱物品列表（支援超過 64 個時拆分為多堆疊）
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

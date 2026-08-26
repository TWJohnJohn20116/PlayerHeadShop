package com.twjo.playerheadshop.treasury;

import com.twjo.playerheadshop.PlayerHeadShop;
import com.twjo.playerheadshop.database.DatabaseManager;
import com.twjo.playerheadshop.util.ExperienceUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * 管理收益金庫的資產存取、實體物品槽、執行緒安全鎖與提領稽核記錄
 */
public class TreasuryManager {

    public static final int ITEM_STORAGE_SLOTS = 45; // 前 5 行 (0 ~ 44) 為物品槽

    private final PlayerHeadShop plugin;
    private final DatabaseManager databaseManager;

    private double vaultBalance = 0.0;
    private int expPoints = 0;
    private final List<ItemStack> items = new ArrayList<>(Collections.nCopies(ITEM_STORAGE_SLOTS, null));

    public TreasuryManager(PlayerHeadShop plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        loadData();
    }

    /**
     * 從資料庫非同步載入金庫資產
     */
    private void loadData() {
        databaseManager.loadTreasuryBalance().thenAccept(balance -> {
            synchronized (this) {
                this.vaultBalance = balance[0];
                this.expPoints = (int) Math.round(balance[1]);
            }
        });

        databaseManager.loadTreasuryItems(ITEM_STORAGE_SLOTS).thenAccept(loadedItems -> {
            synchronized (this) {
                for (int i = 0; i < ITEM_STORAGE_SLOTS && i < loadedItems.size(); i++) {
                    this.items.set(i, loadedItems.get(i));
                }
            }
        });
    }

    /**
     * 存入實體物品至金庫
     */
    public synchronized void depositItems(List<ItemStack> newItems) {
        if (newItems == null || newItems.isEmpty()) return;

        for (ItemStack newItem : newItems) {
            if (newItem == null || newItem.getType().isAir()) continue;
            ItemStack remaining = newItem.clone();

            // 1. 優先嘗試與相同物品堆疊
            for (int i = 0; i < ITEM_STORAGE_SLOTS; i++) {
                ItemStack existing = items.get(i);
                if (existing != null && existing.isSimilar(remaining)) {
                    int maxStack = existing.getMaxStackSize();
                    int space = maxStack - existing.getAmount();
                    if (space > 0) {
                        int add = Math.min(space, remaining.getAmount());
                        existing.setAmount(existing.getAmount() + add);
                        remaining.setAmount(remaining.getAmount() - add);
                        if (remaining.getAmount() <= 0) break;
                    }
                }
            }

            // 2. 若仍有剩餘，尋找空格放入
            if (remaining.getAmount() > 0) {
                for (int i = 0; i < ITEM_STORAGE_SLOTS; i++) {
                    if (items.get(i) == null || items.get(i).getType().isAir()) {
                        items.set(i, remaining.clone());
                        remaining.setAmount(0);
                        break;
                    }
                }
            }

            // 3. 若金庫已全滿，記錄日誌
            if (remaining.getAmount() > 0) {
                plugin.getLogger().warning("收益金庫物品槽已滿，未能完全存入: " + remaining.getType() + " x" + remaining.getAmount());
            }
        }

        saveDataAsync();
    }

    /**
     * 存入 Vault 金錢至金庫
     */
    public synchronized void depositVault(double amount) {
        if (amount <= 0) return;
        this.vaultBalance += amount;
        saveDataAsync();
    }

    /**
     * 存入經驗點數至金庫
     */
    public synchronized void depositExp(int points) {
        if (points <= 0) return;
        this.expPoints += points;
        saveDataAsync();
    }

    /**
     * 管理員提領 Vault 金幣（自動寫入稽核表記錄）
     */
    public synchronized boolean withdrawVault(Player admin, double amount) {
        if (admin == null || amount <= 0 || vaultBalance < amount) {
            return false;
        }

        vaultBalance -= amount;
        saveDataAsync();

        // 記錄提領稽核日誌
        databaseManager.logTreasuryWithdrawal(admin.getUniqueId(), admin.getName(), "WITHDRAW_VAULT", "VAULT", amount);
        plugin.getLogger().info("[Audit] 管理員 " + admin.getName() + " 從收益金庫提領了 " + plugin.getVaultHook().format(amount) + " 金幣。");

        return true;
    }

    /**
     * 管理員提領經驗點數（自動寫入稽核表記錄）
     */
    public synchronized boolean withdrawExp(Player admin, int points) {
        if (admin == null || points <= 0 || expPoints < points) {
            return false;
        }

        expPoints -= points;
        saveDataAsync();

        // 注入經驗值給管理員
        ExperienceUtil.setPlayerTotalExp(admin, ExperienceUtil.getPlayerTotalExp(admin) + points);

        // 記錄提領稽核日誌
        databaseManager.logTreasuryWithdrawal(admin.getUniqueId(), admin.getName(), "WITHDRAW_EXP", "EXP_POINTS", points);
        plugin.getLogger().info("[Audit] 管理員 " + admin.getName() + " 從收益金庫提領了 " + points + " 點經驗值。");

        return true;
    }

    /**
     * 同步管理員關閉 GUI 後的物品槽變更，並比對計算拿取物品寫入稽核日誌
     */
    public synchronized void syncItemsFromGui(List<ItemStack> newSlots, Player admin) {
        if (newSlots == null) return;

        Map<String, Integer> oldCounts = countItems(this.items);
        Map<String, Integer> newCounts = countItems(newSlots);

        // 比對減少的物品並寫入稽核日誌
        for (Map.Entry<String, Integer> entry : oldCounts.entrySet()) {
            String matName = entry.getKey();
            int oldCount = entry.getValue();
            int newCount = newCounts.getOrDefault(matName, 0);
            if (oldCount > newCount) {
                int taken = oldCount - newCount;
                databaseManager.logTreasuryWithdrawal(admin.getUniqueId(), admin.getName(), "WITHDRAW_ITEM", matName, taken);
                plugin.getLogger().info("[Audit] 管理員 " + admin.getName() + " 從收益金庫取出了 " + taken + " 個 " + matName + "。");
            }
        }

        // 更新儲存
        for (int i = 0; i < ITEM_STORAGE_SLOTS && i < newSlots.size(); i++) {
            ItemStack s = newSlots.get(i);
            this.items.set(i, (s != null && !s.getType().isAir()) ? s.clone() : null);
        }

        saveDataAsync();
    }

    private Map<String, Integer> countItems(List<ItemStack> list) {
        Map<String, Integer> map = new HashMap<>();
        for (ItemStack stack : list) {
            if (stack != null && !stack.getType().isAir()) {
                String key = stack.getType().name();
                map.put(key, map.getOrDefault(key, 0) + stack.getAmount());
            }
        }
        return map;
    }

    public synchronized double getVaultBalance() {
        return vaultBalance;
    }

    public synchronized int getExpPoints() {
        return expPoints;
    }

    public synchronized List<ItemStack> getItemsSnapshot() {
        List<ItemStack> copy = new ArrayList<>();
        for (ItemStack item : items) {
            copy.add(item != null ? item.clone() : null);
        }
        return copy;
    }

    public synchronized void saveDataAsync() {
        databaseManager.saveTreasuryBalance(this.vaultBalance, this.expPoints);
        databaseManager.saveTreasuryItems(new ArrayList<>(this.items));
    }
}

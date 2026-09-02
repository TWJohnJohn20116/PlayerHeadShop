package com.twjo.playerheadshop.treasury;

import com.twjo.playerheadshop.PlayerHeadShop;
import com.twjo.playerheadshop.database.DatabaseManager;
import com.twjo.playerheadshop.util.ExperienceUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * 管理收益金庫的資產存取、實體物品槽、執行緒安全鎖與提領稽核記錄
 *
 * <p>物品槽以本類別內的 {@link #items} 為唯一真相來源 (single source of truth)。GUI 僅為唯讀投影，
 * 任何提領都必須經由 {@link #withdrawItem} 即時扣減，絕不接受把 GUI 內容整批回寫。</p>
 */
public class TreasuryManager {

    public static final int ITEM_STORAGE_SLOTS = 45; // 前 5 行 (0 ~ 44) 為物品槽

    private final PlayerHeadShop plugin;
    private final DatabaseManager databaseManager;

    private double vaultBalance = 0.0;
    private int expPoints = 0;
    private final List<ItemStack> items = new ArrayList<>(Collections.nCopies(ITEM_STORAGE_SLOTS, null));

    /** 物品槽世代編號：任何內容變動都會遞增，供已開啟的 GUI 偵測快照是否過期 */
    private long itemsGeneration = 0L;

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
                this.itemsGeneration++;
            }
            notifyItemsChanged();
        });
    }

    /**
     * 存入實體物品至金庫
     */
    public void depositItems(List<ItemStack> newItems) {
        if (newItems == null || newItems.isEmpty()) return;

        synchronized (this) {
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
                while (remaining.getAmount() > 0) {
                    int freeSlot = -1;
                    for (int i = 0; i < ITEM_STORAGE_SLOTS; i++) {
                        if (items.get(i) == null || items.get(i).getType().isAir()) {
                            freeSlot = i;
                            break;
                        }
                    }
                    if (freeSlot < 0) break;

                    ItemStack placed = remaining.clone();
                    int put = Math.min(remaining.getAmount(), placed.getMaxStackSize());
                    placed.setAmount(put);
                    items.set(freeSlot, placed);
                    remaining.setAmount(remaining.getAmount() - put);
                }

                // 3. 若金庫已全滿，記錄日誌
                if (remaining.getAmount() > 0) {
                    plugin.getLogger().warning("收益金庫物品槽已滿，未能完全存入: " + remaining.getType() + " x" + remaining.getAmount());
                }
            }

            this.itemsGeneration++;
        }

        saveDataAsync();
        notifyItemsChanged();
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
     * 將先前扣除的金幣退回金庫（供 Vault 發放失敗時回滾使用，不寫入稽核日誌）
     */
    public synchronized void refundVault(double amount) {
        if (amount <= 0) return;
        this.vaultBalance += amount;
        saveDataAsync();
        plugin.getLogger().warning("[Audit] Vault 發放失敗，已將 " + plugin.getVaultHook().format(amount) + " 退回收益金庫。");
    }

    /**
     * 將先前扣除的經驗點數退回金庫（供經驗注入失敗時回滾使用，不寫入稽核日誌）
     */
    public synchronized void refundExp(int points) {
        if (points <= 0) return;
        this.expPoints += points;
        saveDataAsync();
        plugin.getLogger().warning("[Audit] 經驗發放失敗，已將 " + points + " 點經驗退回收益金庫。");
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
     * 管理員即時提領單一物品槽的內容（Compare-And-Swap 語意）
     *
     * <p>{@code expected} 為管理員在 GUI 上實際看到的物品；若與金庫現況不符（例如期間有玩家購買而存入
     * 新物品、或另一位管理員已先取走），本次提領會被拒絕並回傳 {@code null}，呼叫端應重新渲染 GUI。</p>
     *
     * @param halfOnly 是否僅取出一半數量（對應右鍵點擊）
     * @return 實際自金庫取出的物品，或 {@code null} 表示快照過期 / 該槽為空
     */
    public synchronized ItemStack withdrawItem(Player admin, int slot, ItemStack expected, boolean halfOnly) {
        if (admin == null || slot < 0 || slot >= ITEM_STORAGE_SLOTS) {
            return null;
        }

        ItemStack current = items.get(slot);
        if (current == null || current.getType().isAir()) {
            return null;
        }

        // 快照校驗：型別/Meta 與數量都必須與管理員看到的一致，否則視為過期
        if (expected == null
                || !current.isSimilar(expected)
                || current.getAmount() != expected.getAmount()) {
            return null;
        }

        int takeAmount = halfOnly ? Math.max(1, current.getAmount() / 2) : current.getAmount();

        ItemStack taken = current.clone();
        taken.setAmount(takeAmount);

        int leftover = current.getAmount() - takeAmount;
        if (leftover > 0) {
            ItemStack remain = current.clone();
            remain.setAmount(leftover);
            items.set(slot, remain);
        } else {
            items.set(slot, null);
        }

        this.itemsGeneration++;
        saveDataAsync();

        String matName = taken.getType().name();
        databaseManager.logTreasuryWithdrawal(admin.getUniqueId(), admin.getName(), "WITHDRAW_ITEM", matName, takeAmount);
        plugin.getLogger().info("[Audit] 管理員 " + admin.getName() + " 從收益金庫取出了 " + takeAmount + " 個 " + matName + "。");

        return taken;
    }

    public synchronized double getVaultBalance() {
        return vaultBalance;
    }

    public synchronized int getExpPoints() {
        return expPoints;
    }

    public synchronized long getItemsGeneration() {
        return itemsGeneration;
    }

    public synchronized List<ItemStack> getItemsSnapshot() {
        List<ItemStack> copy = new ArrayList<>();
        for (ItemStack item : items) {
            copy.add(item != null ? item.clone() : null);
        }
        return copy;
    }

    /**
     * 通知所有已開啟的金庫 GUI 重新載入最新物品內容，避免管理員對著過期畫面操作
     */
    private void notifyItemsChanged() {
        TreasuryGui gui = plugin.getTreasuryGui();
        if (gui != null) {
            gui.refreshOpenViews();
        }
    }

    public synchronized void saveDataAsync() {
        databaseManager.saveTreasuryBalance(this.vaultBalance, this.expPoints);
        databaseManager.saveTreasuryItems(new ArrayList<>(this.items));
    }
}

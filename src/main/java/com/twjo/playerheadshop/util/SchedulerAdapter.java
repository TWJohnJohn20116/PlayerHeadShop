package com.twjo.playerheadshop.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

/**
 * 排程器抽象層：自動偵測 Folia 並改用區域化排程器 (GlobalRegionScheduler / EntityScheduler /
 * RegionScheduler)，在傳統 Paper / Spigot 上則沿用 BukkitScheduler。
 *
 * <p>Folia 已移除 {@code Bukkit.getScheduler().runTask(...)}，直接呼叫會拋出
 * {@link UnsupportedOperationException}，因此插件內所有主執行緒排程都必須經由此類別。</p>
 */
public final class SchedulerAdapter {

    private final Plugin plugin;
    private final boolean folia;

    public SchedulerAdapter(Plugin plugin) {
        this.plugin = plugin;
        this.folia = detectFolia();
    }

    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public boolean isFolia() {
        return folia;
    }

    /**
     * 於全域 (伺服器層級) 執行緒執行任務。適用於不綁定任何實體或座標的工作。
     */
    public void runGlobal(Runnable task) {
        if (task == null) return;
        if (folia) {
            Bukkit.getGlobalRegionScheduler().run(plugin, ignored -> task.run());
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * 延遲指定 tick 後於全域執行緒執行任務（delayTicks 會自動下限修正為 1）。
     */
    public void runGlobalLater(Runnable task, long delayTicks) {
        if (task == null) return;
        long delay = Math.max(1L, delayTicks);
        if (folia) {
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, ignored -> task.run(), delay);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delay);
        }
    }

    /**
     * 於「擁有該實體的區域執行緒」執行任務。所有操作玩家的行為（開介面、播音效、給物品）都應走此方法。
     *
     * <p>若該實體在任務執行前已被移除 / 玩家已離線，任務會被靜默丟棄。</p>
     */
    public void runForEntity(Entity entity, Runnable task) {
        if (entity == null || task == null) return;
        if (folia) {
            entity.getScheduler().run(plugin, ignored -> task.run(), null);
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * 延遲指定 tick 後於擁有該實體的區域執行緒執行任務（delayTicks 會自動下限修正為 1）。
     */
    public void runForEntityLater(Entity entity, Runnable task, long delayTicks) {
        if (entity == null || task == null) return;
        long delay = Math.max(1L, delayTicks);
        if (folia) {
            entity.getScheduler().runDelayed(plugin, ignored -> task.run(), null, delay);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delay);
        }
    }

    /**
     * 於「擁有該座標所在區塊的區域執行緒」執行任務。所有讀寫方塊的行為都應走此方法。
     */
    public void runForRegion(Location location, Runnable task) {
        if (location == null || task == null) return;
        if (folia) {
            Bukkit.getRegionScheduler().run(plugin, location, ignored -> task.run());
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * 延遲指定 tick 後於擁有該座標的區域執行緒執行任務（delayTicks 會自動下限修正為 1）。
     */
    public void runForRegionLater(Location location, Runnable task, long delayTicks) {
        if (location == null || task == null) return;
        long delay = Math.max(1L, delayTicks);
        if (folia) {
            Bukkit.getRegionScheduler().runDelayed(plugin, location, ignored -> task.run(), delay);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delay);
        }
    }
}

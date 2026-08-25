package com.twjo.playerheadshop.util;

import org.bukkit.entity.Player;

/**
 * 處理 Minecraft 原生玩家經驗值精確計算、等級換算與扣除的工具類別
 */
public final class ExperienceUtil {

    private ExperienceUtil() {}

    /**
     * 獲取升至下一級所需的經驗點數
     */
    public static int getExpToNextLevel(int level) {
        if (level <= 15) {
            return 2 * level + 7;
        } else if (level <= 30) {
            return 5 * level - 38;
        } else {
            return 9 * level - 158;
        }
    }

    /**
     * 獲取升至指定等級所需的累積總經驗點數
     */
    public static int getTotalExpToLevel(int level) {
        if (level <= 0) {
            return 0;
        }
        if (level <= 16) {
            return level * level + 6 * level;
        } else if (level <= 31) {
            return (int) Math.round(2.5 * level * level - 40.5 * level + 360);
        } else {
            return (int) Math.round(4.5 * level * level - 162.5 * level + 2220);
        }
    }

    /**
     * 計算玩家當前擁有的精確總經驗點數 (Total Experience Points)
     */
    public static int getPlayerTotalExp(Player player) {
        if (player == null) return 0;
        int level = player.getLevel();
        int baseExp = getTotalExpToLevel(level);
        int progressExp = Math.round(player.getExp() * getExpToNextLevel(level));
        return baseExp + progressExp;
    }

    /**
     * 精確設定玩家的總經驗點數，並自動同步其等級 (Level) 與經驗進度條 (Exp Bar)
     */
    public static void setPlayerTotalExp(Player player, int totalExp) {
        if (player == null) return;
        int exp = Math.max(0, totalExp);

        // 1. 換算對應的等級
        int level = 0;
        while (getTotalExpToLevel(level + 1) <= exp) {
            level++;
        }

        // 2. 計算該等級內的進度餘額
        int baseExp = getTotalExpToLevel(level);
        int remainder = exp - baseExp;
        int requiredForNext = getExpToNextLevel(level);
        float progress = (requiredForNext > 0) ? ((float) remainder / (float) requiredForNext) : 0.0f;

        // 3. 更新玩家狀態
        player.setLevel(level);
        player.setExp(Math.min(0.9999f, Math.max(0.0f, progress)));
        player.setTotalExperience(exp);
    }

    /**
     * 安全扣除玩家指定的經驗點數
     */
    public static boolean deductPlayerExp(Player player, int pointsToDeduct) {
        if (player == null || pointsToDeduct <= 0) return false;
        int currentTotal = getPlayerTotalExp(player);
        if (currentTotal < pointsToDeduct) {
            return false;
        }
        setPlayerTotalExp(player, currentTotal - pointsToDeduct);
        return true;
    }
}

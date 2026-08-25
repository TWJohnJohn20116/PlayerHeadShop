package com.twjo.playerheadshop.config;

import com.twjo.playerheadshop.PlayerHeadShop;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.logging.Level;

/**
 * 管理 PlayerHeadShop 的主設定檔讀取與方案驗證
 */
public class PluginConfig {

    private final PlayerHeadShop plugin;

    private volatile String language = "auto";
    private volatile int guiRows = 3;
    private volatile boolean fillerEnabled = true;
    private volatile Material fillerMaterial = Material.GRAY_STAINED_GLASS_PANE;
    private volatile String fillerDisplayName = " ";

    private volatile Map<Integer, ShopOption> options = Collections.emptyMap();

    public PluginConfig(PlayerHeadShop plugin) {
        this.plugin = plugin;
        reload();
    }

    /**
     * 重新加載主設定檔
     */
    public synchronized void reload() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        // 1. 讀取語言設定
        this.language = config.getString("language", "auto");

        // 2. 讀取 GUI 設定
        int rows = config.getInt("gui.rows", 3);
        this.guiRows = Math.max(1, Math.min(6, rows));

        this.fillerEnabled = config.getBoolean("gui.filler.enabled", true);
        String fillerMatStr = config.getString("gui.filler.material", "GRAY_STAINED_GLASS_PANE");
        Material matchedFiller = Material.matchMaterial(fillerMatStr);
        this.fillerMaterial = (matchedFiller != null && !matchedFiller.isAir()) ? matchedFiller : Material.GRAY_STAINED_GLASS_PANE;
        this.fillerDisplayName = config.getString("gui.filler.display-name", " ");

        // 3. 讀取 options 兌換方案清單
        Map<Integer, ShopOption> newOptions = new LinkedHashMap<>();
        int maxSlots = this.guiRows * 9;

        if (config.isList("options")) {
            List<Map<?, ?>> rawList = config.getMapList("options");
            for (Map<?, ?> entry : rawList) {
                try {
                    int slot = entry.containsKey("slot") ? ((Number) entry.get("slot")).intValue() : -1;
                    if (slot < 0 || slot >= maxSlots) {
                        plugin.getLogger().warning("方案 Slot 設定超出邊界 (0 ~ " + (maxSlots - 1) + "): " + slot + "，已忽略此方案。");
                        continue;
                    }

                    String displayName = entry.containsKey("display-name") ? String.valueOf(entry.get("display-name")) : null;
                    List<String> lore = null;
                    if (entry.containsKey("lore") && entry.get("lore") instanceof List<?> loreList) {
                        lore = new ArrayList<>();
                        for (Object line : loreList) {
                            lore.add(String.valueOf(line));
                        }
                    }

                    String costItemStr = entry.containsKey("cost-item") ? String.valueOf(entry.get("cost-item")) : "DIAMOND";
                    Material costItem = Material.matchMaterial(costItemStr);
                    if (costItem == null || costItem.isAir()) {
                        plugin.getLogger().warning("方案 Slot " + slot + " 消耗物品無效: " + costItemStr + "，已自動回退為 DIAMOND。");
                        costItem = Material.DIAMOND;
                    }

                    int costAmount = entry.containsKey("cost-amount") ? ((Number) entry.get("cost-amount")).intValue() : 1;
                    int headAmount = entry.containsKey("head-amount") ? ((Number) entry.get("head-amount")).intValue() : 1;

                    ShopOption option = new ShopOption(slot, displayName, lore, costItem, costAmount, headAmount);
                    newOptions.put(slot, option);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "解析兌換方案時發生錯誤: " + entry, e);
                }
            }
        } else if (config.isConfigurationSection("options")) {
            ConfigurationSection section = config.getConfigurationSection("options");
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    int slot = section.getInt(key + ".slot", -1);
                    if (slot < 0 || slot >= maxSlots) continue;

                    String displayName = section.getString(key + ".display-name", null);
                    List<String> lore = section.contains(key + ".lore") ? section.getStringList(key + ".lore") : null;
                    String costItemStr = section.getString(key + ".cost-item", "DIAMOND");
                    Material costItem = Material.matchMaterial(costItemStr);
                    if (costItem == null || costItem.isAir()) {
                        costItem = Material.DIAMOND;
                    }
                    int costAmount = section.getInt(key + ".cost-amount", 1);
                    int headAmount = section.getInt(key + ".head-amount", 1);

                    ShopOption option = new ShopOption(slot, displayName, lore, costItem, costAmount, headAmount);
                    newOptions.put(slot, option);
                }
            }
        }

        // 若無任何方案，建立預設方案
        if (newOptions.isEmpty()) {
            newOptions.put(11, new ShopOption(11, null, null, Material.DIAMOND, 1, 1));
            newOptions.put(13, new ShopOption(13, null, null, Material.DIAMOND, 7, 8));
            newOptions.put(15, new ShopOption(15, null, null, Material.DIAMOND, 50, 64));
        }

        this.options = Collections.unmodifiableMap(newOptions);
    }

    public String getLanguage() {
        return language;
    }

    public int getGuiRows() {
        return guiRows;
    }

    public boolean isFillerEnabled() {
        return fillerEnabled;
    }

    public Material getFillerMaterial() {
        return fillerMaterial;
    }

    public String getFillerDisplayName() {
        return fillerDisplayName;
    }

    public Map<Integer, ShopOption> getOptions() {
        return options;
    }

    public ShopOption getOption(int slot) {
        return options.get(slot);
    }
}

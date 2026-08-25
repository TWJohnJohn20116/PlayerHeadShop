package com.twjo.playerheadshop.config;

import com.twjo.playerheadshop.PlayerHeadShop;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.logging.Level;

/**
 * 管理 PlayerHeadShop 的主設定檔讀取、多種支付模式解析與 GUI 智慧自動排版
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
     * 重新加載主設定檔與方案
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
        int maxSlots = this.guiRows * 9;
        List<ShopOption> manualOptions = new ArrayList<>();
        List<ShopOption> autoOptions = new ArrayList<>();

        if (config.isList("options")) {
            List<Map<?, ?>> rawList = config.getMapList("options");
            for (Map<?, ?> entry : rawList) {
                try {
                    ShopOption option = parseOptionFromMap(entry, maxSlots);
                    if (option != null) {
                        if (option.isAutoSlot()) {
                            autoOptions.add(option);
                        } else {
                            manualOptions.add(option);
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "解析兌換方案時發生錯誤: " + entry, e);
                }
            }
        } else if (config.isConfigurationSection("options")) {
            ConfigurationSection section = config.getConfigurationSection("options");
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    try {
                        ShopOption option = parseOptionFromSection(section.getConfigurationSection(key), maxSlots);
                        if (option != null) {
                            if (option.isAutoSlot()) {
                                autoOptions.add(option);
                            } else {
                                manualOptions.add(option);
                            }
                        }
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "解析兌換方案時發生錯誤: " + key, e);
                    }
                }
            }
        }

        // 4. 計算自動排版位置
        Map<Integer, ShopOption> finalOptions = calculateLayout(manualOptions, autoOptions, maxSlots);

        // 若無任何方案，建立預設方案
        if (finalOptions.isEmpty()) {
            finalOptions.put(11, new ShopOption(11, ShopOption.CostType.ITEM, null, null, Material.DIAMOND, 1, 1));
            finalOptions.put(13, new ShopOption(13, ShopOption.CostType.ITEM, null, null, Material.DIAMOND, 7, 8));
            finalOptions.put(15, new ShopOption(15, ShopOption.CostType.ITEM, null, null, Material.DIAMOND, 50, 64));
        }

        this.options = Collections.unmodifiableMap(finalOptions);
    }

    /**
     * 從 Map 解析單一方案
     */
    private ShopOption parseOptionFromMap(Map<?, ?> entry, int maxSlots) {
        int slot = entry.containsKey("slot") ? ((Number) entry.get("slot")).intValue() : -1;
        if (slot >= maxSlots) {
            plugin.getLogger().warning("方案 Slot 設定超出邊界 (0 ~ " + (maxSlots - 1) + "): " + slot + "，已轉為自動排版。");
            slot = -1;
        }

        // 判斷支付類型 (ITEM, VAULT, EXP_LEVEL, EXP_POINTS)
        String typeStr = entry.containsKey("cost-type") ? String.valueOf(entry.get("cost-type")).toUpperCase() : "ITEM";
        String costItemStr = entry.containsKey("cost-item") ? String.valueOf(entry.get("cost-item")).toUpperCase() : "DIAMOND";

        ShopOption.CostType costType = resolveCostType(typeStr, costItemStr);

        Material costItem = Material.matchMaterial(costItemStr);
        if (costType == ShopOption.CostType.ITEM && (costItem == null || costItem.isAir())) {
            plugin.getLogger().warning("方案消耗物品無效: " + costItemStr + "，已自動回退為 DIAMOND。");
            costItem = Material.DIAMOND;
        }

        double costAmount = entry.containsKey("cost-amount") ? ((Number) entry.get("cost-amount")).doubleValue() : 1.0;
        int headAmount = entry.containsKey("head-amount") ? ((Number) entry.get("head-amount")).intValue() : 1;

        String displayName = entry.containsKey("display-name") ? String.valueOf(entry.get("display-name")) : null;
        List<String> lore = null;
        if (entry.containsKey("lore") && entry.get("lore") instanceof List<?> loreList) {
            lore = new ArrayList<>();
            for (Object line : loreList) {
                lore.add(String.valueOf(line));
            }
        }

        return new ShopOption(slot, costType, displayName, lore, costItem, costAmount, headAmount);
    }

    /**
     * 從 ConfigurationSection 解析單一方案
     */
    private ShopOption parseOptionFromSection(ConfigurationSection sec, int maxSlots) {
        if (sec == null) return null;
        int slot = sec.contains("slot") ? sec.getInt("slot") : -1;
        if (slot >= maxSlots) {
            slot = -1;
        }

        String typeStr = sec.getString("cost-type", "ITEM").toUpperCase();
        String costItemStr = sec.getString("cost-item", "DIAMOND").toUpperCase();

        ShopOption.CostType costType = resolveCostType(typeStr, costItemStr);

        Material costItem = Material.matchMaterial(costItemStr);
        if (costType == ShopOption.CostType.ITEM && (costItem == null || costItem.isAir())) {
            costItem = Material.DIAMOND;
        }

        double costAmount = sec.getDouble("cost-amount", 1.0);
        int headAmount = sec.getInt("head-amount", 1);
        String displayName = sec.getString("display-name", null);
        List<String> lore = sec.contains("lore") ? sec.getStringList("lore") : null;

        return new ShopOption(slot, costType, displayName, lore, costItem, costAmount, headAmount);
    }

    private ShopOption.CostType resolveCostType(String typeStr, String costItemStr) {
        if (typeStr.equals("VAULT") || typeStr.equals("MONEY") || typeStr.equals("ECONOMY")
                || costItemStr.equals("VAULT") || costItemStr.equals("MONEY") || costItemStr.equals("ECONOMY")) {
            return ShopOption.CostType.VAULT;
        }
        if (typeStr.equals("EXP_LEVEL") || typeStr.equals("LEVEL") || typeStr.equals("EXP_LVL") || typeStr.equals("LVL")
                || costItemStr.equals("EXP_LEVEL") || costItemStr.equals("LEVEL") || costItemStr.equals("LVL")) {
            return ShopOption.CostType.EXP_LEVEL;
        }
        if (typeStr.equals("EXP_POINTS") || typeStr.equals("EXP") || typeStr.equals("POINTS") || typeStr.equals("XP")
                || costItemStr.equals("EXP_POINTS") || costItemStr.equals("EXP") || costItemStr.equals("POINTS") || costItemStr.equals("XP")) {
            return ShopOption.CostType.EXP_POINTS;
        }
        return ShopOption.CostType.ITEM;
    }

    /**
     * 智慧排版計算：結合手動指定 Slot 與未填寫 Slot 的最佳美觀佈局
     */
    private Map<Integer, ShopOption> calculateLayout(List<ShopOption> manualOptions, List<ShopOption> autoOptions, int maxSlots) {
        Map<Integer, ShopOption> resultMap = new LinkedHashMap<>();
        Set<Integer> occupiedSlots = new HashSet<>();

        // 1. 先置入手動指定的方案
        for (ShopOption opt : manualOptions) {
            if (opt.getSlot() >= 0 && opt.getSlot() < maxSlots) {
                resultMap.put(opt.getSlot(), opt);
                occupiedSlots.add(opt.getSlot());
            }
        }

        if (autoOptions.isEmpty()) {
            return resultMap;
        }

        // 2. 針對自動排版方案，計算最佳放置位置
        List<Integer> preferredSlots = getPreferredSlots(autoOptions.size(), this.guiRows);
        int autoIdx = 0;

        // 優先嘗試對稱推薦位置
        for (int candidate : preferredSlots) {
            if (autoIdx >= autoOptions.size()) break;
            if (!occupiedSlots.contains(candidate) && candidate >= 0 && candidate < maxSlots) {
                ShopOption opt = autoOptions.get(autoIdx++);
                resultMap.put(candidate, opt.withSlot(candidate));
                occupiedSlots.add(candidate);
            }
        }

        // 若還有剩餘未放下的方案，依序填補非邊框空位
        if (autoIdx < autoOptions.size()) {
            for (int r = 1; r < this.guiRows - 1; r++) {
                for (int c = 1; c < 8; c++) {
                    int s = r * 9 + c;
                    if (autoIdx >= autoOptions.size()) break;
                    if (!occupiedSlots.contains(s) && s < maxSlots) {
                        ShopOption opt = autoOptions.get(autoIdx++);
                        resultMap.put(s, opt.withSlot(s));
                        occupiedSlots.add(s);
                    }
                }
            }
        }

        // 最後若依然放不下，填入任意剩餘空格
        if (autoIdx < autoOptions.size()) {
            for (int s = 0; s < maxSlots; s++) {
                if (autoIdx >= autoOptions.size()) break;
                if (!occupiedSlots.contains(s)) {
                    ShopOption opt = autoOptions.get(autoIdx++);
                    resultMap.put(s, opt.withSlot(s));
                    occupiedSlots.add(s);
                }
            }
        }

        return resultMap;
    }

    /**
     * 根據數量與行數生成最美觀的置中/對稱候選 Slot 清單
     */
    private List<Integer> getPreferredSlots(int count, int rows) {
        int midRow = rows / 2;
        int rowStart = midRow * 9;

        return switch (count) {
            case 1 -> List.of(rowStart + 4);
            case 2 -> List.of(rowStart + 2, rowStart + 6);
            case 3 -> List.of(rowStart + 2, rowStart + 4, rowStart + 6);
            case 4 -> List.of(rowStart + 1, rowStart + 3, rowStart + 5, rowStart + 7);
            case 5 -> List.of(rowStart + 2, rowStart + 3, rowStart + 4, rowStart + 5, rowStart + 6);
            case 6 -> List.of(rowStart + 1, rowStart + 2, rowStart + 3, rowStart + 5, rowStart + 6, rowStart + 7);
            case 7 -> List.of(rowStart + 1, rowStart + 2, rowStart + 3, rowStart + 4, rowStart + 5, rowStart + 6, rowStart + 7);
            default -> {
                List<Integer> list = new ArrayList<>();
                for (int r = 1; r < rows - 1; r++) {
                    for (int c = 1; c < 8; c++) {
                        list.add(r * 9 + c);
                    }
                }
                yield list;
            }
        };
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

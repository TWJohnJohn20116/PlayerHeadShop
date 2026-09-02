package com.twjo.playerheadshop.config;

import com.twjo.playerheadshop.PlayerHeadShop;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;

/**
 * 管理 PlayerHeadShop 的主設定檔讀取、收益金庫、社群市集、GUI 智慧自動排版與版本自動同步
 */
public class PluginConfig {

    private final PlayerHeadShop plugin;

    private volatile String language = "auto";
    private volatile int guiRows = 3;
    private volatile boolean fillerEnabled = true;
    private volatile Material fillerMaterial = Material.GRAY_STAINED_GLASS_PANE;
    private volatile String fillerDisplayName = " ";

    // 收益金庫設定
    private volatile boolean poolEnabled = true;
    private volatile boolean poolCollectItems = true;
    private volatile boolean poolCollectVault = true;
    private volatile boolean poolCollectExp = true;

    // 社群市集設定
    private volatile boolean marketEnabled = true;
    private volatile int marketMaxListings = 5;
    private volatile double marketSellerPayoutPercent = 100.0;
    private volatile ShopOption.CostType marketDefaultCostType = ShopOption.CostType.ITEM;
    private volatile Material marketDefaultCostItem = Material.DIAMOND;
    private volatile double marketDefaultCostAmount = 1.0;
    private volatile int marketDefaultHeadAmount = 1;

    private volatile Map<Integer, ShopOption> options = Collections.emptyMap();

    public PluginConfig(PlayerHeadShop plugin) {
        this.plugin = plugin;
        reload();
    }

    /**
     * 重新加載主設定檔與方案，並自動增量同步新版本設定項目
     */
    public synchronized void reload() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        // 0. 自動增量同步缺失的新版本設定項至本地 config.yml
        syncConfigFile(config);

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

        // 3. 讀取收益金庫設定
        this.poolEnabled = config.getBoolean("pool.enabled", true);
        this.poolCollectItems = config.getBoolean("pool.collect-items", true);
        this.poolCollectVault = config.getBoolean("pool.collect-vault", true);
        this.poolCollectExp = config.getBoolean("pool.collect-exp", true);

        // 4. 讀取社群市集設定
        this.marketEnabled = config.getBoolean("market.enabled", true);
        this.marketMaxListings = config.getInt("market.max-listings-per-player", 5);
        double payout = config.getDouble("market.seller-payout-percent", 100.0);
        this.marketSellerPayoutPercent = Math.max(0.0, Math.min(100.0, payout));
        String defCostTypeStr = config.getString("market.default-price.cost-type", "ITEM").toUpperCase();
        String defCostItemStr = config.getString("market.default-price.cost-item", "DIAMOND").toUpperCase();
        this.marketDefaultCostType = resolveCostType(defCostTypeStr, defCostItemStr);
        Material mDefMat = Material.matchMaterial(defCostItemStr);
        this.marketDefaultCostItem = (mDefMat != null && !mDefMat.isAir()) ? mDefMat : Material.DIAMOND;
        this.marketDefaultCostAmount = config.getDouble("market.default-price.cost-amount", 1.0);
        this.marketDefaultHeadAmount = config.getInt("market.default-price.head-amount", 1);

        // 5. 讀取 options 兌換方案清單
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

        // 6. 計算自動排版位置
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
     * 自動增量同步：將本地缺少的新版本設定項插入既有區段內，完整保留使用者的註解與排版
     *
     * <p>舊版做法是以 {@code config.save(file)} 整檔重寫，Bukkit 的 YamlConfiguration 不保留註解，
     * 實測會把使用者的註解從 46 行吃到只剩 20 行。</p>
     *
     * <p>注意：新設定項必須插入既有區段「內部」，不可整段附加於檔案末端 —— YAML 重複的頂層鍵會讓
     * 後者完全覆蓋前者，使用者原本的 {@code market.enabled} 等設定會被靜默丟棄。</p>
     */
    private void syncConfigFile(FileConfiguration config) {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            return;
        }

        try (InputStream defaultStream = plugin.getResource("config.yml")) {
            if (defaultStream == null) {
                return;
            }

            YamlConfiguration defaultCfg = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));

            // 收集缺少的「葉節點」鍵值（跳過純容器節點，避免重複寫入整個區段）
            List<String> missingKeys = new ArrayList<>();
            for (String defaultKey : defaultCfg.getKeys(true)) {
                if (defaultCfg.isConfigurationSection(defaultKey)) {
                    continue;
                }
                if (!config.contains(defaultKey, true)) {
                    missingKeys.add(defaultKey);
                }
            }

            if (missingKeys.isEmpty()) {
                return;
            }

            List<String> lines = new ArrayList<>(Files.readAllLines(configFile.toPath(), StandardCharsets.UTF_8));
            int inserted = 0;

            for (String key : missingKeys) {
                Object value = defaultCfg.get(key);
                if (insertKey(lines, key, value)) {
                    inserted++;
                }
                // 同步進記憶體中的設定，讓本次啟動即可讀到新值
                config.set(key, value);
            }

            if (inserted > 0) {
                Files.write(configFile.toPath(), lines, StandardCharsets.UTF_8);
                plugin.getLogger().info("已自動將 " + inserted
                        + " 項新版本設定同步至 config.yml（既有註解與設定均完整保留）。");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "同步 config.yml 預設設定時發生錯誤", e);
        }
    }

    /**
     * 將單一設定項插入 YAML 文字的正確位置
     *
     * <p>若父區段已存在，插入於該區段內容的末端；若父區段完全不存在，才附加於檔案末端
     * （此時不會產生重複鍵）。</p>
     *
     * @return 是否成功寫入
     */
    private boolean insertKey(List<String> lines, String fullKey, Object value) {
        String[] parts = fullKey.split("\\.");

        // 逐層尋找已存在的最深層父區段
        int searchStart = 0;
        int searchEnd = lines.size();
        int matchedDepth = 0;
        int parentBlockEnd = -1;

        for (int depth = 0; depth < parts.length - 1; depth++) {
            int lineIdx = findKeyLine(lines, parts[depth], depth, searchStart, searchEnd);
            if (lineIdx < 0) {
                break;
            }
            searchEnd = findBlockEnd(lines, lineIdx, depth);
            searchStart = lineIdx + 1;
            matchedDepth = depth + 1;
            parentBlockEnd = searchEnd;
        }

        // 序列化葉節點名稱與其值（交由 SnakeYAML 處理引號、型別與列表格式）
        YamlConfiguration fragment = new YamlConfiguration();
        fragment.set(parts[parts.length - 1], value);
        String rendered = fragment.saveToString().stripTrailing();
        if (rendered.isEmpty()) {
            return false;
        }

        List<String> block = new ArrayList<>();

        if (matchedDepth == parts.length - 1 && parentBlockEnd >= 0) {
            // 父區段已存在：直接以對應縮排插入其末端
            String indent = "  ".repeat(matchedDepth);
            for (String line : rendered.split("\n", -1)) {
                block.add(line.isEmpty() ? "" : indent + line);
            }
            lines.addAll(parentBlockEnd, block);
        } else {
            // 父區段不存在：於檔案末端建立完整巢狀結構（無重複鍵風險）
            if (!lines.isEmpty() && !lines.get(lines.size() - 1).trim().isEmpty()) {
                block.add("");
            }
            for (int depth = matchedDepth; depth < parts.length - 1; depth++) {
                block.add("  ".repeat(depth) + parts[depth] + ":");
            }
            String indent = "  ".repeat(parts.length - 1);
            for (String line : rendered.split("\n", -1)) {
                block.add(line.isEmpty() ? "" : indent + line);
            }
            lines.addAll(block);
        }

        return true;
    }

    /**
     * 在指定範圍內尋找位於特定縮排層級的鍵所在行號，忽略註解與空行
     */
    private int findKeyLine(List<String> lines, String key, int depth, int from, int to) {
        String expectedIndent = "  ".repeat(depth);
        String needle = expectedIndent + key + ":";

        for (int i = from; i < Math.min(to, lines.size()); i++) {
            String line = lines.get(i);
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            if (indentOf(line) != expectedIndent.length()) {
                continue;
            }
            if (line.startsWith(needle) || line.stripTrailing().equals(needle)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 計算某個區段內容的結束位置（回傳「最後一行實質內容之後」的索引）
     *
     * <p>刻意排除區段尾端的空行與註解，那些通常屬於下一個區段的說明文字。</p>
     */
    private int findBlockEnd(List<String> lines, int sectionLine, int depth) {
        int parentIndent = "  ".repeat(depth).length();
        int lastContent = sectionLine;

        for (int i = sectionLine + 1; i < lines.size(); i++) {
            String line = lines.get(i);
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            if (indentOf(line) <= parentIndent) {
                break;
            }
            lastContent = i;
        }

        return lastContent + 1;
    }

    private int indentOf(String line) {
        int n = 0;
        while (n < line.length() && line.charAt(n) == ' ') {
            n++;
        }
        return n;
    }

    private ShopOption parseOptionFromMap(Map<?, ?> entry, int maxSlots) {
        int slot = entry.containsKey("slot") ? ((Number) entry.get("slot")).intValue() : -1;
        if (slot >= maxSlots) {
            plugin.getLogger().warning("方案 Slot 設定超出邊界 (0 ~ " + (maxSlots - 1) + "): " + slot + "，已轉為自動排版。");
            slot = -1;
        }

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

    private Map<Integer, ShopOption> calculateLayout(List<ShopOption> manualOptions, List<ShopOption> autoOptions, int maxSlots) {
        Map<Integer, ShopOption> resultMap = new LinkedHashMap<>();
        Set<Integer> occupiedSlots = new HashSet<>();

        for (ShopOption opt : manualOptions) {
            if (opt.getSlot() >= 0 && opt.getSlot() < maxSlots) {
                resultMap.put(opt.getSlot(), opt);
                occupiedSlots.add(opt.getSlot());
            }
        }

        if (autoOptions.isEmpty()) {
            return resultMap;
        }

        List<Integer> preferredSlots = getPreferredSlots(autoOptions.size(), this.guiRows);
        int autoIdx = 0;

        for (int candidate : preferredSlots) {
            if (autoIdx >= autoOptions.size()) break;
            if (!occupiedSlots.contains(candidate) && candidate >= 0 && candidate < maxSlots) {
                ShopOption opt = autoOptions.get(autoIdx++);
                resultMap.put(candidate, opt.withSlot(candidate));
                occupiedSlots.add(candidate);
            }
        }

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

    public boolean isPoolEnabled() {
        return poolEnabled;
    }

    public boolean isPoolCollectItems() {
        return poolCollectItems;
    }

    public boolean isPoolCollectVault() {
        return poolCollectVault;
    }

    public boolean isPoolCollectExp() {
        return poolCollectExp;
    }

    public boolean isMarketEnabled() {
        return marketEnabled;
    }

    public int getMarketMaxListings() {
        return marketMaxListings;
    }

    /**
     * 市集賣家分潤比例 (0 ~ 100)：買家支付的 Vault 金幣中撥給賣家的百分比，餘額進入伺服器金庫
     */
    public double getMarketSellerPayoutPercent() {
        return marketSellerPayoutPercent;
    }

    public ShopOption.CostType getMarketDefaultCostType() {
        return marketDefaultCostType;
    }

    public Material getMarketDefaultCostItem() {
        return marketDefaultCostItem;
    }

    public double getMarketDefaultCostAmount() {
        return marketDefaultCostAmount;
    }

    public int getMarketDefaultHeadAmount() {
        return marketDefaultHeadAmount;
    }

    public Map<Integer, ShopOption> getOptions() {
        return options;
    }

    public ShopOption getOption(int slot) {
        return options.get(slot);
    }
}

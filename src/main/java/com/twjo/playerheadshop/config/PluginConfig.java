package com.twjo.playerheadshop.config;

import com.twjo.playerheadshop.PlayerHeadShop;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.logging.Level;

/**
 * 管理 PlayerHeadShop 的設定檔讀取、驗證、GUI 設定與 MiniMessage 訊息解析。
 */
public class PluginConfig {

    private final PlayerHeadShop plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    // GUI 設定
    private volatile String guiTitle = "<gradient:#FFAA00:#FF5555><bold>自訂頭顱商店</bold></gradient>";
    private volatile int guiRows = 3;
    private volatile boolean fillerEnabled = true;
    private volatile Material fillerMaterial = Material.GRAY_STAINED_GLASS_PANE;
    private volatile String fillerDisplayName = " ";

    // 多方案清單 (以 slot 為 key)
    private volatile Map<Integer, ShopOption> options = Collections.emptyMap();

    // 訊息設定
    private volatile String prefix = "<gray>[<gold>HeadShop</gold>]</gray> ";
    private volatile String successMsg = "<green>你花費了 <gold><cost_amount> 個 <cost_item></gold> 購買了 <gold><head_amount> 個</gold> 自己的頭顱！</green>";
    private volatile String notEnoughItemsMsg = "<red>物品不足！需要 <gold><required> 個 <item></gold>，你目前只有 <gold><current> 個</gold>（缺少 <missing> 個）。</red>";
    private volatile String inventoryFullMsg = "<yellow>你的背包已滿，溢出的頭顱已掉落在你的腳下！</yellow>";
    private volatile String reloadSuccessMsg = "<green>PlayerHeadShop 設定檔已成功重新加載！</green>";
    private volatile String noPermissionMsg = "<red>你沒有權限使用此指令！</red>";
    private volatile String playerOnlyMsg = "<red>此指令僅限遊戲內玩家使用。</red>";

    public PluginConfig(PlayerHeadShop plugin) {
        this.plugin = plugin;
        reload();
    }

    /**
     * 重新加載設定檔與訊息
     */
    public synchronized void reload() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        // 1. 讀取 GUI 設定
        this.guiTitle = config.getString("gui.title", "<gradient:#FFAA00:#FF5555><bold>自訂頭顱商店</bold></gradient>");
        int rows = config.getInt("gui.rows", 3);
        this.guiRows = Math.max(1, Math.min(6, rows));

        this.fillerEnabled = config.getBoolean("gui.filler.enabled", true);
        String fillerMatStr = config.getString("gui.filler.material", "GRAY_STAINED_GLASS_PANE");
        Material matchedFiller = Material.matchMaterial(fillerMatStr);
        this.fillerMaterial = (matchedFiller != null && !matchedFiller.isAir()) ? matchedFiller : Material.GRAY_STAINED_GLASS_PANE;
        this.fillerDisplayName = config.getString("gui.filler.display-name", " ");

        // 2. 讀取 options 兌換方案清單
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

                    String displayName = entry.containsKey("display-name") ? String.valueOf(entry.get("display-name")) : "<yellow>玩家頭顱</yellow>";
                    
                    List<String> lore = new ArrayList<>();
                    if (entry.containsKey("lore") && entry.get("lore") instanceof List<?> loreList) {
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
                    if (slot < 0 || slot >= maxSlots) {
                        continue;
                    }
                    String displayName = section.getString(key + ".display-name", "<yellow>玩家頭顱</yellow>");
                    List<String> lore = section.getStringList(key + ".lore");
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

        // 若無任何方案，建立預設方案避免空白
        if (newOptions.isEmpty()) {
            ShopOption defaultOption = new ShopOption(13, "<yellow><bold>1 個頭顱</bold></yellow>",
                    List.of("<gray>購買印有自身皮膚外觀的頭顱。", "", "<white>消耗: <gold>1 個 DIAMOND</gold></white>", "<green>▶ 點擊立即購買！</green>"),
                    Material.DIAMOND, 1, 1);
            newOptions.put(13, defaultOption);
        }

        this.options = Collections.unmodifiableMap(newOptions);

        // 3. 讀取訊息設定
        this.prefix = config.getString("messages.prefix", "<gray>[<gold>HeadShop</gold>]</gray> ");
        this.successMsg = config.getString("messages.success", "<green>你花費了 <gold><cost_amount> 個 <cost_item></gold> 購買了 <gold><head_amount> 個</gold> 自己的頭顱！</green>");
        this.notEnoughItemsMsg = config.getString("messages.not-enough-items", "<red>物品不足！需要 <gold><required> 個 <item></gold>，你目前只有 <gold><current> 個</gold>（缺少 <missing> 個）。</red>");
        this.inventoryFullMsg = config.getString("messages.inventory-full", "<yellow>你的背包已滿，溢出的頭顱已掉落在你的腳下！</yellow>");
        this.reloadSuccessMsg = config.getString("messages.reload-success", "<green>PlayerHeadShop 設定檔已成功重新加載！</green>");
        this.noPermissionMsg = config.getString("messages.no-permission", "<red>你沒有權限使用此指令！</red>");
        this.playerOnlyMsg = config.getString("messages.player-only", "<red>此指令僅限遊戲內玩家使用。</red>");
    }

    public String getGuiTitle() {
        return guiTitle;
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

    public void sendSuccess(Audience audience, int costAmount, String costItem, int headAmount) {
        sendMessage(audience, successMsg,
                Placeholder.parsed("cost_amount", String.valueOf(costAmount)),
                Placeholder.parsed("cost_item", costItem),
                Placeholder.parsed("head_amount", String.valueOf(headAmount)),
                Placeholder.parsed("amount", String.valueOf(costAmount)),
                Placeholder.parsed("item", costItem)
        );
    }

    public void sendNotEnoughItems(Audience audience, int required, String item, int current, int missing) {
        sendMessage(audience, notEnoughItemsMsg,
                Placeholder.parsed("required", String.valueOf(required)),
                Placeholder.parsed("item", item),
                Placeholder.parsed("current", String.valueOf(current)),
                Placeholder.parsed("missing", String.valueOf(missing))
        );
    }

    public void sendInventoryFull(Audience audience) {
        sendMessage(audience, inventoryFullMsg);
    }

    public void sendReloadSuccess(Audience audience) {
        sendMessage(audience, reloadSuccessMsg);
    }

    public void sendNoPermission(Audience audience) {
        sendMessage(audience, noPermissionMsg);
    }

    public void sendPlayerOnly(Audience audience) {
        sendMessage(audience, playerOnlyMsg);
    }

    private void sendMessage(Audience audience, String template, TagResolver... tagResolvers) {
        if (audience == null || template == null || template.isEmpty()) {
            return;
        }
        String fullMessage = prefix + template;
        Component component = miniMessage.deserialize(fullMessage, tagResolvers);
        audience.sendMessage(component);
    }
}

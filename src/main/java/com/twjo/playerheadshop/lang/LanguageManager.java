package com.twjo.playerheadshop.lang;

import com.twjo.playerheadshop.PlayerHeadShop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;

/**
 * 完整多語言 (i18n) 管理器，支援多語系檔案、客戶端語言自動偵測與多層回退
 */
public class LanguageManager {

    public static final String DEFAULT_LANG = "zh_tw";
    private static final List<String> BUILTIN_LANGS = List.of("zh_TW", "zh_CN", "en_US");

    private final PlayerHeadShop plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<String, FileConfiguration> langConfigs = new HashMap<>();

    public LanguageManager(PlayerHeadShop plugin) {
        this.plugin = plugin;
        load();
    }

    /**
     * 載入與熱重載所有語言檔案
     */
    public synchronized void load() {
        langConfigs.clear();
        File langDir = new File(plugin.getDataFolder(), "languages");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }

        // 保存內建語言檔案
        for (String langName : BUILTIN_LANGS) {
            File langFile = new File(langDir, langName + ".yml");
            if (!langFile.exists()) {
                plugin.saveResource("languages/" + langName + ".yml", false);
            }
        }

        // 讀取 languages 目錄下的所有 .yml 檔案
        File[] files = langDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                try {
                    String code = file.getName().substring(0, file.getName().length() - 4).toLowerCase().replace("-", "_");
                    FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

                    // 載入 JAR 內建預設值作為 fallback
                    String resourcePath = "languages/" + file.getName();
                    InputStream defaultStream = plugin.getResource(resourcePath);
                    if (defaultStream != null) {
                        YamlConfiguration defaultCfg = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
                        cfg.setDefaults(defaultCfg);
                    }

                    langConfigs.put(code, cfg);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "無法載入語言檔案: " + file.getName(), e);
                }
            }
        }
    }

    /**
     * 判斷指定發送者/玩家所適用的語言代碼
     */
    public String resolveLanguage(CommandSender sender) {
        String configured = plugin.getPluginConfig().getLanguage();
        if (configured != null && !configured.equalsIgnoreCase("auto")) {
            String norm = configured.toLowerCase().replace("-", "_");
            if (langConfigs.containsKey(norm)) {
                return norm;
            }
        }

        // auto 自動偵測
        if (sender instanceof Player player) {
            try {
                Locale locale = player.locale();
                String lang = locale.getLanguage().toLowerCase();
                String country = locale.getCountry().toLowerCase();
                String combined = lang + "_" + country;

                if (langConfigs.containsKey(combined)) {
                    return combined;
                }
                if (combined.startsWith("zh_cn") || combined.startsWith("zh_sg") || combined.contains("hans")) {
                    return "zh_cn";
                }
                if (combined.startsWith("zh_tw") || combined.startsWith("zh_hk") || combined.startsWith("zh_mo") || combined.contains("hant")) {
                    return "zh_tw";
                }
                if (lang.equals("en")) {
                    return "en_us";
                }
            } catch (Throwable ignored) {}
        }

        return DEFAULT_LANG;
    }

    /**
     * 獲取指定鍵值的字串內容
     */
    public String getRaw(CommandSender sender, String key, String defaultValue) {
        String lang = resolveLanguage(sender);
        FileConfiguration cfg = langConfigs.getOrDefault(lang, langConfigs.get(DEFAULT_LANG));
        if (cfg != null && cfg.contains(key)) {
            return cfg.getString(key, defaultValue);
        }
        FileConfiguration fallback = langConfigs.get(DEFAULT_LANG);
        if (fallback != null && fallback.contains(key)) {
            return fallback.getString(key, defaultValue);
        }
        return defaultValue;
    }

    /**
     * 獲取指定鍵值的字串列表
     */
    public List<String> getRawList(CommandSender sender, String key) {
        String lang = resolveLanguage(sender);
        FileConfiguration cfg = langConfigs.getOrDefault(lang, langConfigs.get(DEFAULT_LANG));
        if (cfg != null && cfg.isList(key)) {
            return cfg.getStringList(key);
        }
        FileConfiguration fallback = langConfigs.get(DEFAULT_LANG);
        if (fallback != null && fallback.isList(key)) {
            return fallback.getStringList(key);
        }
        return Collections.emptyList();
    }

    /**
     * 取得已格式化的 MiniMessage Component
     */
    public Component getComponent(CommandSender sender, String key, TagResolver... resolvers) {
        String raw = getRaw(sender, key, "");
        if (raw == null || raw.isEmpty()) {
            return Component.empty();
        }
        return miniMessage.deserialize(raw, resolvers);
    }

    /**
     * 發送附帶 Prefix 的本地化訊息給對象
     */
    public void sendMessage(CommandSender sender, String messageKey, TagResolver... resolvers) {
        if (sender == null) return;
        String prefix = getRaw(sender, "messages.prefix", "<gray>[<gold>HeadShop</gold>]</gray> ");
        String msg = getRaw(sender, "messages." + messageKey, "");
        if (msg.isEmpty()) return;

        Component comp = miniMessage.deserialize(prefix + msg, resolvers);
        sender.sendMessage(comp);
    }

    /**
     * 本地化格式化數量描述（大於 64 時自動轉換為組數與餘數說明）
     */
    public String formatAmount(CommandSender sender, int amount) {
        if (amount <= 64) {
            String pattern = getRaw(sender, "units.item", "<amount> 個");
            return pattern.replace("<amount>", String.valueOf(amount));
        }

        int stacks = amount / 64;
        int rem = amount % 64;

        if (rem == 0) {
            String pattern = getRaw(sender, "units.stacks-exact", "<amount> 個 (<stacks> 組)");
            return pattern.replace("<amount>", String.valueOf(amount))
                          .replace("<stacks>", String.valueOf(stacks));
        }

        String pattern = getRaw(sender, "units.stacks-rem", "<amount> 個 (<stacks> 組 + <rem> 個)");
        return pattern.replace("<amount>", String.valueOf(amount))
                      .replace("<stacks>", String.valueOf(stacks))
                      .replace("<rem>", String.valueOf(rem));
    }

    /**
     * 本地化格式化經驗等級
     */
    public String formatExpLevel(CommandSender sender, int levels) {
        String pattern = getRaw(sender, "units.exp-level", "<amount> 等級");
        return pattern.replace("<amount>", String.valueOf(levels));
    }

    /**
     * 本地化格式化經驗點數
     */
    public String formatExpPoints(CommandSender sender, int points) {
        String pattern = getRaw(sender, "units.exp-points", "<amount> 點經驗");
        return pattern.replace("<amount>", String.valueOf(points));
    }
}

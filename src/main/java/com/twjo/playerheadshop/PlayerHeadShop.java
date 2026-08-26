package com.twjo.playerheadshop;

import com.twjo.playerheadshop.command.BuyHeadCommand;
import com.twjo.playerheadshop.config.PluginConfig;
import com.twjo.playerheadshop.database.DatabaseManager;
import com.twjo.playerheadshop.economy.VaultHook;
import com.twjo.playerheadshop.gui.HeadShopGui;
import com.twjo.playerheadshop.gui.HeadShopListener;
import com.twjo.playerheadshop.lang.LanguageManager;
import com.twjo.playerheadshop.service.HeadShopService;
import com.twjo.playerheadshop.treasury.TreasuryGui;
import com.twjo.playerheadshop.treasury.TreasuryListener;
import com.twjo.playerheadshop.treasury.TreasuryManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * PlayerHeadShop 主插件類別
 */
public final class PlayerHeadShop extends JavaPlugin {

    private static PlayerHeadShop instance;
    private PluginConfig pluginConfig;
    private LanguageManager languageManager;
    private VaultHook vaultHook;
    private DatabaseManager databaseManager;
    private TreasuryManager treasuryManager;
    private HeadShopService headShopService;
    private HeadShopGui headShopGui;
    private TreasuryGui treasuryGui;

    @Override
    public void onEnable() {
        instance = this;

        // 儲存並初始化預設設定檔
        saveDefaultConfig();

        // 載入配置、多語言、Vault 經濟、資料庫、金庫與服務
        this.pluginConfig = new PluginConfig(this);
        this.languageManager = new LanguageManager(this);
        this.vaultHook = new VaultHook(this);
        this.databaseManager = new DatabaseManager(this);
        this.treasuryManager = new TreasuryManager(this, this.databaseManager);
        this.headShopService = new HeadShopService(this, this.languageManager, this.databaseManager);
        this.headShopGui = new HeadShopGui(this, this.pluginConfig, this.languageManager);
        this.treasuryGui = new TreasuryGui(this, this.languageManager, this.treasuryManager);

        // 註冊事件監聽器 (包含商店主選單、放置兌換介面與收益金庫介面)
        getServer().getPluginManager().registerEvents(new HeadShopListener(this.headShopService, this.headShopGui), this);
        getServer().getPluginManager().registerEvents(new TreasuryListener(this, this.languageManager, this.treasuryManager, this.treasuryGui), this);

        // 註冊指令至伺服器 CommandMap (相容 Paper 現代架構與傳統伺服器)
        BuyHeadCommand buyHeadCommand = new BuyHeadCommand(this, this.pluginConfig, this.languageManager, this.headShopGui,
                this.databaseManager, this.treasuryManager, this.treasuryGui);
        getServer().getCommandMap().register("playerheadshop", buyHeadCommand);

        if (this.vaultHook.hasEconomy()) {
            getLogger().info("已成功連接至 Vault 經濟系統 (貨幣: " + this.vaultHook.getCurrencyName() + ")！");
        } else {
            getLogger().info("未檢測到 Vault 經濟插件，僅啟用物品兌換模式。");
        }

        getLogger().info("PlayerHeadShop v" + getPluginMeta().getVersion() + " (Vault, Database, Treasury & i18n enabled) 已成功加載並啟用！");
    }

    @Override
    public void onDisable() {
        if (treasuryManager != null) {
            treasuryManager.saveDataAsync();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("PlayerHeadShop 已安全卸載。");
        instance = null;
    }

    public static PlayerHeadShop getInstance() {
        return instance;
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public VaultHook getVaultHook() {
        return vaultHook;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public TreasuryManager getTreasuryManager() {
        return treasuryManager;
    }

    public HeadShopService getHeadShopService() {
        return headShopService;
    }

    public HeadShopGui getHeadShopGui() {
        return headShopGui;
    }

    public TreasuryGui getTreasuryGui() {
        return treasuryGui;
    }
}

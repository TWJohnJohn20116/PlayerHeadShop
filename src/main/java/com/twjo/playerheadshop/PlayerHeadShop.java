package com.twjo.playerheadshop;

import com.twjo.playerheadshop.command.BuyHeadCommand;
import com.twjo.playerheadshop.config.PluginConfig;
import com.twjo.playerheadshop.gui.HeadShopGui;
import com.twjo.playerheadshop.gui.HeadShopListener;
import com.twjo.playerheadshop.service.HeadShopService;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * PlayerHeadShop 主插件類別
 */
public final class PlayerHeadShop extends JavaPlugin {

    private static PlayerHeadShop instance;
    private PluginConfig pluginConfig;
    private HeadShopService headShopService;
    private HeadShopGui headShopGui;

    @Override
    public void onEnable() {
        instance = this;

        // 儲存並初始化預設設定檔
        saveDefaultConfig();

        // 載入配置、服務與 GUI 介面
        this.pluginConfig = new PluginConfig(this);
        this.headShopService = new HeadShopService(this.pluginConfig);
        this.headShopGui = new HeadShopGui(this.pluginConfig);

        // 註冊事件監聽器
        getServer().getPluginManager().registerEvents(new HeadShopListener(this.headShopService), this);

        // 註冊指令至伺服器 CommandMap (相容 Paper 現代架構與傳統伺服器)
        BuyHeadCommand buyHeadCommand = new BuyHeadCommand(this.pluginConfig, this.headShopGui);
        getServer().getCommandMap().register("playerheadshop", buyHeadCommand);

        getLogger().info("PlayerHeadShop v" + getPluginMeta().getVersion() + " 已成功加載並啟用！");
    }

    @Override
    public void onDisable() {
        getLogger().info("PlayerHeadShop 已安全卸載。");
        instance = null;
    }

    public static PlayerHeadShop getInstance() {
        return instance;
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public HeadShopService getHeadShopService() {
        return headShopService;
    }

    public HeadShopGui getHeadShopGui() {
        return headShopGui;
    }
}

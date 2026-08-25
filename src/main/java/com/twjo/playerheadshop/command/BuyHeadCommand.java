package com.twjo.playerheadshop.command;

import com.twjo.playerheadshop.config.PluginConfig;
import com.twjo.playerheadshop.gui.HeadShopGui;
import com.twjo.playerheadshop.lang.LanguageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 處理 /buyhead 指令與 Tab 補全（整合多語言 i18n 系統）
 */
public class BuyHeadCommand extends Command {

    private static final String PERMISSION_USE = "playerheadshop.use";
    private static final String PERMISSION_ADMIN = "playerheadshop.admin";

    private final PluginConfig config;
    private final LanguageManager lang;
    private final HeadShopGui gui;

    public BuyHeadCommand(PluginConfig config, LanguageManager lang, HeadShopGui gui) {
        super("buyhead", "購買自己的玩家頭顱", "/buyhead [reload]", List.of("playerheadshop", "headshop"));
        this.config = config;
        this.lang = lang;
        this.gui = gui;
        setPermission(PERMISSION_USE);
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        // 處理重載子指令 /buyhead reload
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission(PERMISSION_ADMIN)) {
                lang.sendMessage(sender, "no-permission");
                return true;
            }
            config.reload();
            lang.load();
            lang.sendMessage(sender, "reload-success");
            return true;
        }

        // 開啟 GUI 指令限遊戲內玩家使用
        if (!(sender instanceof Player player)) {
            lang.sendMessage(sender, "player-only");
            return true;
        }

        // 檢查一般玩家使用權限
        if (!player.hasPermission(PERMISSION_USE)) {
            lang.sendMessage(player, "no-permission");
            return true;
        }

        // 開啟頭顱商店 GUI
        gui.open(player);
        return true;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            if (sender.hasPermission(PERMISSION_ADMIN) && "reload".startsWith(args[0].toLowerCase())) {
                completions.add("reload");
            }
            return completions;
        }
        return Collections.emptyList();
    }
}

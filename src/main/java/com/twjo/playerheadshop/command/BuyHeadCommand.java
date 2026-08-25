package com.twjo.playerheadshop.command;

import com.twjo.playerheadshop.PlayerHeadShop;
import com.twjo.playerheadshop.config.PluginConfig;
import com.twjo.playerheadshop.database.DatabaseManager;
import com.twjo.playerheadshop.database.TradeRecord;
import com.twjo.playerheadshop.gui.HeadShopGui;
import com.twjo.playerheadshop.lang.LanguageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 處理 /buyhead 指令、管理員熱重載、歷史查詢與 Tab 補全
 */
public class BuyHeadCommand extends Command {

    private static final String PERMISSION_USE = "playerheadshop.use";
    private static final String PERMISSION_ADMIN = "playerheadshop.admin";
    private static final int PAGE_SIZE = 6;

    private final PlayerHeadShop plugin;
    private final PluginConfig config;
    private final LanguageManager lang;
    private final HeadShopGui gui;
    private final DatabaseManager databaseManager;

    public BuyHeadCommand(PlayerHeadShop plugin, PluginConfig config, LanguageManager lang, HeadShopGui gui, DatabaseManager databaseManager) {
        super("buyhead", "購買自己的玩家頭顱", "/buyhead [reload|history]", List.of("playerheadshop", "headshop"));
        this.plugin = plugin;
        this.config = config;
        this.lang = lang;
        this.gui = gui;
        this.databaseManager = databaseManager;
        setPermission(PERMISSION_USE);
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        // 1. 處理重載子指令 /buyhead reload
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

        // 2. 處理兌換歷史查詢子指令 /buyhead history [玩家] [頁碼]
        if (args.length > 0 && (args[0].equalsIgnoreCase("history") || args[0].equalsIgnoreCase("logs") || args[0].equalsIgnoreCase("log"))) {
            if (!sender.hasPermission(PERMISSION_ADMIN)) {
                lang.sendMessage(sender, "no-permission");
                return true;
            }
            handleHistoryQuery(sender, args);
            return true;
        }

        // 3. 一般玩家開啟 GUI
        if (!(sender instanceof Player player)) {
            lang.sendMessage(sender, "player-only");
            return true;
        }

        if (!player.hasPermission(PERMISSION_USE)) {
            lang.sendMessage(player, "no-permission");
            return true;
        }

        gui.open(player);
        return true;
    }

    /**
     * 處理非同步歷史記錄查詢
     */
    private void handleHistoryQuery(CommandSender sender, String[] args) {
        String playerFilter = null;
        int requestedPage = 1;

        if (args.length == 2) {
            try {
                requestedPage = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                playerFilter = args[1];
            }
        } else if (args.length >= 3) {
            playerFilter = args[1];
            try {
                requestedPage = Integer.parseInt(args[2]);
            } catch (NumberFormatException ignored) {}
        }

        final String finalFilter = playerFilter;
        final int targetPage = Math.max(1, requestedPage);

        databaseManager.getTotalRecords(finalFilter).thenAccept(total -> {
            if (total == 0) {
                lang.sendMessage(sender, "history-empty");
                return;
            }

            int maxPage = (int) Math.ceil((double) total / PAGE_SIZE);
            if (targetPage > maxPage) {
                lang.sendMessage(sender, "history-invalid-page",
                        Placeholder.parsed("max_page", String.valueOf(maxPage))
                );
                return;
            }

            databaseManager.getRecords(finalFilter, targetPage, PAGE_SIZE).thenAccept(records -> {
                // 1. 發送頂部 Header
                Component header;
                if (finalFilter == null || finalFilter.isEmpty()) {
                    header = lang.getComponent(sender, "messages.history-header",
                            Placeholder.parsed("page", String.valueOf(targetPage)),
                            Placeholder.parsed("max_page", String.valueOf(maxPage)),
                            Placeholder.parsed("total", String.valueOf(total))
                    );
                } else {
                    header = lang.getComponent(sender, "messages.history-player-header",
                            Placeholder.parsed("player", finalFilter),
                            Placeholder.parsed("page", String.valueOf(targetPage)),
                            Placeholder.parsed("max_page", String.valueOf(maxPage)),
                            Placeholder.parsed("total", String.valueOf(total))
                    );
                }
                if (!header.equals(Component.empty())) {
                    sender.sendMessage(header);
                }

                // 2. 輸出每筆記錄
                for (TradeRecord rec : records) {
                    Component entry = lang.getComponent(sender, "messages.history-entry",
                            Placeholder.parsed("time", rec.getFormattedTime()),
                            Placeholder.parsed("player", rec.getPlayerName()),
                            Placeholder.parsed("cost_amount", formatCost(sender, rec.getCostItem(), rec.getCostAmount())),
                            Placeholder.parsed("cost_item", rec.getCostItem()),
                            Placeholder.parsed("head_amount", lang.formatAmount(sender, rec.getHeadAmount()))
                    );
                    if (!entry.equals(Component.empty())) {
                        sender.sendMessage(entry);
                    }
                }

                // 3. 發送底部 Footer
                Component footer = lang.getComponent(sender, "messages.history-footer");
                if (!footer.equals(Component.empty())) {
                    sender.sendMessage(footer);
                }
            });
        });
    }

    private String formatCost(CommandSender sender, String costItem, int costAmount) {
        if ("VAULT".equalsIgnoreCase(costItem)) {
            return plugin.getVaultHook().format(costAmount);
        } else if ("EXP_LEVEL".equalsIgnoreCase(costItem)) {
            return lang.formatExpLevel(sender, costAmount);
        } else if ("EXP_POINTS".equalsIgnoreCase(costItem)) {
            return lang.formatExpPoints(sender, costAmount);
        } else {
            return lang.formatAmount(sender, costAmount);
        }
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission(PERMISSION_ADMIN)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> sub = List.of("reload", "history", "logs");
            List<String> list = new ArrayList<>();
            for (String s : sub) {
                if (s.startsWith(args[0].toLowerCase())) {
                    list.add(s);
                }
            }
            return list;
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("history") || args[0].equalsIgnoreCase("logs") || args[0].equalsIgnoreCase("log"))) {
            List<String> list = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    list.add(p.getName());
                }
            }
            list.add("1");
            list.add("2");
            return list;
        }

        if (args.length == 3 && (args[0].equalsIgnoreCase("history") || args[0].equalsIgnoreCase("logs") || args[0].equalsIgnoreCase("log"))) {
            return List.of("1", "2", "3");
        }

        return Collections.emptyList();
    }
}

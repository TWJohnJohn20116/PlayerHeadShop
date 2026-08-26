package com.twjo.playerheadshop.command;

import com.twjo.playerheadshop.PlayerHeadShop;
import com.twjo.playerheadshop.config.PluginConfig;
import com.twjo.playerheadshop.database.DatabaseManager;
import com.twjo.playerheadshop.database.TradeRecord;
import com.twjo.playerheadshop.database.TreasuryLogRecord;
import com.twjo.playerheadshop.gui.HeadShopGui;
import com.twjo.playerheadshop.lang.LanguageManager;
import com.twjo.playerheadshop.treasury.TreasuryGui;
import com.twjo.playerheadshop.treasury.TreasuryManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 處理 /buyhead 指令、幫助清單、管理員熱重載、交易歷史與收益金庫管理
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
    private final TreasuryManager treasuryManager;
    private final TreasuryGui treasuryGui;

    public BuyHeadCommand(PlayerHeadShop plugin, PluginConfig config, LanguageManager lang, HeadShopGui gui,
                          DatabaseManager databaseManager, TreasuryManager treasuryManager, TreasuryGui treasuryGui) {
        super("buyhead", "購買與自訂玩家頭顱", "/buyhead [help|history|pool|reload]", List.of("playerheadshop", "headshop"));
        this.plugin = plugin;
        this.config = config;
        this.lang = lang;
        this.gui = gui;
        this.databaseManager = databaseManager;
        this.treasuryManager = treasuryManager;
        this.treasuryGui = treasuryGui;
        setPermission(PERMISSION_USE);
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        // 1. 處理幫助說明指令 /buyhead help 或 /buyhead ?
        if (args.length > 0 && (args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("?") || args[0].equalsIgnoreCase("info"))) {
            sendHelp(sender);
            return true;
        }

        // 2. 處理重載子指令 /buyhead reload
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

        // 3. 處理兌換歷史查詢子指令 /buyhead history [玩家] [頁碼]
        if (args.length > 0 && (args[0].equalsIgnoreCase("history") || args[0].equalsIgnoreCase("logs") || args[0].equalsIgnoreCase("log"))) {
            if (!sender.hasPermission(PERMISSION_ADMIN)) {
                lang.sendMessage(sender, "no-permission");
                return true;
            }
            handleHistoryQuery(sender, args);
            return true;
        }

        // 4. 處理收益金庫子指令 /buyhead pool [...]
        if (args.length > 0 && (args[0].equalsIgnoreCase("pool") || args[0].equalsIgnoreCase("treasury") || args[0].equalsIgnoreCase("bank"))) {
            if (!sender.hasPermission(PERMISSION_ADMIN)) {
                lang.sendMessage(sender, "no-permission");
                return true;
            }
            handlePoolCommand(sender, args);
            return true;
        }

        // 5. 一般玩家開啟購買選單
        if (!(sender instanceof Player player)) {
            sendHelp(sender);
            return true;
        }

        if (!player.hasPermission(PERMISSION_USE)) {
            lang.sendMessage(player, "no-permission");
            return true;
        }

        gui.open(player);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(lang.getComponent(sender, "messages.help-header"));
        sender.sendMessage(lang.getComponent(sender, "messages.help-buyhead"));
        sender.sendMessage(lang.getComponent(sender, "messages.help-help"));

        if (sender.hasPermission(PERMISSION_ADMIN)) {
            sender.sendMessage(lang.getComponent(sender, "messages.help-admin-header"));
            sender.sendMessage(lang.getComponent(sender, "messages.help-reload"));
            sender.sendMessage(lang.getComponent(sender, "messages.help-pool"));
            sender.sendMessage(lang.getComponent(sender, "messages.help-pool-info"));
            sender.sendMessage(lang.getComponent(sender, "messages.help-pool-logs"));
            sender.sendMessage(lang.getComponent(sender, "messages.help-pool-withdraw"));
            sender.sendMessage(lang.getComponent(sender, "messages.help-history"));
        }
    }

    private void handlePoolCommand(CommandSender sender, String[] args) {
        if (!config.isPoolEnabled()) {
            lang.sendMessage(sender, "treasury-disabled");
            return;
        }

        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                sendPoolInfo(sender);
                return;
            }
            treasuryGui.open(player);
            return;
        }

        String sub = args[1].toLowerCase();

        if (sub.equals("info") || sub.equals("balance") || sub.equals("status")) {
            sendPoolInfo(sender);
            return;
        }

        if (sub.equals("logs") || sub.equals("history") || sub.equals("audit")) {
            int page = 1;
            if (args.length >= 3) {
                try {
                    page = Integer.parseInt(args[2]);
                } catch (NumberFormatException ignored) {}
            }
            handleTreasuryLogsQuery(sender, page);
            return;
        }

        if (sub.equals("withdraw") || sub.equals("take")) {
            if (!(sender instanceof Player player)) {
                lang.sendMessage(sender, "player-only");
                return;
            }

            String type = (args.length >= 3) ? args[2].toLowerCase() : "all";
            if (type.equals("money") || type.equals("vault")) {
                withdrawPoolVault(player);
            } else if (type.equals("exp") || type.equals("xp")) {
                withdrawPoolExp(player);
            } else {
                withdrawPoolVault(player);
                withdrawPoolExp(player);
            }
            return;
        }

        if (sender instanceof Player player) {
            treasuryGui.open(player);
        } else {
            sendPoolInfo(sender);
        }
    }

    private void sendPoolInfo(CommandSender sender) {
        double vault = treasuryManager.getVaultBalance();
        int exp = treasuryManager.getExpPoints();
        int itemStacks = 0;
        for (ItemStack s : treasuryManager.getItemsSnapshot()) {
            if (s != null && !s.getType().isAir()) {
                itemStacks++;
            }
        }

        sender.sendMessage(lang.getComponent(sender, "messages.treasury-info-header"));
        sender.sendMessage(lang.getComponent(sender, "messages.treasury-info-vault",
                Placeholder.parsed("amount", plugin.getVaultHook().format(vault))));
        sender.sendMessage(lang.getComponent(sender, "messages.treasury-info-exp",
                Placeholder.parsed("amount", lang.formatExpPoints(sender, exp))));
        sender.sendMessage(lang.getComponent(sender, "messages.treasury-info-items",
                Placeholder.parsed("amount", String.valueOf(itemStacks))));
    }

    private void withdrawPoolVault(Player player) {
        double balance = treasuryManager.getVaultBalance();
        if (balance <= 0) {
            lang.sendMessage(player, "treasury-empty-vault");
            return;
        }
        if (treasuryManager.withdrawVault(player, balance)) {
            plugin.getVaultHook().deposit(player, balance);
            lang.sendMessage(player, "treasury-withdraw-vault-success",
                    Placeholder.parsed("amount", plugin.getVaultHook().format(balance))
            );
            try {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            } catch (Throwable ignored) {}
        }
    }

    private void withdrawPoolExp(Player player) {
        int exp = treasuryManager.getExpPoints();
        if (exp <= 0) {
            lang.sendMessage(player, "treasury-empty-exp");
            return;
        }
        if (treasuryManager.withdrawExp(player, exp)) {
            lang.sendMessage(player, "treasury-withdraw-exp-success",
                    Placeholder.parsed("amount", lang.formatExpPoints(player, exp))
            );
            try {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            } catch (Throwable ignored) {}
        }
    }

    private void handleTreasuryLogsQuery(CommandSender sender, int page) {
        final int targetPage = Math.max(1, page);

        databaseManager.getTotalTreasuryLogs().thenAccept(total -> {
            if (total == 0) {
                lang.sendMessage(sender, "treasury-logs-empty");
                return;
            }

            int maxPage = (int) Math.ceil((double) total / PAGE_SIZE);
            if (targetPage > maxPage) {
                lang.sendMessage(sender, "treasury-logs-invalid-page",
                        Placeholder.parsed("max_page", String.valueOf(maxPage))
                );
                return;
            }

            databaseManager.getTreasuryLogs(targetPage, PAGE_SIZE).thenAccept(logs -> {
                sender.sendMessage(lang.getComponent(sender, "messages.treasury-logs-header",
                        Placeholder.parsed("page", String.valueOf(targetPage)),
                        Placeholder.parsed("max_page", String.valueOf(maxPage)),
                        Placeholder.parsed("total", String.valueOf(total))
                ));

                for (TreasuryLogRecord rec : logs) {
                    String detailStr = formatTreasuryLogDetail(sender, rec);
                    sender.sendMessage(lang.getComponent(sender, "messages.treasury-logs-entry",
                            Placeholder.parsed("time", rec.getFormattedTime()),
                            Placeholder.parsed("admin", rec.getAdminName()),
                            Placeholder.parsed("detail", detailStr)
                    ));
                }
            });
        });
    }

    private String formatTreasuryLogDetail(CommandSender sender, TreasuryLogRecord rec) {
        if ("WITHDRAW_VAULT".equalsIgnoreCase(rec.getActionType())) {
            return plugin.getVaultHook().format(rec.getAmount()) + " 金幣";
        } else if ("WITHDRAW_EXP".equalsIgnoreCase(rec.getActionType())) {
            return lang.formatExpPoints(sender, (int) Math.round(rec.getAmount()));
        } else {
            return (int) Math.round(rec.getAmount()) + " 個 " + rec.getDetail();
        }
    }

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
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            list.add("help");
            if (sender.hasPermission(PERMISSION_ADMIN)) {
                list.add("reload");
                list.add("history");
                list.add("logs");
                list.add("pool");
            }
            List<String> res = new ArrayList<>();
            for (String s : list) {
                if (s.startsWith(args[0].toLowerCase())) {
                    res.add(s);
                }
            }
            return res;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("pool") && sender.hasPermission(PERMISSION_ADMIN)) {
            return List.of("info", "logs", "withdraw");
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("pool") && args[1].equalsIgnoreCase("withdraw") && sender.hasPermission(PERMISSION_ADMIN)) {
            return List.of("money", "exp", "all");
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("history") || args[0].equalsIgnoreCase("logs") || args[0].equalsIgnoreCase("log")) && sender.hasPermission(PERMISSION_ADMIN)) {
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

        return Collections.emptyList();
    }
}

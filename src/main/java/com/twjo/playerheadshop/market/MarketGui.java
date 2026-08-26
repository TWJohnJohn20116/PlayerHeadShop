package com.twjo.playerheadshop.market;

import com.twjo.playerheadshop.PlayerHeadShop;
import com.twjo.playerheadshop.config.ShopOption;
import com.twjo.playerheadshop.database.DatabaseManager;
import com.twjo.playerheadshop.lang.LanguageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 構建與開啟社群頭顱市集 GUI 介面
 */
public class MarketGui {

    private final PlayerHeadShop plugin;
    private final DatabaseManager databaseManager;
    private final LanguageManager lang;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public MarketGui(PlayerHeadShop plugin, DatabaseManager databaseManager, LanguageManager lang) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.lang = lang;
    }

    public void open(Player player, int page, boolean myListingsOnly) {
        if (player == null || !player.isOnline()) return;

        final int targetPage = Math.max(1, page);

        if (myListingsOnly) {
            databaseManager.getPlayerSharedHeads(player.getUniqueId()).thenAccept(list -> {
                Bukkit.getScheduler().runTask(plugin, () -> renderGui(player, targetPage, true, list, list.size()));
            });
        } else {
            databaseManager.getTotalActiveSharedHeads().thenAccept(total -> {
                databaseManager.getActiveSharedHeads(targetPage, MarketGuiHolder.PAGE_SIZE).thenAccept(list -> {
                    Bukkit.getScheduler().runTask(plugin, () -> renderGui(player, targetPage, false, list, total));
                });
            });
        }
    }

    private void renderGui(Player player, int page, boolean myListingsOnly, List<SharedHeadRecord> records, int total) {
        if (!player.isOnline()) return;

        Component title = myListingsOnly
                ? lang.getComponent(player, "gui.market-my-title")
                : lang.getComponent(player, "gui.market-title", Placeholder.parsed("page", String.valueOf(page)));

        MarketGuiHolder holder = new MarketGuiHolder(player, page, myListingsOnly);
        Inventory inv = Bukkit.createInventory(holder, MarketGuiHolder.SIZE, title);
        holder.setInventory(inv);

        int maxPage = Math.max(1, (int) Math.ceil((double) total / MarketGuiHolder.PAGE_SIZE));

        // 1. 渲染上架的頭顱清單 (Slots 0 ~ 44)
        for (int i = 0; i < records.size() && i < MarketGuiHolder.PAGE_SIZE; i++) {
            SharedHeadRecord rec = records.get(i);
            inv.setItem(i, createSharedHeadItem(player, rec));
            holder.getSlotMap().put(i, rec);
        }

        // 2. 底排裝飾填充
        ItemStack filler = createFillerItem();
        int[] fillers = new int[]{46, 47, 51, 52};
        for (int s : fillers) {
            inv.setItem(s, filler);
        }

        // 3. 上一頁 (Slot 45)
        if (page > 1) {
            inv.setItem(MarketGuiHolder.PREV_SLOT, createNavigationButton(player, "gui.market-prev", Material.ARROW));
        } else {
            inv.setItem(MarketGuiHolder.PREV_SLOT, filler);
        }

        // 4. ➕ 上傳我的頭顱 (Slot 48)
        inv.setItem(MarketGuiHolder.SHARE_SLOT, createShareButton(player));

        // 5. 🎒 切換全部/我的上架 (Slot 49)
        inv.setItem(MarketGuiHolder.MY_LISTINGS_SLOT, createToggleMyListingsButton(player, myListingsOnly));

        // 6. 🔄 重新整理 (Slot 50)
        inv.setItem(MarketGuiHolder.REFRESH_SLOT, createRefreshButton(player));

        // 7. 下一頁 (Slot 53)
        if (page < maxPage && !myListingsOnly) {
            inv.setItem(MarketGuiHolder.NEXT_SLOT, createNavigationButton(player, "gui.market-next", Material.ARROW));
        } else {
            inv.setItem(MarketGuiHolder.NEXT_SLOT, filler);
        }

        player.openInventory(inv);
    }

    private ItemStack createSharedHeadItem(Player viewer, SharedHeadRecord rec) {
        int headCount = Math.min(64, Math.max(1, rec.getHeadAmount()));
        ItemStack item = new ItemStack(Material.PLAYER_HEAD, headCount);
        ItemMeta meta = item.getItemMeta();

        if (meta instanceof SkullMeta skullMeta) {
            try {
                OfflinePlayer skinPlayer = Bukkit.getOfflinePlayer(rec.getSkinOwner());
                skullMeta.setOwningPlayer(skinPlayer);
            } catch (Throwable ignored) {}

            String formattedPrice = formatPrice(viewer, rec);

            TagResolver[] resolvers = new TagResolver[]{
                    Placeholder.parsed("title", rec.getHeadName()),
                    Placeholder.parsed("seller", rec.getSellerName()),
                    Placeholder.parsed("date", rec.getFormattedDate()),
                    Placeholder.parsed("head_amount", lang.formatAmount(viewer, rec.getHeadAmount())),
                    Placeholder.parsed("price", formattedPrice),
                    Placeholder.parsed("sales", String.valueOf(rec.getSalesCount()))
            };

            skullMeta.displayName(miniMessage.deserialize("<gold><bold>" + rec.getHeadName() + "</bold></gold>"));

            List<String> rawLore = lang.getRawList(viewer, "gui.market-head-lore");
            List<Component> lore = new ArrayList<>();
            for (String line : rawLore) {
                lore.add(miniMessage.deserialize(line, resolvers));
            }

            // 若為賣家本人或管理員，加入下架提示
            if (rec.getSellerUuid().equals(viewer.getUniqueId()) || viewer.hasPermission("playerheadshop.admin")) {
                lore.add(Component.empty());
                lore.add(lang.getComponent(viewer, "gui.market-unlist-hint"));
            } else {
                lore.add(Component.empty());
                lore.add(lang.getComponent(viewer, "gui.market-buy-hint"));
            }

            skullMeta.lore(lore);
            item.setItemMeta(skullMeta);
        }

        return item;
    }

    private String formatPrice(Player player, SharedHeadRecord rec) {
        ShopOption.CostType type = rec.getCostType();
        if (type == ShopOption.CostType.VAULT) {
            return plugin.getVaultHook().format(rec.getCostAmount());
        } else if (type == ShopOption.CostType.EXP_LEVEL) {
            return lang.formatExpLevel(player, rec.getCostAmountInt());
        } else if (type == ShopOption.CostType.EXP_POINTS) {
            return lang.formatExpPoints(player, rec.getCostAmountInt());
        } else {
            return lang.formatAmount(player, rec.getCostAmountInt()) + " " + rec.getCostItem();
        }
    }

    private ItemStack createFillerItem() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createNavigationButton(Player player, String key, Material mat) {
        ItemStack item = new ItemStack(mat, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(lang.getComponent(player, key));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createShareButton(Player player) {
        ItemStack item = new ItemStack(Material.NETHER_STAR, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(lang.getComponent(player, "gui.market-share-btn.name"));
            List<String> raw = lang.getRawList(player, "gui.market-share-btn.lore");
            List<Component> lore = new ArrayList<>();
            for (String l : raw) lore.add(miniMessage.deserialize(l));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createToggleMyListingsButton(Player player, boolean currentlyMyListings) {
        ItemStack item = new ItemStack(Material.CHEST, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String key = currentlyMyListings ? "gui.market-all-btn" : "gui.market-my-btn";
            meta.displayName(lang.getComponent(player, key + ".name"));
            List<String> raw = lang.getRawList(player, key + ".lore");
            List<Component> lore = new ArrayList<>();
            for (String l : raw) lore.add(miniMessage.deserialize(l));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createRefreshButton(Player player) {
        ItemStack item = new ItemStack(Material.CLOCK, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(lang.getComponent(player, "gui.treasury-refresh.name"));
            item.setItemMeta(meta);
        }
        return item;
    }
}

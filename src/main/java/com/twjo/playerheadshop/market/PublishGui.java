package com.twjo.playerheadshop.market;

import com.twjo.playerheadshop.PlayerHeadShop;
import com.twjo.playerheadshop.config.ShopOption;
import com.twjo.playerheadshop.lang.LanguageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 構建與開啟頭顱上架設定選單 (Publish Setup GUI)
 */
public class PublishGui {

    private final PlayerHeadShop plugin;
    private final LanguageManager lang;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public PublishGui(PlayerHeadShop plugin, LanguageManager lang) {
        this.plugin = plugin;
        this.lang = lang;
    }

    public void open(Player player, PublishGuiHolder holder) {
        if (player == null || !player.isOnline()) return;

        if (holder == null) {
            holder = new PublishGuiHolder(player);
        }

        Component title = lang.getComponent(player, "gui.publish-title");
        Inventory inv = Bukkit.createInventory(holder, PublishGuiHolder.SIZE, title);
        holder.setInventory(inv);

        // 1. 填充背景
        ItemStack filler = createFillerItem();
        for (int i = 0; i < PublishGuiHolder.SIZE; i++) {
            inv.setItem(i, filler);
        }

        // 2. 支付模式按鈕 (Slot 10)
        inv.setItem(PublishGuiHolder.MODE_SLOT, createModeButton(player, holder));

        // 3. 售價設定按鈕 (Slot 12)
        inv.setItem(PublishGuiHolder.PRICE_SLOT, createPriceButton(player, holder));

        // 4. 自訂名稱按鈕 (Slot 14)
        inv.setItem(PublishGuiHolder.TITLE_SLOT, createTitleButton(player, holder));

        // 5. 商品預覽頭顱 (Slot 16)
        inv.setItem(PublishGuiHolder.PREVIEW_SLOT, createPreviewHead(player, holder));

        // 6. 返回市集按鈕 (Slot 18)
        inv.setItem(PublishGuiHolder.BACK_SLOT, createBackButton(player));

        // 7. 確認上架按鈕 (Slot 22)
        inv.setItem(PublishGuiHolder.CONFIRM_SLOT, createConfirmButton(player, holder));

        player.openInventory(inv);
    }

    private ItemStack createModeButton(Player player, PublishGuiHolder holder) {
        Material mat = holder.getCostItem();
        ItemStack item = new ItemStack(mat != null && !mat.isAir() ? mat : Material.DIAMOND, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String modeName = getModeDisplayName(player, holder.getCostType(), holder.getCostItem());
            TagResolver resolver = Placeholder.parsed("mode", modeName);

            meta.displayName(lang.getComponent(player, "gui.publish-mode.name", resolver));

            List<String> raw = lang.getRawList(player, "gui.publish-mode.lore");
            List<Component> lore = new ArrayList<>();
            for (String l : raw) lore.add(miniMessage.deserialize(l, resolver));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String getModeDisplayName(Player player, ShopOption.CostType type, Material item) {
        return switch (type) {
            case VAULT -> "Vault 經濟金幣";
            case EXP_LEVEL -> "經驗等級";
            case EXP_POINTS -> "經驗點數";
            default -> (item != null ? item.name() : "DIAMOND") + " (實體物品)";
        };
    }

    private ItemStack createPriceButton(Player player, PublishGuiHolder holder) {
        ItemStack item = new ItemStack(Material.GOLD_INGOT, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String formatted = formatPrice(player, holder);
            TagResolver resolver = Placeholder.parsed("price", formatted);

            meta.displayName(lang.getComponent(player, "gui.publish-price.name", resolver));

            List<String> raw = lang.getRawList(player, "gui.publish-price.lore");
            List<Component> lore = new ArrayList<>();
            for (String l : raw) lore.add(miniMessage.deserialize(l, resolver));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String formatPrice(Player player, PublishGuiHolder holder) {
        ShopOption.CostType type = holder.getCostType();
        if (type == ShopOption.CostType.VAULT) {
            return plugin.getVaultHook().format(holder.getCostAmount());
        } else if (type == ShopOption.CostType.EXP_LEVEL) {
            return lang.formatExpLevel(player, (int) Math.round(holder.getCostAmount()));
        } else if (type == ShopOption.CostType.EXP_POINTS) {
            return lang.formatExpPoints(player, (int) Math.round(holder.getCostAmount()));
        } else {
            return lang.formatAmount(player, (int) Math.round(holder.getCostAmount())) + " " + holder.getCostItem().name();
        }
    }

    private ItemStack createTitleButton(Player player, PublishGuiHolder holder) {
        ItemStack item = new ItemStack(Material.NAME_TAG, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            TagResolver resolver = Placeholder.parsed("title", holder.getCustomTitle());
            meta.displayName(lang.getComponent(player, "gui.publish-title-btn.name", resolver));

            List<String> raw = lang.getRawList(player, "gui.publish-title-btn.lore");
            List<Component> lore = new ArrayList<>();
            for (String l : raw) lore.add(miniMessage.deserialize(l, resolver));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createPreviewHead(Player player, PublishGuiHolder holder) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD, holder.getHeadAmount());
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof SkullMeta skullMeta) {
            try {
                skullMeta.setPlayerProfile(player.getPlayerProfile());
            } catch (Throwable ignored) {
                skullMeta.setOwningPlayer(player);
            }

            TagResolver[] resolvers = new TagResolver[]{
                    Placeholder.parsed("title", holder.getCustomTitle()),
                    Placeholder.parsed("seller", player.getName()),
                    Placeholder.parsed("price", formatPrice(player, holder)),
                    Placeholder.parsed("head_amount", lang.formatAmount(player, holder.getHeadAmount()))
            };

            skullMeta.displayName(miniMessage.deserialize("<gold><bold>" + holder.getCustomTitle() + "</bold></gold>"));

            List<String> raw = lang.getRawList(player, "gui.publish-preview.lore");
            List<Component> lore = new ArrayList<>();
            for (String l : raw) lore.add(miniMessage.deserialize(l, resolvers));
            skullMeta.lore(lore);
            item.setItemMeta(skullMeta);
        }
        return item;
    }

    private ItemStack createBackButton(Player player) {
        ItemStack item = new ItemStack(Material.ARROW, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(lang.getComponent(player, "gui.back-button.name"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createConfirmButton(Player player, PublishGuiHolder holder) {
        ItemStack item = new ItemStack(Material.EMERALD, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(lang.getComponent(player, "gui.publish-confirm.name"));
            List<String> raw = lang.getRawList(player, "gui.publish-confirm.lore");
            List<Component> lore = new ArrayList<>();
            for (String l : raw) lore.add(miniMessage.deserialize(l));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
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
}

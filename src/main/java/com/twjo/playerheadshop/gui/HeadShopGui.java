package com.twjo.playerheadshop.gui;

import com.twjo.playerheadshop.config.PluginConfig;
import com.twjo.playerheadshop.config.ShopOption;
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
import java.util.Map;

/**
 * 負責構建與開啟 PlayerHeadShop 的箱子 GUI 介面
 */
public class HeadShopGui {

    private final PluginConfig config;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public HeadShopGui(PluginConfig config) {
        this.config = config;
    }

    /**
     * 為指定玩家建立並開啟自訂頭顱商店 GUI
     *
     * @param player 目標玩家
     */
    public void open(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        int rows = config.getGuiRows();
        int size = rows * 9;
        Component title = miniMessage.deserialize(config.getGuiTitle());

        Map<Integer, ShopOption> options = config.getOptions();
        HeadShopGuiHolder holder = new HeadShopGuiHolder(player, options);
        Inventory inventory = Bukkit.createInventory(holder, size, title);
        holder.setInventory(inventory);

        // 1. 若開啟背景填充，預先填入玻璃板
        if (config.isFillerEnabled()) {
            ItemStack filler = createFillerItem();
            for (int i = 0; i < size; i++) {
                inventory.setItem(i, filler);
            }
        }

        // 2. 放置各個兌換方案的商品圖示
        for (ShopOption option : options.values()) {
            if (option.getSlot() >= 0 && option.getSlot() < size) {
                ItemStack icon = createOptionIcon(player, option);
                inventory.setItem(option.getSlot(), icon);
            }
        }

        // 3. 開啟介面
        player.openInventory(inventory);
    }

    /**
     * 建立填充玻璃板
     */
    private ItemStack createFillerItem() {
        Material material = config.getFillerMaterial();
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String nameStr = config.getFillerDisplayName();
            Component name = (nameStr == null || nameStr.isEmpty() || nameStr.equals(" "))
                    ? Component.text(" ")
                    : miniMessage.deserialize(nameStr);
            meta.displayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * 建立印有該玩家皮膚外觀與自訂 Name/Lore 的商品頭顱圖示
     */
    private ItemStack createOptionIcon(Player player, ShopOption option) {
        int iconAmount = Math.min(64, Math.max(1, option.getHeadAmount()));
        ItemStack item = new ItemStack(Material.PLAYER_HEAD, iconAmount);

        if (item.getItemMeta() instanceof SkullMeta skullMeta) {
            // 設定皮膚外觀
            try {
                skullMeta.setPlayerProfile(player.getPlayerProfile());
            } catch (Throwable ignored) {
                skullMeta.setOwningPlayer(player);
            }

            // 標籤佔位符解析
            TagResolver[] resolvers = new TagResolver[]{
                    Placeholder.parsed("player", player.getName()),
                    Placeholder.parsed("head_amount", String.valueOf(option.getHeadAmount())),
                    Placeholder.parsed("cost_amount", String.valueOf(option.getCostAmount())),
                    Placeholder.parsed("cost_item", option.getCostItem().name()),
                    Placeholder.parsed("amount", String.valueOf(option.getCostAmount())),
                    Placeholder.parsed("item", option.getCostItem().name())
            };

            // 設定顯示名稱
            if (option.getDisplayName() != null && !option.getDisplayName().isEmpty()) {
                Component displayName = miniMessage.deserialize(option.getDisplayName(), resolvers);
                skullMeta.displayName(displayName);
            }

            // 設定說明文字 (Lore)
            if (!option.getLore().isEmpty()) {
                List<Component> loreComponents = new ArrayList<>();
                for (String line : option.getLore()) {
                    loreComponents.add(miniMessage.deserialize(line, resolvers));
                }
                skullMeta.lore(loreComponents);
            }

            item.setItemMeta(skullMeta);
        }

        return item;
    }
}

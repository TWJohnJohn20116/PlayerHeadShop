package com.twjo.playerheadshop.treasury;

import com.twjo.playerheadshop.PlayerHeadShop;
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

import java.util.ArrayList;
import java.util.List;

/**
 * 構建與開啟管理員收益金庫 GUI 介面
 */
public class TreasuryGui {

    private final PlayerHeadShop plugin;
    private final LanguageManager lang;
    private final TreasuryManager treasuryManager;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public TreasuryGui(PlayerHeadShop plugin, LanguageManager lang, TreasuryManager treasuryManager) {
        this.plugin = plugin;
        this.lang = lang;
        this.treasuryManager = treasuryManager;
    }

    /**
     * 為管理員開啟收益金庫介面
     */
    public void open(Player admin) {
        if (admin == null || !admin.isOnline()) return;

        Component title = lang.getComponent(admin, "gui.treasury-title");
        TreasuryGuiHolder holder = new TreasuryGuiHolder(admin);
        Inventory inventory = Bukkit.createInventory(holder, TreasuryGuiHolder.SIZE, title);
        holder.setInventory(inventory);

        // 1. 填入目前金庫中的實體物品 (Slots 0 ~ 44)
        List<ItemStack> items = treasuryManager.getItemsSnapshot();
        for (int i = 0; i < TreasuryGuiHolder.ITEM_STORAGE_LIMIT && i < items.size(); i++) {
            inventory.setItem(i, items.get(i));
        }

        // 2. 底排裝飾填充玻璃板
        ItemStack filler = createFillerItem();
        int[] fillerSlots = new int[]{45, 46, 47, 49, 51};
        for (int s : fillerSlots) {
            inventory.setItem(s, filler);
        }

        // 3. Slot 48: 金幣提領按鈕 (Gold Ingot)
        inventory.setItem(TreasuryGuiHolder.VAULT_SLOT, createVaultButton(admin));

        // 4. Slot 50: 經驗提領按鈕 (Exp Bottle)
        inventory.setItem(TreasuryGuiHolder.EXP_SLOT, createExpButton(admin));

        // 5. Slot 52: 稽核日誌捷徑 (Book)
        inventory.setItem(TreasuryGuiHolder.LOGS_SLOT, createLogsButton(admin));

        // 6. Slot 53: 重新整理按鈕 (Clock)
        inventory.setItem(TreasuryGuiHolder.REFRESH_SLOT, createRefreshButton(admin));

        // 開啟介面
        admin.openInventory(inventory);
    }

    private ItemStack createFillerItem() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createVaultButton(Player admin) {
        ItemStack item = new ItemStack(Material.GOLD_INGOT, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            double balance = treasuryManager.getVaultBalance();
            String formattedBalance = plugin.getVaultHook().format(balance);

            TagResolver[] resolvers = new TagResolver[]{
                    Placeholder.parsed("amount", formattedBalance),
                    Placeholder.parsed("currency", plugin.getVaultHook().getCurrencyName())
            };

            meta.displayName(lang.getComponent(admin, "gui.treasury-vault.name", resolvers));

            List<String> rawLore = lang.getRawList(admin, "gui.treasury-vault.lore");
            List<Component> lore = new ArrayList<>();
            for (String line : rawLore) {
                lore.add(miniMessage.deserialize(line, resolvers));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createExpButton(Player admin) {
        ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            int exp = treasuryManager.getExpPoints();
            String formattedExp = lang.formatExpPoints(admin, exp);

            TagResolver[] resolvers = new TagResolver[]{
                    Placeholder.parsed("amount", formattedExp),
                    Placeholder.parsed("points", String.valueOf(exp))
            };

            meta.displayName(lang.getComponent(admin, "gui.treasury-exp.name", resolvers));

            List<String> rawLore = lang.getRawList(admin, "gui.treasury-exp.lore");
            List<Component> lore = new ArrayList<>();
            for (String line : rawLore) {
                lore.add(miniMessage.deserialize(line, resolvers));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createLogsButton(Player admin) {
        ItemStack item = new ItemStack(Material.BOOK, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(lang.getComponent(admin, "gui.treasury-logs.name"));
            List<String> rawLore = lang.getRawList(admin, "gui.treasury-logs.lore");
            List<Component> lore = new ArrayList<>();
            for (String line : rawLore) {
                lore.add(miniMessage.deserialize(line));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createRefreshButton(Player admin) {
        ItemStack item = new ItemStack(Material.CLOCK, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(lang.getComponent(admin, "gui.treasury-refresh.name"));
            List<String> rawLore = lang.getRawList(admin, "gui.treasury-refresh.lore");
            List<Component> lore = new ArrayList<>();
            for (String line : rawLore) {
                lore.add(miniMessage.deserialize(line));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}

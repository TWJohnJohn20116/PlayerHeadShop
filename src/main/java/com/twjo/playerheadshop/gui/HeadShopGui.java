package com.twjo.playerheadshop.gui;

import com.twjo.playerheadshop.PlayerHeadShop;
import com.twjo.playerheadshop.config.PluginConfig;
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
import java.util.Map;

/**
 * 負責構建與開啟 PlayerHeadShop 的箱子 GUI 介面（支援物品、Vault、經驗等級與經驗點數等多種支付模式）
 */
public class HeadShopGui {

    private final PlayerHeadShop plugin;
    private final PluginConfig config;
    private final LanguageManager lang;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public HeadShopGui(PlayerHeadShop plugin, PluginConfig config, LanguageManager lang) {
        this.plugin = plugin;
        this.config = config;
        this.lang = lang;
    }

    /**
     * 為指定玩家建立並開啟自訂頭顱商店主選單 GUI
     */
    public void open(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        int rows = config.getGuiRows();
        int size = rows * 9;
        Component title = lang.getComponent(player, "gui.main-title");

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
     * 開啟專屬放置兌換介面 (Deposit & Trade GUI)
     */
    public void openDepositGui(Player player, ShopOption option) {
        if (player == null || !player.isOnline() || option == null) {
            return;
        }

        int size = 27; // 3 行
        Component title = lang.getComponent(player, "gui.deposit-title",
                Placeholder.parsed("head_amount", lang.formatAmount(player, option.getHeadAmount()))
        );

        DepositGuiHolder holder = new DepositGuiHolder(player, option);
        Inventory inventory = Bukkit.createInventory(holder, size, title);
        holder.setInventory(inventory);

        // 1. 背景填滿玻璃板 (除放置區以外)
        ItemStack filler = createFillerItem();
        for (int i = 0; i < size; i++) {
            if (!DepositGuiHolder.isInputSlot(i)) {
                inventory.setItem(i, filler);
            }
        }

        // 2. 確保放置區清空 (Slots: 10, 11, 12, 19, 20, 21)
        for (int slot : DepositGuiHolder.INPUT_SLOTS) {
            inventory.setItem(slot, null);
        }

        // 3. Slot 14 放置確認兌換按鈕
        ItemStack confirmButton = createConfirmButton(player, option);
        inventory.setItem(DepositGuiHolder.CONFIRM_SLOT, confirmButton);

        // 4. Slot 16 放置產出預覽頭顱
        ItemStack previewHead = createPreviewHead(player, option);
        inventory.setItem(DepositGuiHolder.PREVIEW_SLOT, previewHead);

        // 5. Slot 18 放置返回按鈕
        ItemStack backButton = createBackButton(player);
        inventory.setItem(DepositGuiHolder.BACK_SLOT, backButton);

        // 6. 開啟介面
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
     * 建立主選單中的商品頭顱圖示
     */
    private ItemStack createOptionIcon(Player player, ShopOption option) {
        int iconAmount = Math.min(64, Math.max(1, option.getHeadAmount()));
        ItemStack item = new ItemStack(Material.PLAYER_HEAD, iconAmount);

        if (item.getItemMeta() instanceof SkullMeta skullMeta) {
            try {
                skullMeta.setPlayerProfile(player.getPlayerProfile());
            } catch (Throwable ignored) {
                skullMeta.setOwningPlayer(player);
            }

            String formattedCost;
            String costItemName;
            String defaultLoreKey;

            switch (option.getCostType()) {
                case VAULT -> {
                    formattedCost = plugin.getVaultHook().format(option.getCostAmount());
                    costItemName = plugin.getVaultHook().getCurrencyName();
                    defaultLoreKey = "gui.vault-button.lore";
                }
                case EXP_LEVEL -> {
                    formattedCost = lang.formatExpLevel(player, option.getCostAmountInt());
                    costItemName = lang.formatExpLevel(player, option.getCostAmountInt());
                    defaultLoreKey = "gui.exp-level-button.lore";
                }
                case EXP_POINTS -> {
                    formattedCost = lang.formatExpPoints(player, option.getCostAmountInt());
                    costItemName = lang.formatExpPoints(player, option.getCostAmountInt());
                    defaultLoreKey = "gui.exp-points-button.lore";
                }
                default -> {
                    formattedCost = lang.formatAmount(player, option.getCostAmountInt());
                    costItemName = option.getCostItem().name();
                    defaultLoreKey = "gui.confirm-button.lore";
                }
            }

            TagResolver[] resolvers = new TagResolver[]{
                    Placeholder.parsed("player", player.getName()),
                    Placeholder.parsed("head_amount", lang.formatAmount(player, option.getHeadAmount())),
                    Placeholder.parsed("cost_amount", formattedCost),
                    Placeholder.parsed("cost_item", costItemName),
                    Placeholder.parsed("amount", formattedCost),
                    Placeholder.parsed("item", costItemName)
            };

            // 若自訂了名稱則優先使用自訂名稱，否則使用語言檔
            if (option.getDisplayName() != null && !option.getDisplayName().isEmpty()) {
                Component displayName = miniMessage.deserialize(option.getDisplayName(), resolvers);
                skullMeta.displayName(displayName);
            } else {
                Component displayName = lang.getComponent(player, "gui.preview-head.name", resolvers);
                skullMeta.displayName(displayName);
            }

            // 若自訂了說明文字則優先使用，否則使用語言檔
            if (option.getLore() != null && !option.getLore().isEmpty()) {
                List<Component> loreComponents = new ArrayList<>();
                for (String line : option.getLore()) {
                    loreComponents.add(miniMessage.deserialize(line, resolvers));
                }
                skullMeta.lore(loreComponents);
            } else {
                List<String> rawLore = lang.getRawList(player, defaultLoreKey);
                List<Component> loreComponents = new ArrayList<>();
                for (String line : rawLore) {
                    loreComponents.add(miniMessage.deserialize(line, resolvers));
                }
                skullMeta.lore(loreComponents);
            }

            item.setItemMeta(skullMeta);
        }

        return item;
    }

    /**
     * 建立確認兌換按鈕
     */
    private ItemStack createConfirmButton(Player player, ShopOption option) {
        ItemStack item = new ItemStack(Material.EMERALD, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String formattedCost = lang.formatAmount(player, option.getCostAmountInt());
            String costItemName = option.getCostItem().name();

            TagResolver[] resolvers = new TagResolver[]{
                    Placeholder.parsed("player", player.getName()),
                    Placeholder.parsed("head_amount", lang.formatAmount(player, option.getHeadAmount())),
                    Placeholder.parsed("cost_amount", formattedCost),
                    Placeholder.parsed("cost_item", costItemName),
                    Placeholder.parsed("amount", String.valueOf(option.getCostAmountInt())),
                    Placeholder.parsed("item", costItemName)
            };

            meta.displayName(lang.getComponent(player, "gui.confirm-button.name", resolvers));

            List<String> rawLore = lang.getRawList(player, "gui.confirm-button.lore");
            List<Component> lore = new ArrayList<>();
            for (String line : rawLore) {
                lore.add(miniMessage.deserialize(line, resolvers));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * 建立產出預覽頭顱
     */
    private ItemStack createPreviewHead(Player player, ShopOption option) {
        int amount = Math.min(64, Math.max(1, option.getHeadAmount()));
        ItemStack item = new ItemStack(Material.PLAYER_HEAD, amount);
        if (item.getItemMeta() instanceof SkullMeta skullMeta) {
            try {
                skullMeta.setPlayerProfile(player.getPlayerProfile());
            } catch (Throwable ignored) {
                skullMeta.setOwningPlayer(player);
            }

            String formattedCost = lang.formatAmount(player, option.getCostAmountInt());
            String costItemName = option.getCostItem().name();

            TagResolver[] resolvers = new TagResolver[]{
                    Placeholder.parsed("player", player.getName()),
                    Placeholder.parsed("head_amount", lang.formatAmount(player, option.getHeadAmount())),
                    Placeholder.parsed("cost_amount", formattedCost),
                    Placeholder.parsed("cost_item", costItemName),
                    Placeholder.parsed("amount", String.valueOf(option.getCostAmountInt())),
                    Placeholder.parsed("item", costItemName)
            };

            skullMeta.displayName(lang.getComponent(player, "gui.preview-head.name", resolvers));

            List<String> rawLore = lang.getRawList(player, "gui.preview-head.lore");
            List<Component> lore = new ArrayList<>();
            for (String line : rawLore) {
                lore.add(miniMessage.deserialize(line, resolvers));
            }
            skullMeta.lore(lore);
            item.setItemMeta(skullMeta);
        }
        return item;
    }

    /**
     * 建立返回主選單按鈕
     */
    private ItemStack createBackButton(Player player) {
        ItemStack item = new ItemStack(Material.ARROW, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(lang.getComponent(player, "gui.back-button.name"));
            List<String> rawLore = lang.getRawList(player, "gui.back-button.lore");
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

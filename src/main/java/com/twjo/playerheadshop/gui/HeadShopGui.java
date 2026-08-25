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
 * 負責構建與開啟 PlayerHeadShop 的箱子 GUI 介面（支援主選單與多格放置兌換介面）
 */
public class HeadShopGui {

    private final PluginConfig config;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public HeadShopGui(PluginConfig config) {
        this.config = config;
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
     * 開啟專屬放置兌換介面 (Deposit & Trade GUI)
     */
    public void openDepositGui(Player player, ShopOption option) {
        if (player == null || !player.isOnline() || option == null) {
            return;
        }

        int size = 27; // 3 行
        Component title = miniMessage.deserialize("<gradient:#FFAA00:#FF5555><bold>放置物品兌換</bold></gradient> <gray>-</gray> <yellow>" + option.getHeadAmount() + " 個頭顱</yellow>");

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
        ItemStack confirmButton = createConfirmButton(option);
        inventory.setItem(DepositGuiHolder.CONFIRM_SLOT, confirmButton);

        // 4. Slot 16 放置產出預覽頭顱
        ItemStack previewHead = createPreviewHead(player, option);
        inventory.setItem(DepositGuiHolder.PREVIEW_SLOT, previewHead);

        // 5. Slot 18 放置返回按鈕
        ItemStack backButton = createBackButton();
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

            TagResolver[] resolvers = new TagResolver[]{
                    Placeholder.parsed("player", player.getName()),
                    Placeholder.parsed("head_amount", formatAmountDescription(option.getHeadAmount())),
                    Placeholder.parsed("cost_amount", formatAmountDescription(option.getCostAmount())),
                    Placeholder.parsed("cost_item", option.getCostItem().name()),
                    Placeholder.parsed("amount", String.valueOf(option.getCostAmount())),
                    Placeholder.parsed("item", option.getCostItem().name())
            };

            if (option.getDisplayName() != null && !option.getDisplayName().isEmpty()) {
                Component displayName = miniMessage.deserialize(option.getDisplayName(), resolvers);
                skullMeta.displayName(displayName);
            }

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

    /**
     * 建立確認兌換按鈕
     */
    private ItemStack createConfirmButton(ShopOption option) {
        ItemStack item = new ItemStack(Material.EMERALD, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(miniMessage.deserialize("<green><bold>▶ 點擊確認兌換</bold></green>"));
            List<Component> lore = List.of(
                    miniMessage.deserialize("<gray>請在左側放置區放入所需物品：</gray>"),
                    miniMessage.deserialize("<white>需求: <aqua>" + formatAmountDescription(option.getCostAmount()) + " " + option.getCostItem().name() + "</aqua></white>"),
                    miniMessage.deserialize("<white>獲得: <gold>" + formatAmountDescription(option.getHeadAmount()) + " 你的個人頭顱</gold></white>"),
                    Component.empty(),
                    miniMessage.deserialize("<gray>(放置區支援放置多組物品，超過 64 個可分格放置)</gray>"),
                    Component.empty(),
                    miniMessage.deserialize("<yellow>放好後點擊此處即可完成購買！</yellow>")
            );
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
            skullMeta.displayName(miniMessage.deserialize("<gold><bold>獲得: " + formatAmountDescription(option.getHeadAmount()) + " 個人頭顱</bold></gold>"));
            skullMeta.lore(List.of(
                    miniMessage.deserialize("<gray>這是你即將獲得的皮膚外觀頭顱預覽。</gray>"),
                    miniMessage.deserialize("<gray>數量超過 64 個時將自動為您分組打包。</gray>"),
                    Component.empty(),
                    miniMessage.deserialize("<green>點擊此處亦可確認兌換！</green>")
            ));
            item.setItemMeta(skullMeta);
        }
        return item;
    }

    /**
     * 建立返回主選單按鈕
     */
    private ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.ARROW, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(miniMessage.deserialize("<red><bold>◀ 返回方案選單</bold></red>"));
            meta.lore(List.of(
                    miniMessage.deserialize("<gray>返回主選單，未兌換的物品將自動歸還至背包。</gray>")
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * 格式化數量描述（當數量超過 64 個時標註組數）
     */
    private String formatAmountDescription(int amount) {
        if (amount <= 64) {
            return amount + " 個";
        }
        int stacks = amount / 64;
        int rem = amount % 64;
        if (rem == 0) {
            return amount + " 個 (" + stacks + " 組)";
        }
        return amount + " 個 (" + stacks + " 組 + " + rem + " 個)";
    }
}

package com.twjo.playerheadshop.market;

import com.twjo.playerheadshop.PlayerHeadShop;
import com.twjo.playerheadshop.config.ShopOption;
import com.twjo.playerheadshop.database.DatabaseManager;
import com.twjo.playerheadshop.lang.LanguageManager;
import com.twjo.playerheadshop.util.ExperienceUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 管理社群頭顱分享、市集上架、下架與購買結算
 */
public class MarketManager {

    private final PlayerHeadShop plugin;
    private final DatabaseManager databaseManager;
    private final LanguageManager lang;

    public MarketManager(PlayerHeadShop plugin, DatabaseManager databaseManager, LanguageManager lang) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.lang = lang;
    }

    /**
     * 玩家發布/上架頭顱至社群市集
     *
     * <p>上架數量上限的檢查與寫入由 {@link DatabaseManager#addSharedHead} 在同一個資料庫任務中完成，
     * 因此連點兩下不會同時通過檢查。</p>
     */
    public CompletableFuture<Boolean> publishHead(Player seller, String title, ShopOption.CostType costType,
                                                  String costItem, double costAmount, int headAmount) {
        if (!plugin.getPluginConfig().isMarketEnabled()) {
            lang.sendMessage(seller, "market-disabled");
            return CompletableFuture.completedFuture(false);
        }

        int maxListings = seller.hasPermission("playerheadshop.admin")
                ? 0 // 0 代表不限制
                : plugin.getPluginConfig().getMarketMaxListings();

        String finalTitle = (title != null && !title.isEmpty())
                ? title
                : lang.getRaw(seller, "market.default-head-name", "<player> 的頭顱")
                        .replace("<player>", seller.getName());

        return databaseManager.addSharedHead(
                seller.getUniqueId(),
                seller.getName(),
                finalTitle,
                seller.getName(),
                seller.getUniqueId(),
                costType.name(),
                costItem,
                costAmount,
                headAmount,
                maxListings
        ).thenApply(id -> {
            // 所有玩家互動都必須跳回該玩家所屬的區域執行緒
            plugin.getSchedulerAdapter().runForEntity(seller, () -> {
                if (id == -2L) {
                    lang.sendMessage(seller, "market-max-listings",
                            Placeholder.parsed("max", String.valueOf(plugin.getPluginConfig().getMarketMaxListings()))
                    );
                    playSound(seller, Sound.ENTITY_VILLAGER_NO, 1.0f);
                } else if (id > 0) {
                    lang.sendMessage(seller, "market-publish-success",
                            Placeholder.parsed("id", String.valueOf(id)),
                            Placeholder.parsed("title", finalTitle)
                    );
                    playSound(seller, Sound.ENTITY_PLAYER_LEVELUP, 1.2f);
                } else {
                    lang.sendMessage(seller, "market-publish-failed");
                }
            });
            return id > 0;
        });
    }

    /**
     * 下架社群頭顱
     */
    public CompletableFuture<Boolean> unlistHead(Player player, long id, boolean isAdmin) {
        return databaseManager.removeSharedHead(id, player.getUniqueId(), isAdmin).thenApply(success -> {
            plugin.getSchedulerAdapter().runForEntity(player, () -> {
                if (success) {
                    lang.sendMessage(player, "market-unlist-success", Placeholder.parsed("id", String.valueOf(id)));
                } else {
                    lang.sendMessage(player, "market-unlist-failed");
                }
            });
            return success;
        });
    }

    /**
     * 購買社群分享的頭顱
     *
     * <p>{@code head} 必須是剛從資料庫重新讀取的最新狀態（見
     * {@link DatabaseManager#getActiveSharedHeadById}），不可使用 GUI 開啟時的快照，否則會買到已下架商品。</p>
     */
    public boolean purchaseSharedHead(Player buyer, SharedHeadRecord head) {
        if (buyer == null || !buyer.isOnline() || head == null || !head.isActive()) {
            return false;
        }

        ShopOption.CostType type = head.getCostType();
        int headAmount = head.getHeadAmount();

        // 1. 處理 Vault 金幣付款
        if (type == ShopOption.CostType.VAULT) {
            double cost = head.getCostAmount();
            if (!plugin.getVaultHook().hasEconomy()) {
                lang.sendMessage(buyer, "vault-disabled");
                return false;
            }
            if (!plugin.getVaultHook().has(buyer, cost)) {
                double current = plugin.getVaultHook().getBalance(buyer);
                lang.sendMessage(buyer, "not-enough-money",
                        Placeholder.parsed("required", plugin.getVaultHook().format(cost)),
                        Placeholder.parsed("current", plugin.getVaultHook().format(current)),
                        Placeholder.parsed("missing", plugin.getVaultHook().format(cost - current))
                );
                playSound(buyer, Sound.ENTITY_VILLAGER_NO, 1.0f);
                return false;
            }
            if (!plugin.getVaultHook().withdraw(buyer, cost)) {
                return false;
            }
            distributeVaultRevenue(head, cost);
        }
        // 2. 處理經驗等級付款
        else if (type == ShopOption.CostType.EXP_LEVEL) {
            int reqLvl = head.getCostAmountInt();
            int curLvl = buyer.getLevel();
            if (curLvl < reqLvl) {
                lang.sendMessage(buyer, "not-enough-exp-level",
                        Placeholder.parsed("required", lang.formatExpLevel(buyer, reqLvl)),
                        Placeholder.parsed("current", lang.formatExpLevel(buyer, curLvl)),
                        Placeholder.parsed("missing", lang.formatExpLevel(buyer, reqLvl - curLvl))
                );
                playSound(buyer, Sound.ENTITY_VILLAGER_NO, 1.0f);
                return false;
            }
            buyer.setLevel(curLvl - reqLvl);
            if (plugin.getPluginConfig().isPoolEnabled() && plugin.getPluginConfig().isPoolCollectExp() && plugin.getTreasuryManager() != null) {
                int points = ExperienceUtil.getTotalExpToLevel(curLvl) - ExperienceUtil.getTotalExpToLevel(curLvl - reqLvl);
                plugin.getTreasuryManager().depositExp(points);
            }
        }
        // 3. 處理經驗點數付款
        else if (type == ShopOption.CostType.EXP_POINTS) {
            int reqPts = head.getCostAmountInt();
            int curPts = ExperienceUtil.getPlayerTotalExp(buyer);
            if (curPts < reqPts) {
                lang.sendMessage(buyer, "not-enough-exp-points",
                        Placeholder.parsed("required", lang.formatExpPoints(buyer, reqPts)),
                        Placeholder.parsed("current", lang.formatExpPoints(buyer, curPts)),
                        Placeholder.parsed("missing", lang.formatExpPoints(buyer, reqPts - curPts))
                );
                playSound(buyer, Sound.ENTITY_VILLAGER_NO, 1.0f);
                return false;
            }
            ExperienceUtil.deductPlayerExp(buyer, reqPts);
            if (plugin.getPluginConfig().isPoolEnabled() && plugin.getPluginConfig().isPoolCollectExp() && plugin.getTreasuryManager() != null) {
                plugin.getTreasuryManager().depositExp(reqPts);
            }
        }
        // 4. 處理實體物品付款 (ITEM)
        else {
            Material mat = Material.matchMaterial(head.getCostItem());
            if (mat == null) mat = Material.DIAMOND;
            int reqItem = head.getCostAmountInt();
            int curItem = countItems(buyer, mat);
            if (curItem < reqItem) {
                lang.sendMessage(buyer, "not-enough-items",
                        Placeholder.parsed("required", lang.formatAmount(buyer, reqItem)),
                        Placeholder.parsed("item", mat.name()),
                        Placeholder.parsed("current", lang.formatAmount(buyer, curItem)),
                        Placeholder.parsed("missing", lang.formatAmount(buyer, reqItem - curItem))
                );
                playSound(buyer, Sound.ENTITY_VILLAGER_NO, 1.0f);
                return false;
            }
            deductItems(buyer, mat, reqItem);
            if (plugin.getPluginConfig().isPoolEnabled() && plugin.getPluginConfig().isPoolCollectItems() && plugin.getTreasuryManager() != null) {
                plugin.getTreasuryManager().depositItems(List.of(new ItemStack(mat, reqItem)));
            }
        }

        // 5. 生成帶有原創者皮膚的頭顱物品並發放
        giveSharedHeadToPlayer(buyer, head.getSkinOwnerUuid(), headAmount);

        // 6. 增加市集銷量與記錄日誌
        databaseManager.incrementSharedHeadSales(head.getId());
        databaseManager.logTrade(buyer.getUniqueId(), buyer.getName(), head.getCostItem(), head.getCostAmountInt(), headAmount);

        // 7. 發送成功提示
        lang.sendMessage(buyer, "market-buy-success",
                Placeholder.parsed("seller", head.getSellerName()),
                Placeholder.parsed("head_amount", lang.formatAmount(buyer, headAmount))
        );

        playSound(buyer, Sound.ENTITY_PLAYER_LEVELUP, 1.2f);

        return true;
    }

    /**
     * 依 market.seller-payout-percent 將 Vault 收入分配給賣家，餘額進入伺服器金庫
     */
    private void distributeVaultRevenue(SharedHeadRecord head, double cost) {
        double percent = plugin.getPluginConfig().getMarketSellerPayoutPercent();
        double sellerShare = cost * (percent / 100.0);
        double treasuryShare = cost - sellerShare;

        if (sellerShare > 0) {
            OfflinePlayer seller = Bukkit.getOfflinePlayer(head.getSellerUuid());
            if (!plugin.getVaultHook().deposit(seller, sellerShare)) {
                // 無法付給賣家時，該筆金額改入伺服器金庫，避免金錢憑空消失
                plugin.getLogger().warning("無法將 " + plugin.getVaultHook().format(sellerShare)
                        + " 分潤付給賣家 " + head.getSellerName() + "，已改為匯入收益金庫。");
                treasuryShare += sellerShare;
            } else {
                Player onlineSeller = seller.getPlayer();
                if (onlineSeller != null) {
                    plugin.getSchedulerAdapter().runForEntity(onlineSeller, () ->
                            lang.sendMessage(onlineSeller, "market-sale-payout",
                                    Placeholder.parsed("amount", plugin.getVaultHook().format(sellerShare)),
                                    Placeholder.parsed("title", head.getHeadName())
                            ));
                }
            }
        }

        if (treasuryShare > 0
                && plugin.getPluginConfig().isPoolEnabled()
                && plugin.getPluginConfig().isPoolCollectVault()
                && plugin.getTreasuryManager() != null) {
            plugin.getTreasuryManager().depositVault(treasuryShare);
        }
    }

    private void giveSharedHeadToPlayer(Player buyer, UUID skinOwnerUuid, int totalAmount) {
        List<ItemStack> list = new ArrayList<>();
        int remaining = totalAmount;

        OfflinePlayer skinPlayer = Bukkit.getOfflinePlayer(skinOwnerUuid);

        while (remaining > 0) {
            int size = Math.min(64, remaining);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD, size);
            if (head.getItemMeta() instanceof SkullMeta skullMeta) {
                try {
                    skullMeta.setOwningPlayer(skinPlayer);
                } catch (Throwable ignored) {}
                head.setItemMeta(skullMeta);
            }
            list.add(head);
            remaining -= size;
        }

        boolean hasOverflow = false;
        for (ItemStack stack : list) {
            HashMap<Integer, ItemStack> overflow = buyer.getInventory().addItem(stack);
            if (!overflow.isEmpty()) {
                hasOverflow = true;
                for (ItemStack leftover : overflow.values()) {
                    buyer.getWorld().dropItemNaturally(buyer.getLocation(), leftover);
                }
            }
        }

        if (hasOverflow) {
            lang.sendMessage(buyer, "inventory-full");
        }
    }

    private void playSound(Player player, Sound sound, float pitch) {
        try {
            player.playSound(player.getLocation(), sound, 1.0f, pitch);
        } catch (Throwable ignored) {}
    }

    private int countItems(Player player, Material material) {
        int total = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item != null && item.getType() == material) {
                total += item.getAmount();
            }
        }
        return total;
    }

    private void deductItems(Player player, Material material, int amountToDeduct) {
        ItemStack[] storage = player.getInventory().getStorageContents();
        int remaining = amountToDeduct;
        for (int i = 0; i < storage.length; i++) {
            ItemStack item = storage[i];
            if (item != null && item.getType() == material) {
                int count = item.getAmount();
                if (count <= remaining) {
                    remaining -= count;
                    storage[i] = null;
                } else {
                    item.setAmount(count - remaining);
                    remaining = 0;
                }
                if (remaining <= 0) break;
            }
        }
        player.getInventory().setStorageContents(storage);
    }
}

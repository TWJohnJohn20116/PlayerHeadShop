package com.twjo.playerheadshop.market;

import com.twjo.playerheadshop.config.ShopOption;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * 社群分享頭顱資料模型
 */
public class SharedHeadRecord {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private final long id;
    private final UUID sellerUuid;
    private final String sellerName;
    private final String headName;
    private final String skinOwner;
    private final UUID skinOwnerUuid;
    private final ShopOption.CostType costType;
    private final String costItem;
    private final double costAmount;
    private final int headAmount;
    private final long createdAt;
    private final int salesCount;
    private final boolean isActive;

    public SharedHeadRecord(long id, UUID sellerUuid, String sellerName, String headName, String skinOwner,
                            UUID skinOwnerUuid, ShopOption.CostType costType, String costItem, double costAmount,
                            int headAmount, long createdAt, int salesCount, boolean isActive) {
        this.id = id;
        this.sellerUuid = sellerUuid;
        this.sellerName = sellerName;
        this.headName = headName;
        this.skinOwner = skinOwner;
        this.skinOwnerUuid = skinOwnerUuid;
        this.costType = costType;
        this.costItem = costItem;
        this.costAmount = costAmount;
        this.headAmount = headAmount;
        this.createdAt = createdAt;
        this.salesCount = salesCount;
        this.isActive = isActive;
    }

    public long getId() {
        return id;
    }

    public UUID getSellerUuid() {
        return sellerUuid;
    }

    public String getSellerName() {
        return sellerName;
    }

    public String getHeadName() {
        return headName;
    }

    public String getSkinOwner() {
        return skinOwner;
    }

    /**
     * 皮膚擁有者 UUID。以 UUID 取得 OfflinePlayer 不會觸發阻塞式 Mojang API 名稱查詢，
     * 且玩家改名後皮膚依然正確。
     */
    public UUID getSkinOwnerUuid() {
        return skinOwnerUuid;
    }

    public ShopOption.CostType getCostType() {
        return costType;
    }

    public String getCostItem() {
        return costItem;
    }

    public double getCostAmount() {
        return costAmount;
    }

    public int getCostAmountInt() {
        return (int) Math.round(costAmount);
    }

    public int getHeadAmount() {
        return headAmount;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public String getFormattedDate() {
        synchronized (DATE_FORMAT) {
            return DATE_FORMAT.format(new Date(createdAt));
        }
    }

    public int getSalesCount() {
        return salesCount;
    }

    public boolean isActive() {
        return isActive;
    }
}

package com.twjo.playerheadshop.database;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * 兌換交易記錄模型
 */
public class TradeRecord {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final long id;
    private final long timestamp;
    private final UUID playerUuid;
    private final String playerName;
    private final String costItem;
    private final int costAmount;
    private final int headAmount;

    public TradeRecord(long id, long timestamp, UUID playerUuid, String playerName, String costItem, int costAmount, int headAmount) {
        this.id = id;
        this.timestamp = timestamp;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.costItem = costItem;
        this.costAmount = costAmount;
        this.headAmount = headAmount;
    }

    public long getId() {
        return id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getFormattedTime() {
        synchronized (DATE_FORMAT) {
            return DATE_FORMAT.format(new Date(timestamp));
        }
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getCostItem() {
        return costItem;
    }

    public int getCostAmount() {
        return costAmount;
    }

    public int getHeadAmount() {
        return headAmount;
    }
}

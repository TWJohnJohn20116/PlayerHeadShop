package com.twjo.playerheadshop.database;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * 金庫管理員提領稽核記錄模型
 */
public class TreasuryLogRecord {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final long id;
    private final long timestamp;
    private final UUID adminUuid;
    private final String adminName;
    private final String actionType;
    private final String detail;
    private final double amount;

    public TreasuryLogRecord(long id, long timestamp, UUID adminUuid, String adminName, String actionType, String detail, double amount) {
        this.id = id;
        this.timestamp = timestamp;
        this.adminUuid = adminUuid;
        this.adminName = adminName;
        this.actionType = actionType;
        this.detail = detail;
        this.amount = amount;
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

    public UUID getAdminUuid() {
        return adminUuid;
    }

    public String getAdminName() {
        return adminName;
    }

    public String getActionType() {
        return actionType;
    }

    public String getDetail() {
        return detail;
    }

    public double getAmount() {
        return amount;
    }
}

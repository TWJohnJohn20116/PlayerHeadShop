package com.twjo.playerheadshop.database;

import com.twjo.playerheadshop.PlayerHeadShop;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/**
 * 管理 SQLite 資料庫連線、記錄儲存與非同步歷史查詢
 */
public class DatabaseManager {

    private final PlayerHeadShop plugin;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Connection connection;

    public DatabaseManager(PlayerHeadShop plugin) {
        this.plugin = plugin;
        initDatabase();
    }

    /**
     * 初始化 SQLite 資料庫與表格結構
     */
    private void initDatabase() {
        executor.submit(() -> {
            try {
                File dataFolder = plugin.getDataFolder();
                if (!dataFolder.exists()) {
                    dataFolder.mkdirs();
                }

                File dbFile = new File(dataFolder, "database.db");
                String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();

                this.connection = DriverManager.getConnection(url);

                try (Statement stmt = connection.createStatement()) {
                    // 建立交易記錄表
                    stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS trade_history (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            timestamp INTEGER NOT NULL,
                            player_uuid TEXT NOT NULL,
                            player_name TEXT NOT NULL,
                            cost_item TEXT NOT NULL,
                            cost_amount INTEGER NOT NULL,
                            head_amount INTEGER NOT NULL
                        );
                    """);

                    // 建立索引提升查詢效率
                    stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_player ON trade_history(player_name);");
                    stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_time ON trade_history(timestamp DESC);");
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "無法初始化 SQLite 資料庫", e);
            }
        });
    }

    /**
     * 非同步記錄一筆兌換交易
     */
    public CompletableFuture<Void> logTrade(UUID playerUuid, String playerName, String costItem, int costAmount, int headAmount) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (connection == null || connection.isClosed()) {
                    return;
                }

                String sql = "INSERT INTO trade_history (timestamp, player_uuid, player_name, cost_item, cost_amount, head_amount) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setLong(1, System.currentTimeMillis());
                    pstmt.setString(2, playerUuid.toString());
                    pstmt.setString(3, playerName);
                    pstmt.setString(4, costItem);
                    pstmt.setInt(5, costAmount);
                    pstmt.setInt(6, headAmount);
                    pstmt.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "記錄交易失敗: " + playerName, e);
            }
        }, executor);
    }

    /**
     * 非同步獲取記錄總數（可選指定玩家）
     */
    public CompletableFuture<Integer> getTotalRecords(String playerFilter) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (connection == null || connection.isClosed()) {
                    return 0;
                }

                String sql = (playerFilter != null && !playerFilter.isEmpty())
                        ? "SELECT COUNT(*) FROM trade_history WHERE LOWER(player_name) = LOWER(?)"
                        : "SELECT COUNT(*) FROM trade_history";

                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    if (playerFilter != null && !playerFilter.isEmpty()) {
                        pstmt.setString(1, playerFilter);
                    }
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            return rs.getInt(1);
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "查詢記錄總數失敗", e);
            }
            return 0;
        }, executor);
    }

    /**
     * 非同步分頁查詢記錄清單
     */
    public CompletableFuture<List<TradeRecord>> getRecords(String playerFilter, int page, int pageSize) {
        return CompletableFuture.supplyAsync(() -> {
            List<TradeRecord> list = new ArrayList<>();
            try {
                if (connection == null || connection.isClosed()) {
                    return list;
                }

                int offset = Math.max(0, (page - 1) * pageSize);
                String sql = (playerFilter != null && !playerFilter.isEmpty())
                        ? "SELECT id, timestamp, player_uuid, player_name, cost_item, cost_amount, head_amount FROM trade_history WHERE LOWER(player_name) = LOWER(?) ORDER BY timestamp DESC LIMIT ? OFFSET ?"
                        : "SELECT id, timestamp, player_uuid, player_name, cost_item, cost_amount, head_amount FROM trade_history ORDER BY timestamp DESC LIMIT ? OFFSET ?";

                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    int idx = 1;
                    if (playerFilter != null && !playerFilter.isEmpty()) {
                        pstmt.setString(idx++, playerFilter);
                    }
                    pstmt.setInt(idx++, pageSize);
                    pstmt.setInt(idx, offset);

                    try (ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) {
                            long id = rs.getLong("id");
                            long timestamp = rs.getLong("timestamp");
                            UUID uuid = UUID.fromString(rs.getString("player_uuid"));
                            String name = rs.getString("player_name");
                            String costItem = rs.getString("cost_item");
                            int costAmount = rs.getInt("cost_amount");
                            int headAmount = rs.getInt("head_amount");

                            list.add(new TradeRecord(id, timestamp, uuid, name, costItem, costAmount, headAmount));
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "分頁查詢記錄失敗", e);
            }
            return list;
        }, executor);
    }

    /**
     * 關閉資料庫連線與執行緒池
     */
    public void close() {
        executor.submit(() -> {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException ignored) {}
        });
        executor.shutdown();
    }
}

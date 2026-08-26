package com.twjo.playerheadshop.database;

import com.twjo.playerheadshop.PlayerHeadShop;
import com.twjo.playerheadshop.config.ShopOption;
import com.twjo.playerheadshop.market.SharedHeadRecord;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

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
 * 管理 SQLite 資料庫連線、交易記錄、收益金庫持久化、管理員提領稽核日誌與社群頭顱市集
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
                try {
                    Class.forName("org.sqlite.JDBC");
                } catch (ClassNotFoundException ignored) {}

                File dataFolder = plugin.getDataFolder();
                if (!dataFolder.exists()) {
                    dataFolder.mkdirs();
                }

                File dbFile = new File(dataFolder, "database.db");
                String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();

                this.connection = DriverManager.getConnection(url);

                try (Statement stmt = connection.createStatement()) {
                    // 1. 交易記錄表
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
                    stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_player ON trade_history(player_name);");
                    stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_time ON trade_history(timestamp DESC);");

                    // 2. 金庫資產表 (金幣與經驗)
                    stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS treasury_balance (
                            id INTEGER PRIMARY KEY,
                            vault_balance REAL NOT NULL DEFAULT 0.0,
                            exp_points INTEGER NOT NULL DEFAULT 0
                        );
                    """);
                    stmt.executeUpdate("INSERT OR IGNORE INTO treasury_balance (id, vault_balance, exp_points) VALUES (1, 0.0, 0);");

                    // 3. 金庫物品表 (實體物品槽)
                    stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS treasury_items (
                            slot INTEGER PRIMARY KEY,
                            material TEXT NOT NULL,
                            amount INTEGER NOT NULL,
                            item_bytes BLOB
                        );
                    """);

                    // 4. 管理員提領稽核表記錄表
                    stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS treasury_logs (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            timestamp INTEGER NOT NULL,
                            admin_uuid TEXT NOT NULL,
                            admin_name TEXT NOT NULL,
                            action_type TEXT NOT NULL,
                            detail TEXT NOT NULL,
                            amount REAL NOT NULL
                        );
                    """);
                    stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_treasury_time ON treasury_logs(timestamp DESC);");

                    // 5. 社群頭顱分享與市集表
                    stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS shared_heads (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            seller_uuid TEXT NOT NULL,
                            seller_name TEXT NOT NULL,
                            head_name TEXT NOT NULL,
                            skin_owner TEXT NOT NULL,
                            cost_type TEXT NOT NULL,
                            cost_item TEXT NOT NULL,
                            cost_amount REAL NOT NULL,
                            head_amount INTEGER NOT NULL,
                            created_at INTEGER NOT NULL,
                            sales_count INTEGER DEFAULT 0,
                            is_active INTEGER DEFAULT 1
                        );
                    """);
                    stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_shared_active ON shared_heads(is_active, created_at DESC);");
                    stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_shared_seller ON shared_heads(seller_uuid, is_active);");
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "無法初始化 SQLite 資料庫", e);
            }
        });
    }

    // ==========================================
    // 交易歷史記錄 (Trade History)
    // ==========================================

    public CompletableFuture<Void> logTrade(UUID playerUuid, String playerName, String costItem, int costAmount, int headAmount) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (connection == null || connection.isClosed()) return;
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

    public CompletableFuture<Integer> getTotalRecords(String playerFilter) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (connection == null || connection.isClosed()) return 0;
                String sql = (playerFilter != null && !playerFilter.isEmpty())
                        ? "SELECT COUNT(*) FROM trade_history WHERE LOWER(player_name) = LOWER(?)"
                        : "SELECT COUNT(*) FROM trade_history";
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    if (playerFilter != null && !playerFilter.isEmpty()) {
                        pstmt.setString(1, playerFilter);
                    }
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) return rs.getInt(1);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "查詢記錄總數失敗", e);
            }
            return 0;
        }, executor);
    }

    public CompletableFuture<List<TradeRecord>> getRecords(String playerFilter, int page, int pageSize) {
        return CompletableFuture.supplyAsync(() -> {
            List<TradeRecord> list = new ArrayList<>();
            try {
                if (connection == null || connection.isClosed()) return list;
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

    // ==========================================
    // 收益金庫持久化 (Treasury Balance & Items)
    // ==========================================

    public CompletableFuture<double[]> loadTreasuryBalance() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (connection == null || connection.isClosed()) return new double[]{0.0, 0.0};
                String sql = "SELECT vault_balance, exp_points FROM treasury_balance WHERE id = 1";
                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    if (rs.next()) {
                        return new double[]{rs.getDouble("vault_balance"), (double) rs.getInt("exp_points")};
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "讀取金庫餘額失敗", e);
            }
            return new double[]{0.0, 0.0};
        }, executor);
    }

    public CompletableFuture<Void> saveTreasuryBalance(double vaultBalance, int expPoints) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (connection == null || connection.isClosed()) return;
                String sql = "UPDATE treasury_balance SET vault_balance = ?, exp_points = ? WHERE id = 1";
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setDouble(1, vaultBalance);
                    pstmt.setInt(2, expPoints);
                    pstmt.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "保存金庫餘額失敗", e);
            }
        }, executor);
    }

    public CompletableFuture<List<ItemStack>> loadTreasuryItems(int maxSlots) {
        return CompletableFuture.supplyAsync(() -> {
            List<ItemStack> items = new ArrayList<>(Collections.nCopies(maxSlots, null));
            try {
                if (connection == null || connection.isClosed()) return items;
                String sql = "SELECT slot, material, amount, item_bytes FROM treasury_items";
                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    while (rs.next()) {
                        int slot = rs.getInt("slot");
                        if (slot >= 0 && slot < maxSlots) {
                            byte[] bytes = rs.getBytes("item_bytes");
                            if (bytes != null && bytes.length > 0) {
                                try {
                                    items.set(slot, ItemStack.deserializeBytes(bytes));
                                    continue;
                                } catch (Throwable ignored) {}
                            }
                            String matStr = rs.getString("material");
                            int amount = rs.getInt("amount");
                            Material mat = Material.matchMaterial(matStr);
                            if (mat != null && !mat.isAir() && amount > 0) {
                                items.set(slot, new ItemStack(mat, amount));
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "讀取金庫物品失敗", e);
            }
            return items;
        }, executor);
    }

    public CompletableFuture<Void> saveTreasuryItems(List<ItemStack> items) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (connection == null || connection.isClosed()) return;
                try (Statement stmt = connection.createStatement()) {
                    stmt.executeUpdate("DELETE FROM treasury_items");
                }

                String sql = "INSERT INTO treasury_items (slot, material, amount, item_bytes) VALUES (?, ?, ?, ?)";
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    for (int i = 0; i < items.size(); i++) {
                        ItemStack item = items.get(i);
                        if (item != null && !item.getType().isAir() && item.getAmount() > 0) {
                            pstmt.setInt(1, i);
                            pstmt.setString(2, item.getType().name());
                            pstmt.setInt(3, item.getAmount());
                            try {
                                pstmt.setBytes(4, item.serializeAsBytes());
                            } catch (Throwable ignored) {
                                pstmt.setBytes(4, null);
                            }
                            pstmt.addBatch();
                        }
                    }
                    pstmt.executeBatch();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "保存金庫物品失敗", e);
            }
        }, executor);
    }

    // ==========================================
    // 管理員提領稽核日誌 (Treasury Audit Logs)
    // ==========================================

    public CompletableFuture<Void> logTreasuryWithdrawal(UUID adminUuid, String adminName, String actionType, String detail, double amount) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (connection == null || connection.isClosed()) return;
                String sql = "INSERT INTO treasury_logs (timestamp, admin_uuid, admin_name, action_type, detail, amount) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setLong(1, System.currentTimeMillis());
                    pstmt.setString(2, adminUuid != null ? adminUuid.toString() : "CONSOLE");
                    pstmt.setString(3, adminName != null ? adminName : "Console");
                    pstmt.setString(4, actionType);
                    pstmt.setString(5, detail);
                    pstmt.setDouble(6, amount);
                    pstmt.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "記錄管理員金庫提領日誌失敗: " + adminName, e);
            }
        }, executor);
    }

    public CompletableFuture<Integer> getTotalTreasuryLogs() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (connection == null || connection.isClosed()) return 0;
                String sql = "SELECT COUNT(*) FROM treasury_logs";
                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    if (rs.next()) return rs.getInt(1);
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "查詢金庫稽核日誌總數失敗", e);
            }
            return 0;
        }, executor);
    }

    public CompletableFuture<List<TreasuryLogRecord>> getTreasuryLogs(int page, int pageSize) {
        return CompletableFuture.supplyAsync(() -> {
            List<TreasuryLogRecord> list = new ArrayList<>();
            try {
                if (connection == null || connection.isClosed()) return list;
                int offset = Math.max(0, (page - 1) * pageSize);
                String sql = "SELECT id, timestamp, admin_uuid, admin_name, action_type, detail, amount FROM treasury_logs ORDER BY timestamp DESC LIMIT ? OFFSET ?";

                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setInt(1, pageSize);
                    pstmt.setInt(2, offset);

                    try (ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) {
                            long id = rs.getLong("id");
                            long timestamp = rs.getLong("timestamp");
                            String uuidStr = rs.getString("admin_uuid");
                            UUID uuid = null;
                            try {
                                uuid = UUID.fromString(uuidStr);
                            } catch (Exception ignored) {}
                            String name = rs.getString("admin_name");
                            String action = rs.getString("action_type");
                            String detail = rs.getString("detail");
                            double amount = rs.getDouble("amount");

                            list.add(new TreasuryLogRecord(id, timestamp, uuid, name, action, detail, amount));
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "分頁查詢金庫稽核日誌失敗", e);
            }
            return list;
        }, executor);
    }

    // ==========================================
    // 社群頭顱分享與市集 (Shared Heads / Market)
    // ==========================================

    public CompletableFuture<Long> addSharedHead(UUID sellerUuid, String sellerName, String headName, String skinOwner,
                                                 String costType, String costItem, double costAmount, int headAmount) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (connection == null || connection.isClosed()) return -1L;
                String sql = "INSERT INTO shared_heads (seller_uuid, seller_name, head_name, skin_owner, cost_type, cost_item, cost_amount, head_amount, created_at, sales_count, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 1)";
                try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setString(1, sellerUuid.toString());
                    pstmt.setString(2, sellerName);
                    pstmt.setString(3, headName);
                    pstmt.setString(4, skinOwner);
                    pstmt.setString(5, costType);
                    pstmt.setString(6, costItem);
                    pstmt.setDouble(7, costAmount);
                    pstmt.setInt(8, headAmount);
                    pstmt.setLong(9, System.currentTimeMillis());
                    pstmt.executeUpdate();

                    try (ResultSet rs = pstmt.getGeneratedKeys()) {
                        if (rs.next()) return rs.getLong(1);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "新增社群分享頭顱失敗: " + sellerName, e);
            }
            return -1L;
        }, executor);
    }

    public CompletableFuture<Boolean> removeSharedHead(long id, UUID sellerUuid, boolean isAdmin) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (connection == null || connection.isClosed()) return false;
                String sql = isAdmin
                        ? "UPDATE shared_heads SET is_active = 0 WHERE id = ?"
                        : "UPDATE shared_heads SET is_active = 0 WHERE id = ? AND seller_uuid = ?";
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setLong(1, id);
                    if (!isAdmin) {
                        pstmt.setString(2, sellerUuid.toString());
                    }
                    int rows = pstmt.executeUpdate();
                    return rows > 0;
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "下架社群頭顱失敗: " + id, e);
            }
            return false;
        }, executor);
    }

    public CompletableFuture<Integer> getTotalActiveSharedHeads() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (connection == null || connection.isClosed()) return 0;
                String sql = "SELECT COUNT(*) FROM shared_heads WHERE is_active = 1";
                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    if (rs.next()) return rs.getInt(1);
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "查詢市集頭顱總數失敗", e);
            }
            return 0;
        }, executor);
    }

    public CompletableFuture<Integer> getPlayerActiveListingsCount(UUID sellerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (connection == null || connection.isClosed()) return 0;
                String sql = "SELECT COUNT(*) FROM shared_heads WHERE seller_uuid = ? AND is_active = 1";
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setString(1, sellerUuid.toString());
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) return rs.getInt(1);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "查詢玩家上架數量失敗", e);
            }
            return 0;
        }, executor);
    }

    public CompletableFuture<List<SharedHeadRecord>> getActiveSharedHeads(int page, int pageSize) {
        return CompletableFuture.supplyAsync(() -> {
            List<SharedHeadRecord> list = new ArrayList<>();
            try {
                if (connection == null || connection.isClosed()) return list;
                int offset = Math.max(0, (page - 1) * pageSize);
                String sql = "SELECT id, seller_uuid, seller_name, head_name, skin_owner, cost_type, cost_item, cost_amount, head_amount, created_at, sales_count, is_active FROM shared_heads WHERE is_active = 1 ORDER BY created_at DESC LIMIT ? OFFSET ?";

                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setInt(1, pageSize);
                    pstmt.setInt(2, offset);

                    try (ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) {
                            long id = rs.getLong("id");
                            UUID sellerUuid = UUID.fromString(rs.getString("seller_uuid"));
                            String sellerName = rs.getString("seller_name");
                            String headName = rs.getString("head_name");
                            String skinOwner = rs.getString("skin_owner");
                            String costTypeStr = rs.getString("cost_type");
                            ShopOption.CostType costType = ShopOption.CostType.valueOf(costTypeStr);
                            String costItem = rs.getString("cost_item");
                            double costAmount = rs.getDouble("cost_amount");
                            int headAmount = rs.getInt("head_amount");
                            long createdAt = rs.getLong("created_at");
                            int salesCount = rs.getInt("sales_count");
                            boolean isActive = rs.getInt("is_active") == 1;

                            list.add(new SharedHeadRecord(id, sellerUuid, sellerName, headName, skinOwner, costType, costItem, costAmount, headAmount, createdAt, salesCount, isActive));
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "分頁查詢市集頭顱失敗", e);
            }
            return list;
        }, executor);
    }

    public CompletableFuture<List<SharedHeadRecord>> getPlayerSharedHeads(UUID sellerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<SharedHeadRecord> list = new ArrayList<>();
            try {
                if (connection == null || connection.isClosed()) return list;
                String sql = "SELECT id, seller_uuid, seller_name, head_name, skin_owner, cost_type, cost_item, cost_amount, head_amount, created_at, sales_count, is_active FROM shared_heads WHERE seller_uuid = ? AND is_active = 1 ORDER BY created_at DESC";

                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setString(1, sellerUuid.toString());
                    try (ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) {
                            long id = rs.getLong("id");
                            String sellerName = rs.getString("seller_name");
                            String headName = rs.getString("head_name");
                            String skinOwner = rs.getString("skin_owner");
                            String costTypeStr = rs.getString("cost_type");
                            ShopOption.CostType costType = ShopOption.CostType.valueOf(costTypeStr);
                            String costItem = rs.getString("cost_item");
                            double costAmount = rs.getDouble("cost_amount");
                            int headAmount = rs.getInt("head_amount");
                            long createdAt = rs.getLong("created_at");
                            int salesCount = rs.getInt("sales_count");
                            boolean isActive = rs.getInt("is_active") == 1;

                            list.add(new SharedHeadRecord(id, sellerUuid, sellerName, headName, skinOwner, costType, costItem, costAmount, headAmount, createdAt, salesCount, isActive));
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "查詢玩家上架頭顱失敗", e);
            }
            return list;
        }, executor);
    }

    public CompletableFuture<Void> incrementSharedHeadSales(long id) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (connection == null || connection.isClosed()) return;
                String sql = "UPDATE shared_heads SET sales_count = sales_count + 1 WHERE id = ?";
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setLong(1, id);
                    pstmt.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "更新銷量失敗: " + id, e);
            }
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

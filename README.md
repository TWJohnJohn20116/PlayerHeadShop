# 👑 PlayerHeadShop (自訂玩家頭顱商店 & 社群市集)

![PlayerHeadShop Plugin Icon](file:///C:/Users/TW_Jo/.gemini/antigravity/brain/fde998ad-86f5-40ab-9c17-c0e7057b9660/icon.png)

![Version](https://img.shields.io/badge/Version-2.0.0-gold.svg)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21%2B-brightgreen.svg)
![Server](https://img.shields.io/badge/Platform-Paper%20%7C%20Purpur%20%7C%20Folia-blue.svg)
![Java](https://img.shields.io/badge/Java-21%2B-orange.svg)
![License](https://img.shields.io/badge/License-MIT-green.svg)

**PlayerHeadShop** 是一款專為現代 Paper / Folia 伺服器打造的高效能、高自訂性**玩家皮膚頭顱商店、社群頭顱市集與收益金庫**插件。

---

## 🌟 核心特色

### 1. 🛒 現代化純 GUI 自訂商店
- 輸入 `/buyhead` 即可開啟精美箱子選單。
- 支援實體物品放置兌換、Vault 經濟貨幣、經驗等級與經驗點數等多種自訂兌換方案。
- 自動居中智慧排版，無需手動微調 Slot。

### 2. 🌍 社群頭顱分享與市集 (Community Head Market)
- **直接由主選單直達**：點擊商店選單右下角「✦ 前往社群頭顱市集」直達全服市集。
- **即時皮膚預覽**：所有上架商品直接顯示該玩家的原創皮膚外觀。
- **純 GUI 視覺化上架**：支援 💎 鑽石 / 🟢 綠寶石 / 💰 Vault 金幣 / 🔮 經驗等級 / ✨ 經驗點數定價。
- **🪧 原生告示牌文字輸入 (Sign GUI)**：點擊設定售價或自訂名稱時，直接跳出 Minecraft 告示牌編輯介面輸入！
- **玩家自主管理**：玩家或管理員點擊自己的商品即可一鍵下架。

### 3. 🏦 伺服器收益金庫 (Treasury Revenue Pool)
- 玩家購買頭顱支付的物品、金幣與經驗可自動存入伺服器公用金庫。
- 管理員可隨時透過 `/buyhead pool` 查看、提領資產。
- 完整 SQLite 提領稽核日誌 (`/buyhead pool logs`)，杜絕弊端。

### 4. 🌐 多語言與動態熱更新 (i18n)
- 內建繁體中文 (`zh_TW`)、簡體中文 (`zh_CN`) 與英文 (`en_US`)。
- 支援根據客戶端語言自動切換 (`language: "auto"`)。
- 升級時自動增量補齊新翻譯鍵值，絕不覆蓋服主既有自訂設定。

---

## 📋 指令與權限

| 指令 | 說明 | 預設權限 |
| :--- | :--- | :--- |
| `/buyhead` | 開啟自訂頭顱商店選單 (含社群市集入口) | `playerheadshop.use` (預設所有人) |
| `/buyhead help` | 查看指令說明清單 | `playerheadshop.use` (預設所有人) |
| `/buyhead reload` | 重新載入設定檔與語言檔 | `playerheadshop.admin` (預設 OP) |
| `/buyhead pool` | 開啟收益金庫管理 GUI | `playerheadshop.admin` (預設 OP) |
| `/buyhead pool info` | 查看收益金庫資產狀況 | `playerheadshop.admin` (預設 OP) |
| `/buyhead pool logs [頁碼]` | 查詢管理員金庫提領稽核記錄 | `playerheadshop.admin` (預設 OP) |
| `/buyhead pool withdraw [類型]` | 提領金庫累積資金或經驗 | `playerheadshop.admin` (預設 OP) |
| `/buyhead history [玩家] [頁碼]` | 查詢全服頭顱兌換交易記錄 | `playerheadshop.admin` (預設 OP) |

---

## 🛠️ 開發與建置

```bash
# 使用 Maven 建置
mvn clean package

# 使用 Gradle 建置
./gradlew build
```
產生的 JAR 檔案位於 `target/PlayerHeadShop-2.0.0.jar` 或 `build/libs/`。

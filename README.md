<p align="center">
  <img src="assets/icon.png" alt="PlayerHeadShop Logo" width="220" />
</p>

<h1 align="center">PlayerHeadShop</h1>

<p align="center">
  <b>專為 Paper / Folia 1.21.4+ (Java 21) 設計的輕量級 Minecraft 自訂玩家頭顱購買插件</b><br>
  支援箱子 GUI 選單、個人皮膚即時預覽、多元支付模式、智慧自動排版、收益金庫池與管理員提領稽核日誌、完整多語言系統
</p>

<p align="center">
  <a href="README.md"><b>繁體中文</b></a> •
  <a href="README_zh_CN.md">简体中文</a> •
  <a href="README_EN.md">English</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Paper%20%7C%20Folia-brightgreen.svg" alt="Platform" />
  <img src="https://img.shields.io/badge/Minecraft-1.21.4+-orange.svg" alt="Minecraft Version" />
  <img src="https://img.shields.io/badge/Java-21+-blue.svg" alt="Java Version" />
  <img src="https://img.shields.io/badge/License-MIT-green.svg" alt="License" />
</p>

---

## 🌟 功能特色

- **互動式箱子 GUI 介面**：輸入 `/buyhead` 即開啟直觀的箱子選單。
- **智慧自動排版 (Auto-Layout)**：在 `config.yml` 中若省略 `slot:`，系統自動為所有方案計算最美觀的居中與對稱排版！
- **四大多元支付模式 (Payment Types)**：
  - `ITEM`：實體物品支付（如鑽石、綠寶石），支援主動放置介面 (Deposit GUI) 與大於 64 個物品的多格放置。
  - `VAULT`：伺服器經濟金幣支付（直接扣款，軟依賴 Vault）。
  - `EXP_LEVEL`：經驗等級支付（直接扣除玩家等級數，如 5 等級）。
  - `EXP_POINTS`：精確經驗點數支付（原生公式計算並扣除經驗點數，如 300 點經驗）。
- **🏛️ 收益金庫/收益池系統 (Treasury Pool)**：
  - 設定檔自由選擇是否開啟 (`pool.enabled: true`)。
  - 玩家購買頭顱支付的物品、Vault 金幣或經驗值自動匯入收益池。
  - 管理員專屬 6 行金庫 GUI (`/buyhead pool`)，支援直接拿取物品、一鍵提領金幣與吸收經驗。
  - **管理員提領稽核日誌 (Audit Log)**：精確記錄每位管理員提領時間、數量與項目，提供 `/buyhead pool logs` 查詢防止濫權。
- **動態玩家皮膚預覽**：GUI 內的商品圖示自動渲染為點擊玩家自身的皮膚外觀 (`PLAYER_HEAD`)。
- **完善防吞/防刷保護**：嚴格監聽所有事件，關閉介面或中斷時，放置區物品 **100% 自動安全歸還背包**。
- **SQLite 交易歷史記錄**：內建非同步資料庫，完整記錄每筆兌換時間、玩家與消耗，管理員可隨時分頁查詢。
- **完整 i18n 多語言支援**：內建繁中 (`zh_TW`)、簡中 (`zh_CN`)、英文 (`en_US`)，支援 `language: "auto"` 自動依客戶端語言切換。
- **原生 Folia 執行緒相容**：完美支援 Folia 區域多執行緒（Regionized Multi-threading），操作安全穩定。

---

## 📋 指令與權限

| 指令 | 說明 | 預設權限 |
| :--- | :--- | :--- |
| `/buyhead` | 開啟自訂頭顱商店 GUI 選單 | `playerheadshop.use` (所有人) |
| `/buyhead history [玩家] [頁碼]` | 查詢全服或特定玩家的兌換歷史記錄（別名 `/buyhead logs`） | `playerheadshop.admin` (OP) |
| `/buyhead pool` | 開啟管理員收益金庫 GUI 選單 | `playerheadshop.admin` (OP) |
| `/buyhead pool info` | 查看收益金庫即時資產狀況 | `playerheadshop.admin` (OP) |
| `/buyhead pool withdraw <money\|exp\|all>` | 指令快速提領金庫金幣或經驗值 | `playerheadshop.admin` (OP) |
| `/buyhead pool logs [頁碼]` | 查詢管理員提領稽核日誌 (Audit Logs) | `playerheadshop.admin` (OP) |
| `/buyhead reload` | 重新加載 `config.yml` 設定檔與語言檔 | `playerheadshop.admin` (OP) |

---

## ⚙️ 設定檔範例 (`config.yml`)

```yaml
# =======================================================
#               PlayerHeadShop 插件設定檔
# =======================================================

# 語言設定 (auto / zh_TW / zh_CN / en_US)
language: "auto"

# GUI 箱子介面設定
gui:
  rows: 3
  filler:
    enabled: true
    material: "GRAY_STAINED_GLASS_PANE"
    display-name: " "

# 收益金庫 (Treasury Pool) 設定
pool:
  enabled: true                  # 是否啟用收益金庫（false 則直接銷毀消耗）
  collect-items: true            # 是否收集實體物品 (鑽石、綠寶石等)
  collect-vault: true            # 是否收集 Vault 經濟貨幣
  collect-exp: true              # 是否收集 經驗等級與經驗點數

# 多種兌換方案清單 (若不填 slot 則自動計算對稱排版)
options:
  # 方案一：使用鑽石物品 (實體放置介面)
  - cost-type: "ITEM"
    cost-item: "DIAMOND"
    cost-amount: 1
    head-amount: 1

  # 方案二：使用 Vault 伺服器貨幣
  - cost-type: "VAULT"
    cost-amount: 500
    head-amount: 1

  # 方案三：使用 5 點經驗等級
  - cost-type: "EXP_LEVEL"
    cost-amount: 5
    head-amount: 1

  # 方案四：使用 300 點經驗值
  - cost-type: "EXP_POINTS"
    cost-amount: 300
    head-amount: 1
```

---

## 🛠️ 建構方式 (Build)

### 使用 Maven
```bash
mvn clean package
```
產出的 JAR 檔案將位於 `target/PlayerHeadShop-2.0.0-beta-2.jar`。

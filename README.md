<p align="center">
  <img src="assets/icon.png" alt="PlayerHeadShop Logo" width="220" />
</p>

<h1 align="center">PlayerHeadShop</h1>

<p align="center">
  <b>專為 Paper / Folia 1.21.4+ (Java 21) 設計的輕量級 Minecraft 自訂玩家頭顱購買插件</b><br>
  支援箱子 GUI 選單、個人皮膚即時預覽、多種兌換方案、放置式兌換、交易歷史查詢與完整多語言系統
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
- **主動放置兌換 (Deposit Trade GUI)**：支援玩家主動將物品放入放置區進行兌換，更具儀式感。
- **多格累計支援 (> 64 個物品)**：放置區提供 6 個放置格，支援單次兌換超過 64 個物品（多組物品），並自動分組打包發放。
- **動態玩家皮膚預覽**：GUI 內的商品圖示自動渲染為點擊玩家自身的皮膚外觀 (`PLAYER_HEAD`)。
- **多種自訂方案 (Multi-Options)**：可在 `config.yml` 中自由配置任意數量與格子的兌換方案（自訂消耗物品、數量、獲得頭顱數、顯示名稱與說明）。
- **完善防吞/防刷保護**：嚴格監聽所有事件，關閉介面或中斷時，放置區物品 **100% 自動安全歸還背包**。
- **SQLite 交易歷史記錄**：內建非同步資料庫，完整記錄每筆兌換時間、玩家與消耗，管理員可隨時分頁查詢。
- **完整 i18n 多語言支援**：內建繁中 (`zh_TW`)、簡中 (`zh_CN`)、英文 (`en_US`)，支援 `language: "auto"` 自動依客戶端語言切換。
- **原生 Folia 執行緒相容**：完美支援 Folia 區域多執行緒（Regionized Multi-threading），操作安全穩定。
- **即時熱重載**：提供 `/buyhead reload` 即時重載設定檔與語言檔。

---

## 📋 指令與別名

| 指令 | 別名 | 說明 | 預設權限 |
| :--- | :--- | :--- | :--- |
| `/buyhead` | `/playerheadshop`, `/headshop` | 開啟自訂頭顱商店 GUI 選單 | `playerheadshop.use` (所有人) |
| `/buyhead history [玩家] [頁碼]` | `/buyhead logs`, `/buyhead log` | 查詢全服或特定玩家的兌換歷史記錄 | `playerheadshop.admin` (OP) |
| `/buyhead reload` | - | 重新加載 `config.yml` 設定檔與語言檔 | `playerheadshop.admin` (OP) |

---

## 🔐 權限節點

| 權限節點 | 說明 | 預設擁有者 |
| :--- | :--- | :--- |
| `playerheadshop.use` | 允許玩家使用 `/buyhead` 開啟商店介面 | `true` (所有玩家) |
| `playerheadshop.admin` | 允許管理員執行 `/buyhead reload` 與 `/buyhead history` | `op` (伺服器管理員) |

---

## ⚙️ 設定檔 (`config.yml`)

```yaml
# =======================================================
#               PlayerHeadShop 插件設定檔
# =======================================================

# 語言設定 (Language)
# 可選值:
#   "auto"  - 根據每位玩家的 Minecraft 客戶端語言自動切換 (繁中 / 簡中 / 英文)
#   "zh_TW" - 強制全服使用 繁體中文
#   "zh_CN" - 强制全服使用 简体中文
#   "en_US" - Force English
language: "auto"

# GUI 箱子介面設定
gui:
  rows: 3
  filler:
    enabled: true
    material: "GRAY_STAINED_GLASS_PANE"
    display-name: " "

# 多種兌換方案清單
# slot: 箱子格子編號 (0 ~ 53，例如 3 行介面為 0 ~ 26)
# cost-item: 消耗物品 (有效 Bukkit Material 名稱，如 DIAMOND, EMERALD, GOLD_INGOT, NETHERITE_INGOT 等)
# cost-amount: 消耗數量 (支援任意數量，大於 64 可在放置區分格放置)
# head-amount: 獲得頭顱數量 (支援任意數量，大於 64 自動分組堆疊發放)
options:
  - slot: 11
    cost-item: "DIAMOND"
    cost-amount: 1
    head-amount: 1

  - slot: 13
    cost-item: "DIAMOND"
    cost-amount: 7
    head-amount: 8

  - slot: 15
    cost-item: "DIAMOND"
    cost-amount: 50
    head-amount: 64
```

---

## 🛠️ 建構方式 (Build)

### 使用 Maven
```bash
mvn clean package
```
產出的 JAR 檔案將位於 `target/PlayerHeadShop-1.0.0.jar`。

### 使用 Gradle
```bash
./gradlew build
```
產出的 JAR 檔案將位於 `build/libs/PlayerHeadShop-1.0.0.jar`。

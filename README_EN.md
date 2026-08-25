<p align="center">
  <img src="assets/icon.png" alt="PlayerHeadShop Logo" width="220" />
</p>

<h1 align="center">PlayerHeadShop</h1>

<p align="center">
  <b>A lightweight Minecraft plugin for Paper / Folia 1.21.4+ (Java 21) to purchase custom player heads.</b><br>
  Features chest GUI menu, real-time player skin preview, deposit trade GUI, transaction logs & admin history lookup, and complete i18n support.
</p>

<p align="center">
  <a href="README.md">繁體中文</a> •
  <a href="README_zh_CN.md">简体中文</a> •
  <a href="README_EN.md"><b>English</b></a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Paper%20%7C%20Folia-brightgreen.svg" alt="Platform" />
  <img src="https://img.shields.io/badge/Minecraft-1.21.4+-orange.svg" alt="Minecraft Version" />
  <img src="https://img.shields.io/badge/Java-21+-blue.svg" alt="Java Version" />
  <img src="https://img.shields.io/badge/License-MIT-green.svg" alt="License" />
</p>

---

## 🌟 Features

- **Interactive Chest GUI**: Type `/buyhead` to open an intuitive chest shop menu.
- **Deposit & Trade GUI**: Dedicated deposit interface where players actively place items to complete the exchange.
- **Multi-Slot Support (> 64 items)**: 6 input deposit slots support single trades requiring more than 64 items across multiple stacks.
- **Dynamic Skin Preview**: Head icons dynamically render the current viewer's Minecraft skin (`PLAYER_HEAD`).
- **Multi-Tier Pricing Options**: Fully customizable trade options in `config.yml` (custom cost item, amount, head amount, display name, and lore per slot).
- **100% Anti-Loss & Anti-Dupe Protection**: Unused items in the deposit slots are automatically refunded to the player upon closing or cancellation.
- **SQLite Transaction History**: Built-in async SQLite database records every transaction with timestamp, player, and cost, viewable by admins anytime.
- **Complete i18n Multi-Language Support**: Built-in Traditional Chinese (`zh_TW`), Simplified Chinese (`zh_CN`), and English (`en_US`) with automatic client language detection (`language: "auto"`).
- **Native Folia Support**: Thread-safe execution under Folia regionized multi-threading (`folia-supported: true`).
- **In-Game Hot Reload**: Reload configuration and language files instantly with `/buyhead reload`.

---

## 📋 Commands & Aliases

| Command | Aliases | Description | Default Permission |
| :--- | :--- | :--- | :--- |
| `/buyhead` | `/playerheadshop`, `/headshop` | Open the Player Head Shop GUI | `playerheadshop.use` (Everyone) |
| `/buyhead history [player] [page]` | `/buyhead logs`, `/buyhead log` | View global or player-specific trade history | `playerheadshop.admin` (OP) |
| `/buyhead reload` | - | Reload `config.yml` configuration and language files | `playerheadshop.admin` (OP) |

---

## 🔐 Permissions

| Permission Node | Description | Default |
| :--- | :--- | :--- |
| `playerheadshop.use` | Allows players to use `/buyhead` to open the shop | `true` (All players) |
| `playerheadshop.admin` | Allows administrators to run `/buyhead reload` and `/buyhead history` | `op` (Operators) |

---

## ⚙️ Configuration (`config.yml`)

```yaml
# =======================================================
#               PlayerHeadShop Configuration
# =======================================================

# Language Setting
# Options:
#   "auto"  - Automatically detects per-player Minecraft client language
#   "zh_TW" - Force Traditional Chinese
#   "zh_CN" - Force Simplified Chinese
#   "en_US" - Force English
language: "auto"

# GUI Chest Interface Settings
gui:
  rows: 3
  filler:
    enabled: true
    material: "GRAY_STAINED_GLASS_PANE"
    display-name: " "

# Multi-Option Pricing List
# slot: Chest inventory slot index (0 ~ 53, e.g. 0 ~ 26 for 3 rows)
# cost-item: Required payment material (Valid Bukkit Material, e.g. DIAMOND, EMERALD, GOLD_INGOT)
# cost-amount: Required payment amount (Amounts > 64 can be placed across deposit slots)
# head-amount: Amount of player heads given (Amounts > 64 are auto-bundled in stacks)
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

## 🛠️ Building the Plugin

### Using Maven
```bash
mvn clean package
```
The compiled JAR file will be located at `target/PlayerHeadShop-1.0.0.jar`.

### Using Gradle
```bash
./gradlew build
```
The compiled JAR file will be located at `build/libs/PlayerHeadShop-1.0.0.jar`.

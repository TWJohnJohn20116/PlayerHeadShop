<p align="center">
  <img src="assets/icon.png" alt="PlayerHeadShop Logo" width="220" />
</p>

<h1 align="center">PlayerHeadShop</h1>

<p align="center">
  <b>A lightweight Minecraft plugin for Paper / Folia 1.21.4+ (Java 21) to purchase custom player heads.</b><br>
  Features chest GUI menu, real-time player skin preview, Items / Vault / EXP Level / EXP Points multi-payments, auto-layout, transaction logs & admin history lookup, and complete i18n support.
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
- **Intelligent Auto-Layout**: If `slot:` is omitted in `config.yml`, the plugin automatically calculates a centered and symmetrical layout!
- **Four Payment Modes**:
  - `ITEM`: Physical item payment (e.g. Diamonds, Emeralds) with dedicated deposit trade GUI.
  - `VAULT`: Server economy currency (instant deduction, soft dependency on Vault).
  - `EXP_LEVEL`: Experience levels (directly deducts player levels, e.g. 5 levels).
  - `EXP_POINTS`: Exact experience points (calculates and deducts raw XP points, e.g. 300 points).
- **Dynamic Skin Preview**: Head icons dynamically render the current viewer's Minecraft skin (`PLAYER_HEAD`).
- **100% Anti-Loss & Anti-Dupe Protection**: Unused items in deposit slots are refunded to the player upon closing.
- **SQLite Transaction History**: Built-in async SQLite database records every transaction with timestamp, player, cost type (`ITEM`, `VAULT`, `EXP_LEVEL`, `EXP_POINTS`), viewable by admins anytime.
- **Complete i18n Multi-Language Support**: Built-in Traditional Chinese (`zh_TW`), Simplified Chinese (`zh_CN`), and English (`en_US`) with automatic client language detection (`language: "auto"`).
- **Native Folia Support**: Thread-safe execution under Folia regionized multi-threading.

---

## 📋 Commands & Aliases

| Command | Aliases | Description | Default Permission |
| :--- | :--- | :--- | :--- |
| `/buyhead` | `/playerheadshop`, `/headshop` | Open the Player Head Shop GUI | `playerheadshop.use` (Everyone) |
| `/buyhead history [player] [page]` | `/buyhead logs`, `/buyhead log` | View global or player-specific trade history | `playerheadshop.admin` (OP) |
| `/buyhead reload` | - | Reload `config.yml` configuration and language files | `playerheadshop.admin` (OP) |

---

## ⚙️ Configuration Example (`config.yml`)

```yaml
# =======================================================
#               PlayerHeadShop Configuration
# =======================================================

# Language Setting (auto / zh_TW / zh_CN / en_US)
language: "auto"

# GUI Chest Interface Settings
gui:
  rows: 3
  filler:
    enabled: true
    material: "GRAY_STAINED_GLASS_PANE"
    display-name: " "

# Multi-Option Pricing List (Auto-layout if slot is omitted)
options:
  # Option 1: Physical Item (Diamond)
  - cost-type: "ITEM"
    cost-item: "DIAMOND"
    cost-amount: 1
    head-amount: 1

  # Option 2: Vault Currency ($500)
  - cost-type: "VAULT"
    cost-amount: 500
    head-amount: 1

  # Option 3: 5 Experience Levels
  - cost-type: "EXP_LEVEL"
    cost-amount: 5
    head-amount: 1

  # Option 4: 300 Experience Points
  - cost-type: "EXP_POINTS"
    cost-amount: 300
    head-amount: 1
```

---

## 🛠️ Building the Plugin

### Using Maven
```bash
mvn clean package
```
The compiled JAR file will be located at `target/PlayerHeadShop-1.0.0.jar`.

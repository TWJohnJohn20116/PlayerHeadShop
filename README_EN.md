<p align="center">
  <img src="assets/icon.png" alt="PlayerHeadShop Logo" width="220" />
</p>

<h1 align="center">PlayerHeadShop</h1>

<p align="center">
  <b>A modern, lightweight Minecraft plugin for Paper / Folia 1.21.4+ (Java 21) to purchase custom player heads.</b><br>
  Features Chest GUI, Live Skin Preview, Multi-Payment Modes, Intelligent Auto-Layout, Revenue Treasury Pool with Admin Audit Logs, and Full i18n Localization.
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

## 🌟 Key Features

- **Interactive Chest GUI**: Use `/buyhead` to open an intuitive shop interface.
- **Intelligent Auto-Layout**: Omit `slot:` in `config.yml` and the plugin calculates centered, symmetrical layouts automatically.
- **4 Payment Modes**:
  - `ITEM`: Physical item payments (e.g. Diamond, Emerald) with interactive deposit slots and multi-stack support (>64 items).
  - `VAULT`: Server economy currency payments (soft-dependency on Vault).
  - `EXP_LEVEL`: Experience level deductions (e.g. 5 levels).
  - `EXP_POINTS`: Native formula-accurate experience point deductions (e.g. 300 points).
- **🏛️ Revenue Treasury Pool**:
  - Configurable toggle (`pool.enabled: true/false`).
  - Payments automatically deposit into the shared treasury pool.
  - Dedicated 6-row Admin Treasury GUI (`/buyhead pool`) with direct item withdrawal, one-click money withdrawal, and exp absorption.
  - **Admin Withdrawal Audit Log**: Records timestamp, admin UUID/name, and amounts to SQLite with `/buyhead pool logs`.
- **Dynamic Skin Preview**: Head icons render in real-time with the viewer's Minecraft skin.
- **100% Anti-Dupe & Item Safety**: Leftover deposit items are safely returned to inventory on close.
- **SQLite Trade History**: Async SQLite database recording all transactions with paginated `/buyhead history [player] [page]`.
- **Full i18n Localization**: Built-in Traditional Chinese (`zh_TW`), Simplified Chinese (`zh_CN`), English (`en_US`), and `language: "auto"` client locale auto-detection.
- **Paper & Folia Native**: Full regionized multi-threading compatibility on modern servers.

---

## 📋 Commands & Permissions

| Command | Description | Default Permission |
| :--- | :--- | :--- |
| `/buyhead` | Opens the Player Head Shop GUI | `playerheadshop.use` (Everyone) |
| `/buyhead history [player] [page]` | View paginated trade history logs | `playerheadshop.admin` (OP) |
| `/buyhead pool` | Opens the Admin Revenue Treasury GUI | `playerheadshop.admin` (OP) |
| `/buyhead pool info` | View real-time treasury assets summary | `playerheadshop.admin` (OP) |
| `/buyhead pool withdraw <money\|exp\|all>` | Fast CLI withdrawal for money or exp | `playerheadshop.admin` (OP) |
| `/buyhead pool logs [page]` | View admin withdrawal audit trail | `playerheadshop.admin` (OP) |
| `/buyhead reload` | Reload configuration and language files | `playerheadshop.admin` (OP) |

---

## ⚙️ Configuration Example (`config.yml`)

```yaml
# =======================================================
#               PlayerHeadShop Configuration
# =======================================================

# Language setting (auto / zh_TW / zh_CN / en_US)
language: "auto"

# GUI Settings
gui:
  rows: 3
  filler:
    enabled: true
    material: "GRAY_STAINED_GLASS_PANE"
    display-name: " "

# Revenue Treasury Pool Settings
pool:
  enabled: true                  # Whether treasury pool is enabled (false = burn payments)
  collect-items: true            # Collect physical items (diamonds, emeralds, etc.)
  collect-vault: true            # Collect Vault economy money
  collect-exp: true              # Collect experience levels & points

# Shop Options (Omitting slot enables auto-layout)
options:
  # Option 1: Physical items (Diamond)
  - cost-type: "ITEM"
    cost-item: "DIAMOND"
    cost-amount: 1
    head-amount: 1

  # Option 2: Vault Economy Money
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

## 🛠️ Build

### With Maven
```bash
mvn clean package
```
Compiled JAR will be located at `target/PlayerHeadShop-2.0.0-beta-2.jar`.

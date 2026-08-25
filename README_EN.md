<p align="center">
  <img src="assets/icon.png" alt="PlayerHeadShop Logo" width="220" />
</p>

<h1 align="center">PlayerHeadShop</h1>

<p align="center">
  <b>A lightweight Minecraft plugin for Paper / Folia 1.21.4+ (Java 21) to purchase custom player heads.</b><br>
  Features chest GUI menu, real-time player skin preview, multi-tier pricing options, and MiniMessage text formatting.
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
- **Dynamic Skin Preview**: Head icons dynamically render the current viewer's Minecraft skin (`PLAYER_HEAD`).
- **Multi-Tier Pricing Options**: Fully customizable trade options in `config.yml` (custom cost item, amount, head amount, display name, and lore per slot).
- **Filler Glass Panes**: Fill empty GUI slots with custom colored glass panes for a clean aesthetic.
- **Strict Security & Anti-Dupe**: Comprehensive `InventoryClickListener` and `InventoryDragEvent` handling to prevent item stealing, shift-clicking, or hotkey swapping.
- **Inventory Overflow Protection**: When a player's inventory is full, purchased heads safely drop at their feet (`dropItemNaturally`).
- **Native Folia Support**: Thread-safe execution under Folia regionized multi-threading (`folia-supported: true`).
- **MiniMessage Formatting**: Full Kyori Adventure MiniMessage support (gradients, colors, hover/click events, dynamic placeholders).
- **In-Game Hot Reload**: Reload configuration and GUI setup instantly with `/buyhead reload`.

---

## 📋 Commands & Aliases

| Command | Aliases | Description | Default Permission |
| :--- | :--- | :--- | :--- |
| `/buyhead` | `/playerheadshop`, `/headshop` | Open the Player Head Shop GUI | `playerheadshop.use` (Everyone) |
| `/buyhead reload` | - | Reload `config.yml` configuration and options | `playerheadshop.admin` (OP) |

---

## 🔐 Permissions

| Permission Node | Description | Default |
| :--- | :--- | :--- |
| `playerheadshop.use` | Allows players to use `/buyhead` to open the shop | `true` (All players) |
| `playerheadshop.admin` | Allows administrators to run `/buyhead reload` | `op` (Operators) |

---

## ⚙️ Configuration (`config.yml`)

```yaml
# =======================================================
#               PlayerHeadShop Configuration
# =======================================================

# GUI Chest Interface Settings
gui:
  title: "<gradient:#FFAA00:#FF5555><bold>Player Head Shop</bold></gradient>"
  rows: 3
  filler:
    enabled: true
    material: "GRAY_STAINED_GLASS_PANE"
    display-name: " "

# Multi-Option Pricing List
# slot: Chest inventory slot index (0 ~ 53, e.g. 0 ~ 26 for 3 rows)
# display-name: Item display name (Supports MiniMessage format & placeholders)
# lore: Item lore description lines (Supports MiniMessage format & placeholders)
# cost-item: Required payment material (Valid Bukkit Material, e.g. DIAMOND, EMERALD, GOLD_INGOT)
# cost-amount: Required payment amount
# head-amount: Amount of player heads given
options:
  - slot: 11
    display-name: "<yellow><bold>1x Player Head</bold></yellow>"
    lore:
      - "<gray>Purchase a head with your own skin."
      - ""
      - "<white>Receive: <gold>1x Head</gold></white>"
      - "<white>Cost: <aqua><cost_amount>x <cost_item></aqua></white>"
      - ""
      - "<green>▶ Click to purchase!</green>"
    cost-item: "DIAMOND"
    cost-amount: 1
    head-amount: 1

  - slot: 13
    display-name: "<gold><bold>8x Player Heads (Bundle)</bold></gold>"
    lore:
      - "<gray>Purchase 8 player heads in a bundle."
      - ""
      - "<white>Receive: <gold>8x Heads</gold></white>"
      - "<white>Cost: <aqua><cost_amount>x <cost_item></aqua></white>"
      - ""
      - "<green>▶ Click to purchase!</green>"
    cost-item: "DIAMOND"
    cost-amount: 7
    head-amount: 8

  - slot: 15
    display-name: "<light_purple><bold>64x Player Heads (Stack Box)</bold></light_purple>"
    lore:
      - "<gray>A full stack of 64 heads for building!"
      - ""
      - "<white>Receive: <gold>64x Heads</gold></white>"
      - "<white>Cost: <aqua><cost_amount>x <cost_item></aqua></white>"
      - ""
      - "<green>▶ Click to purchase!</green>"
    cost-item: "DIAMOND"
    cost-amount: 50
    head-amount: 64

# Custom Messages (Supports MiniMessage format)
# Supported Placeholders: <amount>, <item>, <cost_amount>, <cost_item>, <head_amount>, <required>, <current>, <missing>, <player>
messages:
  prefix: "<gray>[<gold>HeadShop</gold>]</gray> "
  success: "<green>You spent <gold><cost_amount>x <cost_item></gold> and received <gold><head_amount>x</gold> of your own heads!</green>"
  not-enough-items: "<red>Not enough items! Required <gold><required>x <item></gold>, you only have <gold><current>x</gold> (Missing <missing>x).</red>"
  inventory-full: "<yellow>Your inventory was full! The overflow heads have been dropped at your feet.</yellow>"
  reload-success: "<green>PlayerHeadShop configuration successfully reloaded!</green>"
  no-permission: "<red>You do not have permission to execute this command!</red>"
  player-only: "<red>This command can only be executed by in-game players.</red>"
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

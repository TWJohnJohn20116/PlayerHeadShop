<p align="center">
  <img src="assets/icon.png" alt="PlayerHeadShop Logo" width="220" />
</p>

<h1 align="center">PlayerHeadShop</h1>

<p align="center">
  <b>专为 Paper / Folia 1.21.4+ (Java 21) 设计的轻量级 Minecraft 自定义玩家头颅购买插件</b><br>
  支持箱子 GUI 菜单、个人皮肤即时预览、多方案兑换与 MiniMessage 文本样式
</p>

<p align="center">
  <a href="README.md">繁體中文</a> •
  <a href="README_zh_CN.md"><b>简体中文</b></a> •
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

- **交互式箱子 GUI 界面**：输入 `/buyhead` 即可开启直观的箱子菜单。
- **动态玩家皮肤预览**：GUI 内的商品图标自动渲染为点击玩家自身的皮肤外观 (`PLAYER_HEAD`)。
- **多种自定义方案 (Multi-Options)**：可在 `config.yml` 中自由配置任意数量与格子的兑换方案（自定义消耗物品、数量、获得头颅数、显示名称与说明）。
- **美化填充板 (Filler)**：支持以玻璃板填补空闲格子，保持界面美观整洁。
- **完善防偷渡防护**：严格监听点击与拖曳事件，杜绝拿取、Shift 移动或快捷键置换。
- **背包满溢保护**：当玩家背包空间不足时，溢出的头颅将安全掉落于脚下 (`dropItemNaturally`)。
- **原生 Folia 线程兼容**：完美支持 Folia 区域多线程（Regionized Multi-threading），操作安全稳定。
- **MiniMessage 消息与样式支持**：支持现代 Paper Adventure MiniMessage 颜色与样式（包含渐变 `<gradient>`、彩色标签与自定义占位符）。
- **即时热重载**：提供 `/buyhead reload` 即时重载配置文件与 GUI 内容。

---

## 📋 指令与别名

| 指令 | 别名 | 说明 | 默认权限 |
| :--- | :--- | :--- | :--- |
| `/buyhead` | `/playerheadshop`, `/headshop` | 开启自定义头颅商店 GUI 菜单 | `playerheadshop.use` (所有人) |
| `/buyhead reload` | - | 重新加载 `config.yml` 配置文件与方案 | `playerheadshop.admin` (OP) |

---

## 🔐 权限节点

| 权限节点 | 说明 | 默认拥有者 |
| :--- | :--- | :--- |
| `playerheadshop.use` | 允许玩家使用 `/buyhead` 开启商店界面 | `true` (所有玩家) |
| `playerheadshop.admin` | 允许管理员执行 `/buyhead reload` | `op` (服务器管理员) |

---

## ⚙️ 配置文件 (`config.yml`)

```yaml
# =======================================================
#               PlayerHeadShop 插件配置文件
# =======================================================

# GUI 箱子界面设定
gui:
  title: "<gradient:#FFAA00:#FF5555><bold>自定义头颅商店</bold></gradient>"
  rows: 3
  filler:
    enabled: true
    material: "GRAY_STAINED_GLASS_PANE"
    display-name: " "

# 多种兑换方案列表
# slot: 箱子格子编号 (0 ~ 53，例如 3 行界面为 0 ~ 26)
# display-name: 商品显示名称 (支持 MiniMessage 格式与占位符)
# lore: 商品说明文本 (支持 MiniMessage 格式与占位符)
# cost-item: 消耗物品 (有效 Bukkit Material 名称，如 DIAMOND, EMERALD, GOLD_INGOT, NETHERITE_INGOT 等)
# cost-amount: 消耗数量
# head-amount: 获得头颅数量
options:
  - slot: 11
    display-name: "<yellow><bold>1 个头颅</bold></yellow>"
    lore:
      - "<gray>购买印有自身皮肤外观的头颅。"
      - ""
      - "<white>获得数量: <gold>1 个</gold></white>"
      - "<white>消耗物品: <aqua><cost_amount> 个 <cost_item></aqua></white>"
      - ""
      - "<green>▶ 点击立即购买！</green>"
    cost-item: "DIAMOND"
    cost-amount: 1
    head-amount: 1

  - slot: 13
    display-name: "<gold><bold>8 个头颅 (特惠包)</bold></gold>"
    lore:
      - "<gray>一次性购买 8 个个人头颅。"
      - ""
      - "<white>获得数量: <gold>8 个</gold></white>"
      - "<white>消耗物品: <aqua><cost_amount> 个 <cost_item></aqua></white>"
      - ""
      - "<green>▶ 点击立即购买！</green>"
    cost-item: "DIAMOND"
    cost-amount: 7
    head-amount: 8

  - slot: 15
    display-name: "<light_purple><bold>64 个头颅 (超值箱)</bold></light_purple>"
    lore:
      - "<gray>一整组 64 个个人头颅，建造必备！"
      - ""
      - "<white>获得数量: <gold>64 个</gold></white>"
      - "<white>消耗物品: <aqua><cost_amount> 个 <cost_item></aqua></white>"
      - ""
      - "<green>▶ 点击立即购买！</green>"
    cost-item: "DIAMOND"
    cost-amount: 50
    head-amount: 64

# 自定义消息（支持 MiniMessage 格式）
# 支持占位符: <amount>, <item>, <cost_amount>, <cost_item>, <head_amount>, <required>, <current>, <missing>, <player>
messages:
  prefix: "<gray>[<gold>HeadShop</gold>]</gray> "
  success: "<green>你花费了 <gold><cost_amount> 个 <cost_item></gold> 购买了 <gold><head_amount> 个</gold> 自己的头颅！</green>"
  not-enough-items: "<red>物品不足！需要 <gold><required> 个 <item></gold>，你目前只有 <gold><current> 个</gold>（缺少 <missing> 个）。</red>"
  inventory-full: "<yellow>你的背包已满，溢出的头颅已掉落在你的脚下！</yellow>"
  reload-success: "<green>PlayerHeadShop 配置文件已成功重新加载！</green>"
  no-permission: "<red>你没有权限使用此指令！</red>"
  player-only: "<red>此指令仅限游戏内玩家使用。</red>"
```

---

## 🛠️ 构建方式 (Build)

### 使用 Maven
```bash
mvn clean package
```
生成的 JAR 文件将位于 `target/PlayerHeadShop-1.0.0.jar`。

### 使用 Gradle
```bash
./gradlew build
```
生成的 JAR 文件将位于 `build/libs/PlayerHeadShop-1.0.0.jar`。

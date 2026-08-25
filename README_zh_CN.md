<p align="center">
  <img src="assets/icon.png" alt="PlayerHeadShop Logo" width="220" />
</p>

<h1 align="center">PlayerHeadShop</h1>

<p align="center">
  <b>专为 Paper / Folia 1.21.4+ (Java 21) 设计的轻量级 Minecraft 自定义玩家头颅购买插件</b><br>
  支持箱子 GUI 菜单、个人皮肤即时预览、多方案兑换、放置式兑换、交易历史记录查询与完整多语言系统
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
- **主动放置兑换 (Deposit Trade GUI)**：支持玩家主动将物品放入放置区进行兑换，更具仪式感。
- **多格累计支持 (> 64 个物品)**：放置区提供 6 个放置格，支持单次兑换超过 64 个物品（多组物品），并自动分组打包发放。
- **动态玩家皮肤预览**：GUI 内的商品图标自动渲染为点击玩家自身的皮肤外观 (`PLAYER_HEAD`)。
- **多种自定义方案 (Multi-Options)**：可在 `config.yml` 中自由配置任意数量与格子的兑换方案（自定义消耗物品、数量、获得头颅数、显示名称与说明）。
- **完善防吞/防刷保护**：严格监听所有事件，关闭界面或中断时，放置区物品 **100% 自动安全归还背包**。
- **SQLite 交易历史记录**：内置异步数据库，完整记录每笔兑换时间、玩家与消耗，管理员可随时分页查询。
- **完整 i18n 多语言支持**：内置繁中 (`zh_TW`)、简中 (`zh_CN`)、英文 (`en_US`)，支持 `language: "auto"` 自动依客户端语言切换。
- **原生 Folia 线程兼容**：完美支持 Folia 区域多线程（Regionized Multi-threading），操作安全稳定。
- **即时热重载**：提供 `/buyhead reload` 即时重载配置文件与语言文件。

---

## 📋 指令与别名

| 指令 | 别名 | 说明 | 默认权限 |
| :--- | :--- | :--- | :--- |
| `/buyhead` | `/playerheadshop`, `/headshop` | 开启自定义头颅商店 GUI 菜单 | `playerheadshop.use` (所有人) |
| `/buyhead history [玩家] [页码]` | `/buyhead logs`, `/buyhead log` | 查询全服或特定玩家的兑换历史记录 | `playerheadshop.admin` (OP) |
| `/buyhead reload` | - | 重新加载 `config.yml` 配置文件与语言文件 | `playerheadshop.admin` (OP) |

---

## 🔐 权限节点

| 权限节点 | 说明 | 默认拥有者 |
| :--- | :--- | :--- |
| `playerheadshop.use` | 允许玩家使用 `/buyhead` 开启商店界面 | `true` (所有玩家) |
| `playerheadshop.admin` | 允许管理员执行 `/buyhead reload` 与 `/buyhead history` | `op` (服务器管理员) |

---

## ⚙️ 配置文件 (`config.yml`)

```yaml
# =======================================================
#               PlayerHeadShop 插件配置文件
# =======================================================

# 语言设定 (Language)
# 可选值:
#   "auto"  - 根据每位玩家的 Minecraft 客户端语言自动切换 (繁中 / 简中 / 英文)
#   "zh_TW" - 强制全服使用 繁体中文
#   "zh_CN" - 强制全服使用 简体中文
#   "en_US" - Force English
language: "auto"

# GUI 箱子界面设定
gui:
  rows: 3
  filler:
    enabled: true
    material: "GRAY_STAINED_GLASS_PANE"
    display-name: " "

# 多种兑换方案列表
# slot: 箱子格子编号 (0 ~ 53，例如 3 行界面为 0 ~ 26)
# cost-item: 消耗物品 (有效 Bukkit Material 名称，如 DIAMOND, EMERALD, GOLD_INGOT, NETHERITE_INGOT 等)
# cost-amount: 消耗数量 (支持任意数量，大于 64 可在放置区分格放置)
# head-amount: 获得头颅数量 (支持任意数量，大于 64 自动分组堆叠发放)
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

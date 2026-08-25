<p align="center">
  <img src="assets/icon.png" alt="PlayerHeadShop Logo" width="220" />
</p>

<h1 align="center">PlayerHeadShop</h1>

<p align="center">
  <b>专为 Paper / Folia 1.21.4+ (Java 21) 设计的轻量级 Minecraft 自定义玩家头颅购买插件</b><br>
  支持箱子 GUI 菜单、个人皮肤即时预览、物品 / Vault 金币 / 经验等级 / 经验点数多元支付、智能自动排版、交易历史查询与完整多语言系统
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
- **智能自动排版 (Auto-Layout)**：在 `config.yml` 中若省略 `slot:`，系统自动为所有方案计算最美观的居中与对称排版！
- **四大多元支付模式 (Payment Types)**：
  - `ITEM`：实体物品支付（如钻石、绿宝石），支持主动放置界面 (Deposit GUI) 与大于 64 个物品的多格放置。
  - `VAULT`：服务器经济金币支付（直接扣款，软依赖 Vault）。
  - `EXP_LEVEL`：经验等级支付（直接扣除玩家等级数，如 5 等级）。
  - `EXP_POINTS`：精确经验点数支付（原生公式计算并扣除经验点数，如 300 点经验）。
- **动态玩家皮肤预览**：GUI 内的商品图标自动渲染为点击玩家自身的皮肤外观 (`PLAYER_HEAD`)。
- **完善防吞/防刷保护**：严格监听所有事件，关闭界面或中断时，放置区物品 **100% 自动安全归还背包**。
- **SQLite 交易历史记录**：内置异步数据库，完整记录每笔兑换时间、玩家与消耗（标注 `ITEM`, `VAULT`, `EXP_LEVEL`, `EXP_POINTS`），管理员可随时分页查询。
- **完整 i18n 多语言支持**：内置繁中 (`zh_TW`)、简中 (`zh_CN`)、英文 (`en_US`)，支持 `language: "auto"` 自动依客户端语言切换。
- **原生 Folia 线程兼容**：完美支持 Folia 区域多线程（Regionized Multi-threading），操作安全稳定。

---

## 📋 指令与别名

| 指令 | 别名 | 说明 | 默认权限 |
| :--- | :--- | :--- | :--- |
| `/buyhead` | `/playerheadshop`, `/headshop` | 开启自定义头颅商店 GUI 菜单 | `playerheadshop.use` (所有人) |
| `/buyhead history [玩家] [页码]` | `/buyhead logs`, `/buyhead log` | 查询全服或特定玩家的兑换历史记录 | `playerheadshop.admin` (OP) |
| `/buyhead reload` | - | 重新加载 `config.yml` 配置文件与语言文件 | `playerheadshop.admin` (OP) |

---

## ⚙️ 配置文件范例 (`config.yml`)

```yaml
# =======================================================
#               PlayerHeadShop 插件配置文件
# =======================================================

# 语言设定 (auto / zh_TW / zh_CN / en_US)
language: "auto"

# GUI 箱子界面设定
gui:
  rows: 3
  filler:
    enabled: true
    material: "GRAY_STAINED_GLASS_PANE"
    display-name: " "

# 多种兑换方案列表 (若不填 slot 则自动计算对称排版)
options:
  # 方案一：使用钻石物品 (实体放置界面)
  - cost-type: "ITEM"
    cost-item: "DIAMOND"
    cost-amount: 1
    head-amount: 1

  # 方案二：使用 Vault 服务器货币
  - cost-type: "VAULT"
    cost-amount: 500
    head-amount: 1

  # 方案三：使用 5 点经验等级
  - cost-type: "EXP_LEVEL"
    cost-amount: 5
    head-amount: 1

  # 方案四：使用 300 点经验值
  - cost-type: "EXP_POINTS"
    cost-amount: 300
    head-amount: 1
```

---

## 🛠️ 构建方式 (Build)

### 使用 Maven
```bash
mvn clean package
```
生成的 JAR 文件将位于 `target/PlayerHeadShop-1.0.0.jar`。

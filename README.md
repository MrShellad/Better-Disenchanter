<div align="center">

<img src="assets/logo.png" alt="Better Disenchanter Logo" width="1000" /><br>

# Better Disenchanter (NeoForge 1.21.1)

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen.svg?style=for-the-badge&logo=minecraft)](https://minecraft.net/)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.186+-orange.svg?style=for-the-badge)](https://neoforged.net/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg?style=for-the-badge)](LICENSE)

**沉浸式 3D 附魔回收台 · 催化剂机制 · 原生 Item Display 看板**  
*An immersive in-world disenchanting table with catalyst rituals and native 3D Item Displays*

[English](#english) | [中文说明](#chinese)

---

</div>

<a name="english"></a>
## 🌟 About Better Disenchanter

**Better Disenchanter** combines the tactile, immersive 3D in-world interaction of *[Infusion Table](https://github.com/jptrzy/infusion-table-mod)* with the deep, data-driven catalyst mechanics of *[Disenchanter](https://github.com/gliscowo/disenchanter)*, built from the ground up for **Minecraft 1.21.1 (NeoForge)**.

Say goodbye to clunky 2D GUIs! Place down the Disenchanter Table, lay down a standard Book, place your enchanted gear, and hold up your catalyst of choice to see a dynamic, floating **Item Display preview**. Right-click to initiate the disenchanting ritual accompanied by unique amethyst chime acoustics and mystical particles!

---

## 🎮 How It Works

1. **Place the Disenchanter Table**: Craft and place the table in your world (uses vanilla enchanting table base styling).
2. **Place a Book**: Right-click the table with an unenchanted **Book** (`minecraft:book`). The 3D enchanted tome will open, gently hovering and turning pages.
3. **Place your Enchanted Equipment**: Right-click with any enchanted weapon, tool, armor, or book. The enchanted gear will float gracefully above the open book.
4. **Hold a Catalyst (Native Item Display)**:
   - When holding a valid catalyst (e.g. Emerald, Diamond, Nether Star), a 3D rotating catalyst icon and an informative text billboard appear directly above the table!
   - Shows required counts, inventory status, and ritual outcomes (e.g. `Emerald x1 ➔ ❌ Item Consumed`).
5. **Right-Click to Disenchant**:
   - Right-click with the catalyst to consume it and start the ritual.
   - *Or right-click with an empty hand* to trigger default disenchanting (extracts 1st enchantment, item consumed).
6. **Safe Retrieval**:
   - Change your mind? Simply **left-click (punch)** or sneak-interact with the table to safely pop the item or book back into your hands without wasting resources!

---

## 🔮 Catalysts & Effects

| Catalyst | Default Count | Effect | Original Item | Start Sound |
| :--- | :---: | :--- | :---: | :--- |
| **None (Empty Hand)** | 0 | Extracts **1st enchantment** onto the book | ❌ Consumed | Amethyst Fall (Pitch 1.0) |
| 🟢 **Emerald** | 1 | Extracts up to **2 random enchantments** | ❌ Consumed | Amethyst + High Chime |
| 💎 **Diamond** | 1 | Extracts **1st enchantment + 2 random enchantments** (up to 3 total) | ❌ Consumed | Amethyst + Resonant Ring |
| 🔮 **Ender Pearl** | 1 | Extracts **1 random enchantment**; preserves item with **500 durability damage** | ✔ **Preserved** | Amethyst + Warp Resonance |
| 🌊 **Heart of the Sea** | 1 | Extracts **all enchantments** with levels **reduced by 1** | ❌ Consumed | Amethyst + Conduit Surge |
| 🟣 **Amethyst Shard** | 1 | Extracts **1st enchantment** cleanly; wipes remaining enchantments | ✔ **Preserved** | Pure Amethyst Resonance |
| ⭐ **Nether Star** | 1 | **Master extraction!** Extracts **all enchantments** at full level | ✔ **Preserved** | Amethyst + Celestial Beacon |
| 🧪 **Bottle o' Enchanting** | 1 | Extracts all **highest-level enchantments** | ❌ Consumed | Amethyst + Levelup Chime |

---

## 🛠️ Crafting Recipe

Crafted at a crafting table using **Crying Obsidian**, **Emerald Blocks**, and a **Black Carpet**:

```
[           ]  [Black Carpet]   [           ]
[Emerald Blk]  [Crying Obsid]   [Emerald Blk]
[Crying Obsid] [Crying Obsid]   [Crying Obsid]
```

---

## ⚙️ Configuration

Better Disenchanter features dedicated client and common configuration files with full Chinese comment support:

### 1. Client Visuals (`config/betterdisenchanter-client.toml`)
Fine-tune scales, billboard positions, background plate, and text shadows:
```toml
[visuals]
    # Scale of floating weapon/item (Default: 0.95)
    floatingItemScale = 0.95
    # Scale of 3D catalyst item (Default: 0.60)
    catalystItemScale = 0.60
    # Scale of text display billboard (Default: 0.022)
    textDisplayScale = 0.022
    # Weapon floating height (Default: 1.25)
    floatingItemYOffset = 1.25
    # Catalyst floating height (Default: 1.75)
    catalystItemYOffset = 1.75
    # Text billboard height, leaves 1 line space above catalyst (Default: 2.55)
    displayBillboardYOffset = 2.55
    # Toggle semi-transparent dark background plate (Default: true)
    showTextBackground = true
    # Toggle text drop shadow (Default: false)
    textDropShadow = false
```

### 2. Common Gameplay & Catalysts (`config/betterdisenchanter-common.toml`)
Control gameplay rules and customize catalyst counts:
```toml
[general]
    # Allow disenchanting without catalyst via empty hand (Default: true)
    allowDisenchantingWithoutCatalyst = true

[catalysts]
    [catalysts.emerald]
        enabled = true
        requiredCount = 1
    [catalysts.diamond]
        enabled = true
        requiredCount = 1
    # ... supports all 7 catalysts ...

[custom_catalysts]
    # Globally disable specific items as catalysts (e.g. ["minecraft:iron_ingot"])
    disabledCatalysts = []
    # Custom required item counts (e.g. ["minecraft:emerald=5", "minecraft:diamond=2"])
    customRequiredCounts = []
```

---

## 📦 Datapack Customization

All catalyst recipes are completely data-driven (`betterdisenchanter:catalyst`). Create custom catalysts via JSON in `data/<namespace>/recipe/catalyst/<name>.json`:

```json
{
  "type": "betterdisenchanter:catalyst",
  "ingredient": {
    "item": "minecraft:echo_shard"
  },
  "count": 1,
  "action": "extract_all",
  "max_enchantments": 1,
  "keep_item": true,
  "damage_item": 0,
  "level_cost": 0,
  "description": "Recovers all enchantments safely!"
}
```

---

<a name="chinese"></a>
## 📖 中文说明

**Better Disenchanter (更好的祛魔台)** 是基于 **Minecraft 1.21.1 (NeoForge)** 全新重制的沉浸式附魔回收模组。

融合了 *[Infusion Table](https://github.com/jptrzy/infusion-table-mod)* 的 **全 3D 沉浸式方块实体交互** 与 *[Disenchanter](https://github.com/gliscowo/disenchanter)* 的 **数据驱动催化剂机制**，摒弃了传统冰冷的 2D 菜单，将附魔回收过程升华为富有仪式感的魔法体验！

### 核心亮点：
- 📖 **全 3D 沉浸式交互**：无需打开 GUI。放书、放装备、手持催化剂右键一气呵成；支持左键敲击（空手空击）或潜行右键安全取回物品。
- 🔮 **动态 Item Display 悬浮预览**：手持催化剂靠近时，祛魔台上方自动升起 3D 旋转催化剂与双行悬浮看板，实时显示所需数量、背包余量及装备损毁/保留状态。
- 🎵 **个性化紫水晶音效**：采用紫水晶簇碎落（`amethyst_cluster.fall`）搭配每种催化剂独特的共鸣音调与和弦，仪式感满满。
- 🛡️ **原生抗穿模抗闪烁**：运用原版 `Font.DisplayMode.POLYGON_OFFSET` 与独立背景底板，在任何光影包（Iris / Oculus）、视距与视角下均永不闪烁、缺字或穿模。
- ⚙️ **双端独立配置文件**：
  - `config/betterdisenchanter-client.toml`：调节悬浮物大小、悬浮高度、背景黑框开关、文字阴影；
  - `config/betterdisenchanter-common.toml`：配置每种催化剂的启用状态、消耗数量（1~64）、空手祛魔开关等，全中文注释友好支持。

---

### 催化剂机制速查表：

| 催化剂物品 | 默认消耗 | 祛魔效果 | 原装备是否保留 | 仪式音效特点 |
| :--- | :---: | :--- | :---: | :--- |
| **无催化剂 (空手)** | 0 | 回收第 1 条附魔至附魔书 | ❌ 损毁消耗 | 标准紫水晶音效 (音调 1.0) |
| 🟢 **绿宝石** | 1 | 回收随机 2 条附魔 | ❌ 损毁消耗 | 紫水晶 + 高音清脆铃音 |
| 💎 **钻石** | 1 | 回收第 1 条 + 随机 2 条附魔 (最多3条) | ❌ 损毁消耗 | 紫水晶 + 低沉共振和弦 |
| 🔮 **末影珍珠** | 1 | 回收随机 1 条附魔；保留装备并损耗 500 耐久 | ✔ **保留** (耐久耗尽则爆) | 紫水晶 + 末影传送余韵 |
| 🌊 **海洋之心** | 1 | 回收所有附魔，附魔等级降低 1 级 (最低1级) | ❌ 损毁消耗 | 紫水晶 + 潮涌核心声浪 |
| 🟣 **紫水晶碎片** | 1 | 回收第 1 条附魔；抹除装备上其余附魔 | ✔ **无损保留** | 纯净紫水晶双重共振 |
| ⭐ **下界之星** | 1 | **完美回收**！满级回收全部附魔；抹除装备附魔 | ✔ **无损保留** | 紫水晶 + 烽火台天籁长鸣 |
| 🧪 **附魔之瓶** | 1 | 仅回收装备上等级最高的所有附魔 | ❌ 损毁消耗 | 紫水晶 + 升级叮当和音 |

---

## 📜 许可证 (License)

本项目采用 [MIT License](LICENSE) 开源。欢迎整合包作者自由引入与二次创作！
# 🥗 Healthy Diet

**Minecraft Forge 1.20.1**

A nutrition tracking mod that encourages players to eat a balanced diet.
Each food item can be assigned to one or more nutrition groups. Players earn nutrition points by eating, lose them over time through natural decay, and gain stat bonuses or effects when they maintain healthy levels.

---

## 🌟 Features

- 📊 **Nutrition Groups** — Configurable categories (Vegetables, Fruits, Meat, etc.), each with its own color, icon, and decay rate
- 🍔 **Food Fatigue System** — Eating the same food repeatedly causes fatigue; variety is rewarded
- 🏆 **Stage System** — Define named stages for each group (e.g. "Deficient / Balanced / Excellent") with custom descriptions and click-triggered commands
- ⚡ **Attribute & Effect Bonuses** — Grant players attribute modifiers or potion effects when their nutrition falls within a specified range
- 🧱 **Block Interaction System** — Right-clicking a configured block applies effects, runs commands, or changes the player's nutrition
- 🎯 **Conditional Commands** — Food items can execute a command only when the player's nutrition is within a set range at the time of eating
- 🖥️ **HUD Screen** — Press the keybind to open a stylish nutrition status screen with colored progress bars and active bonus tooltips
- 📝 **Fully Config-Driven** — Everything is controlled through JSON config files; no hardcoded values

---

## 📁 Config Files

All config files are located in `.minecraft/config/healthy_diet/`.

| File / Folder | Description |
|---|---|
| `groups.json` | Defines all nutrition groups (name, color, icon, decay rate) |
| `display.json` | Controls the HUD screen appearance |
| `items/<ns>/<path>.json` | Per-item config: nutrition values, effects, food fatigue, on-eat commands, conditional commands |
| `bonuses/<groupId>.json` | Range-based attribute/effect bonuses tied to a nutrition group |
| `stages/<groupId>.json` | Stage definitions for a nutrition group (name, range, descriptions, click commands) |
| `blocks/<ns>/<path>.json` | Block right-click config: effects, commands, nutrition changes |

---

## 🎮 Keybind

| Key | Action |
|---|---|
| `H` (default) | Open the nutrition status screen |

---

## 💻 Commands

> 🔒 All configuration commands require **OP level 4** unless otherwise noted.

---

### 👤 Player Nutrition — `/HeaDiet`

Manage a player's live nutrition values directly.

| Command | Description |
|---|---|
| `/HeaDiet get <player> <group>` | Get a player's current nutrition value |
| `/HeaDiet set <player> <group> <value>` | Set a player's nutrition to an exact value |
| `/HeaDiet add <player> <group> <value>` | Add to a player's nutrition |
| `/HeaDiet subtract <player> <group> <value>` | Subtract from a player's nutrition |
| `/HeaDiet reset <player> <group>` | Reset a player's nutrition to 0 |
| `/HeaDiet pause <player> <group>` | Pause decay for a group |
| `/HeaDiet resume <player> <group>` | Resume decay for a group |
| `/HeaDiet clear <player>` | Clear all nutrition data for a player |

---

### 🍎 Item Config — Food Nutrition

#### Set nutrition value
```
/HeaDietSet hand <group> <value>
/HeaDietSet <itemId> <group> <value>
```
Assigns nutrition gain to a food item when eaten. Supports decimals and negative values.

#### Set on-eat potion effect
```
/HeaDietSetEffect hand <effect> <level> <durationSecs> [showParticles]
/HeaDietSetEffect <itemId> <effect> <level> <durationSecs> [showParticles]
```
`level`: 0-based (0 = Level 1, 1 = Level 2, …)

#### Set food fatigue
```
/HeaDietSetFood hand <maxUses> <cooldownCount> <cooldownMinutes> <restoreHunger>
/HeaDietSetFood <itemId> <maxUses> <cooldownCount> <cooldownMinutes> <restoreHunger>
```
- `maxUses` — how many times in a row before fatigue kicks in
- `cooldownCount` — how many *different* foods must be eaten to reset the counter
- `cooldownMinutes` — real-time minutes before the counter resets (`-1` = no time limit)
- `restoreHunger` — hunger points still restored even while on cooldown (`0` = full block)

#### Set on-eat command
```
/HeaDietSetCmd hand <command>
/HeaDietSetCmd <itemId> <command>
/HeaDietSetCmd hand ClearCommand
/HeaDietSetCmd <itemId> ClearCommand
```
Commands support `@s` targeting the player. Use `ClearCommand` to remove all commands from an item.

#### Set conditional command
```
/HeaDietSetCondCmd hand <group> <min> <max> <command>
/HeaDietSetCondCmd <itemId> <group> <min> <max> <command>
```
Executes `<command>` when eating this item **only if** the player's `<group>` nutrition is within `[min, max]` at that moment.

#### View / Remove item config
```
/HeaDietInfo hand
/HeaDietInfo <itemId>

/HeaDietRemove <itemId> nutrition <group>
/HeaDietRemove <itemId> effect <index>
/HeaDietRemove <itemId> command <index>
/HeaDietRemove <itemId> food
/HeaDietRemove <itemId> condcmd <index>
```

---

### 📈 Group Bonuses — `HDEffect` / `HDAddition`

Bonuses activate when a player's nutrition falls within the specified range `[min, max]` (0–100).

#### Add potion effect bonus
```
/HDEffect <group> <min> <max> <effect> <level> <durationSecs> [showParticles]
```

#### Add attribute bonus
```
/HDAddition <group> <min> <max> <attribute> <amount>
```

#### View / Remove group bonuses
```
/HDInfo <group>          ← All players can view; only OP sees [X] buttons
/HDRemove <group> <index>
```

---

### 🏅 Stages — `HeaDietSetStage` / `HeaDietStage`

Stages are named segments of a nutrition group's 0–100 range, displayed in the HUD and optionally clickable.

#### Add a stage
```
/HeaDietSetStage <group> <min> <max> <stageName>
```

#### List / Remove stages
```
/HeaDietStage list <group>
/HeaDietStage remove <group> <index>
```

#### Stage tooltip descriptions
```
/HeaDietStageDesc <group> <stageIndex> add <text>
/HeaDietStageDesc <group> <stageIndex> list
/HeaDietStageDesc <group> <stageIndex> remove <lineIndex>
```

#### Stage click commands
```
/HeaDietStageCmd <group> <stageIndex> add <command>
/HeaDietStageCmd <group> <stageIndex> list
/HeaDietStageCmd <group> <stageIndex> remove <cmdIndex>
```

---

### 🧱 Block Right-Click System

Triggered when a player **right-clicks** a specific block with their **main hand** (server-side only).

#### Add potion effect
```
/HDBlockEffect <blockId> <effect> <level> <durationSecs> [showParticles]
```

#### Add command
```
/HDBlockCmd <blockId> <command>
```

#### Add nutrition change
```
/HDBlockGroup <blockId> <group> <value>
```
Positive `value` adds nutrition; negative value subtracts.

#### View / Remove block config
```
/HDBlockList <blockId>
/HDBlockRemove <blockId> effect <index>
/HDBlockRemove <blockId> cmd <index>
/HDBlockRemove <blockId> group <index>
```

---

## 📖 Examples

### 🥕 Carrot gives +5 Vegetables nutrition
```
/HeaDietSet minecraft:carrot vegetables 5
```

### 🍖 Cooked Beef gives Strength on eat (Lv.1, 10 seconds)
```
/HeaDietSetEffect minecraft:cooked_beef strength 0 10
```

### 😴 Bread has fatigue: max 5 uses, needs 3 other foods to reset
```
/HeaDietSetFood minecraft:bread 5 3 -1 0
```

### 🌙 Eating Apple when Fruits < 25 triggers a subtraction command
```
/HeaDietSetCondCmd minecraft:apple fruits 0 25 /HeaDiet add @s fruits 10
```

### ⚡ When Vegetables nutrition is 75–100, give +4 Max Health
```
/HDAddition vegetables 75 100 minecraft:generic.max_health 4
```

### 🌿 When Grains nutrition is 50–100, give Haste II every tick
```
/HDEffect grains 50 100 minecraft:haste 1 40 false
```

### 🪨 Right-clicking a Crafting Table gives +3 Grains
```
/HDBlockGroup minecraft:crafting_table grains 3
```

### 🏆 Stage: Vegetables 0–30 = "Deficient", 31–70 = "Balanced", 71–100 = "Excellent"
```
/HeaDietSetStage vegetables 0 30 Deficient
/HeaDietSetStage vegetables 31 70 Balanced
/HeaDietSetStage vegetables 71 100 Excellent
```

---

## 🗂️ Config Tips

- **Multi-group bonuses** can be created by manually editing `bonuses/<groupId>.json` — a single `BonusEntry` can have multiple conditions (one per group), all of which must be satisfied simultaneously.
- **Decay rate** in `groups.json` is in nutrition points per minute.
- **Stage names** support color codes using `§` (e.g. `§aExcellent`).
- **Display config** (`display.json`) includes annotated `_note_` fields explaining each setting in both English and Chinese.

---

---

# 🥗 健康饮食 (Healthy Diet)

**Minecraft Forge 1.20.1**

一款营养追踪模组，鼓励玩家保持均衡饮食。
每种食物可被分配到一个或多个营养组，玩家通过进食积累营养值，营养值随时间自然衰减，在保持健康水平时可获得属性加成或药水效果。

---

## 🌟 功能特性

- 📊 **营养组** — 可自由配置的分类（蔬菜、水果、肉类等），每组拥有独立的颜色、图标和衰减速率
- 🍔 **食物厌倦系统** — 反复食用同一种食物会产生厌倦；多样化饮食才能保持最佳状态
- 🏆 **阶段系统** — 为每个营养组定义命名阶段（如"缺乏 / 均衡 / 优秀"），支持自定义描述和点击指令
- ⚡ **属性与效果加成** — 当玩家营养值处于指定范围内时，授予属性修正或药水效果
- 🧱 **方块交互系统** — 右键点击已配置的方块，可触发药水效果、执行指令或改变营养值
- 🎯 **条件指令** — 食物在被食用时，若玩家营养值处于指定范围内才执行对应指令
- 🖥️ **HUD 界面** — 按下快捷键打开营养状态界面，展示彩色进度条和当前激活的加成提示
- 📝 **纯配置驱动** — 所有内容均通过 JSON 配置文件控制，无硬编码数值

---

## 📁 配置文件

所有配置文件位于 `.minecraft/config/healthy_diet/`。

| 文件 / 目录 | 说明 |
|---|---|
| `groups.json` | 定义所有营养组（名称、颜色、图标、衰减速率）|
| `display.json` | 控制 HUD 界面外观 |
| `items/<命名空间>/<路径>.json` | 单个物品配置：营养值、效果、食物厌倦、食用指令、条件指令 |
| `bonuses/<groupId>.json` | 基于营养值范围的属性/效果加成 |
| `stages/<groupId>.json` | 营养组的阶段定义（名称、范围、描述、点击指令）|
| `blocks/<命名空间>/<路径>.json` | 方块右键配置：药水效果、指令、营养变化 |

---

## 🎮 快捷键

| 按键 | 功能 |
|---|---|
| `H`（默认）| 打开营养状态界面 |

---

## 💻 指令说明

> 🔒 所有配置类指令均需要 **OP 权限（等级 4）**，另有说明除外。

---

### 👤 玩家营养值 — `/HeaDiet`

直接管理玩家的实时营养值。

| 指令 | 说明 |
|---|---|
| `/HeaDiet get <玩家> <组>` | 查询玩家当前营养值 |
| `/HeaDiet set <玩家> <组> <值>` | 将玩家营养值设为指定数值 |
| `/HeaDiet add <玩家> <组> <值>` | 增加玩家营养值 |
| `/HeaDiet subtract <玩家> <组> <值>` | 减少玩家营养值 |
| `/HeaDiet reset <玩家> <组>` | 将玩家营养值重置为 0 |
| `/HeaDiet pause <玩家> <组>` | 暂停该组的衰减 |
| `/HeaDiet resume <玩家> <组>` | 恢复该组的衰减 |
| `/HeaDiet clear <玩家>` | 清空玩家的所有营养数据 |

---

### 🍎 物品配置 — 食物营养

#### 设置营养值
```
/HeaDietSet hand <组> <值>
/HeaDietSet <物品ID> <组> <值>
```
为食物分配食用后的营养增益，支持小数和负值。

#### 设置食用后药水效果
```
/HeaDietSetEffect hand <效果> <等级> <持续秒数> [显示粒子]
/HeaDietSetEffect <物品ID> <效果> <等级> <持续秒数> [显示粒子]
```
`等级`：从 0 开始（0 = Lv.1，1 = Lv.2，以此类推）

#### 设置食物厌倦
```
/HeaDietSetFood hand <最大食用次数> <冷却所需食物数> <冷却分钟数> <厌倦时恢复饱食度>
/HeaDietSetFood <物品ID> <最大食用次数> <冷却所需食物数> <冷却分钟数> <厌倦时恢复饱食度>
```
- `最大食用次数` — 连续食用多少次后触发厌倦
- `冷却所需食物数` — 需要吃多少种**不同**食物才能重置计数器
- `冷却分钟数` — 多少真实分钟后自动重置（`-1` 表示无时间限制）
- `厌倦时恢复饱食度` — 厌倦状态下仍可恢复的饱食度（`0` 表示完全阻止）

#### 设置食用指令
```
/HeaDietSetCmd hand <指令>
/HeaDietSetCmd <物品ID> <指令>
/HeaDietSetCmd hand ClearCommand
/HeaDietSetCmd <物品ID> ClearCommand
```
指令支持 `@s` 以玩家为目标执行。使用 `ClearCommand` 可清空该物品的所有食用指令。

#### 设置条件指令
```
/HeaDietSetCondCmd hand <组> <最小值> <最大值> <指令>
/HeaDietSetCondCmd <物品ID> <组> <最小值> <最大值> <指令>
```
仅当玩家食用时该营养组的值处于 `[最小值, 最大值]` 范围内，才执行 `<指令>`。

#### 查看 / 删除物品配置
```
/HeaDietInfo hand
/HeaDietInfo <物品ID>

/HeaDietRemove <物品ID> nutrition <组>
/HeaDietRemove <物品ID> effect <序号>
/HeaDietRemove <物品ID> command <序号>
/HeaDietRemove <物品ID> food
/HeaDietRemove <物品ID> condcmd <序号>
```

---

### 📈 营养组加成 — `HDEffect` / `HDAddition`

当玩家营养值处于 `[min, max]`（0–100）范围内时，激活对应加成。

#### 添加药水效果加成
```
/HDEffect <组> <min> <max> <效果> <等级> <持续秒数> [显示粒子]
```

#### 添加属性加成
```
/HDAddition <组> <min> <max> <属性> <数值>
```

#### 查看 / 删除加成
```
/HDInfo <组>          ← 所有玩家可查看；仅 OP 显示 [X] 删除按钮
/HDRemove <组> <序号>
```

---

### 🏅 阶段系统 — `HeaDietSetStage` / `HeaDietStage`

阶段是营养组 0–100 范围内的命名区间，在 HUD 界面中显示，可选择点击触发。

#### 添加阶段
```
/HeaDietSetStage <组> <min> <max> <阶段名>
```

#### 列表 / 删除阶段
```
/HeaDietStage list <组>
/HeaDietStage remove <组> <序号>
```

#### 阶段描述行
```
/HeaDietStageDesc <组> <阶段序号> add <文本>
/HeaDietStageDesc <组> <阶段序号> list
/HeaDietStageDesc <组> <阶段序号> remove <行序号>
```

#### 阶段点击指令
```
/HeaDietStageCmd <组> <阶段序号> add <指令>
/HeaDietStageCmd <组> <阶段序号> list
/HeaDietStageCmd <组> <阶段序号> remove <指令序号>
```

---

### 🧱 方块右键系统

当玩家用**主手右键点击**指定方块时触发（仅服务端，每次点击执行一次）。

#### 添加药水效果
```
/HDBlockEffect <方块ID> <效果> <等级> <持续秒数> [显示粒子]
```

#### 添加执行指令
```
/HDBlockCmd <方块ID> <指令>
```

#### 添加营养变化
```
/HDBlockGroup <方块ID> <组> <值>
```
正值为增加，负值为减少。

#### 查看 / 删除方块配置
```
/HDBlockList <方块ID>
/HDBlockRemove <方块ID> effect <序号>
/HDBlockRemove <方块ID> cmd <序号>
/HDBlockRemove <方块ID> group <序号>
```

---

## 📖 使用示例

### 🥕 胡萝卜提供 +5 蔬菜营养值
```
/HeaDietSet minecraft:carrot vegetables 5
```

### 🍖 熟牛肉食用后获得力量效果（Lv.1，10 秒）
```
/HeaDietSetEffect minecraft:cooked_beef strength 0 10
```

### 😴 面包厌倦设置：最多连续食用 5 次，需吃 3 种其他食物才能重置
```
/HeaDietSetFood minecraft:bread 5 3 -1 0
```

### 🌙 苹果：当水果营养值低于 25 时，食用后追加营养补偿
```
/HeaDietSetCondCmd minecraft:apple fruits 0 25 /HeaDiet add @s fruits 10
```

### ⚡ 蔬菜营养值达到 75–100 时，给予 +4 最大生命值
```
/HDAddition vegetables 75 100 minecraft:generic.max_health 4
```

### 🌿 谷物营养值达到 50–100 时，每 tick 给予急迫 II
```
/HDEffect grains 50 100 minecraft:haste 1 40 false
```

### 🪨 右键点击工作台，谷物营养值 +3
```
/HDBlockGroup minecraft:crafting_table grains 3
```

### 🏆 蔬菜阶段：0–30 = "缺乏"，31–70 = "均衡"，71–100 = "优秀"
```
/HeaDietSetStage vegetables 0 30 缺乏
/HeaDietSetStage vegetables 31 70 均衡
/HeaDietSetStage vegetables 71 100 优秀
```

---

## 🗂️ 配置建议

- **多组联动加成** 可通过手动编辑 `bonuses/<groupId>.json` 实现 — 单个 `BonusEntry` 可包含多个条件（每组一个），所有条件同时满足时才激活。
- **衰减速率**（`decayRate`）的单位为每分钟减少的营养点数。
- **阶段名称**支持使用 `§` 颜色代码（例如 `§a优秀`）。
- **显示配置**（`display.json`）中每个字段前都有中英双语的 `_note_` 注释字段，方便理解各项设置的含义。

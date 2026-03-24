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
- 🎲 **Potion Probability** — Each on-eat potion effect has an optional trigger probability (0–100%, default 100%)
- 🖥️ **HUD Screen** — Press the keybind to open a stylish nutrition status screen with colored progress bars and active bonus tooltips
- 📝 **Fully Config-Driven** — Everything is controlled through JSON config files; no hardcoded values

---

## 📁 Config Files

All config files are located in `.minecraft/config/healthy_diet/`.

| File / Folder | Description |
|---|---|
| `groups.json` | Defines all nutrition groups (name, color, icon, decay rate) |
| `display.json` | Controls the HUD screen appearance and update intervals |
| `items/<ns>/<path>.json` | Per-item config: nutrition values, effects, food fatigue, on-eat commands, conditional commands |
| `bonuses/<groupId>.json` | Range-based attribute/effect bonuses tied to a nutrition group |
| `stages/<groupId>.json` | Stage definitions for a nutrition group (name, range, descriptions, click commands) |
| `blocks/<ns>/<path>.json` | Block right-click config: effects, commands, nutrition changes |

### Key `display.json` fields

| Field | Default | Description |
|---|---|---|
| `bonusUpdateTicks` | `100` (5 s) | How often (in ticks) attribute bonuses are re-evaluated |
| `effectUpdateTicks` | `200` (10 s) | How often (in ticks) potion effect bonuses are re-applied |

---

## 🎮 Keybind

| Key | Action |
|---|---|
| `H` (default) | Open the nutrition status screen |

---

## 💻 Commands

> 🔒 All configuration commands require **OP level 4** unless otherwise noted.
> All command names are **fully lowercase** and are usable inside datapack `.mcfunction` files.

> 💡 **Built-in help:** Typing any command name without arguments (e.g. `/headiet`, `/headietset`) displays a colored usage reference directly in chat — no need to look up the docs.

---

### 👤 Player Nutrition — `/headiet`

Manage a player's live nutrition values directly.

| Command | Description |
|---|---|
| `/headiet open [player]` | Open the nutrition screen (self or target) |
| `/headiet get <player> <group>` | Get a player's current nutrition value |
| `/headiet set <player> <group\|all> <value>` | Set a player's nutrition to an exact value (`all` sets every group) |
| `/headiet add <player> <group> <value>` | Add to a player's nutrition |
| `/headiet subtract <player> <group> <value>` | Subtract from a player's nutrition |
| `/headiet reset <player> <group>` | Reset a player's nutrition to 0 |
| `/headiet pause <player> <group\|all>` | Pause decay for a group (`all` pauses every group) |
| `/headiet resume <player> <group\|all>` | Resume decay for a group (`all` resumes every group) |
| `/headiet clear <player>` | Clear all nutrition data for a player |

---

### 🍎 Item Config — Food Nutrition

#### Set nutrition value
```
/headietset hand <group> <value>
/headietset <itemId> <group> <value>
```
Assigns nutrition gain to a food item when eaten. Supports decimals and negative values.

#### Set on-eat potion effect
```
/headietseteffect hand <effect> <level> <durationSecs> [showParticles] [probability]
/headietseteffect <itemId> <effect> <level> <durationSecs> [showParticles] [probability]
```
- `level`: 0-based amplifier (0 = Level I, 1 = Level II, …)
- `showParticles`: `true` / `false` (default `false`)
- `probability`: trigger chance 0–100 % (default `100` = always triggers)

#### Set food fatigue
```
/headietsetfood hand <maxUses> <cooldownCount> [cooldownMinutes [restoreHunger]]
/headietsetfood <itemId> <maxUses> <cooldownCount> [cooldownMinutes [restoreHunger]]
```
- `maxUses` — how many times in a row before fatigue kicks in
- `cooldownCount` — how many *different* foods must be eaten to reset the counter
- `cooldownMinutes` — real-time minutes before the counter resets (`-1` = no time limit)
- `restoreHunger` — hunger points still restored even while on cooldown (`0` = full block)

#### Set on-eat command
```
/headietsetcommand hand <command>
/headietsetcommand <itemId> <command>
/headietsetcommand hand clearcommand
/headietsetcommand <itemId> clearcommand
```
Commands support `@s` targeting the player. Use `clearcommand` to remove all commands from an item.

#### Set conditional command
```
/headietsetcondcmd hand <group> <min> <max> <command>
/headietsetcondcmd <itemId> <group> <min> <max> <command>
```
Executes `<command>` when eating this item **only if** the player's `<group>` nutrition is within `[min, max]` at that moment.

#### View / Remove item config
```
/headietinfo hand
/headietinfo <itemId>

/headietremove <itemId> nutrition <group>
/headietremove <itemId> effect <index>
/headietremove <itemId> command <index>
/headietremove <itemId> food
/headietremove <itemId> condcmd <index>
```

**`/headietinfo` output format:**
```
§e[Item Name] §fNutrition Info:
§6[Nutrition] §dGroup Name: §f+5
§b[Effect] §aEffect Name II §710s §a[Particles]   ← [Particles] only shown when enabled
§c[Command] §5/say hello
§e[Cond] §dGroup §7[0~50]: §f/say low nutrition
§d[Fatigue] §7Max Uses: §f5 §7Cooldown Foods: §f3
```

---

### 📈 Group Bonuses — `hdeffect` / `hdaddition`

Bonuses activate when a player's nutrition falls within the specified range `[min, max]` (0–100).

#### Add potion effect bonus
```
/hdeffect <group> <min> <max> <effect> <level> <durationSecs> [showParticles]
```

#### Add attribute bonus
```
/hdaddition <group> <min> <max> <attribute> <amount>
```

#### View / Remove group bonuses
```
/hdinfo <group>          ← All players can view; only OP sees [X] buttons
/hdremove <group> <index>
```

---

### 🏅 Stages — `headietsetstage` / `headietstage`

Stages are named segments of a nutrition group's 0–100 range, displayed in the HUD and optionally clickable.

#### Add a stage
```
/headietsetstage <group> <min> <max> <stageName>
```

#### List / Remove stages
```
/headietstage list <group>
/headietstage remove <group> <index>
```

#### Stage tooltip descriptions
```
/headietstagedesc <group> <stageIndex> add <text>
/headietstagedesc <group> <stageIndex> list
/headietstagedesc <group> <stageIndex> remove <lineIndex>
```

#### Stage click commands
```
/headietstagecmd <group> <stageIndex> add <command>
/headietstagecmd <group> <stageIndex> list
/headietstagecmd <group> <stageIndex> remove <cmdIndex>
```

---

### 🧱 Block Right-Click System

Triggered when a player **right-clicks** a specific block with their **main hand** (server-side only).

#### Add potion effect
```
/hdblockeffect <blockId> <effect> <level> <durationSecs> [showParticles]
```

#### Add command
```
/hdblockcmd <blockId> <command>
```

#### Add nutrition change
```
/hdblockgroup <blockId> <group> <value>
```
Positive `value` adds nutrition; negative value subtracts.

#### View / Remove block config
```
/hdblocklist <blockId>
/hdblockremove <blockId> effect <index>
/hdblockremove <blockId> cmd <index>
/hdblockremove <blockId> group <index>
```

---

## 📖 Examples

### 🥕 Carrot gives +5 Vegetables nutrition
```
/headietset minecraft:carrot vegetables 5
```

### 🍖 Cooked Beef gives Strength I on eat (10 seconds)
```
/headietseteffect minecraft:cooked_beef minecraft:strength 0 10
```

### 🎲 Golden Apple has a 50% chance to give Regeneration II (5 seconds)
```
/headietseteffect minecraft:golden_apple minecraft:regeneration 1 5 false 50
```

### 😴 Bread has fatigue: max 5 uses, needs 3 other foods to reset
```
/headietsetfood minecraft:bread 5 3 -1 0
```

### 🌙 Eating Apple when Fruits < 25 triggers a compensation command
```
/headietsetcondcmd minecraft:apple fruits 0 25 /headiet add @s fruits 10
```

### ⚡ When Vegetables nutrition is 75–100, give +4 Max Health
```
/hdaddition vegetables 75 100 minecraft:generic.max_health 4
```

### 🌿 When Grains nutrition is 50–100, give Haste II every 10 s
```
/hdeffect grains 50 100 minecraft:haste 1 40 false
```

### 🪨 Right-clicking a Crafting Table gives +3 Grains
```
/hdblockgroup minecraft:crafting_table grains 3
```

### 🏆 Stage: Vegetables 0–30 = "Deficient", 31–70 = "Balanced", 71–100 = "Excellent"
```
/headietsetstage vegetables 0 30 Deficient
/headietsetstage vegetables 31 70 Balanced
/headietsetstage vegetables 71 100 Excellent
```

### ⏸️ Pause all nutrition groups for a player
```
/headiet pause Steve all
```

### 🔄 Set all nutrition groups to 50 for a player
```
/headiet set Steve all 50
```

---

## 🗂️ Config Tips

- **Multi-group bonuses** can be created by manually editing `bonuses/<groupId>.json` — a single `BonusEntry` can have multiple conditions (one per group), all of which must be satisfied simultaneously.
- **Decay rate** (`decayRate`) in `groups.json`: every `decayRate` hunger points consumed reduces nutrition by 1 point. Decay now produces fractional values (e.g. decayRate 6 → each hunger point consumed lowers nutrition by ~0.167).
- **Stage names** support color codes using `§` (e.g. `§aExcellent`).
- **Display config** (`display.json`) includes annotated `_note_` fields explaining each setting in both English and Chinese.
- **Bonus update interval** is configurable via `bonusUpdateTicks` (default 100 = 5 seconds) and `effectUpdateTicks` (default 200 = 10 seconds) in `display.json`.

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
- 🎲 **药水概率** — 食物的每条药水效果可设置独立触发概率（0–100%，默认 100%）
- 🖥️ **HUD 界面** — 按下快捷键打开营养状态界面，展示彩色进度条和当前激活的加成提示
- 📝 **纯配置驱动** — 所有内容均通过 JSON 配置文件控制，无硬编码数值

---

## 📁 配置文件

所有配置文件位于 `.minecraft/config/healthy_diet/`。

| 文件 / 目录 | 说明 |
|---|---|
| `groups.json` | 定义所有营养组（名称、颜色、图标、衰减速率）|
| `display.json` | 控制 HUD 界面外观及更新间隔 |
| `items/<命名空间>/<路径>.json` | 单个物品配置：营养值、效果、食物厌倦、食用指令、条件指令 |
| `bonuses/<groupId>.json` | 基于营养值范围的属性/效果加成 |
| `stages/<groupId>.json` | 营养组的阶段定义（名称、范围、描述、点击指令）|
| `blocks/<命名空间>/<路径>.json` | 方块右键配置：药水效果、指令、营养变化 |

### `display.json` 关键字段

| 字段 | 默认值 | 说明 |
|---|---|---|
| `bonusUpdateTicks` | `100`（5 秒）| 属性加成重新评估的间隔（tick）|
| `effectUpdateTicks` | `200`（10 秒）| 药水效果加成刷新的间隔（tick）|

---

## 🎮 快捷键

| 按键 | 功能 |
|---|---|
| `H`（默认）| 打开营养状态界面 |

---

## 💻 指令说明

> 🔒 所有配置类指令均需要 **OP 权限（等级 4）**，另有说明除外。
> 所有指令名称均为**全小写**，可在数据包 `.mcfunction` 文件中直接使用。

> 💡 **内置帮助：** 不带参数直接执行任意指令（如 `/headiet`、`/headietset`），即可在聊天框中看到该指令的彩色用法提示，无需查阅文档。

---

### 👤 玩家营养值 — `/headiet`

直接管理玩家的实时营养值。

| 指令 | 说明 |
|---|---|
| `/headiet open [玩家]` | 打开营养界面（自身或指定玩家）|
| `/headiet get <玩家> <组>` | 查询玩家当前营养值 |
| `/headiet set <玩家> <组\|all> <值>` | 设定营养值（`all` 一键设定所有组）|
| `/headiet add <玩家> <组> <值>` | 增加玩家营养值 |
| `/headiet subtract <玩家> <组> <值>` | 减少玩家营养值 |
| `/headiet reset <玩家> <组>` | 将玩家营养值重置为 0 |
| `/headiet pause <玩家> <组\|all>` | 暂停衰减（`all` 暂停所有组）|
| `/headiet resume <玩家> <组\|all>` | 恢复衰减（`all` 恢复所有组）|
| `/headiet clear <玩家>` | 清空玩家的所有营养数据 |

---

### 🍎 物品配置 — 食物营养

#### 设置营养值
```
/headietset hand <组> <值>
/headietset <物品ID> <组> <值>
```
为食物分配食用后的营养增益，支持小数和负值。

#### 设置食用后药水效果
```
/headietseteffect hand <效果> <等级> <持续秒数> [显示粒子] [概率]
/headietseteffect <物品ID> <效果> <等级> <持续秒数> [显示粒子] [概率]
```
- `等级`：从 0 开始（0 = Lv.I，1 = Lv.II，以此类推）
- `显示粒子`：`true` / `false`（默认 `false`）
- `概率`：触发概率 0–100%（默认 `100`，即必定触发）

#### 设置食物厌倦
```
/headietsetfood hand <最大食用次数> <冷却所需食物数> [冷却分钟数 [厌倦时恢复饱食度]]
/headietsetfood <物品ID> <最大食用次数> <冷却所需食物数> [冷却分钟数 [厌倦时恢复饱食度]]
```
- `最大食用次数` — 连续食用多少次后触发厌倦
- `冷却所需食物数` — 需要吃多少种**不同**食物才能重置计数器
- `冷却分钟数` — 多少真实分钟后自动重置（`-1` 表示无时间限制）
- `厌倦时恢复饱食度` — 厌倦状态下仍可恢复的饱食度（`0` 表示完全阻止）

#### 设置食用指令
```
/headietsetcommand hand <指令>
/headietsetcommand <物品ID> <指令>
/headietsetcommand hand clearcommand
/headietsetcommand <物品ID> clearcommand
```
指令支持 `@s` 以玩家为目标执行。使用 `clearcommand` 可清空该物品的所有食用指令。

#### 设置条件指令
```
/headietsetcondcmd hand <组> <最小值> <最大值> <指令>
/headietsetcondcmd <物品ID> <组> <最小值> <最大值> <指令>
```
仅当玩家食用时该营养组的值处于 `[最小值, 最大值]` 范围内，才执行 `<指令>`。

#### 查看 / 删除物品配置
```
/headietinfo hand
/headietinfo <物品ID>

/headietremove <物品ID> nutrition <组>
/headietremove <物品ID> effect <序号>
/headietremove <物品ID> command <序号>
/headietremove <物品ID> food
/headietremove <物品ID> condcmd <序号>
```

**`/headietinfo` 输出格式：**
```
§e[物品显示名] §f营养信息：
§6[营养] §d组名：§f+5
§b[药水] §a效果名 II §710s §a[粒子]   ← [粒子] 仅在开启粒子时显示
§c[指令] §5/say hello
§e[条件] §d组名 §7[0~50]：§f/say 营养不足
§d[厌倦] §7最大食用：§f5 §7冷却所需：§f3
```

---

### 📈 营养组加成 — `hdeffect` / `hdaddition`

当玩家营养值处于 `[min, max]`（0–100）范围内时，激活对应加成。

#### 添加药水效果加成
```
/hdeffect <组> <min> <max> <效果> <等级> <持续秒数> [显示粒子]
```

#### 添加属性加成
```
/hdaddition <组> <min> <max> <属性> <数值>
```

#### 查看 / 删除加成
```
/hdinfo <组>          ← 所有玩家可查看；仅 OP 显示 [X] 删除按钮
/hdremove <组> <序号>
```

---

### 🏅 阶段系统 — `headietsetstage` / `headietstage`

阶段是营养组 0–100 范围内的命名区间，在 HUD 界面中显示，可选择点击触发。

#### 添加阶段
```
/headietsetstage <组> <min> <max> <阶段名>
```

#### 列表 / 删除阶段
```
/headietstage list <组>
/headietstage remove <组> <序号>
```

#### 阶段描述行
```
/headietstagedesc <组> <阶段序号> add <文本>
/headietstagedesc <组> <阶段序号> list
/headietstagedesc <组> <阶段序号> remove <行序号>
```

#### 阶段点击指令
```
/headietstagecmd <组> <阶段序号> add <指令>
/headietstagecmd <组> <阶段序号> list
/headietstagecmd <组> <阶段序号> remove <指令序号>
```

---

### 🧱 方块右键系统

当玩家用**主手右键点击**指定方块时触发（仅服务端，每次点击执行一次）。

#### 添加药水效果
```
/hdblockeffect <方块ID> <效果> <等级> <持续秒数> [显示粒子]
```

#### 添加执行指令
```
/hdblockcmd <方块ID> <指令>
```

#### 添加营养变化
```
/hdblockgroup <方块ID> <组> <值>
```
正值为增加，负值为减少。

#### 查看 / 删除方块配置
```
/hdblocklist <方块ID>
/hdblockremove <方块ID> effect <序号>
/hdblockremove <方块ID> cmd <序号>
/hdblockremove <方块ID> group <序号>
```

---

## 📖 使用示例

### 🥕 胡萝卜提供 +5 蔬菜营养值
```
/headietset minecraft:carrot vegetables 5
```

### 🍖 熟牛肉食用后获得力量 I 效果（10 秒）
```
/headietseteffect minecraft:cooked_beef minecraft:strength 0 10
```

### 🎲 金苹果有 50% 概率触发生命恢复 II（5 秒）
```
/headietseteffect minecraft:golden_apple minecraft:regeneration 1 5 false 50
```

### 😴 面包厌倦设置：最多连续食用 5 次，需吃 3 种其他食物才能重置
```
/headietsetfood minecraft:bread 5 3 -1 0
```

### 🌙 苹果：当水果营养值低于 25 时，食用后追加营养补偿
```
/headietsetcondcmd minecraft:apple fruits 0 25 /headiet add @s fruits 10
```

### ⚡ 蔬菜营养值达到 75–100 时，给予 +4 最大生命值
```
/hdaddition vegetables 75 100 minecraft:generic.max_health 4
```

### 🌿 谷物营养值达到 50–100 时，每 10 秒给予急迫 II
```
/hdeffect grains 50 100 minecraft:haste 1 40 false
```

### 🪨 右键点击工作台，谷物营养值 +3
```
/hdblockgroup minecraft:crafting_table grains 3
```

### 🏆 蔬菜阶段：0–30 = "缺乏"，31–70 = "均衡"，71–100 = "优秀"
```
/headietsetstage vegetables 0 30 缺乏
/headietsetstage vegetables 31 70 均衡
/headietsetstage vegetables 71 100 优秀
```

### ⏸️ 一键暂停玩家所有营养组衰减
```
/headiet pause Steve all
```

### 🔄 一键将玩家所有营养组设为 50
```
/headiet set Steve all 50
```

---

## 🗂️ 配置建议

- **多组联动加成** 可通过手动编辑 `bonuses/<groupId>.json` 实现 — 单个 `BonusEntry` 可包含多个条件（每组一个），所有条件同时满足时才激活。
- **衰减速率**（`decayRate`）：每消耗 `decayRate` 个饱食度，营养值降低 1 点。衰减现采用连续小数计算（例如 decayRate=6，每消耗 1 饱食度降低 约 0.167），数值会在 UI 中实时显示小数。
- **阶段名称**支持使用 `§` 颜色代码（例如 `§a优秀`）。
- **显示配置**（`display.json`）中每个字段前都有中英双语的 `_note_` 注释字段，方便理解各项设置的含义。
- **加成更新间隔** 可通过 `display.json` 中的 `bonusUpdateTicks`（默认 100 = 5 秒）和 `effectUpdateTicks`（默认 200 = 10 秒）自定义。

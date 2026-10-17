# Healthy Diet

**Minecraft 1.20.1 · Forge · 服务器级食物系统 mod / A server-grade food system mod**

[中文](#中文) · [English](#english)

---
Wiki https://www.bilibili.com/video/BV1tHYu6REwz
---
## 中文

把「吃东西」从一根饱食度条，扩展成**营养结构 · 保质期 · 显示表现**三层体系，
并且**绝大部分都可以在游戏内用 GUI 配置** —— 不需要手写 JSON。

### 这个 mod 解决什么问题

原版的食物只有两个维度：回多少饱食度、回多少饱和度。于是最优解永远是「找到一种高效食物，然后一直吃它」。
Healthy Diet 从三个方向改掉这件事：

1. **营养要成结构** —— 食物供给的是**服主定义的营养组**，多个组同时维持在合理区间才能拿到属性与药水加成。
2. **重复吃要有代价** —— 短期厌倦 + 终身厌食，一直啃同一种食物会越吃越不顶。
3. **食物会坏** —— 一整套受环境温度、冰箱、季节影响的保质期系统。

配套还有一层显示系统（渐变名称、品质、品级、可视化 Tooltip 编辑器、TitlePro 标题引擎），
让这些机制在服务器上真的能被玩家看懂。

### 营养系统

- **营养组由服主定义**。全新安装时生成 6 个默认组：乳制品、水果、谷物、蛋白质（id `meat`，肉 / 鱼 / 蛋都归这一组）、糖分、蔬菜。
- `/hdui create` 的「营养值」页可以**新建 / 修改 / 删除**营养组：名称、图标、颜色 / 渐变、衰减率、初始值、排序。
  新建后在线玩家**立刻**获得初始值，不用重新登录。
- 每个物品可以同时给多个组供给营养值。
- `/headiet` 打开自己的营养面板；OP 可以增减 / 设定他人的营养值。

### 阶段（Stage）与联动加成

- 每个营养组的 0–100 可以切成任意数量的**阶段**，每段有自己的名字、悬停说明，并且可以**在进入时执行指令**。
- **加成条目**支持多组联合条件：例如「蛋白质 ≥ 60 **且** 蔬菜 ≥ 40」时，给 `generic.max_health +4` 外加再生 II。
  - 属性加成：`attribute` / `operation` / `value`
  - 药水加成：`effect` / `level` / `duration` / `showParticles`
- `/hdui stage` 打开阶段与加成的管理界面，`/hdui groups [玩家]` 打开玩家营养值编辑界面。

### 食物厌倦 / 终身厌食

| 机制 | 配置项 | 效果 |
|---|---|---|
| 短期厌倦 `foodCooldown` | `maxUses` / `cooldownCount` / `cooldownMinutes` / `restoreHunger` | 短时间内重复吃同一种食物，回复量下降，需要等待或换口味 |
| 终身厌食 `foodFatigue` | `lossStart` / `aversionCount` | 按存档累计的进食次数，永久性地削弱这种食物 |

Tooltip 会直接显示「厌倦度 %」「剩余可吃次数」「恢复倒计时」。`shelf_env/fatigue_exempt.json` 可以豁免特定物品。

> ⚠ 只配 `lossStart`、不配 `aversionCount` 是**合法但无效**的配置（既不衰减也不厌食）—— 这是已知且有意保留的行为，不是 bug。

### 保质期系统（shelf2）

食物有自己的时钟和温度，会随环境变质：

- **环境温度三级回退**：`LSO 完整世界环境` → `Ecliptic Seasons 气候 + HD 方块偏移` → `HD 自带回退`。
  `/hdtemperature` 可以当场对比三条路径的取值、当前实际来源，以及周围哪些方块在贡献温度。
- **冰箱**：独立的目标温度与温变速度，支持**断电**判定，以及离开冰箱后的**冷保（coldRetention）**。
- **温区效果**：食物处于特定温度带时可触发额外效果（融化、升华等方案随包内置）。
- **过期结果**：到期后替换成指定物品，而不是简单消失。
- **合并规则**：任何路径下堆叠合并**一律取剩余时间较短的那份**，杜绝「用新鲜的洗白过期的」。
- **掉落物同样计时**；装了 Jade 可以直接在地上的掉落物上看到剩余保质期。
- **放置食物**（蛋糕及所有带 `bites` 属性的方块）：放下后保质期切到世界时钟继续走，每咬一口按口数分摊营养，
  过期按配置处理；完整的蛋糕被破坏时掉出带原数据的物品。

### 品质 · 品级 · 显示

- **品质**：等级 1~99。在 `/hdui create` 的「品质」页编辑：覆盖层贴图，以及**分段文本**（与 `/hdui tooltip` 同一个分段编辑界面，
  每段各自的文字与渐变）。各段文字拼起来就是品质名称。
- **品质物品注册表**：同一物品可注册多个品质变体（独立物品，带自己的创造模式标签页）；注册只在**启动时**发生，
  `/hdreload` 会把注册表文件推给在线玩家，下次启动生效。
- **品级（稀有度）**：Obscure Tooltips 稀有度标签上的渐变名称。在 `/hdui create` 的「品级」页编辑：改已有品级单机即时生效；
  **新建 / 删除要重启服务端与客户端**。`/hdset rarity <品级|原版稀有度|clear> [物品ID]` 一行命令给物品套用。
- **渐变动画**：流动 / 呼吸 / 波浪 / 脉冲，支持颜色反转与动画逆向；也可以直接用纯色。
- **雾效** `fog_effects.json`、**疲劳渐变** `fatigue_gradient.json` 等纯观感项目单独成文件。

### Tooltip 系统

`/hdui tooltip` 打开可视化 Tooltip 编辑器：

- 逐行编辑、分段文本、文本修饰（颜色 / 粗斜体 / 渐变）
- **模板**（`tooltip_templates.json`）：配好一次，批量套给同类物品
- 可见性默认值：哪些行常显、哪些行按 Shift 才展开
- 可「导出为固定配置」给 JEI Commands
- 第三方兼容：AppleSkin、Obscure Tooltips、JEI、JEED、LSO

### TitlePro

一套独立的**屏幕文本引擎**，与食物无关、但同一个 mod 内提供：

- 屏幕标题、表格、颜色覆盖层三种呈现，全部有可视化编辑器与方案保存
- 可由物品「吃下时」触发，也可以用指令直接下发给玩家
- 相关指令：`/titlepro`、`/titleprosend`、`/titletable`、`/titleproclear`、`/titlescreen`

### 单个物品能配什么

`/hdui`（主手持物品）打开物品配置中心：列出通用配置与该物品的全部 **NBT 变体**，可以加载编辑、重写、
增加 / 删除 NBT 变体、从数据包导入变体、整物品禁用 / 启用。一个配置条目里可以配：

| 配置项 | 说明 |
|---|---|
| `nutrition` | 各营养组的供给值 |
| `hunger` | 覆盖饱食度 / 饱和度 |
| `effects` | 进食给予的药水效果（等级 / 时长 / 粒子 / 概率） |
| `onEatCommands` | 进食时执行的指令 |
| `onEatTitles` | 进食时弹出的 TitlePro 标题 |
| `conditionalCommands` | 「某营养组处于某区间」时才执行的指令 |
| `suppressEffects` | 进食时清除的药水效果 |
| `shelf` | 保质期参数（时长、过期结果、温区、冷保等） |
| `quality` | 品质等级 |
| `foodCooldown` / `foodFatigue` | 厌倦与厌食 |
| `stackSize` | 堆叠上限覆盖（配合 BiggerStacks） |
| `noContainerAfterEat` | 吃完不返还容器（碗 / 瓶） |

`/hdcloneitem` 可以把副手物品的整套 HD 配置克隆到主手物品上。

### 其他

- **方块右键交互**：数据包 `data/<命名空间>/healthy_diet/blocks/<方块>.json` —— 给任意方块配置右键给予的药水效果、
  执行的指令、营养值变化、标题提示；可按方块状态、空手 / 潜行 / 手持物品区分，带冷却与延迟。
- **瓶装填充** `bottle_fills.json`：手持某物品右键某液体源方块时换成什么。
- **容器 / 物品保护**：`/hdtrust` 管理自己的白名单。
- **清空成就弹窗**：`/cleartoast`。

### 指令一览

游戏内用 `/hdhelp` 查看**全部指令**（分组展示；指令名可点击填入输入框，悬停查看说明与参数）。

**需要 OP（权限等级 4）**

| 指令 | 作用 |
|---|---|
| `/hdui` | 主手持有物品时打开物品配置中心 |
| `/hdui tooltip` | 主手物品的 Tooltip 编辑器（有变体时先开主菜单按行打开） |
| `/hdui groups [玩家]` | 编辑玩家营养值 |
| `/hdui stage` | 阶段 / 联动加成管理界面 |
| `/hdui create` | 创建界面：新建 / 修改 / 删除 营养值、品质、品级 |
| `/hdset rarity <品级> [物品ID]` | 给物品设定 / 清除品级或原版稀有度 |
| `/hdcloneitem` | 把副手物品的 HD 配置克隆到主手 |
| `/hdreload` | 只重载本 mod 配置并重推在线玩家，**不触发原版数据包重载** |

**需要 OP（权限等级 2）**：`/titlepro`、`/titleprosend`、`/titletable`、`/titleproclear`、`/titlescreen`（TitlePro 系）。

**任意玩家**

| 指令 | 作用 |
|---|---|
| `/headiet` | 打开自己的营养面板（看他人需权限 2，增减 / 设定需 OP） |
| `/hdtemperature` | 对比 LSO / EC / HD 三条环境温度路径与实际来源 |
| `/cleartoast [玩家]` | 清空成就弹窗（清他人需权限 2） |
| `/hdtrust add\|remove\|list` | 容器 / 物品保护白名单（管理自己的名单） |
| `/hdhelp` | 显示全部指令 |

### 配置文件布局

```
config/healthy_diet/
├── server_gameplay.json        服务端玩法开关（服务端下发）
├── server_privacy.json         指令原文是否下发给所有玩家
├── shelf_merge.json            保质期合并阈值（服务端下发）
├── display.json                本机显示设定（纯观感，各读本地）
├── mixin_toggles.properties    兼容 mixin 开关（改完重启）
├── item_rarities.json          物品 → 品级 赋值
├── tooltip_templates.json      Tooltip 排版模板
├── bottle_fills.json           液体装填规则
├── fog_effects.json            效果联动雾效
├── datapack/                   HD 覆盖层数据包（物品 / Tooltip / 方块 / 营养组 / 阶段 / 加成）
├── rarity_and_colors/
│   ├── rarities.json           品级定义
│   ├── quality/<等级>.json     品质等级，一级一个文件
│   ├── solid_colors.json       纯色表
│   └── color_schemes.json      渐变方案
├── quality_registry/           品质物品注册表（启动时注册）
├── shelf_env/
│   ├── shelf_schemes/          保质期显示方案
│   ├── seasons.json            季节 → 保质期 / 温度倍率
│   ├── lso_compat.json         LSO 兼容参数
│   ├── fatigue_gradient.json   厌倦 / 厌食颜色渐变
│   └── fatigue_exempt.json     不参与厌倦恢复的物品
├── trust/                      保护白名单与过户历史
└── titlepro/                   TitlePro 方案与模板
```

**营养、物品、Tooltip、方块配置全部走数据包结构**，按内容自身的命名空间分目录：

```
datapack/data/healthy_diet/healthy_diet/nutrition_groups/<组>.json    营养组定义
datapack/data/healthy_diet/healthy_diet/nutrition_stages/<组>.json    阶段划分
datapack/data/healthy_diet/healthy_diet/nutrition_bonuses/<名>.json   联动加成
datapack/data/<命名空间>/healthy_diet/items/<物品>.json                物品配置（可含 NBT 变体）
datapack/data/<命名空间>/healthy_diet/tooltips/<物品>.json             Tooltip 配置
datapack/data/<命名空间>/healthy_diet/blocks/<方块>.json               方块右键配置
```

给某个 mod 的食物做的整套配置可以整体打包分发，不会和别的 mod 混在一起。
物品 / Tooltip 文件改名为 `.json.disabled` 即为禁用（可逆）。

`nutrition_bonuses` 的**文件名可以任取** —— 一条加成可以同时约束多个营养组，所以跨组联动通常单独成文件。
每条加成的属性修饰符 UUID 由「文件名 + 条目下标」生成：**同一条联动写在两个文件里，属性会叠加两次**。

> 编辑器保存即生效；手改文件后用 `/hdreload`。品质物品注册表、`mixin_toggles.properties`、新建 / 删除品级需要重启。
> 专服上 `rarities.json` 与 `color_schemes.json` 由各端读取自己本地的文件，改了要把文件同步到每个客户端。

### 兼容性

全部为**软兼容**：没装对应 mod 时相关代码不会被加载，功能自动降级。

| Mod | 用途 |
|---|---|
| **Jade** | 在掉落物 / 容器上显示保质期 |
| **Legendary Survival Overhaul (LSO)** | 完整世界环境温度（保质期首选温度源）、口渴数据 |
| **Ecliptic Seasons** | 季节气候，参与环境温度与保质期倍率 |
| **Refurbished Furniture** | 冰箱接入保质期制冷链路 |
| **Tom's Simple Storage** | 存储终端兼容：保质期刷新、物品所有权保护 |
| **JEI / JEED / JEI Commands** | 说明信息页联动；Tooltip 可导出给 JEI Commands |
| **AppleSkin** | 饥饿 / 饱和图标并入 HD 的饮食值行 |
| **BiggerStacks** | 堆叠上限覆盖 |
| **Obscure Tooltips** | Tooltip 显示兼容、品级渐变标签 |

### 服务器上的设计取向

这个 mod 默认**跑在多人服务器上**，所以：

- **玩法值一律以服务端为准**。营养、阶段、加成、保质期、品质、物品配置全部由服务端下发；
  客户端本地配置只影响观感（布局、是否按 Shift 展开这类）。
- **有协议版本握手**。网络包格式一变就提升协议版本（`common/network/HealthyDietNetwork.java` 的 `PTC_VERSION`），
  版本不符直接拒绝连接 —— 服务端和所有客户端必须使用同一版本的 jar。
- **权限分层下发**。例如阶段配置里的指令原文，非 OP 客户端收到的是裁剪过的版本。

### 许可

All Rights Reserved © XioYim

---

## English

Healthy Diet turns eating from a single hunger bar into a three-layer system — **nutrition structure · shelf life ·
presentation** — and **almost all of it is configurable in-game through GUIs**, no hand-written JSON needed.

### What problem it solves

Vanilla food has only two dimensions: how much hunger and saturation it restores. So the optimal play is always
"find one efficient food and eat nothing else". Healthy Diet changes that in three ways:

1. **Nutrition has structure** — food supplies **server-defined nutrition groups**; attribute and potion bonuses
   require several groups to stay in healthy ranges at once.
2. **Repetition has a cost** — short-term fatigue plus lifetime aversion: eating the same food over and over
   becomes less and less effective.
3. **Food spoils** — a full shelf-life system driven by ambient temperature, fridges and seasons.

On top of that sits a presentation layer (gradient names, quality, rarity, a visual tooltip editor and the TitlePro
title engine) so players on a server can actually see and understand all of this.

### Nutrition

- **Nutrition groups are defined by the server owner.** A fresh install creates 6 default groups: Dairy, Fruits,
  Grains, Protein (id `meat` — meat, fish and eggs all belong here), Sugar and Vegetables.
- The "Nutrition" tab of `/hdui create` can **create / edit / delete** groups: name, icon, color / gradient, decay rate,
  initial value and sort order. Online players receive the initial value of a new group **immediately**.
- One item can supply several groups at once.
- `/headiet` opens your own nutrition panel; OPs can add / subtract / set other players' values.

### Stages and linked bonuses

- Each group's 0–100 range can be split into any number of **stages**, each with its own name, hover description
  and **commands run on entry**.
- **Bonus entries** support multi-group conditions, e.g. "Protein ≥ 60 **and** Vegetables ≥ 40" grants
  `generic.max_health +4` plus Regeneration II.
  - Attribute bonus: `attribute` / `operation` / `value`
  - Potion bonus: `effect` / `level` / `duration` / `showParticles`
- `/hdui stage` opens the stage and bonus manager; `/hdui groups [player]` edits a player's nutrition values.

### Food fatigue / lifetime aversion

| Mechanic | Fields | Effect |
|---|---|---|
| Short-term fatigue `foodCooldown` | `maxUses` / `cooldownCount` / `cooldownMinutes` / `restoreHunger` | Eating the same food repeatedly in a short time lowers its restoration until you wait or switch foods |
| Lifetime aversion `foodFatigue` | `lossStart` / `aversionCount` | Permanently weakens a food based on how many times it has been eaten in this world |

Tooltips show fatigue %, remaining uses and the recovery countdown directly. `shelf_env/fatigue_exempt.json`
exempts specific items.

> ⚠ Setting `lossStart` without `aversionCount` is **valid but has no effect** (no decay, no aversion) — this is known
> and intentionally kept, not a bug.

### Shelf life (shelf2)

Food has its own clock and temperature and spoils with its environment:

- **Three-level ambient temperature fallback**: `LSO full world environment` → `Ecliptic Seasons climate + HD block
  offsets` → `HD built-in fallback`. `/hdtemperature` compares all three paths on the spot, shows which source is
  actually used and which nearby blocks contribute.
- **Fridges**: their own target temperature and rate of change, **power-loss** detection, and **cold retention** after
  leaving the fridge.
- **Temperature-zone effects**: extra effects while food is in a given temperature band (melting, sublimation and other
  schemes ship with the mod).
- **Expiry result**: expired food turns into a configured item instead of just disappearing.
- **Merging**: stacks merged through any path **always keep the shorter remaining time**, so fresh food can't
  "launder" spoiled food.
- **Dropped items keep ticking**; with Jade installed you can see the remaining shelf life on items on the ground.
- **Placed food** (cakes and every block with a `bites` property): once placed, shelf life continues on the world clock,
  each bite gets its share of the nutrition, expiry follows the item config, and a whole cake drops with its original
  data when broken.

### Quality · rarity · presentation

- **Quality**: levels 1–99. Edited on the "Quality" tab of `/hdui create`: overlay texture plus **segmented text**
  (the same segment editor as `/hdui tooltip`, each segment with its own text and gradient). The segments joined
  together form the quality name.
- **Quality item registry**: one item can be registered in several quality variants (separate items with their own
  creative tab). Registration only happens **at startup**; `/hdreload` pushes the registry files to online players and
  they take effect on the next launch.
- **Rarity**: the gradient name on the Obscure Tooltips rarity tag. Edited on the "Rarity" tab of `/hdui create`:
  editing an existing rarity applies instantly in singleplayer; **creating / deleting one requires restarting the server
  and clients**. `/hdset rarity <rarity|vanilla rarity|clear> [itemId]` assigns one to an item.
- **Gradient animations**: flow / breathing / wave / pulse, with color reversal and reversed playback; plain solid
  colors work too.
- Purely cosmetic settings such as **fog** (`fog_effects.json`) and **fatigue gradient** (`fatigue_gradient.json`) live
  in their own files.

### Tooltip system

`/hdui tooltip` opens the visual tooltip editor:

- Per-line editing, segmented text, text decoration (color / bold / italic / gradient)
- **Templates** (`tooltip_templates.json`): configure once, apply to similar items in bulk
- Visibility defaults: which lines are always shown and which expand with Shift
- "Export as fixed config" for JEI Commands
- Third-party compatibility: AppleSkin, Obscure Tooltips, JEI, JEED, LSO

### TitlePro

A standalone **on-screen text engine**, unrelated to food but shipped in the same mod:

- Screen titles, tables and color overlays, all with visual editors and saved schemes
- Can be triggered when an item is eaten, or sent to players directly by command
- Commands: `/titlepro`, `/titleprosend`, `/titletable`, `/titleproclear`, `/titlescreen`

### What a single item can configure

`/hdui` (with the item in your main hand) opens the item config hub: it lists the general entry and all **NBT variants**
of the item, and lets you load/edit, rewrite, add / delete NBT variants, import variants from datapacks, and disable /
enable the whole item. One config entry can set:

| Field | Description |
|---|---|
| `nutrition` | Amount supplied to each nutrition group |
| `hunger` | Hunger / saturation override |
| `effects` | Potion effects on eating (level / duration / particles / chance) |
| `onEatCommands` | Commands run on eating |
| `onEatTitles` | TitlePro titles shown on eating |
| `conditionalCommands` | Commands run only while a group is within a range |
| `suppressEffects` | Potion effects removed on eating |
| `shelf` | Shelf-life settings (duration, expiry result, temperature zones, cold retention…) |
| `quality` | Quality level |
| `foodCooldown` / `foodFatigue` | Fatigue and aversion |
| `stackSize` | Stack size override (with BiggerStacks) |
| `noContainerAfterEat` | Don't return the container (bowl / bottle) after eating |

`/hdcloneitem` copies the full HD config of your offhand item onto your main-hand item.

### Other features

- **Block right-click interactions**: datapack `data/<namespace>/healthy_diet/blocks/<block>.json` — potion effects,
  commands, nutrition changes and titles on right-clicking any block; can depend on block state, empty hand / sneaking /
  held item, with cooldowns and delays.
- **Bottle filling** `bottle_fills.json`: what an item turns into when right-clicking a given fluid source block.
- **Container / item protection**: manage your own whitelist with `/hdtrust`.
- **Clear advancement toasts**: `/cleartoast`.

### Commands

Use `/hdhelp` in game to see **every command** (grouped; click a command to paste it into chat, hover for descriptions
and arguments).

**Requires OP (permission level 4)**

| Command | Purpose |
|---|---|
| `/hdui` | Open the item config hub for your main-hand item |
| `/hdui tooltip` | Tooltip editor for your main-hand item (opens the hub first if it has variants) |
| `/hdui groups [player]` | Edit a player's nutrition values |
| `/hdui stage` | Stage / linked bonus manager |
| `/hdui create` | Create screen: create / edit / delete nutrition groups, quality levels and rarities |
| `/hdset rarity <rarity> [itemId]` | Set / clear an item's rarity or vanilla rarity |
| `/hdcloneitem` | Copy the offhand item's HD config onto the main-hand item |
| `/hdreload` | Reload only this mod's config and re-push it to online players (**does not trigger a vanilla datapack reload**) |

**Requires OP (permission level 2)**: `/titlepro`, `/titleprosend`, `/titletable`, `/titleproclear`, `/titlescreen` (TitlePro).

**Any player**

| Command | Purpose |
|---|---|
| `/headiet` | Open your nutrition panel (viewing others needs level 2, changing values needs OP) |
| `/hdtemperature` | Compare the LSO / EC / HD ambient temperature paths and the source actually used |
| `/cleartoast [player]` | Clear advancement toasts (others need level 2) |
| `/hdtrust add\|remove\|list` | Container / item protection whitelist (your own list) |
| `/hdhelp` | Show every command |

### Config layout

```
config/healthy_diet/
├── server_gameplay.json        Server gameplay switches (pushed by the server)
├── server_privacy.json         Whether command text is sent to all players
├── shelf_merge.json            Shelf-life merge thresholds (pushed by the server)
├── display.json                Local display settings (cosmetic only, each side reads its own)
├── mixin_toggles.properties    Compatibility mixin switches (restart after editing)
├── item_rarities.json          Item → rarity assignments
├── tooltip_templates.json      Tooltip layout templates
├── bottle_fills.json           Fluid filling rules
├── fog_effects.json            Effect-driven fog
├── datapack/                   HD overlay datapack (items / tooltips / blocks / groups / stages / bonuses)
├── rarity_and_colors/
│   ├── rarities.json           Rarity definitions
│   ├── quality/<level>.json    Quality levels, one file per level
│   ├── solid_colors.json       Solid color palette
│   └── color_schemes.json      Gradient schemes
├── quality_registry/           Quality item registry (registered at startup)
├── shelf_env/
│   ├── shelf_schemes/          Shelf-life display schemes
│   ├── seasons.json            Season → shelf-life / temperature multipliers
│   ├── lso_compat.json         LSO compatibility parameters
│   ├── fatigue_gradient.json   Fatigue / aversion color gradient
│   └── fatigue_exempt.json     Items that don't count toward fatigue recovery
├── trust/                      Protection whitelists and ownership history
└── titlepro/                   TitlePro schemes and templates
```

**Nutrition, item, tooltip and block configs all use datapack layout**, split by the content's own namespace:

```
datapack/data/healthy_diet/healthy_diet/nutrition_groups/<group>.json   Group definitions
datapack/data/healthy_diet/healthy_diet/nutrition_stages/<group>.json   Stage splits
datapack/data/healthy_diet/healthy_diet/nutrition_bonuses/<name>.json   Linked bonuses
datapack/data/<namespace>/healthy_diet/items/<item>.json                Item configs (may contain NBT variants)
datapack/data/<namespace>/healthy_diet/tooltips/<item>.json             Tooltip configs
datapack/data/<namespace>/healthy_diet/blocks/<block>.json              Block right-click configs
```

A full config set for one mod's food can be packaged and shared as a unit without mixing with other mods.
Renaming an item / tooltip file to `.json.disabled` disables it (reversibly).

**File names in `nutrition_bonuses` are free-form** — one bonus can constrain several groups, so cross-group links
usually get their own file. The attribute modifier UUID of each bonus is derived from "file name + entry index":
**writing the same link into two files stacks the attribute twice**.

> Editor saves apply immediately; after hand-editing files use `/hdreload`. The quality item registry,
> `mixin_toggles.properties` and creating / deleting rarities require a restart.
> On a dedicated server, `rarities.json` and `color_schemes.json` are read locally on each side — copy them to every
> client after changing them.

### Compatibility

All integrations are **soft dependencies**: without the other mod the related code is never loaded and the feature
degrades gracefully.

| Mod | Purpose |
|---|---|
| **Jade** | Shelf life on dropped items / containers |
| **Legendary Survival Overhaul (LSO)** | Full world ambient temperature (preferred shelf-life source), thirst data |
| **Ecliptic Seasons** | Seasonal climate for ambient temperature and shelf-life multipliers |
| **Refurbished Furniture** | Fridges join the shelf-life cooling chain |
| **Tom's Simple Storage** | Storage terminal compatibility: shelf-life refresh, item ownership protection |
| **JEI / JEED / JEI Commands** | Info page integration; tooltips can be exported to JEI Commands |
| **AppleSkin** | Hunger / saturation icons merged into HD's diet row |
| **BiggerStacks** | Stack size overrides |
| **Obscure Tooltips** | Tooltip compatibility and gradient rarity tags |

### Designed for servers

The mod assumes it **runs on a multiplayer server**, so:

- **Gameplay values are always server-authoritative.** Nutrition, stages, bonuses, shelf life, quality and item configs
  are pushed by the server; local client config only affects presentation (layout, Shift-to-expand and the like).
- **Protocol version handshake.** Any change to the network packet format bumps the protocol version
  (`PTC_VERSION` in `common/network/HealthyDietNetwork.java`); mismatched versions are refused at login — the server
  and every client must run the same jar version.
- **Permission-aware sync.** For example, command text inside stage configs is stripped before being sent to non-OP
  clients.

### License

All Rights Reserved © XioYim

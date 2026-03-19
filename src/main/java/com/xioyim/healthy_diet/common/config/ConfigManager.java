package com.xioyim.healthy_diet.common.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.xioyim.healthy_diet.HealthyDietConstants;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ConfigManager {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("healthy_diet");
    private static final Path GROUPS_FILE = CONFIG_DIR.resolve("groups.json");
    private static final Path ITEMS_DIR = CONFIG_DIR.resolve("items");
    private static final Path BONUSES_DIR = CONFIG_DIR.resolve("bonuses");
    private static final Path STAGES_DIR = CONFIG_DIR.resolve("stages");
    private static final Path BLOCKS_DIR = CONFIG_DIR.resolve("blocks");
    private static final Path DISPLAY_FILE = CONFIG_DIR.resolve("display.json");

    private static Map<String, GroupDefinition> groups = new LinkedHashMap<>();
    private static Map<String, ItemConfig> itemConfigs = new HashMap<>();
    private static Map<String, GroupBonusConfig> groupBonuses = new HashMap<>();
    private static Map<String, StageConfig> stageConfigs = new HashMap<>();
    private static Map<String, BlockConfig> blockConfigs = new HashMap<>();
    private static DisplayConfig displayConfig = new DisplayConfig();

    public static void loadAll() {
        try {
            Files.createDirectories(CONFIG_DIR);
            Files.createDirectories(ITEMS_DIR);
            Files.createDirectories(BONUSES_DIR);
            Files.createDirectories(STAGES_DIR);
            Files.createDirectories(BLOCKS_DIR);
        } catch (IOException e) {
            HealthyDietConstants.LOG.error("Failed to create config directories", e);
        }
        loadGroups();
        loadItemConfigs();
        loadGroupBonuses();
        loadStageConfigs();
        loadBlockConfigs();
        loadDisplayConfig();
        HealthyDietConstants.LOG.info("[HealthyDiet] Loaded {} groups, {} items, {} bonuses, {} stages, {} blocks",
                groups.size(), itemConfigs.size(), groupBonuses.size(), stageConfigs.size(), blockConfigs.size());
    }

    private static void loadGroups() {
        if (!Files.exists(GROUPS_FILE)) { createDefaultGroups(); return; }
        try (Reader r = reader(GROUPS_FILE)) {
            Type type = new TypeToken<Map<String, GroupDefinition>>() {}.getType();
            Map<String, GroupDefinition> loaded = GSON.fromJson(r, type);
            if (loaded != null) {
                groups = new LinkedHashMap<>();
                for (Map.Entry<String, GroupDefinition> e : loaded.entrySet()) {
                    e.getValue().id = e.getKey();
                    groups.put(e.getKey(), e.getValue());
                }
            }
        } catch (IOException e) { HealthyDietConstants.LOG.error("Failed to load groups", e); }
    }

    private static void createDefaultGroups() {
        groups = new LinkedHashMap<>();
        addDefaultGroup("vegetables", "Vegetables", "#55FF55", 6.0, "minecraft:carrot");
        addDefaultGroup("fruits",     "Fruits",     "#FFAA00", 6.0, "minecraft:apple");
        addDefaultGroup("seafood",    "Seafood",    "#00AAFF", 6.0, "minecraft:cod");
        addDefaultGroup("meat",       "Meat",       "#FF5555", 6.0, "minecraft:beef");
        addDefaultGroup("sugar",      "Sugar",      "#FF55FF", 6.0, "minecraft:sugar");
        addDefaultGroup("dairy",      "Dairy",      "#FFFFFF", 6.0, "minecraft:milk_bucket");
        addDefaultGroup("grains",     "Grains",     "#FFFF55", 6.0, "minecraft:wheat");
        saveGroups();
    }

    private static void addDefaultGroup(String id, String name, String color, double decay, String icon) {
        GroupDefinition def = new GroupDefinition();
        def.id = id; def.displayName = name; def.color = color; def.decayRate = decay; def.icon = icon;
        groups.put(id, def);
    }

    public static void saveGroups() {
        try (Writer w = writer(GROUPS_FILE)) { GSON.toJson(groups, w); }
        catch (IOException e) { HealthyDietConstants.LOG.error("Failed to save groups", e); }
    }

    private static void loadItemConfigs() {
        itemConfigs.clear();
        if (!Files.exists(ITEMS_DIR)) return;
        try {
            Files.walk(ITEMS_DIR).filter(p -> p.toString().endsWith(".json")).forEach(ConfigManager::loadItemConfigFile);
        } catch (IOException e) { HealthyDietConstants.LOG.error("Failed to scan items dir", e); }
    }

    private static void loadItemConfigFile(Path path) {
        Path rel = ITEMS_DIR.relativize(path);
        String relStr = rel.toString().replace('\\', '/');
        if (!relStr.endsWith(".json")) return;
        int slash = relStr.indexOf('/');
        if (slash == -1) return;
        String ns = relStr.substring(0, slash);
        String p = relStr.substring(slash + 1, relStr.length() - 5);
        String itemId = ns + ":" + p;
        try (Reader r = reader(path)) {
            ItemConfig config = GSON.fromJson(r, ItemConfig.class);
            if (config != null) itemConfigs.put(itemId, config);
        } catch (IOException e) { HealthyDietConstants.LOG.error("Failed to load item config: {}", path, e); }
    }

    public static void saveItemConfig(String itemId, ItemConfig config) {
        int c = itemId.indexOf(':');
        if (c == -1) return;
        Path dir = ITEMS_DIR.resolve(itemId.substring(0, c));
        Path file = dir.resolve(itemId.substring(c + 1) + ".json");
        try {
            Files.createDirectories(dir);
            try (Writer w = writer(file)) { GSON.toJson(config, w); }
            itemConfigs.put(itemId, config);
        } catch (IOException e) { HealthyDietConstants.LOG.error("Failed to save item config: {}", itemId, e); }
    }

    private static void loadGroupBonuses() {
        groupBonuses.clear();
        if (!Files.exists(BONUSES_DIR)) return;
        try {
            boolean anyJson = Files.walk(BONUSES_DIR).anyMatch(p -> p.toString().endsWith(".json"));
            if (!anyJson) createExampleBonusConfig();
            Files.walk(BONUSES_DIR).filter(p -> p.toString().endsWith(".json")).forEach(path -> {
                String name = path.getFileName().toString();
                String groupId = name.substring(0, name.length() - 5);
                try (Reader r = reader(path)) {
                    GroupBonusConfig config = GSON.fromJson(r, GroupBonusConfig.class);
                    if (config != null) groupBonuses.put(groupId, config);
                } catch (IOException e) { HealthyDietConstants.LOG.error("Failed to load bonus config: {}", path, e); }
            });
        } catch (IOException e) { HealthyDietConstants.LOG.error("Failed to scan bonuses dir", e); }
    }

    /**
     * 首次运行时在 bonuses/ 目录生成一个多组联动示例文件。
     * 该文件不会被 mod 加载为实际配置（groupId 为 "example_link"），
     * 玩家可参照此文件自行创建真实配置。
     */
    private static void createExampleBonusConfig() {
        // --- 案例1：水果+蔬菜 同时在 20%~40% 才激活最大生命值+4 ---
        GroupBonusConfig example = new GroupBonusConfig();

        GroupBonusConfig.BonusEntry entry1 = new GroupBonusConfig.BonusEntry();
        GroupBonusConfig.RangeCondition fruits20_40 = new GroupBonusConfig.RangeCondition();
        fruits20_40.min = 20f; fruits20_40.max = 40f;
        GroupBonusConfig.RangeCondition veg20_40 = new GroupBonusConfig.RangeCondition();
        veg20_40.min = 20f; veg20_40.max = 40f;
        entry1.conditions.put("fruits", fruits20_40);
        entry1.conditions.put("vegetables", veg20_40);
        GroupBonusConfig.AttributeBonus health4 = new GroupBonusConfig.AttributeBonus();
        health4.attribute = "minecraft:generic.max_health";
        health4.operation = "add";
        health4.value = 4.0;
        entry1.attributeBonuses.add(health4);

        // --- 案例2：肉类 10%~30% 且 谷物 20%~30% 才激活移速+0.05 ---
        GroupBonusConfig.BonusEntry entry2 = new GroupBonusConfig.BonusEntry();
        GroupBonusConfig.RangeCondition meat10_30 = new GroupBonusConfig.RangeCondition();
        meat10_30.min = 10f; meat10_30.max = 30f;
        GroupBonusConfig.RangeCondition grain20_30 = new GroupBonusConfig.RangeCondition();
        grain20_30.min = 20f; grain20_30.max = 30f;
        entry2.conditions.put("meat", meat10_30);
        entry2.conditions.put("grains", grain20_30);
        GroupBonusConfig.AttributeBonus speed = new GroupBonusConfig.AttributeBonus();
        speed.attribute = "minecraft:generic.movement_speed";
        speed.operation = "add";
        speed.value = 0.05;
        entry2.attributeBonuses.add(speed);

        // --- 案例3：单组触发药水效果（对比参考） ---
        GroupBonusConfig.BonusEntry entry3 = new GroupBonusConfig.BonusEntry();
        GroupBonusConfig.RangeCondition dairy50_100 = new GroupBonusConfig.RangeCondition();
        dairy50_100.min = 50f; dairy50_100.max = 100f;
        entry3.conditions.put("dairy", dairy50_100);
        GroupBonusConfig.EffectBonus regen = new GroupBonusConfig.EffectBonus();
        regen.effect = "minecraft:regeneration";
        regen.level = 0;
        regen.duration = 200;
        regen.showParticles = false;
        entry3.effectBonuses.add(regen);

        example.bonuses.add(entry1);
        example.bonuses.add(entry2);
        example.bonuses.add(entry3);

        Path exampleFile = BONUSES_DIR.resolve("example_link.json");
        try {
            Files.createDirectories(BONUSES_DIR);
            try (Writer w = writer(exampleFile)) { GSON.toJson(example, w); }
            HealthyDietConstants.LOG.info("[HealthyDiet] Created example multi-group bonus file: {}", exampleFile);
        } catch (IOException e) { HealthyDietConstants.LOG.error("Failed to write example bonus config", e); }
    }

    public static void saveStageConfig(String groupId, StageConfig config) {
        Path file = STAGES_DIR.resolve(groupId + ".json");
        try {
            Files.createDirectories(STAGES_DIR);
            try (Writer w = writer(file)) { GSON.toJson(config, w); }
            stageConfigs.put(groupId, config);
        } catch (IOException e) { HealthyDietConstants.LOG.error("Failed to save stage config: {}", groupId, e); }
    }

    public static void saveGroupBonusConfig(String groupId, GroupBonusConfig config) {
        Path file = BONUSES_DIR.resolve(groupId + ".json");
        try {
            Files.createDirectories(BONUSES_DIR);
            try (Writer w = writer(file)) { GSON.toJson(config, w); }
            groupBonuses.put(groupId, config);
        } catch (IOException e) { HealthyDietConstants.LOG.error("Failed to save bonus config: {}", groupId, e); }
    }

    private static void loadStageConfigs() {
        stageConfigs.clear();
        if (!Files.exists(STAGES_DIR)) return;
        try {
            Files.walk(STAGES_DIR).filter(p -> p.toString().endsWith(".json")).forEach(path -> {
                String name = path.getFileName().toString();
                String groupId = name.substring(0, name.length() - 5);
                try (Reader r = reader(path)) {
                    StageConfig config = GSON.fromJson(r, StageConfig.class);
                    if (config != null) stageConfigs.put(groupId, config);
                } catch (IOException e) { HealthyDietConstants.LOG.error("Failed to load stage config: {}", path, e); }
            });
        } catch (IOException e) { HealthyDietConstants.LOG.error("Failed to scan stages dir", e); }
    }

    private static void loadBlockConfigs() {
        blockConfigs.clear();
        if (!Files.exists(BLOCKS_DIR)) return;
        try {
            Files.walk(BLOCKS_DIR).filter(p -> p.toString().endsWith(".json")).forEach(path -> {
                Path rel = BLOCKS_DIR.relativize(path);
                String relStr = rel.toString().replace('\\', '/');
                if (!relStr.endsWith(".json")) return;
                int slash = relStr.indexOf('/');
                if (slash == -1) return;
                String ns = relStr.substring(0, slash);
                String p  = relStr.substring(slash + 1, relStr.length() - 5);
                String blockId = ns + ":" + p;
                try (Reader r = reader(path)) {
                    BlockConfig config = GSON.fromJson(r, BlockConfig.class);
                    if (config != null) blockConfigs.put(blockId, config);
                } catch (IOException e) { HealthyDietConstants.LOG.error("Failed to load block config: {}", path, e); }
            });
        } catch (IOException e) { HealthyDietConstants.LOG.error("Failed to scan blocks dir", e); }
    }

    public static void saveBlockConfig(String blockId, BlockConfig config) {
        int c = blockId.indexOf(':');
        if (c == -1) return;
        Path dir  = BLOCKS_DIR.resolve(blockId.substring(0, c));
        Path file = dir.resolve(blockId.substring(c + 1) + ".json");
        try {
            Files.createDirectories(dir);
            try (Writer w = writer(file)) { GSON.toJson(config, w); }
            blockConfigs.put(blockId, config);
        } catch (IOException e) { HealthyDietConstants.LOG.error("Failed to save block config: {}", blockId, e); }
    }

    private static void loadDisplayConfig() {
        if (Files.exists(DISPLAY_FILE)) {
            try (Reader r = reader(DISPLAY_FILE)) {
                DisplayConfig loaded = GSON.fromJson(r, DisplayConfig.class);
                if (loaded != null) displayConfig = loaded;
            } catch (IOException e) { HealthyDietConstants.LOG.error("Failed to load display config", e); }
        }
        // 每次加载后都回写，确保新增字段出现在文件中（旧字段值保持不变）
        try (Writer w = writer(DISPLAY_FILE)) { GSON.toJson(displayConfig, w); }
        catch (IOException e) { HealthyDietConstants.LOG.error("Failed to write display config", e); }
    }

    private static Reader reader(Path p) throws IOException {
        return new InputStreamReader(new FileInputStream(p.toFile()), StandardCharsets.UTF_8);
    }
    private static Writer writer(Path p) throws IOException {
        return new OutputStreamWriter(new FileOutputStream(p.toFile()), StandardCharsets.UTF_8);
    }

    // ---- Getters ----
    public static Map<String, GroupDefinition> getGroups() { return Collections.unmodifiableMap(groups); }
    public static GroupDefinition getGroup(String id) { return groups.get(id); }
    public static ItemConfig getItemConfig(String itemId) { return itemConfigs.get(itemId); }
    public static Set<String> getItemConfigKeys() { return Collections.unmodifiableSet(itemConfigs.keySet()); }
    public static GroupBonusConfig getGroupBonusConfig(String groupId) { return groupBonuses.get(groupId); }
    public static Map<String, GroupBonusConfig> getAllGroupBonuses() { return Collections.unmodifiableMap(groupBonuses); }
    public static StageConfig getStageConfig(String groupId) { return stageConfigs.get(groupId); }
    public static Map<String, StageConfig> getAllStageConfigs() { return Collections.unmodifiableMap(stageConfigs); }
    public static BlockConfig getBlockConfig(String blockId) { return blockConfigs.get(blockId); }
    public static Set<String> getBlockConfigKeys() { return Collections.unmodifiableSet(blockConfigs.keySet()); }
    public static DisplayConfig getDisplayConfig() { return displayConfig; }
    public static Path getConfigDir() { return CONFIG_DIR; }
}

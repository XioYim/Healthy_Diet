package com.xioyim.healthy_diet.common.config;

import java.util.ArrayList;
import java.util.List;

/**
 * 方块右键交互配置。
 * 存储于 config/healthy_diet/blocks/<namespace>/<path>.json
 */
public class BlockConfig {
    /** 右键时给玩家施加的药水效果 */
    public List<ItemConfig.EffectEntry> effects = new ArrayList<>();
    /** 右键时以玩家身份（OP 权限）执行的指令 */
    public List<String> onClickCommands = new ArrayList<>();
    /** 右键时对玩家营养值的变化（正数增加，负数减少） */
    public List<NutritionChange> nutritionChanges = new ArrayList<>();

    public static class NutritionChange {
        /** 营养组 ID */
        public String group = "";
        /** 变化量，正数为增加，负数为减少 */
        public float value = 0f;
    }
}

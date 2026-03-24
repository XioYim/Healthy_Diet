package com.xioyim.healthy_diet.common.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemConfig {
    public Map<String, Float> nutrition = new HashMap<>();
    public List<EffectEntry> effects = new ArrayList<>();
    public FoodCooldownConfig foodCooldown = null;
    /** Commands executed (as the player at OP level) when eating this item */
    public List<String> onEatCommands = new ArrayList<>();

    /**
     * 条件指令：仅当玩家对应营养组的值在 [min, max] 范围内时才执行。
     * Conditional commands: executed only if the player's nutrition for 'group' is within [min, max].
     */
    public List<ConditionalCommand> conditionalCommands = new ArrayList<>();

    public static class ConditionalCommand {
        /** 检测的营养组 ID / Nutrition group ID to check */
        public String group = "";
        /** 触发范围下限（含），0~100 / Lower bound (inclusive), 0~100 */
        public float min = 0f;
        /** 触发范围上限（含），0~100 / Upper bound (inclusive), 0~100 */
        public float max = 100f;
        /** 满足条件时执行的命令（以玩家身份，OP 权限）/ Command to run when condition is met */
        public String command = "";
    }

    public static class EffectEntry {
        public String effect = "";
        /** Amplifier: 0 = Level 1, 255 = Level 256 */
        public int level = 0;
        /** Duration in TICKS */
        public int duration = 100;
        public boolean showParticles = false;
        /** Trigger probability 0-100 (%). Default 100 = always triggers. */
        public int probability = 100;
    }

    public static class FoodCooldownConfig {
        public int maxUses = 5;
        public int cooldownCount = 3;
        /** Cooldown duration in REAL MINUTES. -1 = no time limit. 1 minute = 1200 ticks */
        public float cooldownMinutes = -1f;
        /** Hunger points to restore even while on cooldown. 0 = no restore (full block). */
        public int restoreHunger = 0;
    }
}

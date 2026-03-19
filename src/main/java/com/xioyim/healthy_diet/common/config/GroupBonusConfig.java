package com.xioyim.healthy_diet.common.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupBonusConfig {
    public List<BonusEntry> bonuses = new ArrayList<>();

    public static class BonusEntry {
        public Map<String, RangeCondition> conditions = new HashMap<>();
        public List<AttributeBonus> attributeBonuses = new ArrayList<>();
        public List<EffectBonus> effectBonuses = new ArrayList<>();
    }

    public static class RangeCondition {
        public float min = 0f;
        public float max = 100f;
    }

    public static class AttributeBonus {
        public String attribute = "minecraft:generic.max_health";
        public String operation = "add";
        public double value = 0.0;
    }

    public static class EffectBonus {
        public String effect = "";
        public int level = 0;
        public int duration = 200;
        public boolean showParticles = false;
    }
}

package com.xioyim.healthy_diet.client;

import com.xioyim.healthy_diet.common.network.packet.SPacketNutrition;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ClientNutritionData {
    private static Map<String, Float> values = new HashMap<>();
    private static Map<String, SPacketNutrition.CooldownState> cooldownStates = new HashMap<>();

    public static void update(Map<String, Float> newValues, Map<String, SPacketNutrition.CooldownState> newStates) {
        values = new HashMap<>(newValues);
        cooldownStates = new HashMap<>(newStates);
    }

    public static float getValue(String group) {
        return values.getOrDefault(group, 0f);
    }

    public static boolean isOnCooldown(String itemId) {
        SPacketNutrition.CooldownState state = cooldownStates.get(itemId);
        return state != null && state.onCooldown();
    }

    /** 返回食物的厌倦进度数据，无记录时返回 null */
    public static SPacketNutrition.CooldownState getCooldownState(String itemId) {
        return cooldownStates.get(itemId);
    }

    public static Map<String, Float> getAllValues() {
        return Collections.unmodifiableMap(values);
    }
}

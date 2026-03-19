package com.xioyim.healthy_diet.common.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.util.Map;

public interface INutritionTracker {
    float getValue(String group);
    void setValue(String group, float value);
    void addValue(String group, float amount);
    void subtractValue(String group, float amount);
    void resetValue(String group);
    void pauseGroup(String group);
    void resumeGroup(String group);
    boolean isPaused(String group);
    Map<String, Float> getAllValues();
    Map<String, Boolean> getCooldownStatuses();

    boolean processEating(String itemId);
    void onOtherFoodEaten(String currentItemId);
    boolean isOnCooldown(String itemId);

    /** Clears all nutrition values, food cooldowns, paused groups, and attribute modifiers. */
    void clearAll();

    void tick();
    void sync();
    void save(CompoundTag tag);
    void load(CompoundTag tag);
    void copy(Player original);
}

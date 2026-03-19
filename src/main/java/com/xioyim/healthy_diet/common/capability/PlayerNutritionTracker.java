package com.xioyim.healthy_diet.common.capability;

import com.xioyim.healthy_diet.HealthyDietConstants;
import com.xioyim.healthy_diet.common.config.ConfigManager;
import com.xioyim.healthy_diet.common.config.GroupBonusConfig;
import com.xioyim.healthy_diet.common.config.GroupDefinition;
import com.xioyim.healthy_diet.common.config.ItemConfig;
import com.xioyim.healthy_diet.common.network.HealthyDietNetwork;
import com.xioyim.healthy_diet.common.network.packet.SPacketNutrition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class PlayerNutritionTracker implements INutritionTracker {

    private final Player player;
    private final Map<String, Float> values = new LinkedHashMap<>();
    private final Set<String> paused = new HashSet<>();
    private final Map<String, Float> decayAccumulators = new HashMap<>();
    private final Map<String, FoodCooldownState> foodCooldowns = new HashMap<>();
    private int prevFoodLevel = 20;
    private boolean dirty = false;

    private int bonusTickCounter = 0;
    private int effectTickCounter = 0;

    private final Map<UUID, String> modifierToAttribute = new HashMap<>();
    private final Set<UUID> activeAttributeModifiers = new HashSet<>();
    private final List<GroupBonusConfig.EffectBonus> activeEffectBonusList = new ArrayList<>();

    public PlayerNutritionTracker(Player player) {
        this.player = player;
        for (String groupId : ConfigManager.getGroups().keySet()) values.put(groupId, 0.0f);
    }

    @Override public float getValue(String group) { return values.getOrDefault(group, 0.0f); }

    @Override
    public void setValue(String group, float value) {
        if (!ConfigManager.getGroups().containsKey(group)) return;
        values.put(group, Math.max(0f, Math.min(100f, value)));
        dirty = true;
    }

    @Override public void addValue(String group, float amount) { if (!isPaused(group)) setValue(group, getValue(group) + amount); }
    @Override public void subtractValue(String group, float amount) { if (!isPaused(group)) setValue(group, getValue(group) - amount); }
    @Override public void resetValue(String group) { setValue(group, 0f); }
    @Override public void pauseGroup(String group) { paused.add(group); dirty = true; }
    @Override public void resumeGroup(String group) { paused.remove(group); dirty = true; }
    @Override public boolean isPaused(String group) { return paused.contains(group); }
    @Override public Map<String, Float> getAllValues() { return Collections.unmodifiableMap(values); }

    @Override
    public Map<String, Boolean> getCooldownStatuses() {
        Map<String, Boolean> r = new HashMap<>();
        foodCooldowns.forEach((k, v) -> r.put(k, v.onCooldown));
        return r;
    }

    @Override
    public void clearAll() {
        // 仅将所有营养值归零
        values.replaceAll((k, v) -> 0f);
        dirty = true;
    }

    @Override
    public boolean processEating(String itemId) {
        FoodCooldownState state = foodCooldowns.get(itemId);
        if (state != null && state.onCooldown) return false;
        ItemConfig config = ConfigManager.getItemConfig(itemId);
        if (config != null && config.foodCooldown != null) {
            if (state == null) { state = new FoodCooldownState(); foodCooldowns.put(itemId, state); }
            state.eatenCount++;
            dirty = true; // 每次食用都同步，更新厌倦度显示
            if (state.eatenCount >= config.foodCooldown.maxUses) {
                state.onCooldown = true;
                state.otherFoodsEaten = 0;
                state.cooldownStartTick = player.level().getGameTime();
                if (player instanceof ServerPlayer sp) {
                    sp.displayClientMessage(Component.translatable("message.healthy_diet.food_on_cooldown"), true);
                }
            }
        }
        return true;
    }

    @Override
    public void onOtherFoodEaten(String currentItemId) {
        List<String> toReset = new ArrayList<>();
        for (Map.Entry<String, FoodCooldownState> e : foodCooldowns.entrySet()) {
            if (e.getKey().equals(currentItemId)) continue;
            FoodCooldownState state = e.getValue();
            if (!state.onCooldown) continue;
            ItemConfig config = ConfigManager.getItemConfig(e.getKey());
            if (config == null || config.foodCooldown == null) continue;
            state.otherFoodsEaten++;
            dirty = true; // 每次恢复进度变化都同步，更新恢复度显示
            if (state.otherFoodsEaten >= config.foodCooldown.cooldownCount) toReset.add(e.getKey());
        }
        toReset.forEach(k -> foodCooldowns.get(k).resetCooldown());
    }

    @Override public boolean isOnCooldown(String itemId) { FoodCooldownState s = foodCooldowns.get(itemId); return s != null && s.onCooldown; }

    @Override
    public void tick() {
        if (player.level().isClientSide()) return;

        // Decay
        int curFood = player.getFoodData().getFoodLevel();
        int delta = prevFoodLevel - curFood;
        if (delta > 0) {
            for (Map.Entry<String, GroupDefinition> e : ConfigManager.getGroups().entrySet()) {
                String gid = e.getKey();
                GroupDefinition def = e.getValue();
                if (def.decayRate <= 0 || isPaused(gid)) continue;
                float acc = decayAccumulators.getOrDefault(gid, 0f) + delta;
                while (acc >= def.decayRate) {
                    float cur = values.getOrDefault(gid, 0f);
                    if (cur > 0) { values.put(gid, Math.max(0f, cur - 1f)); dirty = true; }
                    acc -= (float) def.decayRate;
                }
                decayAccumulators.put(gid, acc);
            }
        }
        prevFoodLevel = curFood;

        // Cooldown time (real minutes)
        boolean cdChanged = false;
        for (Map.Entry<String, FoodCooldownState> e : foodCooldowns.entrySet()) {
            FoodCooldownState state = e.getValue();
            if (!state.onCooldown) continue;
            ItemConfig config = ConfigManager.getItemConfig(e.getKey());
            if (config == null || config.foodCooldown == null || config.foodCooldown.cooldownMinutes <= 0) continue;
            long elapsed = player.level().getGameTime() - state.cooldownStartTick;
            long cooldownTicks = (long)(config.foodCooldown.cooldownMinutes * 1200L);
            if (elapsed >= cooldownTicks) { state.resetCooldown(); cdChanged = true; }
        }
        if (cdChanged) dirty = true;

        bonusTickCounter++;
        if (bonusTickCounter >= 20) { bonusTickCounter = 0; evaluateBonuses(); }

        effectTickCounter++;
        if (effectTickCounter >= 200) { effectTickCounter = 0; applyEffectBonuses(); }

        if (dirty) { sync(); dirty = false; }
    }

    private void evaluateBonuses() {
        new HashSet<>(activeAttributeModifiers).forEach(this::removeAttributeModifier);
        activeAttributeModifiers.clear();
        activeEffectBonusList.clear();

        for (Map.Entry<String, GroupBonusConfig> e : ConfigManager.getAllGroupBonuses().entrySet()) {
            GroupBonusConfig bc = e.getValue();
            if (bc == null || bc.bonuses == null) continue;
            for (int i = 0; i < bc.bonuses.size(); i++) {
                GroupBonusConfig.BonusEntry bonus = bc.bonuses.get(i);
                if (!checkConditions(bonus.conditions)) continue;
                for (int j = 0; j < bonus.attributeBonuses.size(); j++) {
                    GroupBonusConfig.AttributeBonus ab = bonus.attributeBonuses.get(j);
                    String key = "healthy_diet:" + e.getKey() + "_" + i + "_attr_" + j;
                    UUID uuid = UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
                    applyAttributeModifier(uuid, key, ab);
                    activeAttributeModifiers.add(uuid);
                }
                activeEffectBonusList.addAll(bonus.effectBonuses);
            }
        }
    }

    private boolean checkConditions(Map<String, GroupBonusConfig.RangeCondition> conditions) {
        for (Map.Entry<String, GroupBonusConfig.RangeCondition> c : conditions.entrySet()) {
            float v = values.getOrDefault(c.getKey(), 0f);
            if (v < c.getValue().min || v > c.getValue().max) return false;
        }
        return true;
    }

    private void applyAttributeModifier(UUID uuid, String name, GroupBonusConfig.AttributeBonus bonus) {
        try {
            Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(ResourceLocation.tryParse(bonus.attribute));
            if (attr == null) return;
            AttributeInstance inst = player.getAttribute(attr);
            if (inst == null || inst.getModifier(uuid) != null) return;
            AttributeModifier.Operation op = switch (bonus.operation.toLowerCase()) {
                case "multiply_base" -> AttributeModifier.Operation.MULTIPLY_BASE;
                case "multiply_total" -> AttributeModifier.Operation.MULTIPLY_TOTAL;
                default -> AttributeModifier.Operation.ADDITION;
            };
            inst.addTransientModifier(new AttributeModifier(uuid, name, bonus.value, op));
            modifierToAttribute.put(uuid, bonus.attribute);
        } catch (Exception e) { HealthyDietConstants.LOG.warn("Failed to apply modifier for {}", bonus.attribute); }
    }

    private void removeAttributeModifier(UUID uuid) {
        String attrId = modifierToAttribute.remove(uuid);
        if (attrId == null) return;
        try {
            Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(ResourceLocation.tryParse(attrId));
            if (attr == null) return;
            AttributeInstance inst = player.getAttribute(attr);
            if (inst != null) inst.removeModifier(uuid);
        } catch (Exception ignored) {}
    }

    private void applyEffectBonuses() {
        for (GroupBonusConfig.EffectBonus eb : activeEffectBonusList) {
            try {
                MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.tryParse(eb.effect));
                if (effect != null) player.addEffect(new MobEffectInstance(effect, eb.duration, eb.level, false, eb.showParticles));
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void sync() {
        if (player instanceof ServerPlayer sp) {
            Map<String, SPacketNutrition.CooldownState> states = new HashMap<>();
            foodCooldowns.forEach((k, v) -> states.put(k, new SPacketNutrition.CooldownState(
                    v.eatenCount, v.onCooldown, v.otherFoodsEaten, v.cooldownStartTick)));
            HealthyDietNetwork.sendToPlayer(sp, new SPacketNutrition(new HashMap<>(values), states));
        }
    }

    @Override
    public void save(CompoundTag tag) {
        CompoundTag vt = new CompoundTag();
        values.forEach(vt::putFloat);
        tag.put("values", vt);

        ListTag pt = new ListTag();
        paused.forEach(g -> pt.add(StringTag.valueOf(g)));
        tag.put("paused", pt);

        CompoundTag dt = new CompoundTag();
        decayAccumulators.forEach(dt::putFloat);
        tag.put("decay", dt);

        CompoundTag ct = new CompoundTag();
        foodCooldowns.forEach((k, s) -> {
            CompoundTag st = new CompoundTag();
            st.putInt("eatenCount", s.eatenCount);
            st.putBoolean("onCooldown", s.onCooldown);
            st.putInt("otherFoodsEaten", s.otherFoodsEaten);
            st.putLong("cooldownStartTick", s.cooldownStartTick);
            ct.put(k, st);
        });
        tag.put("foodCooldowns", ct);
        tag.putInt("prevFoodLevel", prevFoodLevel);
    }

    @Override
    public void load(CompoundTag tag) {
        if (tag.contains("values")) {
            CompoundTag vt = tag.getCompound("values");
            vt.getAllKeys().forEach(k -> values.put(k, vt.getFloat(k)));
        }
        ConfigManager.getGroups().keySet().forEach(g -> values.putIfAbsent(g, 0f));

        if (tag.contains("paused", Tag.TAG_LIST)) {
            ListTag pt = tag.getList("paused", Tag.TAG_STRING);
            paused.clear();
            for (int i = 0; i < pt.size(); i++) paused.add(pt.getString(i));
        }
        if (tag.contains("decay")) {
            CompoundTag dt = tag.getCompound("decay");
            dt.getAllKeys().forEach(k -> decayAccumulators.put(k, dt.getFloat(k)));
        }
        if (tag.contains("foodCooldowns")) {
            CompoundTag ct = tag.getCompound("foodCooldowns");
            ct.getAllKeys().forEach(k -> {
                CompoundTag st = ct.getCompound(k);
                FoodCooldownState s = new FoodCooldownState();
                s.eatenCount = st.getInt("eatenCount");
                s.onCooldown = st.getBoolean("onCooldown");
                s.otherFoodsEaten = st.getInt("otherFoodsEaten");
                s.cooldownStartTick = st.getLong("cooldownStartTick");
                foodCooldowns.put(k, s);
            });
        }
        if (tag.contains("prevFoodLevel")) prevFoodLevel = tag.getInt("prevFoodLevel");
    }

    @Override
    public void copy(Player original) {
        NutritionCapability.get(original).ifPresent(t -> { CompoundTag tag = new CompoundTag(); t.save(tag); load(tag); });
    }

    public static class FoodCooldownState {
        public int eatenCount = 0;
        public boolean onCooldown = false;
        public int otherFoodsEaten = 0;
        public long cooldownStartTick = 0L;
        public void resetCooldown() { onCooldown = false; otherFoodsEaten = 0; cooldownStartTick = 0L; eatenCount = 0; }
    }
}

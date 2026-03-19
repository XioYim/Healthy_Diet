package com.xioyim.healthy_diet.common.capability;

import com.xioyim.healthy_diet.HealthyDietConstants;
import com.xioyim.healthy_diet.common.config.ConfigManager;
import com.xioyim.healthy_diet.common.config.ItemConfig;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = HealthyDietConstants.MOD_ID)
public class NutritionCapabilityEvents {

    /**
     * 记录食物即将被吃掉之前的 [饥饿值, 饱和度(bits)] 快照。
     * Tick 事件 duration==0 时触发（此时 finishUsingItem 尚未执行），
     * 厌倦状态下在 Finish 事件中用此快照精确还原。
     */
    private static final Map<UUID, int[]> PRE_EAT_STATE = new HashMap<>();

    @SubscribeEvent
    public static void attachCapabilities(final AttachCapabilitiesEvent<Entity> evt) {
        if (evt.getObject() instanceof Player player) {
            final PlayerNutritionTracker tracker = new PlayerNutritionTracker(player);
            final LazyOptional<INutritionTracker> cap = LazyOptional.of(() -> tracker);
            evt.addCapability(NutritionCapability.NUTRITION_TRACKER_ID, new Provider(tracker, cap));
        }
    }

    @SubscribeEvent
    public static void playerClone(final PlayerEvent.Clone evt) {
        if (evt.getEntity() instanceof ServerPlayer player) {
            Player original = evt.getOriginal();
            original.reviveCaps();
            NutritionCapability.get(player).ifPresent(t -> t.copy(original));
            original.invalidateCaps();
        }
    }

    @SubscribeEvent
    public static void playerLoggedIn(final PlayerEvent.PlayerLoggedInEvent evt) {
        if (evt.getEntity() instanceof ServerPlayer sp) NutritionCapability.get(sp).ifPresent(INutritionTracker::sync);
    }

    @SubscribeEvent
    public static void playerRespawned(final PlayerEvent.PlayerRespawnEvent evt) {
        if (evt.getEntity() instanceof ServerPlayer sp) NutritionCapability.get(sp).ifPresent(INutritionTracker::sync);
    }

    @SubscribeEvent
    public static void playerDimensionTravel(final PlayerEvent.PlayerChangedDimensionEvent evt) {
        if (evt.getEntity() instanceof ServerPlayer sp) NutritionCapability.get(sp).ifPresent(INutritionTracker::sync);
    }

    @SubscribeEvent
    public static void playerTick(final TickEvent.PlayerTickEvent evt) {
        if (evt.side == LogicalSide.SERVER && evt.phase == TickEvent.Phase.END) {
            NutritionCapability.get(evt.player).ifPresent(INutritionTracker::tick);
        }
    }

    /**
     * 当食物使用计时归零（即将调用 finishUsingItem）时，保存吃前的饥饿/饱和快照。
     * 此时 player.eat() 尚未执行，快照是真正的"吃前"状态。
     * 使用 isEdible() 而非 getFoodProperties()==null，兼容其他模组自定义食物。
     */
    @SubscribeEvent
    public static void itemUseTick(final LivingEntityUseItemEvent.Tick evt) {
        if (evt.getDuration() > 0) return;
        if (evt.getEntity().level().isClientSide()) return;
        if (!(evt.getEntity() instanceof Player player)) return;
        if (!evt.getItem().isEdible()) return;

        PRE_EAT_STATE.put(player.getUUID(), new int[]{
                player.getFoodData().getFoodLevel(),
                Float.floatToRawIntBits(player.getFoodData().getSaturationLevel())
        });
    }

    /**
     * 食物使用完成：应用营养值、效果、命令；厌倦状态下还原饥饿/饱和。
     * 使用 isEdible() 入口检查，确保其他模组的食物（getFoodProperties 可能为 null）也能正常处理。
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void finishItemUse(final LivingEntityUseItemEvent.Finish evt) {
        ItemStack stack = evt.getItem();
        LivingEntity living = evt.getEntity();
        if (living.level().isClientSide() || !(living instanceof Player player)) return;

        // isEdible() 兼容其他模组；getFoodProperties 可能为 null（由模组自行处理营养）
        if (!stack.isEdible()) return;
        FoodProperties food = stack.getFoodProperties(living); // 可能为 null

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null) return;
        String itemIdStr = itemId.toString();
        boolean isEating = stack.getItem().getUseAnimation(stack) == UseAnim.EAT
                        || stack.getItem().getUseAnimation(stack) == UseAnim.DRINK;

        NutritionCapability.get(player).ifPresent(tracker -> {
            boolean normalEat = tracker.processEating(itemIdStr);

            if (normalEat) {
                ItemConfig config = ConfigManager.getItemConfig(itemIdStr);
                if (config != null) {
                    // 营养值仅在有 FoodProperties 时应用（配合原版饥饿系统）
                    if (food != null) {
                        for (Map.Entry<String, Float> entry : config.nutrition.entrySet()) {
                            tracker.addValue(entry.getKey(), entry.getValue());
                        }
                    }
                    // 效果和命令不依赖 FoodProperties，所有可食用物品均可触发
                    for (ItemConfig.EffectEntry effectEntry : config.effects) {
                        try {
                            MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.tryParse(effectEntry.effect));
                            if (effect != null) {
                                player.addEffect(new MobEffectInstance(effect, effectEntry.duration,
                                        effectEntry.level, false, effectEntry.showParticles));
                            }
                        } catch (Exception ignored) {}
                    }
                    if (!config.onEatCommands.isEmpty() || !config.conditionalCommands.isEmpty()) {
                        ServerPlayer spExec = (ServerPlayer) player;
                        net.minecraft.server.MinecraftServer server = spExec.getServer();
                        if (server != null) {
                            net.minecraft.commands.CommandSourceStack source =
                                    spExec.createCommandSourceStack().withPermission(4).withSuppressedOutput();

                            // 无条件指令
                            for (String cmd : config.onEatCommands) {
                                String c = cmd.startsWith("/") ? cmd.substring(1) : cmd;
                                server.getCommands().performPrefixedCommand(source, c);
                            }

                            // 条件指令：检查玩家当前营养值是否在范围内，满足才执行
                            for (ItemConfig.ConditionalCommand cc : config.conditionalCommands) {
                                float nutritionVal = tracker.getValue(cc.group);
                                if (nutritionVal >= cc.min && nutritionVal <= cc.max) {
                                    String c = cc.command.startsWith("/") ? cc.command.substring(1) : cc.command;
                                    server.getCommands().performPrefixedCommand(source, c);
                                }
                            }
                        }
                    }
                }
            } else if (food != null) {
                // 食物厌倦：还原到吃前快照（仅对有 FoodProperties 的食物撤销饥饿恢复）
                int[] pre = PRE_EAT_STATE.remove(player.getUUID());
                if (pre != null) {
                    player.getFoodData().setFoodLevel(pre[0]);
                    player.getFoodData().setSaturation(Float.intBitsToFloat(pre[1]));
                } else {
                    restoreFoodMath(player, food);
                }
                // 若配置了厌倦时可恢复饱食度，在快照基础上追加
                ItemConfig cooldownCfg = ConfigManager.getItemConfig(itemIdStr);
                int restoreHunger = (cooldownCfg != null && cooldownCfg.foodCooldown != null)
                        ? cooldownCfg.foodCooldown.restoreHunger : 0;
                if (restoreHunger > 0) {
                    int newLevel = Math.min(20, player.getFoodData().getFoodLevel() + restoreHunger);
                    player.getFoodData().setFoodLevel(newLevel);
                }
                // 强制推包，确保客户端立即更新
                if (player instanceof ServerPlayer sp) {
                    sp.connection.send(new ClientboundSetHealthPacket(
                            sp.getHealth(),
                            sp.getFoodData().getFoodLevel(),
                            sp.getFoodData().getSaturationLevel()));
                    // 有部分恢复时，发送带饱食度的提示（覆盖 Tracker 中的普通提示）
                    if (restoreHunger > 0) {
                        sp.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                                "message.healthy_diet.food_on_cooldown_with_hunger",
                                restoreHunger), true);
                    }
                }
            }

            if (isEating) tracker.onOtherFoodEaten(itemIdStr);
        });

        // 清理正常吃食物时可能残留的快照
        PRE_EAT_STATE.remove(player.getUUID());
    }

    /** 无快照时的数学回退：从当前值倒减食物属性 */
    private static void restoreFoodMath(Player player, FoodProperties food) {
        int newFood = Math.max(0, player.getFoodData().getFoodLevel() - food.getNutrition());
        player.getFoodData().setFoodLevel(newFood);
        float satGained = food.getNutrition() * food.getSaturationModifier() * 2.0f;
        float newSat = Math.max(0f, player.getFoodData().getSaturationLevel() - satGained);
        newSat = Math.min(newSat, newFood);
        player.getFoodData().setSaturation(newSat);
    }

    private record Provider(PlayerNutritionTracker instance, LazyOptional<INutritionTracker> capability)
            implements ICapabilitySerializable<Tag> {
        @Nonnull
        @Override
        public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
            if (NutritionCapability.NUTRITION_TRACKER != null) {
                return NutritionCapability.NUTRITION_TRACKER.orEmpty(cap, this.capability);
            }
            return LazyOptional.empty();
        }
        @Override
        public Tag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            if (this.instance != null) this.instance.save(tag);
            return tag;
        }
        @Override
        public void deserializeNBT(Tag nbt) {
            if (this.instance != null && nbt instanceof CompoundTag tag) this.instance.load(tag);
        }
    }
}

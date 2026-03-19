package com.xioyim.healthy_diet.client;

import com.xioyim.healthy_diet.HealthyDietConstants;
import com.xioyim.healthy_diet.client.screen.HealthyDietScreen;
import com.xioyim.healthy_diet.common.config.ConfigManager;
import com.xioyim.healthy_diet.common.config.DisplayConfig;
import com.xioyim.healthy_diet.common.config.GroupDefinition;
import com.xioyim.healthy_diet.common.config.ItemConfig;
import com.xioyim.healthy_diet.common.network.packet.SPacketNutrition;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;

@Mod.EventBusSubscriber(modid = HealthyDietConstants.MOD_ID, value = Dist.CLIENT)
public class HealthyDietClientEvents {

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onKeyPress(final InputEvent.Key evt) {
        if (HealthyDietKeys.OPEN_SCREEN.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen == null) mc.setScreen(new HealthyDietScreen());
        }
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onItemTooltip(final ItemTooltipEvent evt) {
        ItemStack stack = evt.getItemStack();
        if (!stack.isEdible()) return;
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null) return;
        String itemIdStr = itemId.toString();
        ItemConfig config = ConfigManager.getItemConfig(itemIdStr);
        DisplayConfig dc = ConfigManager.getDisplayConfig();

        // --- 营养值 Tooltip ---
        if (dc.showNutritionLore && config != null && !config.nutrition.isEmpty()) {
            MutableComponent line = Component.empty();
            boolean first = true;
            for (Map.Entry<String, Float> entry : config.nutrition.entrySet()) {
                GroupDefinition groupDef = ConfigManager.getGroup(entry.getKey());
                String name = groupDef != null ? groupDef.displayName : entry.getKey();
                float val = entry.getValue();
                // 整数时不显示小数点，有小数时保留 1 位
                String sign = val >= 0 ? "+" : "";
                String text = name + sign + (val == Math.floor(val)
                        ? String.valueOf((int) val)
                        : String.format("%.1f", val));
                if (!first) line.append(Component.literal("  "));
                first = false;
                Style style = groupDef != null
                        ? Style.EMPTY.withColor(TextColor.fromRgb(groupDef.getColorArgb() & 0xFFFFFF))
                        : Style.EMPTY.withColor(TextColor.fromRgb(val >= 0 ? 0x55FF55 : 0xFF5555));
                line.append(Component.literal(text).withStyle(style));
            }
            evt.getToolTip().add(line);
        }

        // --- 食物厌倦度 / 恢复度 Tooltip ---
        if (dc.showCooldownStatusLore && config != null && config.foodCooldown != null) {
            ItemConfig.FoodCooldownConfig cd = config.foodCooldown;
            SPacketNutrition.CooldownState state = ClientNutritionData.getCooldownState(itemIdStr);
            int eatenCount = state != null ? state.eatenCount() : 0;

            if (state != null && state.onCooldown()) {
                // 满厌倦：始终显示次数恢复度（上行）
                int pct = cd.cooldownCount > 0
                        ? Math.min(100, (int)(state.otherFoodsEaten() * 100f / cd.cooldownCount))
                        : 100;
                evt.getToolTip().add(Component.translatable("tooltip.healthy_diet.fatigue_recovery_pct", pct)
                        .withStyle(ChatFormatting.RED));
                // 若有时间冷却，下行显示剩余时间（整数分钟，自然每分钟更新一次）
                if (cd.cooldownMinutes > 0) {
                    Minecraft mc = Minecraft.getInstance();
                    long currentTick = mc.level != null ? mc.level.getGameTime() : 0L;
                    long cooldownTicks = (long)(cd.cooldownMinutes * 1200L);
                    long remaining = Math.max(0L, cooldownTicks - (currentTick - state.cooldownStartTick()));
                    int minutes = (int)(remaining / 1200); // 1分钟=1200ticks，整数向下取整
                    evt.getToolTip().add(Component.translatable("tooltip.healthy_diet.fatigue_recovery_time", minutes)
                            .withStyle(ChatFormatting.RED));
                }
            } else if (eatenCount > 0) {
                // 未满：显示厌倦进度百分比（浅灰色）
                int pct = cd.maxUses > 0
                        ? Math.min(99, (int)(eatenCount * 100f / cd.maxUses))
                        : 0;
                if (pct > 0) {
                    evt.getToolTip().add(Component.translatable("tooltip.healthy_diet.fatigue_level", pct)
                            .withStyle(ChatFormatting.GRAY));
                }
            }
        }
    }
}

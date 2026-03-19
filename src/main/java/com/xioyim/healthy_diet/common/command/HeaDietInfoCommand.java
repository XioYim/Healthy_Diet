package com.xioyim.healthy_diet.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.xioyim.healthy_diet.common.config.ConfigManager;
import com.xioyim.healthy_diet.common.config.GroupDefinition;
import com.xioyim.healthy_diet.common.config.ItemConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;

public class HeaDietInfoCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("HeaDietInfo")
                .requires(src -> src.hasPermission(4))
                .then(Commands.literal("hand")
                        .executes(ctx -> execute(ctx, false)))
                .then(Commands.argument("targetId", ResourceLocationArgument.id())
                        .suggests((ctx, b) -> {
                            ConfigManager.getItemConfigKeys().forEach(b::suggest);
                            return b.buildFuture();
                        })
                        .executes(ctx -> execute(ctx, true)))
        );
    }

    private static int execute(CommandContext<CommandSourceStack> ctx, boolean hasExplicitId) throws CommandSyntaxException {
        String id = HeaDietSetCommand.resolveTargetId(ctx, hasExplicitId);
        if (id == null) return 0;

        ItemConfig config = ConfigManager.getItemConfig(id);
        if (config == null) {
            ctx.getSource().sendFailure(Component.translatable("command.healthy_diet.error.item_not_found", id));
            return 0;
        }

        // ── 物品 ID 标题 ──
        ctx.getSource().sendSuccess(() ->
                Component.translatable("command.healthy_diet.info.header", id)
                        .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD), false);

        // ── 营养值 ──
        if (!config.nutrition.isEmpty()) {
            ctx.getSource().sendSuccess(() ->
                    Component.translatable("command.healthy_diet.info.header_nutrition"), false);
            for (Map.Entry<String, Float> e : config.nutrition.entrySet()) {
                GroupDefinition g = ConfigManager.getGroup(e.getKey());
                String name = g != null ? g.displayName : e.getKey();
                String sign = e.getValue() >= 0 ? "+" : "";
                String valStr = sign + (int)(float)e.getValue();
                String group = e.getKey();
                MutableComponent line = Component.translatable(
                        "command.healthy_diet.info.nutrition_entry", name, valStr);
                line.append(Component.literal(" "));
                line.append(xButton("/HeaDietRemove " + id + " nutrition " + group,
                        Component.translatable("command.healthy_diet.info.remove_nutrition")));
                ctx.getSource().sendSuccess(() -> line, false);
            }
        }

        // ── 药水效果 ──
        if (!config.effects.isEmpty()) {
            ctx.getSource().sendSuccess(() ->
                    Component.translatable("command.healthy_diet.info.header_effect"), false);
            for (int i = 0; i < config.effects.size(); i++) {
                ItemConfig.EffectEntry ef = config.effects.get(i);
                MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.tryParse(ef.effect));
                String eName = effect != null
                        ? Component.translatable(effect.getDescriptionId()).getString()
                        : ef.effect;
                final int idx = i;
                MutableComponent line = Component.translatable(
                        "command.healthy_diet.info.effect_entry",
                        eName, ef.level + 1, ef.duration / 20);
                if (!ef.showParticles)
                    line.append(Component.translatable("command.healthy_diet.info.effect_no_particles"));
                line.append(Component.literal(" "));
                line.append(xButton("/HeaDietRemove " + id + " effect " + idx,
                        Component.translatable("command.healthy_diet.info.remove_effect")));
                ctx.getSource().sendSuccess(() -> line, false);
            }
        }

        // ── 食物厌倦 ──
        if (config.foodCooldown != null) {
            ItemConfig.FoodCooldownConfig cd = config.foodCooldown;
            ctx.getSource().sendSuccess(() ->
                    Component.translatable("command.healthy_diet.info.header_food"), false);
            MutableComponent info = Component.translatable(
                    "command.healthy_diet.info.food_base", cd.maxUses, cd.cooldownCount);
            if (cd.cooldownMinutes > 0)
                info.append(Component.translatable(
                        "command.healthy_diet.info.food_time", cd.cooldownMinutes));
            if (cd.restoreHunger > 0)
                info.append(Component.translatable(
                        "command.healthy_diet.info.food_restore", cd.restoreHunger));
            info.append(Component.literal(" "));
            info.append(xButton("/HeaDietRemove " + id + " food",
                    Component.translatable("command.healthy_diet.info.remove_food")));
            ctx.getSource().sendSuccess(() -> info, false);
        }

        // ── 食用指令 ──
        if (!config.onEatCommands.isEmpty()) {
            ctx.getSource().sendSuccess(() ->
                    Component.translatable("command.healthy_diet.info.header_command"), false);
            for (int i = 0; i < config.onEatCommands.size(); i++) {
                final String cmd = config.onEatCommands.get(i);
                final int idx = i;
                MutableComponent line = Component.translatable(
                        "command.healthy_diet.info.command_entry", cmd);
                line.append(Component.literal(" "));
                line.append(xButton("/HeaDietRemove " + id + " command " + idx,
                        Component.translatable("command.healthy_diet.info.remove_command")));
                ctx.getSource().sendSuccess(() -> line, false);
            }
        }

        // ── 条件指令 ──
        if (!config.conditionalCommands.isEmpty()) {
            ctx.getSource().sendSuccess(() ->
                    Component.translatable("command.healthy_diet.info.header_condcmd"), false);
            for (int i = 0; i < config.conditionalCommands.size(); i++) {
                ItemConfig.ConditionalCommand cc = config.conditionalCommands.get(i);
                GroupDefinition gd = ConfigManager.getGroup(cc.group);
                String gName = gd != null ? gd.displayName : cc.group;
                String minStr = cc.min == Math.floor(cc.min) ? String.valueOf((int) cc.min) : String.valueOf(cc.min);
                String maxStr = cc.max == Math.floor(cc.max) ? String.valueOf((int) cc.max) : String.valueOf(cc.max);
                final int idx = i;
                MutableComponent line = Component.translatable(
                        "command.healthy_diet.info.condcmd_entry",
                        gName, minStr, maxStr, cc.command);
                line.append(Component.literal(" "));
                line.append(xButton("/HeaDietRemove " + id + " condcmd " + idx,
                        Component.translatable("command.healthy_diet.info.remove_condcmd")));
                ctx.getSource().sendSuccess(() -> line, false);
            }
        }

        return 1;
    }

    /** Creates a red [X] button that runs {@code command} when clicked. */
    private static MutableComponent xButton(String command, Component hoverText) {
        return Component.translatable("command.healthy_diet.info.btn_remove")
                .withStyle(style -> style
                        .withColor(ChatFormatting.RED)
                        .withBold(false)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText)));
    }
}

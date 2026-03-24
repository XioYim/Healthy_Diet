package com.xioyim.healthy_diet.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.xioyim.healthy_diet.common.config.ConfigManager;
import com.xioyim.healthy_diet.common.config.GroupBonusConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;

public class HDEffectCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("hdeffect")
                .requires(src -> src.hasPermission(4))
                .executes(ctx -> sendUsage(ctx.getSource()))
                .then(Commands.argument("group", StringArgumentType.word())
                        .suggests((ctx, b) -> { ConfigManager.getGroups().keySet().forEach(b::suggest); return b.buildFuture(); })
                        .then(Commands.argument("min", FloatArgumentType.floatArg(0, 100))
                                .then(Commands.argument("max", FloatArgumentType.floatArg(0, 100))
                                        .then(Commands.argument("effect", ResourceLocationArgument.id())
                                                .suggests((ctx, b) -> {
                                                    ForgeRegistries.MOB_EFFECTS.getKeys().stream()
                                                            .map(ResourceLocation::toString).forEach(b::suggest);
                                                    return b.buildFuture();
                                                })
                                                .then(Commands.argument("level", IntegerArgumentType.integer(0, 255))
                                                        .then(Commands.argument("durationSecs", IntegerArgumentType.integer(1))
                                                                .executes(ctx -> execute(ctx, false))
                                                                .then(Commands.argument("showParticles", BoolArgumentType.bool())
                                                                        .executes(ctx -> execute(ctx, true)))))))))
        );
    }

    private static int sendUsage(CommandSourceStack src) {
        src.sendSuccess(() -> Component.translatable("command.healthy_diet.usage.hdeffect.0"), false);
        src.sendSuccess(() -> Component.translatable("command.healthy_diet.usage.hdeffect.1"), false);
        src.sendSuccess(() -> Component.translatable("command.healthy_diet.usage.hdeffect.2"), false);
        return 0;
    }

    private static int execute(CommandContext<CommandSourceStack> ctx, boolean hasParticles) throws CommandSyntaxException {
        String group = StringArgumentType.getString(ctx, "group");
        float min = FloatArgumentType.getFloat(ctx, "min");
        float max = FloatArgumentType.getFloat(ctx, "max");
        ResourceLocation effectRl = ResourceLocationArgument.getId(ctx, "effect");
        int level = IntegerArgumentType.getInteger(ctx, "level");
        int durationSecs = IntegerArgumentType.getInteger(ctx, "durationSecs");
        boolean showParticles = hasParticles && BoolArgumentType.getBool(ctx, "showParticles");

        if (ConfigManager.getGroup(group) == null) {
            ctx.getSource().sendFailure(Component.translatable("command.healthy_diet.error.group_not_found", group));
            return 0;
        }
        if (ForgeRegistries.MOB_EFFECTS.getValue(effectRl) == null) {
            ctx.getSource().sendFailure(Component.translatable("command.healthy_diet.error.effect_not_found", effectRl.toString()));
            return 0;
        }
        if (min > max) {
            ctx.getSource().sendFailure(Component.translatable("command.healthy_diet.error.min_greater_max"));
            return 0;
        }

        GroupBonusConfig bc = ConfigManager.getGroupBonusConfig(group);
        if (bc == null) bc = new GroupBonusConfig();

        GroupBonusConfig.BonusEntry entry = new GroupBonusConfig.BonusEntry();
        GroupBonusConfig.RangeCondition range = new GroupBonusConfig.RangeCondition();
        range.min = min;
        range.max = max;
        entry.conditions = new HashMap<>();
        entry.conditions.put(group, range);

        GroupBonusConfig.EffectBonus eb = new GroupBonusConfig.EffectBonus();
        eb.effect = effectRl.toString();
        eb.level = level;
        eb.duration = durationSecs * 20;
        eb.showParticles = showParticles;
        entry.effectBonuses.add(eb);

        bc.bonuses.add(entry);
        ConfigManager.saveGroupBonusConfig(group, bc);

        String displayName = ConfigManager.getGroup(group).displayName;
        ctx.getSource().sendSuccess(() -> Component.translatable("command.healthy_diet.config_effect.result",
                displayName, effectRl.toString(), level + 1, durationSecs, (int) min + "%", (int) max + "%"), true);
        return 1;
    }
}

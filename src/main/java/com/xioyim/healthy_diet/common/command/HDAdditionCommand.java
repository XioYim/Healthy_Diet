package com.xioyim.healthy_diet.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
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

public class HDAdditionCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("hdaddition")
                .requires(src -> src.hasPermission(4))
                .executes(ctx -> sendUsage(ctx.getSource()))
                .then(Commands.argument("group", StringArgumentType.word())
                        .suggests((ctx, b) -> { ConfigManager.getGroups().keySet().forEach(b::suggest); return b.buildFuture(); })
                        .then(Commands.argument("min", FloatArgumentType.floatArg(0, 100))
                                .then(Commands.argument("max", FloatArgumentType.floatArg(0, 100))
                                        .then(Commands.argument("attribute", ResourceLocationArgument.id())
                                                .suggests((ctx, b) -> {
                                                    ForgeRegistries.ATTRIBUTES.getKeys().stream()
                                                            .map(ResourceLocation::toString).forEach(b::suggest);
                                                    return b.buildFuture();
                                                })
                                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg())
                                                        .executes(ctx -> execute(ctx))))))));
    }

    private static int sendUsage(CommandSourceStack src) {
        src.sendSuccess(() -> Component.translatable("command.healthy_diet.usage.hdaddition.0"), false);
        src.sendSuccess(() -> Component.translatable("command.healthy_diet.usage.hdaddition.1"), false);
        src.sendSuccess(() -> Component.translatable("command.healthy_diet.usage.hdaddition.2"), false);
        return 0;
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String group = StringArgumentType.getString(ctx, "group");
        float min = FloatArgumentType.getFloat(ctx, "min");
        float max = FloatArgumentType.getFloat(ctx, "max");
        ResourceLocation attrRl = ResourceLocationArgument.getId(ctx, "attribute");
        double amount = DoubleArgumentType.getDouble(ctx, "amount");

        if (ConfigManager.getGroup(group) == null) {
            ctx.getSource().sendFailure(Component.translatable("command.healthy_diet.error.group_not_found", group));
            return 0;
        }
        if (ForgeRegistries.ATTRIBUTES.getValue(attrRl) == null) {
            ctx.getSource().sendFailure(Component.translatable("command.healthy_diet.error.attribute_not_found", attrRl.toString()));
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

        GroupBonusConfig.AttributeBonus ab = new GroupBonusConfig.AttributeBonus();
        ab.attribute = attrRl.toString();
        ab.operation = "add";
        ab.value = amount;
        entry.attributeBonuses.add(ab);

        bc.bonuses.add(entry);
        ConfigManager.saveGroupBonusConfig(group, bc);

        String displayName = ConfigManager.getGroup(group).displayName;
        ctx.getSource().sendSuccess(() -> Component.translatable("command.healthy_diet.hd_addition.result",
                displayName, (int) min + "%", (int) max + "%", attrRl.toString(), amount), true);
        return 1;
    }
}

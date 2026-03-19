package com.xioyim.healthy_diet.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.xioyim.healthy_diet.common.config.ConfigManager;
import com.xioyim.healthy_diet.common.config.ItemConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

public class HeaDietSetFoodCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("HeaDietSetFood")
                .requires(src -> src.hasPermission(4))
                // /HeaDietSetFood hand <maxUses> <cooldownCount> [cooldownMinutes [restoreHunger]]
                .then(Commands.literal("hand")
                        .then(Commands.argument("maxUses", IntegerArgumentType.integer(1))
                                .then(Commands.argument("cooldownCount", IntegerArgumentType.integer(0))
                                        .executes(ctx -> execute(ctx, false, false, false))
                                        .then(Commands.argument("cooldownMinutes", FloatArgumentType.floatArg(0f))
                                                .executes(ctx -> execute(ctx, false, true, false))
                                                .then(Commands.argument("restoreHunger", IntegerArgumentType.integer(0, 20))
                                                        .executes(ctx -> execute(ctx, false, true, true)))))))
                // /HeaDietSetFood <itemOrBlockId> <maxUses> <cooldownCount> [cooldownMinutes [restoreHunger]]
                .then(Commands.argument("targetId", ResourceLocationArgument.id())
                        .suggests((ctx, b) -> {
                            ForgeRegistries.ITEMS.getKeys().stream().map(ResourceLocation::toString).forEach(b::suggest);
                            ForgeRegistries.BLOCKS.getKeys().stream().map(ResourceLocation::toString).forEach(b::suggest);
                            return b.buildFuture();
                        })
                        .then(Commands.argument("maxUses", IntegerArgumentType.integer(1))
                                .then(Commands.argument("cooldownCount", IntegerArgumentType.integer(0))
                                        .executes(ctx -> execute(ctx, true, false, false))
                                        .then(Commands.argument("cooldownMinutes", FloatArgumentType.floatArg(0f))
                                                .executes(ctx -> execute(ctx, true, true, false))
                                                .then(Commands.argument("restoreHunger", IntegerArgumentType.integer(0, 20))
                                                        .executes(ctx -> execute(ctx, true, true, true)))))))
        );
    }

    private static int execute(CommandContext<CommandSourceStack> ctx, boolean hasExplicitId, boolean hasTime, boolean hasRestore) throws CommandSyntaxException {
        int maxUses = IntegerArgumentType.getInteger(ctx, "maxUses");
        int cooldownCount = IntegerArgumentType.getInteger(ctx, "cooldownCount");
        float cooldownMinutes = hasTime ? FloatArgumentType.getFloat(ctx, "cooldownMinutes") : -1f;
        int restoreHunger = hasRestore ? IntegerArgumentType.getInteger(ctx, "restoreHunger") : 0;

        String itemIdStr = HeaDietSetCommand.resolveTargetId(ctx, hasExplicitId);
        if (itemIdStr == null) return 0;

        ItemConfig config = ConfigManager.getItemConfig(itemIdStr);
        if (config == null) config = new ItemConfig();
        config.foodCooldown = new ItemConfig.FoodCooldownConfig();
        config.foodCooldown.maxUses = maxUses;
        config.foodCooldown.cooldownCount = cooldownCount;
        config.foodCooldown.cooldownMinutes = cooldownMinutes;
        config.foodCooldown.restoreHunger = restoreHunger;
        ConfigManager.saveItemConfig(itemIdStr, config);

        final String finalId = itemIdStr;
        final float finalMinutes = cooldownMinutes;
        final int finalRestore = restoreHunger;
        ctx.getSource().sendSuccess(() -> finalMinutes > 0
                ? Component.translatable("command.healthy_diet.set_food.result_with_time",
                        finalId, maxUses, cooldownCount, String.format("%.1f", finalMinutes), finalRestore)
                : Component.translatable("command.healthy_diet.set_food.result",
                        finalId, maxUses, cooldownCount, finalRestore),
                true);
        return 1;
    }
}

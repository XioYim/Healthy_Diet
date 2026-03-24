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
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;

public class HeaDietInfoCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("headietinfo")
                .requires(src -> src.hasPermission(4))
                .executes(ctx -> sendUsage(ctx.getSource()))
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

        // ── 标题：[物品显示名] 营养信息：（全部黄色加粗）──
        String itemDisplayName = resolveItemName(id);
        MutableComponent header = Component.literal("[" + itemDisplayName + "] ")
                .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);
        header.append(
                Component.translatable("command.healthy_diet.info.header_suffix")
                        .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        ctx.getSource().sendSuccess(() -> header, false);

        // ── 营养值 ──
        // §6[营养] §d{组名}：§f{+值}  §c[X]
        for (Map.Entry<String, Float> e : config.nutrition.entrySet()) {
            GroupDefinition g = ConfigManager.getGroup(e.getKey());
            String name   = g != null ? g.displayName : e.getKey();
            String valStr = formatValue(e.getValue());
            String group  = e.getKey();

            MutableComponent line = Component.translatable("command.healthy_diet.info.tag_nutrition")
                    .withStyle(ChatFormatting.GOLD);
            line.append(Component.literal(" " + name + "：").withStyle(ChatFormatting.LIGHT_PURPLE));
            line.append(Component.literal(valStr).withStyle(ChatFormatting.WHITE));
            line.append(Component.literal(" "));
            line.append(xButton("/headietremove " + id + " nutrition " + group,
                    Component.translatable("command.healthy_diet.info.remove_nutrition")));
            ctx.getSource().sendSuccess(() -> line, false);
        }

        // ── 药水效果 ──
        // §b[药水] §a{效果名} {罗马等级} §7{秒}s §a[粒子]  §c[X]
        for (int i = 0; i < config.effects.size(); i++) {
            ItemConfig.EffectEntry ef = config.effects.get(i);
            MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.tryParse(ef.effect));
            String eName = effect != null
                    ? Component.translatable(effect.getDescriptionId()).getString()
                    : ef.effect;
            String roman = toRoman(ef.level);
            int secs = ef.duration / 20;
            final int idx = i;

            MutableComponent line = Component.translatable("command.healthy_diet.info.tag_effect")
                    .withStyle(ChatFormatting.AQUA);
            line.append(Component.literal(" " + eName + " " + roman + " ").withStyle(ChatFormatting.GREEN));
            line.append(Component.literal(secs + "s").withStyle(ChatFormatting.GRAY));
            // 仅在 showParticles=true 时追加 [粒子] 提示
            if (ef.showParticles) {
                line.append(Component.literal(" "));
                line.append(Component.translatable("command.healthy_diet.info.tag_particles")
                        .withStyle(ChatFormatting.GREEN));
            }
            // 仅在概率 < 100 时追加概率提示
            int prob = Math.max(0, Math.min(100, ef.probability));
            if (prob < 100) {
                line.append(Component.literal(" (" + prob + "%)").withStyle(ChatFormatting.GRAY));
            }
            line.append(Component.literal(" "));
            line.append(xButton("/headietremove " + id + " effect " + idx,
                    Component.translatable("command.healthy_diet.info.remove_effect")));
            ctx.getSource().sendSuccess(() -> line, false);
        }

        // ── 食物厌倦 ──
        // §d[厌倦] §7最大食用：§f{n} §7冷却所需：§f{n} ...  §c[X]
        if (config.foodCooldown != null) {
            ItemConfig.FoodCooldownConfig cd = config.foodCooldown;

            MutableComponent info = Component.translatable("command.healthy_diet.info.tag_food")
                    .withStyle(ChatFormatting.LIGHT_PURPLE);
            info.append(Component.literal(" "));
            info.append(Component.translatable("command.healthy_diet.info.food_max_uses")
                    .withStyle(ChatFormatting.GRAY));
            info.append(Component.literal(String.valueOf(cd.maxUses)).withStyle(ChatFormatting.WHITE));
            info.append(Component.literal(" "));
            info.append(Component.translatable("command.healthy_diet.info.food_cooldown")
                    .withStyle(ChatFormatting.GRAY));
            info.append(Component.literal(String.valueOf(cd.cooldownCount)).withStyle(ChatFormatting.WHITE));
            if (cd.cooldownMinutes > 0) {
                info.append(Component.literal(" "));
                info.append(Component.translatable("command.healthy_diet.info.food_time_label")
                        .withStyle(ChatFormatting.GRAY));
                info.append(Component.literal(String.valueOf(cd.cooldownMinutes)).withStyle(ChatFormatting.WHITE));
            }
            if (cd.restoreHunger > 0) {
                info.append(Component.literal(" "));
                info.append(Component.translatable("command.healthy_diet.info.food_restore_label")
                        .withStyle(ChatFormatting.GRAY));
                info.append(Component.literal(String.valueOf(cd.restoreHunger)).withStyle(ChatFormatting.WHITE));
            }
            info.append(Component.literal(" "));
            info.append(xButton("/headietremove " + id + " food",
                    Component.translatable("command.healthy_diet.info.remove_food")));
            ctx.getSource().sendSuccess(() -> info, false);
        }

        // ── 食用指令 ──
        // §c[指令] §5{内容}  §c[X]
        for (int i = 0; i < config.onEatCommands.size(); i++) {
            final String cmd = config.onEatCommands.get(i);
            final int idx = i;

            MutableComponent line = Component.translatable("command.healthy_diet.info.tag_command")
                    .withStyle(ChatFormatting.RED);
            line.append(Component.literal(" " + cmd).withStyle(ChatFormatting.DARK_PURPLE));
            line.append(Component.literal(" "));
            line.append(xButton("/headietremove " + id + " command " + idx,
                    Component.translatable("command.healthy_diet.info.remove_command")));
            ctx.getSource().sendSuccess(() -> line, false);
        }

        // ── 条件指令 ──
        // §e[条件] §d{组名} §7[min~max]：§f{指令}  §c[X]
        for (int i = 0; i < config.conditionalCommands.size(); i++) {
            ItemConfig.ConditionalCommand cc = config.conditionalCommands.get(i);
            GroupDefinition gd = ConfigManager.getGroup(cc.group);
            String gName  = gd != null ? gd.displayName : cc.group;
            String minStr = formatValue(cc.min);
            String maxStr = formatValue(cc.max);
            final int idx = i;

            MutableComponent line = Component.translatable("command.healthy_diet.info.tag_condcmd")
                    .withStyle(ChatFormatting.YELLOW);
            line.append(Component.literal(" " + gName + " ").withStyle(ChatFormatting.LIGHT_PURPLE));
            line.append(Component.literal("[" + minStr + "~" + maxStr + "]: ").withStyle(ChatFormatting.GRAY));
            line.append(Component.literal(cc.command).withStyle(ChatFormatting.WHITE));
            line.append(Component.literal(" "));
            line.append(xButton("/headietremove " + id + " condcmd " + idx,
                    Component.translatable("command.healthy_diet.info.remove_condcmd")));
            ctx.getSource().sendSuccess(() -> line, false);
        }

        return 1;
    }

    private static int sendUsage(CommandSourceStack src) {
        src.sendSuccess(() -> Component.translatable("command.healthy_diet.usage.headietinfo.0"), false);
        src.sendSuccess(() -> Component.translatable("command.healthy_diet.usage.headietinfo.1"), false);
        src.sendSuccess(() -> Component.translatable("command.healthy_diet.usage.headietinfo.2"), false);
        return 0;
    }

    /** 从注册表解析物品 lang 显示名，失败时回退为原始 ID。 */
    private static String resolveItemName(String id) {
        try {
            Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(id));
            if (item != null) return Component.translatable(item.getDescriptionId()).getString();
        } catch (Exception ignored) {}
        return id;
    }

    /**
     * 将浮点值格式化为带符号的字符串。
     * 整数显示无小数（+5），有小数保留一位（+5.5）。
     */
    private static String formatValue(float val) {
        String sign = val >= 0 ? "+" : "";
        return (val % 1.0f == 0.0f)
                ? sign + (int) val
                : sign + String.format("%.1f", val);
    }

    /** 将 0-based amplifier 转为罗马数字（0→I，1→II，…）。 */
    private static String toRoman(int amplifier) {
        int n = amplifier + 1;
        if (n <= 0) return String.valueOf(n);
        int[]    vals = {1000,900,500,400,100,90,50,40,10,9,5,4,1};
        String[] syms = {"M","CM","D","CD","C","XC","L","XL","X","IX","V","IV","I"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vals.length; i++)
            while (n >= vals[i]) { sb.append(syms[i]); n -= vals[i]; }
        return sb.toString();
    }

    /** 创建红色可点击的 [X] 删除按钮。 */
    private static MutableComponent xButton(String command, Component hoverText) {
        return Component.translatable("command.healthy_diet.info.btn_remove")
                .withStyle(style -> style
                        .withBold(false)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText)));
    }
}

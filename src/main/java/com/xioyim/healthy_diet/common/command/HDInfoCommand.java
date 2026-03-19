package com.xioyim.healthy_diet.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.xioyim.healthy_diet.common.config.ConfigManager;
import com.xioyim.healthy_diet.common.config.GroupBonusConfig;
import com.xioyim.healthy_diet.common.config.GroupDefinition;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;

/**
 * /HDInfo <group>
 * 所有玩家可查询，OP 才显示 [X] 删除按钮。
 */
public class HDInfoCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("HDInfo")
                .then(Commands.argument("group", StringArgumentType.word())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                ConfigManager.getGroups().keySet(), b))
                        .executes(HDInfoCommand::execute))
        );
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String groupId = StringArgumentType.getString(ctx, "group");
        GroupDefinition groupDef = ConfigManager.getGroup(groupId);
        if (groupDef == null) {
            ctx.getSource().sendFailure(Component.translatable(
                    "command.healthy_diet.error.group_not_found", groupId));
            return 0;
        }

        boolean isOp = ctx.getSource().hasPermission(4);

        // ── 标题 ──
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "command.healthy_diet.hdinfo.header", groupDef.displayName), false);

        GroupBonusConfig bc = ConfigManager.getGroupBonusConfig(groupId);
        if (bc == null || bc.bonuses.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "command.healthy_diet.hdinfo.empty"), false);
            return 1;
        }

        for (int i = 0; i < bc.bonuses.size(); i++) {
            GroupBonusConfig.BonusEntry entry = bc.bonuses.get(i);
            final int idx = i;
            String rangeStr = buildRangeStr(entry, groupId);

            // 药水效果条目
            for (GroupBonusConfig.EffectBonus eb : entry.effectBonuses) {
                MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.tryParse(eb.effect));
                String eName = effect != null
                        ? Component.translatable(effect.getDescriptionId()).getString()
                        : eb.effect;
                MutableComponent line = Component.translatable(
                        "command.healthy_diet.hdinfo.effect_entry",
                        eName, eb.level + 1, eb.duration / 20, rangeStr);
                if (isOp) {
                    line.append(Component.literal(" "));
                    line.append(xButton("/HDRemove " + groupId + " " + idx,
                            Component.translatable("command.healthy_diet.hdinfo.remove_entry")));
                }
                ctx.getSource().sendSuccess(() -> line, false);
            }

            // 属性加成条目
            for (GroupBonusConfig.AttributeBonus ab : entry.attributeBonuses) {
                Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(ResourceLocation.tryParse(ab.attribute));
                String aName = attr != null
                        ? Component.translatable(attr.getDescriptionId()).getString()
                        : ab.attribute;
                String valStr = ab.value >= 0 ? "+" + ab.value : String.valueOf(ab.value);
                MutableComponent line = Component.translatable(
                        "command.healthy_diet.hdinfo.addition_entry",
                        aName, valStr, rangeStr);
                if (isOp) {
                    line.append(Component.literal(" "));
                    line.append(xButton("/HDRemove " + groupId + " " + idx,
                            Component.translatable("command.healthy_diet.hdinfo.remove_entry")));
                }
                ctx.getSource().sendSuccess(() -> line, false);
            }
        }

        return 1;
    }

    /**
     * 构建范围字符串。
     * 单条件（常见）→ "min~max"
     * 多条件联动 → "组A:min~max & 组B:min~max"
     */
    private static String buildRangeStr(GroupBonusConfig.BonusEntry entry, String primaryGroup) {
        if (entry.conditions.size() == 1) {
            GroupBonusConfig.RangeCondition rc = entry.conditions.get(primaryGroup);
            if (rc != null) return fmt(rc.min) + "~" + fmt(rc.max);
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, GroupBonusConfig.RangeCondition> e : entry.conditions.entrySet()) {
            if (sb.length() > 0) sb.append(" & ");
            GroupDefinition gd = ConfigManager.getGroup(e.getKey());
            String gName = gd != null ? gd.displayName : e.getKey();
            sb.append(gName).append(":").append(fmt(e.getValue().min)).append("~").append(fmt(e.getValue().max));
        }
        return sb.toString();
    }

    private static String fmt(float v) {
        return v == Math.floor(v) ? String.valueOf((int) v) : String.valueOf(v);
    }

    private static MutableComponent xButton(String command, Component hoverText) {
        return Component.translatable("command.healthy_diet.info.btn_remove")
                .withStyle(style -> style
                        .withColor(ChatFormatting.RED)
                        .withBold(false)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText)));
    }
}

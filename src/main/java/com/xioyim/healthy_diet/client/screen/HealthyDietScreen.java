package com.xioyim.healthy_diet.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.xioyim.healthy_diet.client.ClientNutritionData;
import com.xioyim.healthy_diet.common.config.*;
import com.xioyim.healthy_diet.common.network.HealthyDietNetwork;
import com.xioyim.healthy_diet.common.network.packet.CPacketStageCommand;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HealthyDietScreen extends Screen {

    // ══════════════════════════════════════════════════════════════
    //  材质路径
    // ══════════════════════════════════════════════════════════════

    /**
     * 面板背景：使用原版 demo_background.png（与 Diet mod 一致）。
     * 该材质内置于 Minecraft，无需提供额外文件。
     */
    /** 面板背景：原版 demo_background.png，无需额外文件 */
    @SuppressWarnings("removal")
    private static final ResourceLocation BACKGROUND =
            new ResourceLocation("minecraft", "textures/gui/demo_background.png");

    /**
     * 进度条背景材质（黑/灰，102×5 px）。
     * 放置路径：assets/healthy_diet/textures/gui/bar_background.png
     */
    @SuppressWarnings("removal")
    private static final ResourceLocation BAR_BG =
            new ResourceLocation("healthy_diet", "textures/gui/bar_background.png");

    /**
     * 进度条填充材质（纯白，102×5 px）。
     * 放置路径：assets/healthy_diet/textures/gui/bar_fill.png
     * 渲染时通过 coloredBlit 以组颜色染色，白色 × 颜色 = 该颜色。
     */
    @SuppressWarnings("removal")
    private static final ResourceLocation BAR_FILL =
            new ResourceLocation("healthy_diet", "textures/gui/bar_fill.png");

    // ══════════════════════════════════════════════════════════════
    //  固定布局常量（材质相关，不可配置）
    // ══════════════════════════════════════════════════════════════

    /** 进度条宽度（像素），与材质宽度一致 */
    private static final int BAR_W = 102;

    /** 进度条高度（像素），与材质高度一致 */
    private static final int BAR_H = 5;

    // ══════════════════════════════════════════════════════════════
    //  运行时字段（面板尺寸在 init 时从配置文件读取）
    // ══════════════════════════════════════════════════════════════

    private int panelX, panelY, panelWidth, panelHeight;
    private Button checkButton;

    /** 当前帧鼠标悬停的阶段（每帧 render 重置） */
    private StageConfig.Stage hoveredStage   = null;
    private String            hoveredGroupId = null;
    private int               hoveredStageIdx = -1;
    private int               stageTooltipX, stageTooltipY;

    public HealthyDietScreen() {
        super(Component.translatable("screen.healthy_diet.nutrition"));
    }

    // ──────────────────────────────────────────────────────────────
    //  初始化
    // ──────────────────────────────────────────────────────────────
    @Override
    protected void init() {
        DisplayConfig dc   = ConfigManager.getDisplayConfig();
        int numGroups      = ConfigManager.getGroups().size();

        // 从配置文件读取所有尺寸参数
        panelWidth  = dc.panelWidth;
        panelHeight = dc.paddingTop + numGroups * dc.rowSpacing + dc.paddingBottom;

        // 面板始终居中于屏幕
        panelX = (this.width  - panelWidth)  / 2;
        panelY = (this.height - panelHeight) / 2;

        // 确认按钮水平居中于面板、垂直贴近底部
        int btnW = 90;
        checkButton = Button.builder(
                        Component.translatable("button.healthy_diet.check_complete"),
                        btn -> this.onClose())
                .bounds(panelX + (panelWidth - btnW) / 2,
                        panelY + panelHeight - 22, btnW, 16)
                .build();
        this.addRenderableWidget(checkButton);
    }

    // ──────────────────────────────────────────────────────────────
    //  主渲染
    // ──────────────────────────────────────────────────────────────
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 先渲染背景面板（含原版遮罩）
        this.renderBackground(graphics);

        // 标题始终居中于面板宽度（使用自定义阴影渲染）
        drawShadowedCentered(graphics, this.title,
                panelX + panelWidth / 2, panelY + 8, 0xFFFFFFFF);

        // 重置悬停状态
        hoveredStage = null; hoveredGroupId = null; hoveredStageIdx = -1;

        // 读取配置中的行间距和进度条偏移
        DisplayConfig dc = ConfigManager.getDisplayConfig();
        int rowSpacing   = dc.rowSpacing;
        int barXOffset   = dc.barXOffset;

        // 遍历营养组，按行渲染
        List<GroupDefinition> groups = new ArrayList<>(ConfigManager.getGroups().values());
        for (int i = 0; i < groups.size(); i++) {
            GroupDefinition group = groups.get(i);
            float value = ClientNutritionData.getValue(group.id);
            int   rowY  = panelY + dc.paddingTop + i * rowSpacing;
            renderGroupRow(graphics, group, value, rowY, rowSpacing, barXOffset, mouseX, mouseY);
        }

        // 渲染子组件（按钮等）
        super.render(graphics, mouseX, mouseY, partialTick);

        // 悬停在确认按钮上 → 显示当前激活加成 Tooltip
        if (checkButton != null
                && mouseX >= checkButton.getX()
                && mouseX <= checkButton.getX() + checkButton.getWidth()
                && mouseY >= checkButton.getY()
                && mouseY <= checkButton.getY() + checkButton.getHeight()) {
            renderBonusTooltip(graphics, mouseX, mouseY);
        }

        // 悬停在阶段区域 → 显示阶段 Tooltip
        if (hoveredStage != null) {
            renderStageTooltip(graphics, stageTooltipX, stageTooltipY);
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  背景面板（3 段式，参考 Diet mod 的 renderBackground 实现）
    // ──────────────────────────────────────────────────────────────
    @Override
    public void renderBackground(GuiGraphics graphics) {
        // 原版全屏暗色遮罩
        super.renderBackground(graphics);

        // 上边条：固定 4px 高，UV(0,0) 248×4，横向拉伸至 panelWidth
        graphics.blit(BACKGROUND, panelX, panelY,
                panelWidth, 4,
                0, 0, 248, 4, 256, 256);

        // 中间主体：UV(0,4) 248×24，纵向拉伸至内容高度，横向拉伸至 panelWidth
        graphics.blit(BACKGROUND, panelX, panelY + 4,
                panelWidth, panelHeight - 8,
                0, 4, 248, 24, 256, 256);

        // 下边条：固定 4px 高，UV(0,162) 248×4，横向拉伸至 panelWidth
        graphics.blit(BACKGROUND, panelX, panelY + panelHeight - 4,
                panelWidth, 4,
                0, 162, 248, 4, 256, 256);
    }

    // ──────────────────────────────────────────────────────────────
    //  单行营养组渲染
    // ──────────────────────────────────────────────────────────────

    /**
     * 渲染一行：图标 → 组名 → 进度条（背景 + 填充）→ 百分比。
     * 进度条使用 coloredBlit 染色，与 Diet mod 实现完全一致。
     *
     * @param rowSpacing 行高（来自 display.json）
     * @param barXOffset 进度条距面板左侧偏移（来自 display.json）
     */
    private void renderGroupRow(GuiGraphics graphics, GroupDefinition group,
                                float value, int rowY, int rowSpacing, int barXOffset,
                                int mouseX, int mouseY) {
        // 解析组颜色 ARGB → 拆分 RGB 分量（0~255，供 coloredBlit 使用）
        int argb = group.getColorArgb();
        int cr   = (argb >> 16) & 0xFF;
        int cg   = (argb >> 8)  & 0xFF;
        int cb   =  argb        & 0xFF;

        // ── 图标（16×16，垂直居中） ───────────────────────────
        int iconX = panelX + 6;
        int iconY = rowY + (rowSpacing - 16) / 2;
        boolean iconOk = false;
        if (group.icon != null && !group.icon.isEmpty()) {
            try {
                Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(group.icon));
                if (item != null && item != Items.AIR) {
                    graphics.renderItem(new ItemStack(item), iconX, iconY);
                    iconOk = true;
                }
            } catch (Exception ignored) {}
        }
        if (!iconOk) {
            // 图标缺失：用组颜色填充 16×16 方块替代
            graphics.fill(iconX, iconY, iconX + 16, iconY + 16, argb);
        }

        // ── 组名（与进度条同色，垂直居中，使用自定义阴影） ──────
        int nameX = panelX + 26;
        int nameY = rowY + (rowSpacing - this.font.lineHeight) / 2;
        drawShadowed(graphics, group.displayName, nameX, nameY, argb);

        // ── 进度条（使用 coloredBlit，参考 Diet mod 实现） ────────
        int barX = panelX + barXOffset;

        // barYOffset 控制进度条在行内的垂直位置（支持小数，正值下移，负值上移）
        DisplayConfig dc = ConfigManager.getDisplayConfig();
        int barY = rowY + (rowSpacing - BAR_H) / 2 + Math.round(dc.barYOffset);

        // 切换到支持顶点色的纹理着色器（POSITION_COLOR_TEX）
        RenderSystem.setShader(GameRenderer::getPositionColorTexShader);

        // 1. 背景条：黑色材质以白色（255,255,255）渲染 → 保持原色（黑/灰）
        RenderSystem.setShaderTexture(0, BAR_BG);
        coloredBlit(graphics.pose(), barX, barY, BAR_W, BAR_H,
                0, 0, BAR_W, BAR_H, BAR_W, BAR_H,
                255, 255, 255, 255);

        // 2. 阶段分隔叠加（fill，半透明，不影响贴图）
        StageConfig stageCfg = ConfigManager.getStageConfig(group.id);
        if (stageCfg != null && !stageCfg.stages.isEmpty()) {
            for (int si = 0; si < stageCfg.stages.size(); si++) {
                StageConfig.Stage stage = stageCfg.stages.get(si);
                int sX = barX + (int)(stage.min / 100f * BAR_W);
                int eX = barX + (int)(stage.max / 100f * BAR_W);
                int band = (si % 2 == 0) ? 0x18FFFFFF : 0x10FFFFFF;
                graphics.fill(sX, barY, eX, barY + BAR_H, band);
                if (stage.min > 0) {
                    graphics.fill(sX, barY, sX + 1, barY + BAR_H, 0xFF777777);
                }
            }
        }

        // 3. 填充条：白色材质以组 RGB 染色 → 精确还原组颜色
        int fillW = (int)(value / 100f * BAR_W);
        if (fillW > 0) {
            RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
            RenderSystem.setShaderTexture(0, BAR_FILL);
            coloredBlit(graphics.pose(), barX, barY, fillW, BAR_H,
                    0, 0, fillW, BAR_H, BAR_W, BAR_H,
                    cr, cg, cb, 255);
        }

        // ── 百分比（紧贴进度条右侧，与进度条同色，使用自定义阴影） ──
        String pctText = String.format("%.1f%%", value);
        int pctX = barX + BAR_W + 4;
        int pctY = rowY + (rowSpacing - this.font.lineHeight) / 2;
        drawShadowed(graphics, pctText, pctX, pctY, argb);

        // ── 阶段悬停检测（含白色边框高亮） ─────────────────────────
        if (mouseX >= barX && mouseX < barX + BAR_W
                && mouseY >= barY && mouseY < barY + BAR_H) {
            float hoverPct = (float)(mouseX - barX) / BAR_W * 100f;
            if (stageCfg != null) {
                int si = stageCfg.getStageIndex(hoverPct);
                if (si >= 0) {
                    StageConfig.Stage stage = stageCfg.stages.get(si);

                    // 计算此阶段在进度条上的像素范围
                    int sX = barX + (int)(stage.min / 100f * BAR_W);
                    int eX = barX + (int)(stage.max / 100f * BAR_W);

                    // 渲染纯白色边框，包围此阶段区间（上/下/左/右各 1px）
                    graphics.fill(sX,     barY - 1,         eX,     barY,              0xFFFFFFFF); // 上边
                    graphics.fill(sX,     barY + BAR_H,     eX,     barY + BAR_H + 1,  0xFFFFFFFF); // 下边
                    graphics.fill(sX,     barY - 1,         sX + 1, barY + BAR_H + 1,  0xFFFFFFFF); // 左边
                    graphics.fill(eX - 1, barY - 1,         eX,     barY + BAR_H + 1,  0xFFFFFFFF); // 右边

                    hoveredStage    = stage;
                    hoveredGroupId  = group.id;
                    hoveredStageIdx = si;
                    stageTooltipX   = mouseX;
                    stageTooltipY   = mouseY;
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  鼠标点击
    // ──────────────────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && hoveredStage != null
                && hoveredGroupId != null && hoveredStageIdx >= 0) {
            if (!hoveredStage.commands.isEmpty()) {
                HealthyDietNetwork.sendToServer(
                        new CPacketStageCommand(hoveredGroupId, hoveredStageIdx));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // ──────────────────────────────────────────────────────────────
    //  Tooltip 渲染
    // ──────────────────────────────────────────────────────────────

    /** 显示当前所有满足条件的属性 / 药水加成 Tooltip */
    private void renderBonusTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("tooltip.healthy_diet.active_bonuses")
                .withStyle(ChatFormatting.YELLOW));
        boolean any = false;

        for (Map.Entry<String, GroupBonusConfig> e : ConfigManager.getAllGroupBonuses().entrySet()) {
            GroupBonusConfig cfg = e.getValue();
            if (cfg == null) continue;
            for (GroupBonusConfig.BonusEntry bonus : cfg.bonuses) {
                boolean met = true;
                for (Map.Entry<String, GroupBonusConfig.RangeCondition> cond :
                        bonus.conditions.entrySet()) {
                    float v = ClientNutritionData.getValue(cond.getKey());
                    if (v < cond.getValue().min || v > cond.getValue().max) { met = false; break; }
                }
                if (!met) continue;
                any = true;
                for (GroupBonusConfig.AttributeBonus ab : bonus.attributeBonuses) {
                    String name = ab.attribute;
                    try {
                        Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(
                                ResourceLocation.tryParse(ab.attribute));
                        if (attr != null) name = Component.translatable(attr.getDescriptionId()).getString();
                    } catch (Exception ignored) {}
                    lines.add(Component.literal("  +" + ab.value + " " + name)
                            .withStyle(ChatFormatting.GREEN));
                }
                for (GroupBonusConfig.EffectBonus eb : bonus.effectBonuses) {
                    String name = eb.effect;
                    try {
                        MobEffect eff = ForgeRegistries.MOB_EFFECTS.getValue(
                                ResourceLocation.tryParse(eb.effect));
                        if (eff != null) name = Component.translatable(eff.getDescriptionId()).getString();
                    } catch (Exception ignored) {}
                    lines.add(Component.literal("  " + name + " Lv." + (eb.level + 1))
                            .withStyle(ChatFormatting.AQUA));
                }
            }
        }
        if (!any) {
            lines.add(Component.translatable("tooltip.healthy_diet.no_active_bonuses")
                    .withStyle(ChatFormatting.GRAY));
        }
        graphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
    }

    /** 阶段详情 Tooltip */
    private void renderStageTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (hoveredStage == null) return;
        List<Component> lines = new ArrayList<>();
        if (!hoveredStage.label.isEmpty()) {
            lines.add(Component.literal(hoveredStage.label).withStyle(ChatFormatting.YELLOW));
        }
        for (String line : hoveredStage.tooltip) {
            lines.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
        }
        if (!hoveredStage.commands.isEmpty()) {
            lines.add(Component.translatable("tooltip.healthy_diet.stage_click_hint")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
        if (!lines.isEmpty()) {
            graphics.renderComponentTooltip(this.font, lines, mouseX, mouseY - 4);
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  coloredBlit（参考 Diet mod DietScreen 完整移植）
    //
    //  与 GuiGraphics.blit() 不同，此方法将颜色信息直接写入顶点
    //  （POSITION_COLOR_TEX 格式），完全不依赖 RenderSystem.setShaderColor，
    //  因此不会出现"只渲染 1 像素"或染色失效的问题。
    //
    //  参数说明：
    //    poseStack     — 当前变换矩阵栈（由 guiGraphics.pose() 传入）
    //    x, y          — 屏幕左上角坐标
    //    width, height — 渲染尺寸（可与 uWidth/vHeight 不同，实现缩放）
    //    uOffset       — 纹理 U 起点（像素）
    //    vOffset       — 纹理 V 起点（像素）
    //    uWidth        — 纹理采样宽度（像素）
    //    vHeight       — 纹理采样高度（像素）
    //    textureWidth  — 纹理总宽度（像素，用于归一化 UV）
    //    textureHeight — 纹理总高度（像素，用于归一化 UV）
    //    red,green,blue,alpha — 顶点颜色（0~255），白色材质 × RGB = 该颜色
    // ──────────────────────────────────────────────────────────────
    private static void coloredBlit(PoseStack poseStack,
                                    int x, int y, int width, int height,
                                    float uOffset, float vOffset,
                                    int uWidth, int vHeight,
                                    int textureWidth, int textureHeight,
                                    int red, int green, int blue, int alpha) {
        int x2 = x + width;
        int y2 = y + height;
        // 将像素坐标归一化为 0~1 UV
        float minU = uOffset                    / (float) textureWidth;
        float maxU = (uOffset + (float) uWidth)  / (float) textureWidth;
        float minV = vOffset                    / (float) textureHeight;
        float maxV = (vOffset + (float) vHeight) / (float) textureHeight;

        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        // POSITION_COLOR_TEX：每个顶点携带位置、RGBA 颜色、UV 坐标
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        buf.vertex(matrix, (float) x,  (float) y2, 0).color(red, green, blue, alpha).uv(minU, maxV).endVertex();
        buf.vertex(matrix, (float) x2, (float) y2, 0).color(red, green, blue, alpha).uv(maxU, maxV).endVertex();
        buf.vertex(matrix, (float) x2, (float) y,  0).color(red, green, blue, alpha).uv(maxU, minV).endVertex();
        buf.vertex(matrix, (float) x,  (float) y,  0).color(red, green, blue, alpha).uv(minU, minV).endVertex();
        BufferUploader.drawWithShader(buf.end());
    }

    // ──────────────────────────────────────────────────────────────
    //  自定义阴影文字渲染辅助
    //
    //  阴影逻辑：先用 shadowColor 在 (x+offsetX, y+offsetY) 渲染一遍文字，
    //  再用原色在 (x, y) 覆盖渲染。所有参数均从 display.json 读取。
    // ──────────────────────────────────────────────────────────────

    /**
     * 带阴影的字符串渲染（String 版）。
     * 阴影开关与偏移由 DisplayConfig 控制。
     * offsetX/Y 支持小数：通过 PoseStack.translate 实现亚像素平移，
     * 再在 (x, y) 整数坐标绘制文字，最后恢复矩阵。
     */
    private void drawShadowed(GuiGraphics g, String text, int x, int y, int color) {
        DisplayConfig dc = ConfigManager.getDisplayConfig();
        if (dc.showTextShadow) {
            // 推入新矩阵，平移至阴影偏移位置（支持小数）
            g.pose().pushPose();
            g.pose().translate(dc.shadowOffsetX, dc.shadowOffsetY, 0f);
            g.drawString(this.font, text, x, y, dc.getShadowColorArgb(), false);
            g.pose().popPose();
        }
        // 在原坐标渲染主体文字覆盖阴影层
        g.drawString(this.font, text, x, y, color, false);
    }

    /**
     * 带阴影的居中 Component 渲染（Component 版）。
     * cx 为水平中心坐标，自动计算左起点。
     */
    private void drawShadowedCentered(GuiGraphics g, Component comp, int cx, int y, int color) {
        int x = cx - this.font.width(comp) / 2;
        DisplayConfig dc = ConfigManager.getDisplayConfig();
        if (dc.showTextShadow) {
            g.pose().pushPose();
            g.pose().translate(dc.shadowOffsetX, dc.shadowOffsetY, 0f);
            g.drawString(this.font, comp, x, y, dc.getShadowColorArgb(), false);
            g.pose().popPose();
        }
        g.drawString(this.font, comp, x, y, color, false);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}

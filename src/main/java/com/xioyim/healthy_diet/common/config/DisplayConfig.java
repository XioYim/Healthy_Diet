package com.xioyim.healthy_diet.common.config;

/**
 * 营养界面显示配置，对应 config/healthy_diet/display.json。
 * Nutrition UI display config, mapped to config/healthy_diet/display.json.
 *
 * _note_xxx 字段为只读说明，修改无效。
 * _note_xxx fields are read-only descriptions; editing them has no effect.
 */
public class DisplayConfig {

    // ══════════════════════════════════════════════════════════════
    //  物品描述显示 / Item tooltip display
    // ══════════════════════════════════════════════════════════════

    public String _note_showNutritionLore =
            "是否在物品提示框显示营养值 | Show nutrition values in item tooltips";
    public boolean showNutritionLore = true;

    public String _note_showCooldownInfoLore =
            "是否显示食物厌倦冷却说明（最大食用次数等）| Show food fatigue cooldown info (max uses, etc.)";
    public boolean showCooldownInfoLore = true;

    public String _note_showCooldownStatusLore =
            "是否显示当前厌倦状态（已食用次数、剩余冷却）| Show current fatigue status (eat count, remaining cooldown)";
    public boolean showCooldownStatusLore = true;

    // ══════════════════════════════════════════════════════════════
    //  面板布局 / Panel layout
    // ══════════════════════════════════════════════════════════════

    public String _note_panelWidth =
            "面板宽度（像素），始终水平居中 | Panel width in pixels, always centered horizontally";
    public int panelWidth = 230;

    public String _note_paddingTop =
            "顶部内边距（像素）：标题文字区高度 | Top padding in pixels: height of the title area";
    public int paddingTop = 22;

    public String _note_paddingBottom =
            "底部内边距（像素）：按钮区高度 | Bottom padding in pixels: height of the button area";
    public int paddingBottom = 28;

    public String _note_rowSpacing =
            "每行营养组的高度（像素），建议 16~28 | Height of each row in pixels, recommended 16~28";
    public int rowSpacing = 20;

    // ══════════════════════════════════════════════════════════════
    //  进度条 / Progress bar
    // ══════════════════════════════════════════════════════════════

    public String _note_barXOffset =
            "进度条左边缘距面板左边的偏移（像素），即图标+组名列宽度，调大可为组名留出更多空间 | X offset of the bar from panel left edge (icon+name column width); increase for wider name column";
    public int barXOffset = 88;

    public String _note_barYOffset =
            "进度条垂直位置偏移（像素，支持小数），正值向下，负值向上 | Vertical offset of the bar in pixels (decimals OK); positive = down, negative = up";
    public float barYOffset = -0.7f;

    // ══════════════════════════════════════════════════════════════
    //  文字阴影 / Text shadow
    // ══════════════════════════════════════════════════════════════

    public String _note_showTextShadow =
            "是否启用 UI 文字阴影 | Whether to enable UI text shadow";
    public boolean showTextShadow = true;

    public String _note_shadowColor =
            "阴影颜色，十六进制含 # 前缀，如 #555555（深灰）、#000000（黑）| Shadow color in hex with # prefix, e.g. #555555 (dark gray), #000000 (black)";
    public String shadowColor = "#555555";

    public String _note_shadowOffsetX =
            "阴影 X 轴偏移（像素，支持小数），正值向右，负值向左 | Shadow X offset in pixels (decimals OK); positive = right, negative = left";
    public float shadowOffsetX = 0.3f;

    public String _note_shadowOffsetY =
            "阴影 Y 轴偏移（像素，支持小数），正值向下，负值向上 | Shadow Y offset in pixels (decimals OK); positive = down, negative = up";
    public float shadowOffsetY = 0.3f;

    // ══════════════════════════════════════════════════════════════
    //  内部工具 / Internal utility
    // ══════════════════════════════════════════════════════════════

    /** 将 shadowColor 十六进制字符串转为不透明 ARGB int，解析失败回退 #555555。*/
    public int getShadowColorArgb() {
        try {
            String hex = shadowColor.startsWith("#") ? shadowColor.substring(1) : shadowColor;
            int rgb = Integer.parseInt(hex, 16);
            return 0xFF000000 | rgb;
        } catch (NumberFormatException e) {
            return 0xFF555555;
        }
    }
}

package com.xioyim.healthy_diet.common.config;

public class GroupDefinition {
    public String id;
    public String displayName = "Group";
    public String color = "#FFFFFF";
    public double decayRate = 6.0;
    public String icon = "";

    public int getColorArgb() {
        try {
            String hex = color.startsWith("#") ? color.substring(1) : color;
            int rgb = Integer.parseInt(hex, 16);
            return 0xFF000000 | rgb;
        } catch (NumberFormatException e) {
            return 0xFFFFFFFF;
        }
    }
}

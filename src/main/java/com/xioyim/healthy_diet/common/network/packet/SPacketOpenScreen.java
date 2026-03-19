package com.xioyim.healthy_diet.common.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;

public class SPacketOpenScreen {

    public static void encode(SPacketOpenScreen msg, FriendlyByteBuf buf) {}

    public static SPacketOpenScreen decode(FriendlyByteBuf buf) {
        return new SPacketOpenScreen();
    }

    public static void handle(SPacketOpenScreen msg) {
        Minecraft.getInstance().execute(() -> {
            Minecraft mc = Minecraft.getInstance();
            mc.setScreen(new com.xioyim.healthy_diet.client.screen.HealthyDietScreen());
        });
    }
}

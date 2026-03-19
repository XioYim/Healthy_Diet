package com.xioyim.healthy_diet.common.network;

import com.xioyim.healthy_diet.HealthyDietConstants;
import com.xioyim.healthy_diet.common.network.packet.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class HealthyDietNetwork {
    private static final String PTC_VERSION = "2";
    public static SimpleChannel INSTANCE;
    private static int id = 0;

    public static void setup() {
        INSTANCE = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(HealthyDietConstants.MOD_ID, "main"))
                .networkProtocolVersion(() -> PTC_VERSION)
                .clientAcceptedVersions(PTC_VERSION::equals)
                .serverAcceptedVersions(PTC_VERSION::equals)
                .simpleChannel();

        // S -> C packets
        registerS2C(SPacketNutrition.class, SPacketNutrition::encode, SPacketNutrition::decode, SPacketNutrition::handle);
        registerS2C(SPacketOpenScreen.class, SPacketOpenScreen::encode, SPacketOpenScreen::decode, SPacketOpenScreen::handle);
        // C -> S packets
        registerC2S(CPacketStageCommand.class, CPacketStageCommand::encode, CPacketStageCommand::decode, CPacketStageCommand::handle);
    }

    private static <M> void registerS2C(Class<M> clazz, BiConsumer<M, FriendlyByteBuf> encoder,
                                         Function<FriendlyByteBuf, M> decoder, Consumer<M> handler) {
        INSTANCE.registerMessage(id++, clazz, encoder, decoder, (msg, ctx) -> {
            NetworkEvent.Context c = ctx.get();
            c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handler.accept(msg)));
            c.setPacketHandled(true);
        });
    }

    private static <M> void registerC2S(Class<M> clazz, BiConsumer<M, FriendlyByteBuf> encoder,
                                         Function<FriendlyByteBuf, M> decoder,
                                         BiConsumer<M, Supplier<NetworkEvent.Context>> handler) {
        INSTANCE.registerMessage(id++, clazz, encoder, decoder, handler::accept);
    }

    public static <M> void sendToPlayer(ServerPlayer player, M message) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static <M> void sendToServer(M message) {
        INSTANCE.sendToServer(message);
    }
}

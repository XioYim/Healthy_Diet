package com.xioyim.healthy_diet.common.network.packet;

import com.xioyim.healthy_diet.common.config.ConfigManager;
import com.xioyim.healthy_diet.common.config.StageConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CPacketStageCommand {
    private final String groupId;
    private final int stageIndex;

    public CPacketStageCommand(String groupId, int stageIndex) {
        this.groupId = groupId;
        this.stageIndex = stageIndex;
    }

    public static void encode(CPacketStageCommand msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.groupId);
        buf.writeInt(msg.stageIndex);
    }

    public static CPacketStageCommand decode(FriendlyByteBuf buf) {
        return new CPacketStageCommand(buf.readUtf(), buf.readInt());
    }

    public static void handle(CPacketStageCommand msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            StageConfig stageConfig = ConfigManager.getStageConfig(msg.groupId);
            if (stageConfig == null || msg.stageIndex < 0 || msg.stageIndex >= stageConfig.stages.size()) return;
            StageConfig.Stage stage = stageConfig.stages.get(msg.stageIndex);
            net.minecraft.server.MinecraftServer server = player.getServer();
            if (server == null) return;
            net.minecraft.commands.CommandSourceStack source = player.createCommandSourceStack().withPermission(4).withSuppressedOutput();
            for (String cmd : stage.commands) {
                String command = cmd.startsWith("/") ? cmd.substring(1) : cmd;
                server.getCommands().performPrefixedCommand(source, command);
            }
        });
        ctx.setPacketHandled(true);
    }
}

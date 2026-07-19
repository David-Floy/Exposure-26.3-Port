package io.github.mortuusars.exposure.network.fabric;

import io.github.mortuusars.exposure.network.packet.C2SPackets;
import io.github.mortuusars.exposure.network.packet.CommonPackets;
import io.github.mortuusars.exposure.network.packet.Packet;
import io.github.mortuusars.exposure.network.packet.PacketDefinition;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.PacketFlow;

public class FabricC2SPackets {
    public static void register() {
        for (var definition : C2SPackets.getDefinitions()) {
            register(definition);
        }

        for (var definition : CommonPackets.getDefinitions()) {
            register(definition);
        }
    }

    private static <T extends Packet> void register(PacketDefinition<T> definition) {
        PayloadTypeRegistry.serverboundPlay().register(definition.type(), definition.codec());
        ServerPlayNetworking.registerGlobalReceiver(definition.type(),
                (payload, context) -> payload.handle(PacketFlow.SERVERBOUND, context.player()));
    }
}

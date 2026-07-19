package io.github.mortuusars.exposure.network.fabric;

import io.github.mortuusars.exposure.network.packet.CommonPackets;
import io.github.mortuusars.exposure.network.packet.Packet;
import io.github.mortuusars.exposure.network.packet.PacketDefinition;
import io.github.mortuusars.exposure.network.packet.S2CPackets;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class FabricS2CPackets {
    public static void register() {
        for (var definition : S2CPackets.getDefinitions()) {
            register(definition);
        }

        for (var definition : CommonPackets.getDefinitions()) {
            register(definition);
        }
    }

    private static <T extends Packet> void register(PacketDefinition<T> definition) {
        PayloadTypeRegistry.clientboundPlay().register(definition.type(), definition.codec());
    }
}

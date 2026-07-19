package io.github.mortuusars.exposure.network.packet;

import io.github.mortuusars.exposure.network.packet.common.ActiveCameraDeactivateCommonPacket;

import java.util.List;

public class CommonPackets {
    public static List<PacketDefinition<? extends Packet>> getDefinitions() {
        return List.of(
                new PacketDefinition<>(ActiveCameraDeactivateCommonPacket.TYPE, ActiveCameraDeactivateCommonPacket.STREAM_CODEC)
        );
    }
}

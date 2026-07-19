package io.github.mortuusars.exposure.network.fabric;

import io.github.mortuusars.exposure.network.packet.Packet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

@Environment(EnvType.CLIENT)
public final class FabricClientPacketSender {
    private FabricClientPacketSender() {
    }

    public static void send(Packet packet) {
        ClientPlayNetworking.send(packet);
    }
}

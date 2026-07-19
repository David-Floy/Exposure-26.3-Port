package io.github.mortuusars.exposure.network;

import io.github.mortuusars.exposure.network.packet.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class Packets {
    private static volatile Service service;

    public static void bind(Service implementation) {
        if (service != null) {
            throw new IllegalStateException("Exposure packet service is already bound.");
        }
        service = java.util.Objects.requireNonNull(implementation, "implementation");
    }

    private static Service service() {
        Service implementation = service;
        if (implementation == null) {
            throw new IllegalStateException("Exposure packet service has not been bound by the Fabric entrypoint.");
        }
        return implementation;
    }

    public static void sendToServer(Packet packet) {
        service().sendToServer(packet);
    }

    public static void sendToClient(Packet packet, ServerPlayer player) {
        service().sendToClient(packet, player);
    }

    public static void sendToClients(Packet packet, Predicate<ServerPlayer> filter) {
        service().sendToClients(packet, filter);
    }

    public static void sendToAllClients(Packet packet) {
        service().sendToAllClients(packet);
    }

    public static void sendToPlayersNear(Packet packet, ServerLevel level, @Nullable ServerPlayer excluded,
                                         double x, double y, double z, double radius) {
        service().sendToPlayersNear(packet, level, excluded, x, y, z, radius);
    }

    // --

    public static void sendToOtherClients(@NotNull ServerPlayer except, Packet packet) {
        except.level().getServer().getPlayerList().getPlayers().forEach(player -> {
            if (!player.equals(except)) {
                sendToClient(packet, player);
            }
        });
    }

    public static void sendToPlayersNear(Packet packet, ServerLevel level, @Nullable ServerPlayer excluded,
                                         Entity entity, double radius) {
        sendToPlayersNear(packet, level, excluded, entity.getX(), entity.getY(), entity.getZ(), radius);
    }

    public interface Service {
        void sendToServer(Packet packet);
        void sendToClient(Packet packet, ServerPlayer player);
        void sendToClients(Packet packet, Predicate<ServerPlayer> filter);
        void sendToAllClients(Packet packet);
        void sendToPlayersNear(Packet packet, ServerLevel level, @Nullable ServerPlayer excluded,
                               double x, double y, double z, double radius);
    }
}

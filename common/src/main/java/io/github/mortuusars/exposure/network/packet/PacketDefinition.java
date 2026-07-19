package io.github.mortuusars.exposure.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * A play-stage payload type and its codec.
 *
 * <p>The codec accepts {@link RegistryFriendlyByteBuf} or any of its buffer supertypes. This keeps
 * registry-independent payloads on {@code FriendlyByteBuf} while allowing registry-aware payloads
 * to use the play protocol's registry buffer without unchecked casts.</p>
 */
public record PacketDefinition<T extends Packet>(
        CustomPacketPayload.Type<T> type,
        StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
}

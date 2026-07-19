package io.github.mortuusars.exposure.network.packet;

import io.github.mortuusars.exposure.network.packet.serverbound.*;

import java.util.List;

public class C2SPackets {
    public static List<PacketDefinition<? extends Packet>> getDefinitions() {
        return List.of(
                new PacketDefinition<>(AlbumSignC2SP.TYPE, AlbumSignC2SP.STREAM_CODEC),
                new PacketDefinition<>(AlbumSyncNoteC2SP.TYPE, AlbumSyncNoteC2SP.STREAM_CODEC),
                new PacketDefinition<>(ActiveCameraSetSettingC2SP.TYPE, ActiveCameraSetSettingC2SP.STREAM_CODEC),
                new PacketDefinition<>(OpenCameraAttachmentsInCreativePacketC2SP.TYPE, OpenCameraAttachmentsInCreativePacketC2SP.STREAM_CODEC),
                new PacketDefinition<>(ExposureRequestC2SP.TYPE, ExposureRequestC2SP.STREAM_CODEC),
                new PacketDefinition<>(ActiveCameraReleaseC2SP.TYPE, ActiveCameraReleaseC2SP.STREAM_CODEC),
                new PacketDefinition<>(InterplanarProjectionFinishedC2SP.TYPE, InterplanarProjectionFinishedC2SP.STREAM_CODEC),
                new PacketDefinition<>(ExposureDataC2SP.TYPE, ExposureDataC2SP.STREAM_CODEC),
                new PacketDefinition<>(CameraStandTurnC2SP.TYPE, CameraStandTurnC2SP.STREAM_CODEC)
        );
    }
}

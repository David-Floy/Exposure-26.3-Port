package io.github.mortuusars.exposure.network.packet;

import io.github.mortuusars.exposure.network.packet.clientbound.*;
import java.util.List;

public class S2CPackets {
    public static List<PacketDefinition<? extends Packet>> getDefinitions() {
        return List.of(
                new PacketDefinition<>(ActiveCameraRemoveS2CP.TYPE, ActiveCameraRemoveS2CP.STREAM_CODEC),
                new PacketDefinition<>(ActiveCameraInHandSetS2CP.TYPE, ActiveCameraInHandSetS2CP.STREAM_CODEC),
                new PacketDefinition<>(ActiveCameraOnStandSetS2CP.TYPE, ActiveCameraOnStandSetS2CP.STREAM_CODEC),
                new PacketDefinition<>(CameraStandSetRotationsS2CP.TYPE, CameraStandSetRotationsS2CP.STREAM_CODEC),
                new PacketDefinition<>(CameraStandStopControllingS2CP.TYPE, CameraStandStopControllingS2CP.STREAM_CODEC),
                new PacketDefinition<>(ShaderApplyS2CP.TYPE, ShaderApplyS2CP.STREAM_CODEC),
                new PacketDefinition<>(ClearRenderingCacheS2CP.TYPE, ClearRenderingCacheS2CP.STREAM_CODEC),
                new PacketDefinition<>(CreateChromaticExposureS2CP.TYPE, CreateChromaticExposureS2CP.STREAM_CODEC),
                new PacketDefinition<>(ExposureDataChangedS2CP.TYPE, ExposureDataChangedS2CP.STREAM_CODEC),
                new PacketDefinition<>(UniqueSoundPlayS2CP.TYPE, UniqueSoundPlayS2CP.STREAM_CODEC),
                new PacketDefinition<>(UniqueSoundPlayShutterTickingS2CP.TYPE, UniqueSoundPlayShutterTickingS2CP.STREAM_CODEC),
                new PacketDefinition<>(UniqueSoundStopS2CP.TYPE, UniqueSoundStopS2CP.STREAM_CODEC),
                new PacketDefinition<>(ShowExposureCommandS2CP.TYPE, ShowExposureCommandS2CP.STREAM_CODEC),
                new PacketDefinition<>(ExposureDataResponseS2CP.TYPE, ExposureDataResponseS2CP.STREAM_CODEC),
                new PacketDefinition<>(ShutterOpenedS2CP.TYPE, ShutterOpenedS2CP.STREAM_CODEC),
                new PacketDefinition<>(CaptureStartS2CP.TYPE, CaptureStartS2CP.STREAM_CODEC),
                new PacketDefinition<>(CaptureStartDebugRGBS2CP.TYPE, CaptureStartDebugRGBS2CP.STREAM_CODEC),
                new PacketDefinition<>(ExportS2CP.TYPE, ExportS2CP.STREAM_CODEC),
                new PacketDefinition<>(ExportStopS2CP.TYPE, ExportStopS2CP.STREAM_CODEC)
        );
    }
}

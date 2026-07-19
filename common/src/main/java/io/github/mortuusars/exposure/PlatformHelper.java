package io.github.mortuusars.exposure;

import io.github.mortuusars.exposure.util.ExtraData;
import io.github.mortuusars.exposure.world.camera.capture.CaptureParameters;
import io.github.mortuusars.exposure.world.camera.frame.Frame;
import io.github.mortuusars.exposure.world.entity.CameraHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class PlatformHelper {
    private static volatile Service service;

    public static void bind(Service implementation) {
        if (service != null) {
            throw new IllegalStateException("Exposure platform service is already bound.");
        }
        service = java.util.Objects.requireNonNull(implementation, "implementation");
    }

    private static Service service() {
        Service implementation = service;
        if (implementation == null) {
            throw new IllegalStateException("Exposure platform service has not been bound by the Fabric entrypoint.");
        }
        return implementation;
    }

    public static @Nullable MinecraftServer getServer() {
        return service().getServer();
    }

    public static boolean canShear(ItemStack stack) {
        return service().canShear(stack);
    }

    public static boolean canStrip(ItemStack stack) {
        return service().canStrip(stack);
    }

    public static void openMenu(ServerPlayer serverPlayer, MenuProvider menuProvider, Consumer<RegistryFriendlyByteBuf> extraDataWriter) {
        service().openMenu(serverPlayer, menuProvider, extraDataWriter);
    }

    public static List<String> getDefaultSpoutDevelopmentColorSequence() {
        return service().getDefaultSpoutDevelopmentColorSequence();
    }

    public static List<String> getDefaultSpoutDevelopmentBWSequence() {
        return service().getDefaultSpoutDevelopmentBWSequence();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isModLoaded(String modId) {
        return service().isModLoaded(modId);
    }

    public static boolean isModLoading(String modId) {
        return service().isModLoading(modId);
    }

    public static boolean isInDevEnv() {
        return service().isInDevEnv();
    }

    // --

    public static void postModifyEntityInFrameExtraDataEvent(CameraHolder cameraHolder, ItemStack camera, LivingEntity entityInFrame, ExtraData data) {
        service().postModifyEntityInFrameExtraDataEvent(cameraHolder, camera, entityInFrame, data);
    }

    public static void postModifyFrameExtraDataEvent(CameraHolder cameraHolder, ItemStack camera, CaptureParameters captureParameters,
                                                     List<BlockPos> positionsInFrame, List<LivingEntity> entitiesInFrame, ExtraData data) {
        service().postModifyFrameExtraDataEvent(cameraHolder, camera, captureParameters, positionsInFrame, entitiesInFrame, data);
    }

    public static void postFrameAddedEvent(CameraHolder holder, ItemStack camera, Frame frame,
                                           List<BlockPos> positionsInFrame, List<LivingEntity> entitiesInFrame) {
        service().postFrameAddedEvent(holder, camera, frame, positionsInFrame, entitiesInFrame);
    }

    public static boolean isCreateDeployer(Player player, InteractionHand hand) {
        return service().isCreateDeployer(player, hand);
    }

    public interface Service {
        @Nullable MinecraftServer getServer();
        boolean canShear(ItemStack stack);
        boolean canStrip(ItemStack stack);
        void openMenu(ServerPlayer serverPlayer, MenuProvider menuProvider, Consumer<RegistryFriendlyByteBuf> extraDataWriter);
        List<String> getDefaultSpoutDevelopmentColorSequence();
        List<String> getDefaultSpoutDevelopmentBWSequence();
        boolean isModLoaded(String modId);
        boolean isModLoading(String modId);
        boolean isInDevEnv();
        void postModifyEntityInFrameExtraDataEvent(CameraHolder cameraHolder, ItemStack camera, LivingEntity entityInFrame, ExtraData data);
        void postModifyFrameExtraDataEvent(CameraHolder cameraHolder, ItemStack camera, CaptureParameters captureParameters,
                                           List<BlockPos> positionsInFrame, List<LivingEntity> entitiesInFrame, ExtraData data);
        void postFrameAddedEvent(CameraHolder holder, ItemStack camera, Frame frame,
                                 List<BlockPos> positionsInFrame, List<LivingEntity> entitiesInFrame);
        boolean isCreateDeployer(Player player, InteractionHand hand);
    }
}

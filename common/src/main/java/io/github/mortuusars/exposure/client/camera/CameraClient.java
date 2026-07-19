package io.github.mortuusars.exposure.client.camera;

import io.github.mortuusars.exposure.client.camera.viewfinder.Viewfinder;
import io.github.mortuusars.exposure.client.camera.viewfinder.ViewfinderRegistry;
import io.github.mortuusars.exposure.client.render.FovModifier;
import io.github.mortuusars.exposure.client.util.Minecrft;
import io.github.mortuusars.exposure.world.camera.Camera;
import io.github.mortuusars.exposure.network.Packets;
import io.github.mortuusars.exposure.network.packet.common.ActiveCameraDeactivateCommonPacket;
import io.github.mortuusars.exposure.world.camera.CameraOnStand;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class CameraClient {
    private static @Nullable Viewfinder activeViewfinder;

    public static void tick() {
        if (activeViewfinder != null) {
            Minecraft minecraft = Minecrft.get();
            if (minecraft.player == null || minecraft.level == null) {
                resetClientState();
                return;
            }
            if (!minecraft.player.isAlive() || !activeViewfinder.camera().isActive()) {
                deactivate();
                return;
            }
            activeViewfinder.tick();
        }
    }

    public static Optional<Camera> getActive() {
        return Minecrft.player().getActiveExposureCameraOptional();
    }

    public static boolean isActive() {
        return getActive().isPresent();
    }

    public static void deactivate() {
        Minecrft.player().getActiveExposureCameraOptional().ifPresent(camera -> {
            camera.map((item, stack) -> item.deactivate(camera.getHolder().asHolderEntity(), stack));
            Minecrft.player().removeActiveExposureCamera();
        });
        Packets.sendToServer(ActiveCameraDeactivateCommonPacket.INSTANCE);
    }

    public static void setCameraEntity(Entity entity) {
        // Do not use Minecraft#setCameraEntity: it also changes the entity post effect.
        // Camera owns the render entity directly in 26.1.2.
        Minecrft.get().gameRenderer.getMainCamera().setEntity(entity);

        // Set eye height to final value to skip transition animation
        Minecrft.get().gameRenderer.getMainCamera().eyeHeight = entity.getEyeHeight();
        Minecrft.get().gameRenderer.getMainCamera().eyeHeightOld = entity.getEyeHeight();
    }

    public static void resetCameraEntity() {
        setCameraEntity(Minecrft.player());
    }

    // --

    public static @Nullable Viewfinder viewfinder() {
        return activeViewfinder;
    }

    public static void setupViewfinder(@NotNull Camera camera) {
        removeViewfinder();
        activeViewfinder = ViewfinderRegistry.get(camera);

        if (camera instanceof CameraOnStand) {
            Minecrft.options().setCameraType(CameraType.FIRST_PERSON);
        }
    }

    public static void removeViewfinder() {
        if (activeViewfinder != null) {
            activeViewfinder.close();
            activeViewfinder = null;
        }
    }

    /** Clears client-only camera state without sending packets during a world transition. */
    public static void resetClientState() {
        removeViewfinder();
        FovModifier.reset();

        Minecraft minecraft = Minecrft.get();
        if (minecraft.player != null) {
            setCameraEntity(minecraft.player);
        }
    }
}

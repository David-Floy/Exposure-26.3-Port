package io.github.mortuusars.exposure.client.capture.action;

import io.github.mortuusars.exposure.client.camera.CameraClient;
import io.github.mortuusars.exposure.client.util.Minecrft;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public class SetCameraEntityAction implements CaptureAction {
    private final Entity cameraEntity;
    private @Nullable Entity cameraEntityBeforeCapture;

    public SetCameraEntityAction(Entity cameraEntity) {
        this.cameraEntity = cameraEntity;
        this.cameraEntityBeforeCapture = Minecrft.player();
    }

    @Override
    public void beforeCapture() {
        cameraEntityBeforeCapture = Minecrft.get().getCameraEntity();
        CameraClient.setCameraEntity(cameraEntity);
    }

    @Override
    public void afterCapture() {
        Minecraft minecraft = Minecrft.get();
        if (minecraft.level == null) {
            return;
        }

        Entity entityToRestore = cameraEntityBeforeCapture;
        if (entityToRestore == null
                || entityToRestore.isRemoved()
                || entityToRestore.level() != minecraft.level) {
            entityToRestore = minecraft.player;
        }

        if (entityToRestore != null) {
            CameraClient.setCameraEntity(entityToRestore);
        }
    }
}

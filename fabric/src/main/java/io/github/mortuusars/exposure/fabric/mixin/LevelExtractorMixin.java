package io.github.mortuusars.exposure.fabric.mixin;

import io.github.mortuusars.exposure.client.util.Minecrft;
import io.github.mortuusars.exposure.world.entity.CameraStandEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 26.2 split level extraction out of {@code LevelRenderer}; {@code extractVisibleEntities} now lives here.
 */
@Mixin(LevelExtractor.class)
public abstract class LevelExtractorMixin {
    @Shadow
    protected abstract EntityRenderState extractEntity(Entity entity, float partialTickTime);

    @Inject(
            method = "extractVisibleEntities(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;Lnet/minecraft/client/DeltaTracker;Lnet/minecraft/client/renderer/state/level/LevelRenderState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;entitiesForRendering()Ljava/lang/Iterable;"))
    private void exposure$extractPlayerWhenUsingCameraStand(Camera camera, Frustum frustum, DeltaTracker deltaTracker,
                                                              LevelRenderState levelRenderState, CallbackInfo ci) {
        if (camera.entity() instanceof CameraStandEntity) {
            exposure$extractEntity(deltaTracker, levelRenderState, Minecrft.player());
        }
    }

    @Unique
    private void exposure$extractEntity(DeltaTracker deltaTracker, LevelRenderState levelRenderState, Entity entity) {
        if (entity.tickCount == 0) {
            entity.xOld = entity.getX();
            entity.yOld = entity.getY();
            entity.zOld = entity.getZ();
        }

        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(
                !Minecrft.level().tickRateManager().isEntityFrozen(entity));
        // 26.3 derives entity outlines from the submitted features, so there is no glowing flag to set here.
        levelRenderState.entityRenderStates.add(extractEntity(entity, partialTick));
    }
}

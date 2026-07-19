package io.github.mortuusars.exposure.fabric.mixin;

import io.github.mortuusars.exposure.client.util.Minecrft;
import io.github.mortuusars.exposure.world.entity.CameraStandEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Final
    @Shadow
    private EntityRenderDispatcher entityRenderDispatcher;

    @Shadow
    protected abstract boolean shouldShowEntityOutlines();

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
        EntityRenderState renderState = entityRenderDispatcher.extractEntity(entity, partialTick);
        levelRenderState.entityRenderStates.add(renderState);

        if (renderState.appearsGlowing() && shouldShowEntityOutlines()) {
            levelRenderState.haveGlowingEntities = true;
        }
    }
}

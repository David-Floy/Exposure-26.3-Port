package io.github.mortuusars.exposure.mixin.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import io.github.mortuusars.exposure.client.capture.task.BackgroundScreenshotCaptureTask;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    /**
     * A SkyRenderer recreated during a background capture frame (for example after a resource reload) would
     * otherwise keep the short-lived capture target. It must always keep the real main render target;
     * {@link SkyRendererMixin} redirects it per frame instead.
     */
    @ModifyArg(method = "addSkyPass", index = 2, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/SkyRenderer;<init>(Lnet/minecraft/client/renderer/texture/TextureManager;Lnet/minecraft/client/resources/model/sprite/AtlasManager;Lcom/mojang/blaze3d/pipeline/RenderTarget;)V"))
    private RenderTarget exposure$keepRealMainTargetForSky(RenderTarget target) {
        return BackgroundScreenshotCaptureTask.unredirectMainRenderTarget(target);
    }
}

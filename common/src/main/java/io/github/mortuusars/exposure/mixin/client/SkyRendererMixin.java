package io.github.mortuusars.exposure.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.pipeline.RenderTarget;
import io.github.mortuusars.exposure.client.capture.task.BackgroundScreenshotCaptureTask;
import net.minecraft.client.renderer.SkyRenderer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 26.3 SkyRenderer renders into the target it was constructed with instead of querying the main render target
 * per frame like 26.1.2 did. Without this, a background capture frame would draw its sky into the real main
 * target and the photograph would have no sky.
 */
@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {
    @ModifyExpressionValue(method = "render", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
            target = "Lnet/minecraft/client/renderer/SkyRenderer;renderTarget:Lcom/mojang/blaze3d/pipeline/RenderTarget;"))
    private RenderTarget exposure$redirectSkyTarget(RenderTarget original) {
        return BackgroundScreenshotCaptureTask.redirectMainRenderTarget(original);
    }
}

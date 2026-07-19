package io.github.mortuusars.exposure.mixin.client;

import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.client.capture.Capture;
import io.github.mortuusars.exposure.client.capture.task.BackgroundScreenshotCaptureTask;
import io.github.mortuusars.exposure.client.util.Shader;
import io.github.mortuusars.exposure.event.ClientEvents;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GameRenderer.class, priority = 500)
public abstract class GameRendererMixin {
    @Inject(method = "render", at = @At("HEAD"))
    void exposure$onRenderFrameStarted(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        BackgroundScreenshotCaptureTask.onRenderFrameStarted(renderLevel);
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/fog/FogRenderer;endFrame()V"))
    void onRender(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        // Processing viewfinder shader should be done before capturing
        // otherwise Direct capture method will not be affected by it.
        Shader.processForGameRenderer();
        ExposureClient.cycles().tick();
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V"))
    void exposure$onBeforeGuiRender(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        BackgroundScreenshotCaptureTask.onBeforeGuiRender();
    }

    @Inject(method = "resetData", at = @At(value = "RETURN"))
    void onResetData(CallbackInfo ci) {
        ClientEvents.resetRenderData();
    }

    @Inject(method = "resize", at = @At("HEAD"))
    void exposure$cancelCaptureOnResize(int width, int height, CallbackInfo ci) {
        Capture.cancelAll();
    }
}

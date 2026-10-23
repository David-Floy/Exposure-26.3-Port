package io.github.mortuusars.exposure.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.pipeline.RenderTarget;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.client.capture.Capture;
import io.github.mortuusars.exposure.client.capture.task.BackgroundScreenshotCaptureTask;
import io.github.mortuusars.exposure.client.util.Shader;
import io.github.mortuusars.exposure.event.ClientEvents;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GameRenderer.class, priority = 500)
public abstract class GameRendererMixin {
    @Shadow @Final private GameRenderState gameRenderState;

    @Inject(method = "render", at = @At("HEAD"))
    void exposure$onRenderFrameStarted(CallbackInfo ci) {
        // 26.3 extracts the frame before render() and no longer passes whether the level is rendered.
        BackgroundScreenshotCaptureTask.onRenderFrameStarted(gameRenderState.shouldRenderLevel);
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/fog/FogRenderer;endFrame()V"))
    void onRender(CallbackInfo ci) {
        // Processing viewfinder shader should be done before capturing
        // otherwise Direct capture method will not be affected by it.
        Shader.processForGameRenderer();
        ExposureClient.cycles().tick();
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/render/GuiRenderer;render()V"))
    void exposure$onBeforeGuiRender(CallbackInfo ci) {
        BackgroundScreenshotCaptureTask.onBeforeGuiRender();
    }

    /**
     * 26.2 moved the main render target from {@code Minecraft} into GameRenderer, which reads the field directly
     * while rendering a frame. Together with {@link #exposure$redirectMainRenderTarget} this keeps the 26.1.2
     * behaviour of {@code Minecraft#getMainRenderTarget}: the whole capture frame renders into the capture target.
     * {@code resize} and {@code close} are deliberately not redirected.
     */
    @ModifyExpressionValue(
            method = {"render", "renderItemInHand", "render3dHud", "integrate3DHudDepth", "applyPostEffects",
                    "takeAutoScreenshot", "processBlurEffect"},
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
                    target = "Lnet/minecraft/client/renderer/GameRenderer;mainRenderTarget:Lcom/mojang/blaze3d/pipeline/RenderTarget;"))
    private RenderTarget exposure$redirectMainRenderTargetField(RenderTarget original) {
        return BackgroundScreenshotCaptureTask.redirectMainRenderTarget(original);
    }

    /**
     * Fixes incompatibility with Iris and Distant Horizons (and potentially others).
     * But this is not working properly if Distant Horizons and Iris are both installed and shaders are used - LODs render weirdly on top.
     * (this issue is also fixes itself when LODs are toggled off and back on again). I don't know how to fix it, so Background capture is disabled if those mods are both detected.
     * If those mods are used by themselves all is ok.
     */
    @ModifyReturnValue(method = "mainRenderTarget", at = @At("RETURN"))
    private RenderTarget exposure$redirectMainRenderTarget(RenderTarget original) {
        return BackgroundScreenshotCaptureTask.redirectMainRenderTarget(original);
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

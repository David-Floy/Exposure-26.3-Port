package io.github.mortuusars.exposure.client.capture.task;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.client.capture.ScreenshotReadback;
import io.github.mortuusars.exposure.client.util.Minecrft;
import io.github.mortuusars.exposure.client.image.WrappedNativeImage;
import io.github.mortuusars.exposure.client.capture.Capture;
import io.github.mortuusars.exposure.client.image.Image;
import io.github.mortuusars.exposure.util.cycles.task.Result;
import io.github.mortuusars.exposure.util.cycles.task.Task;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Captures a screenshot without showing it on screen. Makes photographing a seamless experience™.
 */
public class BackgroundScreenshotCaptureTask extends Task<Result<Image>> {
    private static @Nullable BackgroundScreenshotCaptureTask activeCapture;

    private @Nullable RenderTarget renderTarget;
    private @Nullable CompletableFuture<Result<Image>> future;
    private boolean redirecting;
    private boolean captureFrameRendering;
    private boolean readbackScheduled;
    private boolean targetReleased;

    public static boolean isCapturing() {
        BackgroundScreenshotCaptureTask capture = activeCapture;
        return capture != null && capture.redirecting && capture.renderTarget != null;
    }

    public static @NotNull RenderTarget getRenderTarget() {
        BackgroundScreenshotCaptureTask capture = Objects.requireNonNull(activeCapture);
        return Objects.requireNonNull(capture.renderTarget);
    }

    public static boolean isCaptureFrameRendering() {
        BackgroundScreenshotCaptureTask capture = activeCapture;
        return capture != null && capture.redirecting && capture.captureFrameRendering;
    }

    /**
     * Called at the start of {@code GameRenderer.render}. A capture armed during the previous
     * rendered frame now has freshly extracted camera, FOV, GUI and level render state, so this
     * normal frame can be rendered into the capture target without consuming render state twice.
     */
    public static void onRenderFrameStarted(boolean renderLevel) {
        RenderSystem.assertOnRenderThread();
        BackgroundScreenshotCaptureTask capture = activeCapture;
        if (capture != null && capture.redirecting) {
            capture.captureFrameRendering = renderLevel && Minecrft.get().level != null;
        }
    }

    /**
     * Called after world rendering and Exposure post effects, but before GUI rendering. The main
     * target redirection is removed before GUI submission so HUD and screens never enter the photo.
     */
    public static void onBeforeGuiRender() {
        RenderSystem.assertOnRenderThread();
        BackgroundScreenshotCaptureTask capture = activeCapture;
        if (capture == null || !capture.redirecting || !capture.captureFrameRendering) {
            return;
        }

        capture.scheduleReadback();
    }

    // --

    @Override
    public CompletableFuture<Result<Image>> execute() {
        RenderSystem.assertOnRenderThread();
        if (future != null) {
            return future;
        }

        if (ExposureClient.shouldUseDirectCapture()) {
            Exposure.LOGGER.warn("BackgroundScreenshotCaptureMethod is used while incompatible mods are installed. " +
                    "Captured image most likely will not look as expected.");
        }

        future = new CompletableFuture<>();
        if (activeCapture != null) {
            Exposure.LOGGER.error("Cannot start a background capture while another one is active.");
            future.complete(Result.error(Capture.ERROR_FAILED_GENERIC));
            return future;
        }

        Minecraft minecraft = Minecrft.get();
        RenderTarget mainRenderTarget = minecraft.getMainRenderTarget();
        renderTarget = new TextureTarget("Exposure background screenshot",
                mainRenderTarget.width, mainRenderTarget.height, true);

        redirecting = true;
        activeCapture = this;
        future.whenComplete((result, throwable) -> {
            if (!readbackScheduled) {
                Runnable cleanup = this::abortBeforeReadback;
                if (Minecrft.get().isSameThread()) {
                    cleanup.run();
                } else {
                    Minecrft.execute(cleanup);
                }
            }
        });

        return future;
    }

    private void scheduleReadback() {
        RenderSystem.assertOnRenderThread();
        RenderTarget captureTarget = Objects.requireNonNull(renderTarget);

        readbackScheduled = true;
        redirecting = false;
        captureFrameRendering = false;
        if (activeCapture == this) {
            activeCapture = null;
        }

        ScreenshotReadback.capture(captureTarget)
                .whenComplete((nativeImage, throwable) -> completeReadback(
                        captureTarget, nativeImage, throwable));
    }

    private void completeReadback(RenderTarget captureTarget,
                                  @Nullable com.mojang.blaze3d.platform.NativeImage nativeImage,
                                  @Nullable Throwable throwable) {
        RenderSystem.assertOnRenderThread();
        if (throwable != null || nativeImage == null) {
            releaseTarget(captureTarget);
            Exposure.LOGGER.error("Couldn't read captured image from the GPU.", throwable);
            Objects.requireNonNull(future).complete(Result.error(Capture.ERROR_FAILED_GENERIC));
            return;
        }

        WrappedNativeImage image = new WrappedNativeImage(nativeImage);
        releaseTarget(captureTarget);
        if (!Objects.requireNonNull(future).complete(Result.success(image))) {
            image.close();
        }
    }

    private void abortBeforeReadback() {
        RenderSystem.assertOnRenderThread();
        redirecting = false;
        captureFrameRendering = false;
        if (activeCapture == this) {
            activeCapture = null;
        }

        RenderTarget target = renderTarget;
        if (target != null) {
            releaseTarget(target);
        }
    }

    private void releaseTarget(RenderTarget target) {
        RenderSystem.assertOnRenderThread();
        if (targetReleased) {
            return;
        }

        targetReleased = true;
        if (renderTarget == target) {
            renderTarget = null;
        }
        target.destroyBuffers();
    }
}

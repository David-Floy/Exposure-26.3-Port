package io.github.mortuusars.exposure.client.capture;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Screenshot;

import java.util.concurrent.CompletableFuture;

/** Starts a 26.1.2 GPU texture readback and completes after the pixels are CPU-owned. */
public final class ScreenshotReadback {
    private ScreenshotReadback() {
    }

    public static CompletableFuture<NativeImage> capture(RenderTarget renderTarget) {
        RenderSystem.assertOnRenderThread();
        CompletableFuture<NativeImage> future = new CompletableFuture<>();

        try {
            Screenshot.takeScreenshot(renderTarget, nativeImage -> {
                if (!future.complete(nativeImage)) {
                    nativeImage.close();
                }
            });
        } catch (Throwable throwable) {
            future.completeExceptionally(throwable);
        }

        return future;
    }
}

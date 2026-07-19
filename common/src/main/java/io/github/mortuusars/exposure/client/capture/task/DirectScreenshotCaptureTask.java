package io.github.mortuusars.exposure.client.capture.task;

import io.github.mortuusars.exposure.Config;
import io.github.mortuusars.exposure.client.capture.ScreenshotReadback;
import io.github.mortuusars.exposure.client.image.WrappedNativeImage;
import io.github.mortuusars.exposure.util.cycles.task.Result;
import io.github.mortuusars.exposure.util.cycles.task.Task;
import io.github.mortuusars.exposure.client.image.Image;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class DirectScreenshotCaptureTask extends Task<Result<Image>> {
    // At least 1 frame of delay is needed because some immediate CaptureComponents may only apply on the next frame
    // and in this method we take a screenshot of what's already rendered.
    // Both capture modes therefore wait for a newly extracted normal frame before readback.
    protected int delay = Math.max(1, Config.Client.DIRECT_CAPTURE_DELAY_FRAMES.get());

    @Nullable
    protected CompletableFuture<Result<Image>> future;
    protected boolean readbackStarted;

    @Override
    public @NotNull CompletableFuture<Result<Image>> execute() {
        if (future == null) {
            future = new CompletableFuture<>();
        }
        return future;
    }

    @Override
    public void tick() {
        if (future == null || future.isDone()) {
            return;
        }

        if (delay <= 0 && !readbackStarted) {
            readbackStarted = true;
            ScreenshotReadback.capture(Minecraft.getInstance().getMainRenderTarget())
                    .whenComplete((nativeImage, throwable) -> {
                        if (throwable != null) {
                            future.completeExceptionally(throwable);
                            return;
                        }

                        WrappedNativeImage image = new WrappedNativeImage(nativeImage);
                        if (!future.complete(Result.success(image))) {
                            image.close();
                        }
                    });
        }

        delay--;
    }
}

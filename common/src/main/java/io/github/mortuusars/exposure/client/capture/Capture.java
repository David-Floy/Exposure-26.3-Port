package io.github.mortuusars.exposure.client.capture;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.exposure.Config;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.client.capture.action.CompositeAction;
import io.github.mortuusars.exposure.client.capture.task.*;
import io.github.mortuusars.exposure.client.image.Image;
import io.github.mortuusars.exposure.client.util.Minecrft;
import io.github.mortuusars.exposure.util.cycles.task.Result;
import io.github.mortuusars.exposure.util.cycles.task.Task;
import io.github.mortuusars.exposure.util.TranslatableError;
import io.github.mortuusars.exposure.client.capture.action.CaptureAction;
import org.slf4j.Logger;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.Set;

public class Capture<T> extends Task<Result<T>> {
    public static final TranslatableError ERROR_TIMED_OUT = new TranslatableError("error.exposure.capture.timed_out", "ERR_CAPTURE_TIMED_OUT");
    public static final TranslatableError ERROR_FAILED_GENERIC = new TranslatableError("error.exposure.capture.failed", "ERR_CAPTURE_FAILED");

    public static final int TIMEOUT_MS = 12_000; // 12 seconds
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<Capture<?>> ACTIVE_CAPTURES = ConcurrentHashMap.newKeySet();

    protected final Task<Result<T>> capturingTask;
    protected final CaptureAction component;
    protected final CaptureTimer timer;
    protected final CompletableFuture<Result<T>> completableFuture;
    protected final AtomicBoolean completionHandled = new AtomicBoolean();
    protected volatile CompletableFuture<Result<T>> activeCapturingFuture;
    protected boolean captureActionsApplied;

    public Capture(Task<Result<T>> capturingTask, CaptureAction component) {
        this.capturingTask = capturingTask;
        this.component = component;
        this.timer = new CaptureTimer(component.requiredDelayTicks())
                .whenStarted(this.component::initialize)
                .onGameTick(this.component::delayTick)
                .whenEnded(this::beginCapture);
        this.completableFuture = new CompletableFuture<>();
    }

    public CompletableFuture<Result<T>> execute() {
        if (!isStarted()) {
            setStarted();
            ACTIVE_CAPTURES.add(this);
            try {
                timer.start();
            } catch (Throwable throwable) {
                finishCapture(null, throwable);
            }
        }

        return completableFuture;
    }

    public void tick() {
        if (isDone()) {
            return;
        }

        try {
            capturingTask.tick();
            timer.tick();
        } catch (Throwable throwable) {
            finishCapture(null, throwable);
        }
    }

    private void beginCapture() {
        try {
            // Mark first so a partially-applied non-composite action still receives afterCapture.
            captureActionsApplied = true;
            component.beforeCapture();
            capture();
        } catch (Throwable throwable) {
            finishCapture(null, throwable);
        }
    }

    private void capture() {
        activeCapturingFuture = capturingTask.execute();
        activeCapturingFuture
                .completeOnTimeout(Result.error(ERROR_TIMED_OUT), TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .whenComplete(this::finishCaptureOnClientThread);
    }

    private void finishCaptureOnClientThread(Result<T> result, Throwable throwable) {
        Runnable finisher = () -> finishCapture(result, throwable);
        if (Minecrft.get().isSameThread()) {
            finisher.run();
        } else {
            Minecrft.execute(finisher);
        }
    }

    private void finishCapture(Result<T> suppliedResult, Throwable throwable) {
        if (!completionHandled.compareAndSet(false, true)) {
            return;
        }

        Result<T> result = suppliedResult;
        if (throwable != null || result == null) {
            LOGGER.error("Capturing failed.", throwable);
            result = Result.error(ERROR_FAILED_GENERIC);
        }

        try {
            if (result.isSuccessful()) {
                component.onSuccess();
            } else {
                component.onFailure(result.getError());
            }
        } catch (Throwable callbackFailure) {
            LOGGER.error("Capture completion callback failed.", callbackFailure);
            result = Result.error(ERROR_FAILED_GENERIC);
        } finally {
            if (captureActionsApplied) {
                try {
                    if (result.isSuccessful()) {
                        component.afterCapture();
                    } else {
                        component.afterCaptureFailure();
                    }
                } catch (Throwable restoreFailure) {
                    LOGGER.error("Failed to restore capture state.", restoreFailure);
                    result = Result.error(ERROR_FAILED_GENERIC);
                }
                captureActionsApplied = false;
            }
        }

        ACTIVE_CAPTURES.remove(this);
        setDone();
        completableFuture.complete(result);
    }

    public void cancel() {
        Result<T> cancellation = Result.error(ERROR_FAILED_GENERIC);
        CompletableFuture<Result<T>> future = activeCapturingFuture;
        if (future != null) {
            future.complete(cancellation);
        }
        finishCapture(cancellation, null);
    }

    /** Cancels capture-scoped client state before a world or resource lifecycle reset. */
    public static void cancelAll() {
        for (Capture<?> capture : Set.copyOf(ACTIVE_CAPTURES)) {
            capture.cancel();
        }
    }

    public Task<T> handleErrorAndGetResult() {
        return handleErrorAndGetResult(err -> {});
    }

    public Task<T> handleErrorAndGetResult(Consumer<TranslatableError> errorConsumer) {
        return onError(errorConsumer).then(Result::unwrap);
    }

    public Task<T> logErrorAndGetResult(Logger logger) {
        return onError(error -> logger.error(error.technical().getString())).then(Result::unwrap);
    }

    public static <T> Capture<T> of(Task<Result<T>> capturingTask) {
        return new Capture<>(capturingTask, CaptureAction.EMPTY);
    }

    public static <T> Capture<T> of(Task<Result<T>> capturingTask, CaptureAction action) {
        return new Capture<>(capturingTask, action);
    }

    public static <T> Capture<T> of(Task<Result<T>> capturingTask, CaptureAction... actions) {
        return new Capture<>(capturingTask, new CompositeAction(actions));
    }

    public static Task<Result<Image>> screenshot() {
        return ExposureClient.shouldUseDirectCapture()
                ? new DirectScreenshotCaptureTask()
                : new BackgroundScreenshotCaptureTask();
    }

    public static Task<Result<Image>> fromFile(File file) {
        return new FileCaptureTask(file);
    }

    public static Task<Result<Image>> fromUrl(URI uri) {
        return new UrlCaptureTask(uri);
    }

    public static Task<Result<Image>> path(String path) {
        return new PathCaptureTask(path);
    }
}

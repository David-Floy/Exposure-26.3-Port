package io.github.mortuusars.exposure.client.capture.action;

import io.github.mortuusars.exposure.util.TranslatableError;

import java.util.Arrays;
import java.util.List;

public class CompositeAction implements CaptureAction {
    private final List<CaptureAction> actions;
    private int appliedActions;
    private boolean restored = true;

    public CompositeAction(CaptureAction... actions) {
        this.actions = Arrays.stream(actions).filter(action -> !action.equals(CaptureAction.EMPTY)).toList();
    }

    public List<CaptureAction> getActions() {
        return actions;
    }

    @Override
    public int requiredDelayTicks() {
        return actions.stream().mapToInt(CaptureAction::requiredDelayTicks).max().orElse(0);
    }

    @Override
    public void initialize() {
        actions.forEach(CaptureAction::initialize);
    }

    @Override
    public void delayTick(int delayTicksLeft) {
        actions.forEach(component -> component.delayTick(delayTicksLeft));
    }

    @Override
    public void beforeCapture() {
        if (!restored) {
            throw new IllegalStateException("Capture actions are already applied.");
        }

        restored = false;
        appliedActions = 0;
        try {
            for (int i = 0; i < actions.size(); i++) {
                // Count the action first so a partially-applied action also gets its restore hook.
                appliedActions = i + 1;
                actions.get(i).beforeCapture();
            }
        } catch (Throwable throwable) {
            try {
                restoreAppliedActions(true);
            } catch (Throwable restoreFailure) {
                throwable.addSuppressed(restoreFailure);
            }
            throwUnchecked(throwable);
        }
    }

    @Override
    public void onSuccess() {
        actions.forEach(CaptureAction::onSuccess);
    }

    @Override
    public void onFailure(TranslatableError error) {
        actions.forEach(action -> action.onFailure(error));
    }

    @Override
    public void afterCapture() {
        restoreAppliedActions(false);
    }

    @Override
    public void afterCaptureFailure() {
        restoreAppliedActions(true);
    }

    private void restoreAppliedActions(boolean reverse) {
        if (restored) {
            return;
        }

        Throwable failure = null;
        for (int step = 0; step < appliedActions; step++) {
            int i = reverse ? appliedActions - 1 - step : step;
            try {
                actions.get(i).afterCapture();
            } catch (Throwable throwable) {
                if (failure == null) {
                    failure = throwable;
                } else {
                    failure.addSuppressed(throwable);
                }
            }
        }

        appliedActions = 0;
        restored = true;
        if (failure != null) {
            if (failure instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (failure instanceof Error error) {
                throw error;
            }
            throw new RuntimeException("Failed to restore capture actions.", failure);
        }
    }

    private static void throwUnchecked(Throwable throwable) {
        if (throwable instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        if (throwable instanceof Error error) {
            throw error;
        }
        throw new RuntimeException(throwable);
    }
}

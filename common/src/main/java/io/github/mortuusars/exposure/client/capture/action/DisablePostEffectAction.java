package io.github.mortuusars.exposure.client.capture.action;

import net.minecraft.client.Minecraft;

public class DisablePostEffectAction implements CaptureAction {
    private boolean effectActive;

    @Override
    public void beforeCapture() {
        effectActive = Minecraft.getInstance().gameRenderer.spectatedEntityEffectActive;
        Minecraft.getInstance().gameRenderer.spectatedEntityEffectActive = false;
    }

    @Override
    public void afterCapture() {
        Minecraft.getInstance().gameRenderer.spectatedEntityEffectActive = effectActive;
    }
}

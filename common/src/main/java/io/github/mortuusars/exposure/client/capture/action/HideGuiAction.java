package io.github.mortuusars.exposure.client.capture.action;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;

public class HideGuiAction implements CaptureAction {
    private boolean hideGuiBeforeCapture;

    // 26.3 replaced Options#hideGui with Hud#isHidden, which can only be toggled.
    @Override
    public void beforeCapture() {
        Hud hud = Minecraft.getInstance().gui.hud;
        hideGuiBeforeCapture = hud.isHidden();
        if (!hideGuiBeforeCapture) {
            hud.toggle();
        }
    }

    @Override
    public void afterCapture() {
        Hud hud = Minecraft.getInstance().gui.hud;
        if (hud.isHidden() != hideGuiBeforeCapture) {
            hud.toggle();
        }
    }
}

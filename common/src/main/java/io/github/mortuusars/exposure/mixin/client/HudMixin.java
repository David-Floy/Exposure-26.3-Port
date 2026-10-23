package io.github.mortuusars.exposure.mixin.client;

import io.github.mortuusars.exposure.Config;
import io.github.mortuusars.exposure.client.camera.CameraClient;
import io.github.mortuusars.exposure.client.util.Minecrft;
import io.github.mortuusars.exposure.world.item.PhotographItem;
import io.github.mortuusars.exposure.world.item.StackedPhotographsItem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 26.2 moved the in-game HUD out of {@code Gui}. These injections were on {@code Gui#extractRenderState}
 * and {@code Gui#extractCrosshair} in 26.1.2; {@link Hud} now holds both with unchanged bodies.
 */
@Mixin(Hud.class)
public abstract class HudMixin {
    /**
     * Injected after Hud publishes its hidden state to the GUI render state: that field is not reset per frame,
     * and hand/screen-effect rendering reads it, so cancelling before it would leave it stale.
     */
    @Inject(method = "extractRenderState", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/renderer/state/gui/GuiRenderState;isHudHidden:Z"), cancellable = true)
    private void renderGui(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (CameraClient.viewfinder() != null && CameraClient.viewfinder().isLookingThrough()) {
            CameraClient.viewfinder().overlay().render(guiGraphics, deltaTracker);
            if (Config.Client.HIDE_HUD_WHILE_IN_VIEWFINDER.get()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "extractCrosshair", at = @At(value = "HEAD"), cancellable = true)
    private void renderCrosshair(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        LocalPlayer player = Minecrft.player();
        if (Config.Client.PHOTOGRAPH_IN_HAND_HIDE_CROSSHAIR.get() && player.getXRot() > 25f
                && (player.getMainHandItem().getItem() instanceof PhotographItem || player.getMainHandItem().getItem() instanceof StackedPhotographsItem)
                && player.getOffhandItem().isEmpty())
            ci.cancel();
    }
}

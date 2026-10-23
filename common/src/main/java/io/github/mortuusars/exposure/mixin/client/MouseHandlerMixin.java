package io.github.mortuusars.exposure.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.mortuusars.exposure.client.input.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.MouseHandler.class)
public class MouseHandlerMixin {
    @Inject(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSpectator()Z"),
            cancellable = true)
    private void onScroll(long windowPointer, double xOffset, double yOffset, CallbackInfo ci,
                  @Local(name = "scaledYOffset") double yScroll) {
        if (yScroll != 0 && MouseHandler.scrolled(yScroll)) {
            ci.cancel();
        }
    }

    /**
     * 26.2 moved the overlay to {@code Gui#overlay}. 26.3 also dropped the {@code buttonInfo} local: SDL already
     * delivers the effective button, so {@code rawButtonInfo} is the value vanilla uses from here on.
     */
    @Inject(method = "onButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;overlay()Lnet/minecraft/client/gui/screens/Overlay;",
            ordinal = 0), cancellable = true)
    private void onButton(long windowPointer, MouseButtonInfo rawButtonInfo, int action, CallbackInfo ci) {
        if (MouseHandler.buttonPressed(rawButtonInfo, action))
            ci.cancel();
    }

    @ModifyVariable(method = "turnPlayer", at = @At(value = "STORE"), name = "sens")
    private double modifySensitivity(double sensitivity) {
        return MouseHandler.modifySensitivity(sensitivity);
    }

    @Inject(method = "turnPlayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"), cancellable = true)
    private void onTurnPlayer(double frameTime, CallbackInfo ci,
                              @Local(name = "xo") double xRot,
                              @Local(name = "yo") double yRot) {
        if (MouseHandler.onTurnPlayer(xRot, yRot)) {
            ci.cancel();
        }
    }
}

package io.github.mortuusars.exposure.mixin.client;

import io.github.mortuusars.exposure.client.camera.viewfinder.ViewfinderCameraControlsScreen;
import io.github.mortuusars.exposure.event.ClientEvents;
import io.github.mortuusars.exposure.network.Packets;
import io.github.mortuusars.exposure.network.packet.serverbound.ActiveCameraReleaseC2SP;
import io.github.mortuusars.exposure.world.camera.CameraOnStand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The setScreen hook moved to {@link GuiMixin} and the main render target redirect to {@link GameRendererMixin}
 * with the 26.2 GUI and render target reorganization.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow @Nullable public LocalPlayer player;
    @Shadow @Nullable public ClientLevel level;
    @Shadow @Final public Gui gui;

    @Inject(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isHandsBusy()Z"), cancellable = true)
    void onStartUseItem(CallbackInfo ci) {
        if (player == null || player.isHandsBusy()) return;
        if (gui.screen() instanceof ViewfinderCameraControlsScreen) return; // Screen handles right click.

        if (player.getActiveExposureCamera() instanceof CameraOnStand cameraOnStand && cameraOnStand.isActive()) {
            cameraOnStand.release();
            Packets.sendToServer(ActiveCameraReleaseC2SP.INSTANCE);
            ci.cancel();
        }
    }

    @Inject(method = "startAttack", at = @At(value = "HEAD"), cancellable = true)
    void onStartAttack(CallbackInfoReturnable<Boolean> cir) {
        if (player != null && player.getActiveExposureCamera() instanceof CameraOnStand) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "setLevel", at = @At("HEAD"))
    void onLevelUnload(ClientLevel newLevel, CallbackInfo ci) {
        if (level != null) {
            ClientEvents.levelUnloaded();
        }
    }

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;resetData()V", shift = At.Shift.AFTER))
    void disconnect(Screen nextScreen, boolean keepResourcePacks, boolean updateLevelInEngines, CallbackInfo ci) {
        ClientEvents.disconnect();
    }
}

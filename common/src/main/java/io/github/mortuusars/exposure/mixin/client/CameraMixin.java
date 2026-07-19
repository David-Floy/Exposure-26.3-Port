package io.github.mortuusars.exposure.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.mortuusars.exposure.client.camera.CameraClient;
import io.github.mortuusars.exposure.client.render.FovModifier;
import io.github.mortuusars.exposure.world.item.camera.CameraItem;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void move(float zoom, float dy, float dx);
    @Shadow
    protected abstract void setRotation(float yRot, float xRot);
    @Shadow
    private float xRot;
    @Shadow
    private float yRot;

    /**
     * The second return is the regular player FOV path. Keeping the restoration animation here
     * leaves Camera's panoramic early return untouched when Exposure is inactive.
     */
    @ModifyReturnValue(method = "calculateFov", at = @At(value = "RETURN", ordinal = 1))
    private float exposure$modifyRegularFov(float original) {
        return (float) FovModifier.modify(original);
    }

    /** Active viewfinder/capture overrides also apply to Camera's special early-return path. */
    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void exposure$applyActiveFovOverride(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (FovModifier.shouldOverride()) {
            cir.setReturnValue((float) FovModifier.modify(cir.getReturnValue()));
        }
    }

    @Inject(method = "getMaxZoom", at = @At(value = "RETURN"), cancellable = true)
    private void getMaxZoom(float maxZoom, CallbackInfoReturnable<Float> cir) {
        if (CameraClient.viewfinder() != null && CameraClient.viewfinder().isLookingThrough()) {
            cir.setReturnValue(Math.min(CameraClient.viewfinder().selfie().getMaxCameraDistance(), cir.getReturnValue()));
        }
    }

    @Inject(method = "alignWithEntity", at = @At(value = "RETURN"))
    private void onAlignWithEntity(float partialTick, CallbackInfo ci) {
        if (CameraClient.viewfinder() != null && CameraClient.viewfinder().isLookingThrough()) {
            CameraType cameraType = net.minecraft.client.Minecraft.getInstance().options.getCameraType();
            boolean detached = !cameraType.isFirstPerson();
            if (detached && cameraType.isMirrored() && CameraClient.viewfinder().camera().inSelfieMode()) {
                setRotation((float) (yRot + CameraClient.viewfinder().selfie().getCameraYRot()),
                        (float) (xRot + CameraClient.viewfinder().selfie().getCameraXRot()));
            }

            if (!detached) {
                float yOffset = (CameraClient.viewfinder().camera()
                        .map(CameraItem::getYPositionOffset)
                        .orElse(0.0)
                        .floatValue());
                move(0, yOffset, 0);
            }
        }
    }
}

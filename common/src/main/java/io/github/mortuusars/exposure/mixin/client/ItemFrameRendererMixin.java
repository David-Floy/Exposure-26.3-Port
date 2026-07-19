package io.github.mortuusars.exposure.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.mortuusars.exposure.Config;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.client.render.photograph.ItemFramePhotoRenderState;
import io.github.mortuusars.exposure.client.render.photograph.PhotographRenderer;
import io.github.mortuusars.exposure.world.item.PhotographItem;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces only Exposure photographs; ordinary Item Frame model extraction/submission stays vanilla. */
@Mixin(ItemFrameRenderer.class)
public class ItemFrameRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/decoration/ItemFrame;Lnet/minecraft/client/renderer/entity/state/ItemFrameRenderState;F)V",
            at = @At("TAIL"))
    private void exposure$extractPhotograph(ItemFrame frame, ItemFrameRenderState state, float partialTick, CallbackInfo ci) {
        ItemFramePhotoRenderState photoState = (ItemFramePhotoRenderState) state;
        photoState.exposure$setPhotographRequest(PhotographRenderer.PhotographRenderRequest.EMPTY);
        ItemStack stack = frame.getItem();
        if (!Config.Client.PHOTOGRAPH_RENDERS_IN_ITEM_FRAME.get() || !(stack.getItem() instanceof PhotographItem item)
                || item.getFrame(stack).identifier().isEmpty()) return;

        int light = frame.getType() == EntityType.GLOW_ITEM_FRAME ? 0xF000F0 : state.lightCoords;
        PhotographRenderer.PhotographRenderRequest request = ExposureClient.photographRenderer()
                .prepare(stack, false, false, light, 255, 255, 255, 255);
        if (!request.isEmpty()) {
            // Suppresses only the item model.  Vanilla still submits the frame,
            // name tag, outline, light and all non-Exposure item-frame paths.
            state.item.clear();
            photoState.exposure$setPhotographRequest(request);
        }
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemFrameRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("TAIL"))
    private void exposure$submitPhotograph(ItemFrameRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                                            CameraRenderState camera, CallbackInfo ci) {
        PhotographRenderer.PhotographRenderRequest request = ((ItemFramePhotoRenderState) state).exposure$getPhotographRequest();
        if (request.isEmpty()) return;

        poseStack.pushPose();
        Direction direction = state.direction;
        poseStack.translate(direction.getStepX() * 0.46875, direction.getStepY() * 0.46875, direction.getStepZ() * 0.46875);
        float xRot = direction.getAxis().isHorizontal() ? 0 : -90 * direction.getAxisDirection().getStep();
        float yRot = direction.getAxis().isHorizontal() ? 180.0F - direction.toYRot() : 180.0F;
        poseStack.mulPose(Axis.XP.rotationDegrees(xRot));
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.translate(0, 0, state.isInvisible ? 0.5f : 0.4375f);
        // This is the same matrix at the legacy item-model invocation.  The
        // two scales cancel exactly as the old ClientEvents path did.
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.rotation * 45.0F));
        poseStack.scale(0.5f, 0.5f, 0.5f);
        poseStack.scale(2.0f, 2.0f, 2.0f);
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.rotation * 45.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.scale(0.625f, 0.625f, 0.625f);
        poseStack.translate(-0.5f, -0.5f, 0.045f);
        ExposureClient.photographRenderer().submit(request, poseStack, collector);
        poseStack.popPose();
    }
}

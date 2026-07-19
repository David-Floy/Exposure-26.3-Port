package io.github.mortuusars.exposure.client.render;

import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-frame snapshot for {@link CameraStandEntityRenderer}.  It deliberately
 * contains baked model parts and an item render state, never the entity or an
 * ItemStack, so submission can run after world extraction has finished.
 */
public final class CameraStandRenderState extends EntityRenderState {
    public final List<BlockStateModelPart> standModelParts = new ArrayList<>();
    public final List<BlockStateModelPart> mountModelParts = new ArrayList<>();
    public final ItemStackRenderState camera = new ItemStackRenderState();
    public float yaw;
    public float pitch;
    public float vehicleYaw;
    public float hurtRotation;
    public float cameraScale;
    public boolean hasVehicle;
    public boolean malfunctioned;
}

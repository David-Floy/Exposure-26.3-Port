package io.github.mortuusars.exposure.client.render;

import io.github.mortuusars.exposure.client.render.image.ImageRenderRequest;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

/** Per-frame snapshot for photograph frames.  It owns no entity, level, stack, or image pixels. */
public final class PhotographFrameRenderState extends EntityRenderState {
    public final List<BlockStateModelPart> frameModelParts = new ArrayList<>();
    public final ItemStackRenderState fallbackItem = new ItemStackRenderState();
    public ImageRenderRequest image;
    public Direction direction = Direction.NORTH;
    public int size;
    public int itemRotation;
    public int photographLight;
    public float xRot;
    public float yRot;
    public boolean frameInvisible;
    public boolean glowing;
}

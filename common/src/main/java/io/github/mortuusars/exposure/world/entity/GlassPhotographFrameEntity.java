package io.github.mortuusars.exposure.world.entity;

import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.world.item.PhotographFrameItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class GlassPhotographFrameEntity extends PhotographFrameEntity {
    public GlassPhotographFrameEntity(EntityType<? extends PhotographFrameEntity> entityType, Level level) {
        super(entityType, level);
    }

    public GlassPhotographFrameEntity(Level level, BlockPos pos, Direction facingDirection) {
        super(Exposure.EntityTypes.CLEAR_PHOTOGRAPH_FRAME.get(), level, pos, facingDirection);
    }

    protected GlassPhotographFrameEntity(EntityType<? extends PhotographFrameEntity> entityType, Level level, BlockPos pos, Direction facingDirection) {
        super(entityType, level, pos, facingDirection);
    }

    @Override
    public PhotographFrameItem getBaseFrameItem() {
        return Exposure.Items.CLEAR_PHOTOGRAPH_FRAME.get();
    }

    @Override
    public boolean isFrameInvisible() {
        // An occupied glass frame still needs its dedicated glass block model. Treating every
        // non-empty frame as invisible makes the renderer submit only a full-size photograph quad.
        return super.isFrameInvisible();
    }
}

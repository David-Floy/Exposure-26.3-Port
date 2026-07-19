package io.github.mortuusars.exposure.world.entity;

import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PhotographFrameEntitySynchedDataTests {
    @Test
    void definesTheInheritedHangingDirectionSlotBeforeItsOwnData() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        assertDoesNotThrow(() -> new PhotographFrameEntity(itemFrameType(), null),
                "Photograph frames must include HangingEntity.DATA_DIRECTION in their synched data");
    }

    @Test
    void consumesInheritedDirectionUpdates() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        TestPhotographFrameEntity entity = new TestPhotographFrameEntity(itemFrameType(), null);

        entity.syncDirection(Direction.EAST);

        assertEquals(Direction.EAST, entity.getDirection());
        assertEquals(270.0f, entity.getYRot());
    }

    @Test
    void occupiedGlassFrameKeepsItsDedicatedFrameModelVisible() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        GlassPhotographFrameEntity entity = new GlassPhotographFrameEntity(itemFrameType(), null);
        Items.PAPER.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);

        entity.setItem(new ItemStack(Items.PAPER));

        assertFalse(entity.isFrameInvisible(),
                "an occupied glass frame must still extract and submit its glass model parts");
    }

    @SuppressWarnings("unchecked")
    private static EntityType<? extends PhotographFrameEntity> itemFrameType() {
        return (EntityType<? extends PhotographFrameEntity>) (EntityType<?>) EntityType.ITEM_FRAME;
    }

    private static final class TestPhotographFrameEntity extends PhotographFrameEntity {
        private TestPhotographFrameEntity(EntityType<? extends PhotographFrameEntity> type, Level level) {
            super(type, level);
        }

        private void syncDirection(Direction direction) {
            setDirectionRaw(direction);
        }
    }
}

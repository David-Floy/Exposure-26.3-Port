package io.github.mortuusars.exposure.world.entity;

import net.minecraft.SharedConstants;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PhotographFrameEntitySynchedDataTests {
    @Test
    void definesTheInheritedHangingDirectionSlotBeforeItsOwnData() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        assertDoesNotThrow(() -> new PhotographFrameEntity(itemFrameType(), levelWithoutWorld()),
                "Photograph frames must include HangingEntity.DATA_DIRECTION in their synched data");
    }

    @Test
    void consumesInheritedDirectionUpdates() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        TestPhotographFrameEntity entity = new TestPhotographFrameEntity(itemFrameType(), levelWithoutWorld());

        entity.syncDirection(Direction.EAST);

        assertEquals(Direction.EAST, entity.getDirection());
        assertEquals(270.0f, entity.getYRot());
    }

    @Test
    void occupiedGlassFrameKeepsItsDedicatedFrameModelVisible() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        GlassPhotographFrameEntity entity = new GlassPhotographFrameEntity(itemFrameType(), levelWithoutWorld());
        Items.PAPER.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);

        entity.setItem(new ItemStack(Items.PAPER));

        assertFalse(entity.isFrameInvisible(),
                "an occupied glass frame must still extract and submit its glass model parts");
    }

    /**
     * 26.3 entities take their network id from {@code Level#getNextEntityId()} instead of a static counter, so a
     * null level no longer works. The base implementation returns 0 without reading any state, and ClientLevel
     * does not override it, so an unconstructed ClientLevel is enough for these synched-data tests.
     */
    private static Level levelWithoutWorld() {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            return (Level) ((Unsafe) theUnsafe.get(null)).allocateInstance(ClientLevel.class);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot allocate a level for the test", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static EntityType<? extends PhotographFrameEntity> itemFrameType() {
        return (EntityType<? extends PhotographFrameEntity>) (EntityType<?>) EntityTypes.ITEM_FRAME;
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

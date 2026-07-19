package io.github.mortuusars.exposure.client.render.model;

import com.mojang.serialization.MapCodec;
import io.github.mortuusars.exposure.Config;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.client.util.Minecrft;
import io.github.mortuusars.exposure.world.item.AlbumItem;
import io.github.mortuusars.exposure.world.item.ChromaticSheetItem;
import io.github.mortuusars.exposure.world.item.StackedPhotographsItem;
import io.github.mortuusars.exposure.world.item.camera.Attachment;
import io.github.mortuusars.exposure.world.item.camera.CameraItem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperties;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Client-only item-model properties backing Exposure's former ItemProperties predicates.
 * The identifiers deliberately retain the old resource-pack-facing predicate names.
 */
public final class ExposureItemModelProperties {
    private ExposureItemModelProperties() {
    }

    public static void register() {
        ConditionalItemModelProperties.ID_MAPPER
                .put(Exposure.resource("camera_gold"), CameraGoldProperty.MAP_CODEC)
                .put(Exposure.resource("camera_active"), CameraActiveProperty.MAP_CODEC)
                .put(Exposure.resource("camera_selfie"), CameraSelfieProperty.MAP_CODEC)
                .put(Exposure.resource("camera_selfie_user"), CameraSelfieUserProperty.MAP_CODEC)
                .put(Exposure.resource("camera_has_lens"), CameraHasLensProperty.MAP_CODEC)
                .put(Exposure.resource("camera_has_flash"), CameraHasFlashProperty.MAP_CODEC)
                .put(Exposure.resource("projector_active"), ProjectorActiveProperty.MAP_CODEC);

        RangeSelectItemModelProperties.ID_MAPPER
                .put(Exposure.resource("channels"), ChromaticChannelsProperty.MAP_CODEC)
                .put(Exposure.resource("count"), StackedPhotographsCountProperty.MAP_CODEC)
                .put(Exposure.resource("photos"), AlbumPhotosProperty.MAP_CODEC);
    }

    private record CameraGoldProperty() implements ConditionalItemModelProperty {
        private static final MapCodec<CameraGoldProperty> MAP_CODEC = MapCodec.unit(new CameraGoldProperty());

        @Override
        public boolean get(ItemStack stack, ClientLevel level, LivingEntity entity, int seed, ItemDisplayContext displayContext) {
            return stack.getOrDefault(Exposure.DataComponents.CAMERA_GOLD, false);
        }

        @Override
        public MapCodec<CameraGoldProperty> type() {
            return MAP_CODEC;
        }
    }

    private record CameraActiveProperty() implements ConditionalItemModelProperty {
        private static final MapCodec<CameraActiveProperty> MAP_CODEC = MapCodec.unit(new CameraActiveProperty());

        @Override
        public boolean get(ItemStack stack, ClientLevel level, LivingEntity entity, int seed, ItemDisplayContext displayContext) {
            return stack.getItem() instanceof CameraItem camera && camera.isActive(stack);
        }

        @Override
        public MapCodec<CameraActiveProperty> type() {
            return MAP_CODEC;
        }
    }

    private record CameraSelfieProperty() implements ConditionalItemModelProperty {
        private static final MapCodec<CameraSelfieProperty> MAP_CODEC = MapCodec.unit(new CameraSelfieProperty());

        @Override
        public boolean get(ItemStack stack, ClientLevel level, LivingEntity entity, int seed, ItemDisplayContext displayContext) {
            return stack.getItem() instanceof CameraItem camera && camera.isInSelfieMode(stack);
        }

        @Override
        public MapCodec<CameraSelfieProperty> type() {
            return MAP_CODEC;
        }
    }

    /** The old camera_selfie predicate returned 0.5 for the active view entity; this is its exact branch. */
    private record CameraSelfieUserProperty() implements ConditionalItemModelProperty {
        private static final MapCodec<CameraSelfieUserProperty> MAP_CODEC = MapCodec.unit(new CameraSelfieUserProperty());

        @Override
        public boolean get(ItemStack stack, ClientLevel level, LivingEntity entity, int seed, ItemDisplayContext displayContext) {
            return stack.getItem() instanceof CameraItem camera
                    && camera.isInSelfieMode(stack)
                    && entity != null
                    && entity.equals(Minecrft.get().getCameraEntity());
        }

        @Override
        public MapCodec<CameraSelfieUserProperty> type() {
            return MAP_CODEC;
        }
    }

    private record CameraHasLensProperty() implements ConditionalItemModelProperty {
        private static final MapCodec<CameraHasLensProperty> MAP_CODEC = MapCodec.unit(new CameraHasLensProperty());

        @Override
        public boolean get(ItemStack stack, ClientLevel level, LivingEntity entity, int seed, ItemDisplayContext displayContext) {
            return Attachment.LENS.isPresent(stack);
        }

        @Override
        public MapCodec<CameraHasLensProperty> type() {
            return MAP_CODEC;
        }
    }

    private record CameraHasFlashProperty() implements ConditionalItemModelProperty {
        private static final MapCodec<CameraHasFlashProperty> MAP_CODEC = MapCodec.unit(new CameraHasFlashProperty());

        @Override
        public boolean get(ItemStack stack, ClientLevel level, LivingEntity entity, int seed, ItemDisplayContext displayContext) {
            return Attachment.FLASH.isPresent(stack);
        }

        @Override
        public MapCodec<CameraHasFlashProperty> type() {
            return MAP_CODEC;
        }
    }

    private record ProjectorActiveProperty() implements ConditionalItemModelProperty {
        private static final MapCodec<ProjectorActiveProperty> MAP_CODEC = MapCodec.unit(new ProjectorActiveProperty());

        @Override
        public boolean get(ItemStack stack, ClientLevel level, LivingEntity entity, int seed, ItemDisplayContext displayContext) {
            return Config.Server.CAN_PROJECT.get() && stack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
        }

        @Override
        public MapCodec<ProjectorActiveProperty> type() {
            return MAP_CODEC;
        }
    }

    private record ChromaticChannelsProperty() implements RangeSelectItemModelProperty {
        private static final MapCodec<ChromaticChannelsProperty> MAP_CODEC = MapCodec.unit(new ChromaticChannelsProperty());

        @Override
        public float get(ItemStack stack, ClientLevel level, ItemOwner owner, int seed) {
            return stack.getItem() instanceof ChromaticSheetItem sheet ? sheet.getLayers(stack).size() / 10f : 0f;
        }

        @Override
        public MapCodec<ChromaticChannelsProperty> type() {
            return MAP_CODEC;
        }
    }

    private record StackedPhotographsCountProperty() implements RangeSelectItemModelProperty {
        private static final MapCodec<StackedPhotographsCountProperty> MAP_CODEC = MapCodec.unit(new StackedPhotographsCountProperty());

        @Override
        public float get(ItemStack stack, ClientLevel level, ItemOwner owner, int seed) {
            return stack.getItem() instanceof StackedPhotographsItem photographs
                    ? photographs.getPhotographs(stack).size() / 100f : 0f;
        }

        @Override
        public MapCodec<StackedPhotographsCountProperty> type() {
            return MAP_CODEC;
        }
    }

    private record AlbumPhotosProperty() implements RangeSelectItemModelProperty {
        private static final MapCodec<AlbumPhotosProperty> MAP_CODEC = MapCodec.unit(new AlbumPhotosProperty());

        @Override
        public float get(ItemStack stack, ClientLevel level, ItemOwner owner, int seed) {
            return stack.getItem() instanceof AlbumItem album ? album.getPhotographsCount(stack) / 100f : 0f;
        }

        @Override
        public MapCodec<AlbumPhotosProperty> type() {
            return MAP_CODEC;
        }
    }
}

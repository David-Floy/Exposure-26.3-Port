package io.github.mortuusars.exposure;

import io.github.mortuusars.exposure.client.animation.CameraModelPoses;
import io.github.mortuusars.exposure.client.animation.CameraPoses;
import io.github.mortuusars.exposure.client.capture.template.*;
import io.github.mortuusars.exposure.client.task.ClearStaleRenderedImagesIndefiniteTask;
import io.github.mortuusars.exposure.client.RenderedExposures;
import io.github.mortuusars.exposure.client.camera.viewfinder.*;
import io.github.mortuusars.exposure.client.image.modifier.ImageEffect;
import io.github.mortuusars.exposure.client.render.image.ImageRenderer;
import io.github.mortuusars.exposure.client.render.model.ExposureItemModelProperties;
import io.github.mortuusars.exposure.client.render.photograph.PhotographStyle;
import io.github.mortuusars.exposure.client.render.photograph.PhotographRenderer;
import io.github.mortuusars.exposure.client.render.photograph.PhotographStyles;
import io.github.mortuusars.exposure.client.util.Minecrft;
import io.github.mortuusars.exposure.world.camera.capture.CaptureType;
import io.github.mortuusars.exposure.world.item.camera.Attachment;
import io.github.mortuusars.exposure.world.photograph.PhotographType;
import io.github.mortuusars.exposure.util.cycles.Cycles;
import io.github.mortuusars.exposure.client.ExposureStore;
import io.github.mortuusars.exposure.world.item.AlbumItem;
import io.github.mortuusars.exposure.world.item.camera.CameraItem;
import io.github.mortuusars.exposure.world.item.ChromaticSheetItem;
import io.github.mortuusars.exposure.world.item.StackedPhotographsItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashSet;
import java.util.Set;

public class ExposureClient {
    private static final Cycles CYCLES = new Cycles();
    private static final ExposureStore EXPOSURE_STORE = new ExposureStore();
    private static final RenderedExposures RENDERED_EXPOSURES = new RenderedExposures();
    private static final ImageRenderer IMAGE_RENDERER = new ImageRenderer();
    private static final PhotographRenderer PHOTOGRAPH_RENDERER = new PhotographRenderer();

    public static void init() {
        CameraModelPoses.register(Exposure.Items.CAMERA.get(), new CameraPoses());

        ViewfinderRegistry.register(Exposure.Items.CAMERA.get(), Viewfinder::new);

        CaptureTemplates.register(CaptureType.CAMERA, new CameraCaptureTemplate());
        CaptureTemplates.register(CaptureType.EXPOSE_COMMAND, new ExposeCaptureTemplate());
        CaptureTemplates.register(CaptureType.LOAD_COMMAND, new PathCaptureTemplate());
        CaptureTemplates.register(CaptureType.DEBUG_RGB, new SingleChannelCaptureTemplate());

        PhotographStyles.register(PhotographType.REGULAR, PhotographStyle.REGULAR);
        PhotographStyles.register(PhotographType.AGED, new PhotographStyle(
                ExposureClient.Textures.Photograph.AGED_PAPER,
                ExposureClient.Textures.Photograph.AGED_OVERLAY,
                ExposureClient.Textures.Photograph.AGED_ALBUM_PAPER,
                ExposureClient.Textures.Photograph.AGED_ALBUM_OVERLAY,
                ImageEffect.AGED));

        cycles().addParallelTask(new ClearStaleRenderedImagesIndefiniteTask());

        ExposureItemModelProperties.register();
    }

    public static Cycles cycles() {
        return CYCLES;
    }

    public static ExposureStore exposureStore() {
        return EXPOSURE_STORE;
    }

    public static RenderedExposures renderedExposures() {
        return RENDERED_EXPOSURES;
    }

    public static ImageRenderer imageRenderer() {
        return IMAGE_RENDERER;
    }

    public static PhotographRenderer photographRenderer() {
        return PHOTOGRAPH_RENDERER;
    }

    // --

    public static boolean shouldUseDirectCapture() {
        //TODO: maybe check if neoforge is ok and then enable it only on fabric?
        if (PlatformHelper.isModLoaded("distanthorizons")
                && (PlatformHelper.isModLoaded("oculus") || PlatformHelper.isModLoaded("iris"))) {
            return true;
        }

        return Config.Client.FORCE_DIRECT_CAPTURE.isTrue()
                || Config.Client.FORCE_DIRECT_CAPTURE_MODS.get().stream().anyMatch(PlatformHelper::isModLoaded);
    }

    // --

    public static class Models {
        public static final Set<Identifier> MODELS = new LinkedHashSet<>();

        public static final Identifier PHOTOGRAPH_FRAME_SMALL = register("block/photograph_frame_small");
        public static final Identifier PHOTOGRAPH_FRAME_MEDIUM = register("block/photograph_frame_medium");
        public static final Identifier PHOTOGRAPH_FRAME_LARGE = register("block/photograph_frame_large");
        public static final Identifier CLEAR_PHOTOGRAPH_FRAME_SMALL = register("block/glass_photograph_frame_small");
        public static final Identifier CLEAR_PHOTOGRAPH_FRAME_MEDIUM = register("block/glass_photograph_frame_medium");
        public static final Identifier CLEAR_PHOTOGRAPH_FRAME_LARGE = register("block/glass_photograph_frame_large");
        public static final Identifier CAMERA_STAND = register("block/camera_stand");
        public static final Identifier CAMERA_STAND_MOUNT = register("block/camera_stand_mount");

        public static Identifier register(String path) {
            Identifier location = Exposure.resource(path);
            MODELS.add(location);
            return location;
        }
    }

    public static class Textures {
        public static final Identifier EMPTY = Exposure.resource("textures/empty.png");

        public static class Photograph {
            public static final Identifier REGULAR_PAPER = Exposure.resource("textures/photograph/photograph.png");
            public static final Identifier REGULAR_ALBUM_PAPER = Exposure.resource("textures/photograph/photograph_album.png");

            public static final Identifier AGED_PAPER = Exposure.resource("textures/photograph/aged_photograph.png");
            public static final Identifier AGED_OVERLAY = Exposure.resource("textures/photograph/aged_photograph_overlay.png");
            public static final Identifier AGED_ALBUM_PAPER = Exposure.resource("textures/photograph/aged_photograph_album.png");
            public static final Identifier AGED_ALBUM_OVERLAY = Exposure.resource("textures/photograph/aged_photograph_album_overlay.png");
        }
    }
}

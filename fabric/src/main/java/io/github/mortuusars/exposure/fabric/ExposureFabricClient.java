package io.github.mortuusars.exposure.fabric;

import com.mojang.serialization.MapCodec;
import fuzs.forgeconfigapiport.fabric.api.v5.client.ConfigScreenFactoryRegistry;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.PlatformHelperClient;
import io.github.mortuusars.exposure.client.gui.tooltip.CameraStandTooltip;
import io.github.mortuusars.exposure.client.input.KeyboardHandler;
import io.github.mortuusars.exposure.client.render.CameraStandEntityRenderer;
import io.github.mortuusars.exposure.client.render.GlassPhotographFrameEntityRenderer;
import io.github.mortuusars.exposure.fabric.resources.ExposureFabricClientReloadListener;
import io.github.mortuusars.exposure.fabric.resources.IdentifiableResourceReloadListenerWrapper;
import io.github.mortuusars.exposure.client.gui.tooltip.PhotographClientTooltip;
import io.github.mortuusars.exposure.client.gui.screen.ItemRenameScreen;
import io.github.mortuusars.exposure.client.gui.screen.LightroomScreen;
import io.github.mortuusars.exposure.client.gui.screen.album.AlbumScreen;
import io.github.mortuusars.exposure.client.gui.screen.album.LecternAlbumScreen;
import io.github.mortuusars.exposure.client.gui.screen.camera.CameraAttachmentsScreen;
import io.github.mortuusars.exposure.world.inventory.tooltip.PhotographTooltip;
import io.github.mortuusars.exposure.world.item.camera.CameraItem;
import io.github.mortuusars.exposure.network.fabric.FabricS2CPacketHandler;
import io.github.mortuusars.exposure.client.render.PhotographFrameEntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

public class ExposureFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PlatformHelperClient.bind(new PlatformHelperClientImpl());

        ExposureClient.init();

        ItemTintSources.ID_MAPPER.put(Exposure.resource("camera_glass"), CameraGlassTintSource.MAP_CODEC);

        ConfigScreenFactoryRegistry.INSTANCE.register(Exposure.ID, ConfigurationScreen::new);

        KeyboardHandler.registerKeymappings(KeyMappingHelper::registerKeyMapping);

        MenuScreens.register(Exposure.MenuTypes.CAMERA_IN_HAND.get(), CameraAttachmentsScreen::new);
        MenuScreens.register(Exposure.MenuTypes.CAMERA_ON_STAND.get(), CameraAttachmentsScreen::new);
        MenuScreens.register(Exposure.MenuTypes.ALBUM.get(), AlbumScreen::new);
        MenuScreens.register(Exposure.MenuTypes.LECTERN_ALBUM.get(), LecternAlbumScreen::new);
        MenuScreens.register(Exposure.MenuTypes.LIGHTROOM.get(), LightroomScreen::new);
        MenuScreens.register(Exposure.MenuTypes.ITEM_RENAME.get(), ItemRenameScreen::new);

        ModelLoadingPlugin.register(pluginContext ->
                PlatformHelperClientImpl.registerModels(pluginContext, ExposureClient.Models.MODELS));

        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                ExposureFabricClientReloadListener.ID,
                new IdentifiableResourceReloadListenerWrapper(
                        ExposureFabricClientReloadListener.ID, new ExposureFabricClientReloadListener()));

        EntityRendererRegistry.register(Exposure.EntityTypes.PHOTOGRAPH_FRAME.get(), PhotographFrameEntityRenderer::new);
        EntityRendererRegistry.register(Exposure.EntityTypes.CLEAR_PHOTOGRAPH_FRAME.get(), GlassPhotographFrameEntityRenderer::new);
        EntityRendererRegistry.register(Exposure.EntityTypes.CAMERA_STAND.get(), CameraStandEntityRenderer::new);

        ClientTooltipComponentCallback.EVENT.register(data -> data instanceof PhotographTooltip photographTooltip
                ? new PhotographClientTooltip(photographTooltip) : null);

        HudElementRegistry.addLast(Exposure.resource("camera_stand_tooltip"), CameraStandTooltip::render);

        FabricS2CPacketHandler.register();

    }

    /** The former Fabric item-color provider's tint-index 1 branch. */
    private record CameraGlassTintSource() implements ItemTintSource {
        private static final MapCodec<CameraGlassTintSource> MAP_CODEC = MapCodec.unit(new CameraGlassTintSource());

        @Override
        public int calculate(ItemStack stack, ClientLevel level, LivingEntity entity) {
            return CameraItem.getGlassTintColor(stack, 1);
        }

        @Override
        public MapCodec<CameraGlassTintSource> type() {
            return MAP_CODEC;
        }
    }
}

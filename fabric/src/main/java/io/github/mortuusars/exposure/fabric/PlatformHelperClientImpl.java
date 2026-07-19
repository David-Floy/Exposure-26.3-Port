package io.github.mortuusars.exposure.fabric;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.resources.Identifier;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.FabricModelManager;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class PlatformHelperClientImpl implements io.github.mortuusars.exposure.PlatformHelperClient.Service {
    private static final Map<Identifier, ExtraModelKey<BlockStateModel>> EXTRA_MODELS = new LinkedHashMap<>();

    public static void registerModels(ModelLoadingPlugin.Context context, Collection<Identifier> models) {
        models.forEach(model -> context.addModel(key(model), SimpleUnbakedExtraModel.blockStateModel(model)));
    }

    private static ExtraModelKey<BlockStateModel> key(Identifier model) {
        return EXTRA_MODELS.computeIfAbsent(model, id -> ExtraModelKey.create(id::toString));
    }

    public BlockStateModel getModel(Identifier model) {
        return ((FabricModelManager) Minecraft.getInstance().getModelManager()).getModel(key(model));
    }
}

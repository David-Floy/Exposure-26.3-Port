package io.github.mortuusars.exposure.client.render.model;

import com.mojang.math.Transformation;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.EmptyModel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemModels;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;

import java.util.List;
import java.util.Optional;

/**
 * Composite item model whose layers all use the display transform of its first layer.
 * <p>
 * Upstream merged the camera attachment models into the camera body model, so attachments always used the body's
 * display transforms. Vanilla {@code minecraft:composite} applies each sub-model's own display transforms, and the
 * attachment models define none, which misplaced them wherever the camera has a display transform (dropped, held,
 * in item frames). Local {@code transformation} values still apply per layer.
 */
public class SharedTransformCompositeModel implements ItemModel {
    private final List<ItemModel> models;

    public SharedTransformCompositeModel(List<ItemModel> models) {
        this.models = models;
    }

    @Override
    public void update(ItemStackRenderState output, ItemStack item, ItemModelResolver resolver, ItemDisplayContext displayContext,
                       @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        output.appendModelIdentityElement(this);
        output.ensureCapacity(this.models.size());

        int firstLayer = output.activeLayerCount;
        for (ItemModel model : this.models) {
            model.update(output, item, resolver, displayContext, level, owner, seed);
        }

        if (output.activeLayerCount <= firstLayer) return;

        ItemTransform transform = output.layers[firstLayer].itemTransform;
        for (int i = firstLayer + 1; i < output.activeLayerCount; i++) {
            output.layers[i].setItemTransform(transform);
        }
    }

    public record Unbaked(List<ItemModel.Unbaked> models, Optional<Transformation> transformation) implements ItemModel.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ItemModels.CODEC.listOf().fieldOf("models").forGetter(Unbaked::models),
                Transformation.EXTENDED_CODEC.optionalFieldOf("transformation").forGetter(Unbaked::transformation)
        ).apply(instance, Unbaked::new));

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            for (ItemModel.Unbaked model : this.models) {
                model.resolveDependencies(resolver);
            }
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            if (this.models.isEmpty()) return EmptyModel.INSTANCE;

            Matrix4fc childTransform = Transformation.compose(transformation, this.transformation);
            return this.models.size() == 1
                    ? this.models.getFirst().bake(context, childTransform)
                    : new SharedTransformCompositeModel(this.models.stream().map(model -> model.bake(context, childTransform)).toList());
        }
    }
}

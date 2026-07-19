package io.github.mortuusars.exposure.fabric;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.serialization.MapCodec;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.Register;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.criterion.EntitySubPredicate;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.predicates.DataComponentPredicate;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class RegisterImpl implements Register.Service {
    public <T extends Block> Supplier<T> block(String id, Supplier<T> supplier) {
        T obj = Registry.register(BuiltInRegistries.BLOCK, Exposure.resource(id), supplier.get());
        return () -> obj;
    }

    public <T extends BlockEntityType<E>, E extends BlockEntity> Supplier<T> blockEntityType(String id, Supplier<T> supplier) {
        T obj = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Exposure.resource(id), supplier.get());
        return () -> obj;
    }

    public <T extends BlockEntity> BlockEntityType<T> newBlockEntityType(Register.BlockEntitySupplier<T> blockEntitySupplier, Block... validBlocks) {
        return FabricBlockEntityTypeBuilder.create(blockEntitySupplier::create, validBlocks).build();
    }

    public <T extends Item> Supplier<T> item(String id, Supplier<T> supplier) {
        T obj = Registry.register(BuiltInRegistries.ITEM, Exposure.resource(id), supplier.get());
        return () -> obj;
    }

    public <T extends CreativeModeTab> Supplier<T> creativeTab(String id, Supplier<T> supplier) {
        T obj = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Exposure.resource(id), supplier.get());
        return () -> obj;
    }

    public <T extends Entity> Supplier<EntityType<T>> entityType(String id, EntityType.EntityFactory<T> factory,
                                                                        MobCategory category, float width, float height,
                                                                        int clientTrackingRange, boolean velocityUpdates, int updateInterval) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Exposure.resource(id));
        EntityType<T> type = Registry.register(BuiltInRegistries.ENTITY_TYPE, key,
                EntityType.Builder.of(factory, category)
                        .sized(width, height)
                        .clientTrackingRange(clientTrackingRange)
                        .alwaysUpdateVelocity(velocityUpdates)
                        .updateInterval(updateInterval)
                        .build(key));
        return () -> type;
    }

    public <T extends Entity> Supplier<EntityType<T>> entityType(String id, EntityType.EntityFactory<T> factory, MobCategory category, boolean receiveVelocityUpdates, Consumer<EntityType.Builder<T>> typeBuilder) {
        EntityType.Builder<T> builder = EntityType.Builder.of(factory, category);
        typeBuilder.accept(builder);
        builder.alwaysUpdateVelocity(receiveVelocityUpdates);
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Exposure.resource(id));
        EntityType<T> type = Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
        return () -> type;
    }

    public <T extends SoundEvent> Supplier<T> soundEvent(String id, Supplier<T> supplier) {
        T obj = Registry.register(BuiltInRegistries.SOUND_EVENT, Exposure.resource(id), supplier.get());
        return () -> obj;
    }

    public <E extends AbstractContainerMenu> Supplier<MenuType<E>> menuType(String id, Register.MenuTypeSupplier<E> supplier) {
        ExtendedMenuType<E, byte[]> type = new ExtendedMenuType<>((syncId, inventory, data) -> {
            RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(data), inventory.player.registryAccess());
            try {
                return supplier.create(syncId, inventory, buffer);
            } finally {
                buffer.release();
            }
        }, ByteBufCodecs.BYTE_ARRAY.mapStream(Function.identity()));

        Registry.register(BuiltInRegistries.MENU, Exposure.resource(id), type);

        return () -> type;
    }

    public <T extends Recipe<I>, I extends RecipeInput> Supplier<RecipeType<T>> recipeType(String id, Supplier<RecipeType<T>> supplier) {
        RecipeType<T> obj = Registry.register(BuiltInRegistries.RECIPE_TYPE, Exposure.resource(id), supplier.get());
        return () -> obj;
    }

    public <T extends Recipe<?>> Supplier<RecipeSerializer<T>> recipeSerializer(
            String id, Supplier<RecipeSerializer<T>> supplier) {
        RecipeSerializer<T> obj = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Exposure.resource(id), supplier.get());
        return () -> obj;
    }

    public <T extends CriterionTrigger<?>> Supplier<T> criterionTrigger(String name, Supplier<T> supplier) {
        T obj = Registry.register(BuiltInRegistries.TRIGGER_TYPES, Exposure.resource(name), supplier.get());
        return () -> obj;
    }

    public <T extends DataComponentPredicate> Supplier<DataComponentPredicate.Type<T>> itemSubPredicate(
            String name, Supplier<DataComponentPredicate.Type<T>> supplier) {
        DataComponentPredicate.Type<T> obj = Registry.register(
                BuiltInRegistries.DATA_COMPONENT_PREDICATE_TYPE, Exposure.resource(name), supplier.get());
        return () -> obj;
    }

    public <T extends EntitySubPredicate> Supplier<MapCodec<T>> entitySubPredicate(String name, Supplier<MapCodec<T>> supplier) {
        MapCodec<T> obj = Registry.register(BuiltInRegistries.ENTITY_SUB_PREDICATE_TYPE, Exposure.resource(name), supplier.get());
        return () -> obj;
    }

    public <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>, I extends ArgumentTypeInfo<A, T>>
    Supplier<ArgumentTypeInfo<A, T>> commandArgumentType(String id, Class<A> infoClass, I argumentTypeInfo) {
        ArgumentTypeRegistry.registerArgumentType(Exposure.resource(id), infoClass, argumentTypeInfo);
        return () -> argumentTypeInfo;
    }

    public <T extends FeatureConfiguration> Supplier<Feature<?>> worldGenFeature(String name, Supplier<Feature<T>> featureSupplier) {
        Feature<T> feature = Registry.register(BuiltInRegistries.FEATURE, name, featureSupplier.get());
        return () -> feature;
    }

    public <T> DataComponentType<T> dataComponentType(String name, Consumer<DataComponentType.Builder<T>> builderConsumer) {
        var builder = DataComponentType.<T>builder();
        builderConsumer.accept(builder);
        var componentType = builder.build();
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Exposure.resource(name), componentType);
    }

    public <T extends ParticleType<? extends ParticleOptions>> Supplier<T> particleType(String name, Supplier<T> supplier) {
        T particleType = Registry.register(BuiltInRegistries.PARTICLE_TYPE, name, supplier.get());
        return () -> particleType;
    }
}

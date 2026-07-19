package io.github.mortuusars.exposure;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.serialization.MapCodec;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.criterion.EntitySubPredicate;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.predicates.DataComponentPredicate;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Inventory;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class Register {
    private static volatile Service service;

    public static void bind(Service implementation) {
        if (service != null) {
            throw new IllegalStateException("Exposure registry service is already bound.");
        }
        service = java.util.Objects.requireNonNull(implementation, "implementation");
    }

    private static Service service() {
        Service implementation = service;
        if (implementation == null) {
            throw new IllegalStateException("Exposure registry service has not been bound by the Fabric entrypoint.");
        }
        return implementation;
    }

    public static <T extends Block> Supplier<T> block(String id, Supplier<T> supplier) {
        return service().block(id, supplier);
    }

    public static <T extends BlockEntityType<E>, E extends BlockEntity> Supplier<T> blockEntityType(String id, Supplier<T> sup) {
        return service().blockEntityType(id, sup);
    }

    public static <T extends BlockEntity> BlockEntityType<T> newBlockEntityType(BlockEntitySupplier<T> blockEntitySupplier, Block... validBlocks) {
        return service().newBlockEntityType(blockEntitySupplier, validBlocks);
    }

    @FunctionalInterface
    public interface BlockEntitySupplier<T extends BlockEntity> {

        @NotNull T create(BlockPos pos, BlockState state);
    }

    public static <T extends Item> Supplier<T> item(String id, Supplier<T> supplier) {
        return service().item(id, supplier);
    }

    public static <T extends CreativeModeTab> Supplier<T> creativeTab(String id, Supplier<T> supplier) {
        return service().creativeTab(id, supplier);
    }

    public static <T extends Entity> Supplier<EntityType<T>> entityType(String id, EntityType.EntityFactory<T> factory,
                                                                        MobCategory category, float width, float height,
                                                                        int clientTrackingRange, boolean velocityUpdates, int updateInterval) {
        return service().entityType(id, factory, category, width, height, clientTrackingRange, velocityUpdates, updateInterval);
    }

    public static <T extends Entity> Supplier<EntityType<T>> entityType(String id, EntityType.EntityFactory<T> factory, MobCategory category,
                                                                        boolean receiveVelocityUpdates, Consumer<EntityType.Builder<T>> typeBuilder) {
        return service().entityType(id, factory, category, receiveVelocityUpdates, typeBuilder);
    }

    public static <T extends SoundEvent> Supplier<T> soundEvent(String id, Supplier<T> supplier) {
        return service().soundEvent(id, supplier);
    }

    public static <E extends AbstractContainerMenu> Supplier<MenuType<E>> menuType(String id, MenuTypeSupplier<E> supplier) {
        return service().menuType(id, supplier);
    }

    @FunctionalInterface
    public interface MenuTypeSupplier<T extends AbstractContainerMenu> {
        @NotNull T create(int windowId, Inventory playerInv, RegistryFriendlyByteBuf extraData);
    }

    public static <T extends Recipe<I>, I extends RecipeInput> Supplier<RecipeType<T>> recipeType(String name, Supplier<RecipeType<T>> supplier) {
        return service().recipeType(name, supplier);
    }

    public static <T extends Recipe<?>> Supplier<RecipeSerializer<T>> recipeSerializer(
            String name, Supplier<RecipeSerializer<T>> supplier) {
        return service().recipeSerializer(name, supplier);
    }

    public static <T extends CriterionTrigger<?>> Supplier<T> criterionTrigger(String name, Supplier<T> supplier) {
        return service().criterionTrigger(name, supplier);
    }

    public static <T extends DataComponentPredicate> Supplier<DataComponentPredicate.Type<T>> itemSubPredicate(
            String name, Supplier<DataComponentPredicate.Type<T>> supplier) {
        return service().itemSubPredicate(name, supplier);
    }

    public static <T extends EntitySubPredicate> Supplier<MapCodec<T>> entitySubPredicate(String name, Supplier<MapCodec<T>> supplier) {
        return service().entitySubPredicate(name, supplier);
    }

    public static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>, I extends ArgumentTypeInfo<A, T>>
    Supplier<ArgumentTypeInfo<A, T>> commandArgumentType(String id, Class<A> infoClass, I argumentTypeInfo) {
        return service().commandArgumentType(id, infoClass, argumentTypeInfo);
    }

    public static <T extends FeatureConfiguration> Supplier<Feature<?>> worldGenFeature(String name, Supplier<Feature<T>> featureSupplier) {
        return service().worldGenFeature(name, featureSupplier);
    }

    public static <T> DataComponentType<T> dataComponentType(String name, Consumer<DataComponentType.Builder<T>> builderConsumer) {
        return service().dataComponentType(name, builderConsumer);
    }

    public static <T extends ParticleType<? extends ParticleOptions>> Supplier<T> particleType(String name, Supplier<T> supplier) {
        return service().particleType(name, supplier);
    }

    public interface Service {
        <T extends Block> Supplier<T> block(String id, Supplier<T> supplier);
        <T extends BlockEntityType<E>, E extends BlockEntity> Supplier<T> blockEntityType(String id, Supplier<T> supplier);
        <T extends BlockEntity> BlockEntityType<T> newBlockEntityType(BlockEntitySupplier<T> blockEntitySupplier, Block... validBlocks);
        <T extends Item> Supplier<T> item(String id, Supplier<T> supplier);
        <T extends CreativeModeTab> Supplier<T> creativeTab(String id, Supplier<T> supplier);
        <T extends Entity> Supplier<EntityType<T>> entityType(String id, EntityType.EntityFactory<T> factory,
                                                              MobCategory category, float width, float height,
                                                              int clientTrackingRange, boolean velocityUpdates, int updateInterval);
        <T extends Entity> Supplier<EntityType<T>> entityType(String id, EntityType.EntityFactory<T> factory,
                                                              MobCategory category, boolean receiveVelocityUpdates,
                                                              Consumer<EntityType.Builder<T>> typeBuilder);
        <T extends SoundEvent> Supplier<T> soundEvent(String id, Supplier<T> supplier);
        <E extends AbstractContainerMenu> Supplier<MenuType<E>> menuType(String id, MenuTypeSupplier<E> supplier);
        <T extends Recipe<I>, I extends RecipeInput> Supplier<RecipeType<T>> recipeType(String name, Supplier<RecipeType<T>> supplier);
        <T extends Recipe<?>> Supplier<RecipeSerializer<T>> recipeSerializer(
                String name, Supplier<RecipeSerializer<T>> supplier);
        <T extends CriterionTrigger<?>> Supplier<T> criterionTrigger(String name, Supplier<T> supplier);
        <T extends DataComponentPredicate> Supplier<DataComponentPredicate.Type<T>> itemSubPredicate(
                String name, Supplier<DataComponentPredicate.Type<T>> supplier);
        <T extends EntitySubPredicate> Supplier<MapCodec<T>> entitySubPredicate(String name, Supplier<MapCodec<T>> supplier);
        <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>, I extends ArgumentTypeInfo<A, T>>
        Supplier<ArgumentTypeInfo<A, T>> commandArgumentType(String id, Class<A> infoClass, I argumentTypeInfo);
        <T extends FeatureConfiguration> Supplier<Feature<?>> worldGenFeature(String name, Supplier<Feature<T>> featureSupplier);
        <T> DataComponentType<T> dataComponentType(String name, Consumer<DataComponentType.Builder<T>> builderConsumer);
        <T extends ParticleType<? extends ParticleOptions>> Supplier<T> particleType(String name, Supplier<T> supplier);
    }
}

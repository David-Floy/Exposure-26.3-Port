package io.github.mortuusars.exposure.world.item.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * Preserves the transient entity context that ItemStack exposed before 26.1.
 *
 * <p>This association is deliberately not a data component: it must not be serialized, copied into
 * packet fields, or become part of stack equality. Weak keys and values avoid retaining either the
 * synchronized stack or its owning entity.</p>
 */
public final class ItemStackEntityContext {
    private static final Map<ItemStack, WeakReference<Entity>> CONTEXTS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private ItemStackEntityContext() {
    }

    public static void associate(ItemStack stack, Entity entity) {
        CONTEXTS.put(stack, new WeakReference<>(entity));
    }

    public static Optional<Entity> get(ItemStack stack) {
        WeakReference<Entity> reference = CONTEXTS.get(stack);
        return reference == null ? Optional.empty() : Optional.ofNullable(reference.get());
    }
}

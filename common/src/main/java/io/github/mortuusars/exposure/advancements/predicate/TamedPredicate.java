package io.github.mortuusars.exposure.advancements.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.entity.EntitySubPredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 26.3 entity predicates are a map keyed by sub-predicate type, so this is written as
 * {@code "exposure:tamed": {"is_tamed": true}} instead of a {@code type_specific} entry.
 */
public record TamedPredicate(boolean isTamed) implements EntitySubPredicate {
    public static final Codec<TamedPredicate> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(Codec.BOOL.fieldOf("is_tamed").forGetter(TamedPredicate::isTamed))
                    .apply(instance, TamedPredicate::new)
    );

    @Override
    public boolean matches(Entity entity, ServerLevel level, @Nullable Vec3 position) {
        return entity instanceof TamableAnimal animal && animal.isTame() == isTamed;
    }
}

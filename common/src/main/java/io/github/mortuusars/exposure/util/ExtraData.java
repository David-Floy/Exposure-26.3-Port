package io.github.mortuusars.exposure.util;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import io.github.mortuusars.exposure.Exposure;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.*;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.function.TriConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Type-safe view over an extensible compound tag.
 * Unknown entries are preserved so frame metadata from older versions and other mods survives a round trip.
 */
public final class ExtraData {
    public static final Codec<ExtraData> CODEC = CompoundTag.CODEC.xmap(ExtraData::new, ExtraData::copyTag);
    public static final StreamCodec<ByteBuf, ExtraData> STREAM_CODEC =
            ByteBufCodecs.COMPOUND_TAG.map(ExtraData::new, ExtraData::copyTag);

    public static final ExtraData EMPTY = new ExtraData(new CompoundTag(), false);

    private final CompoundTag tag;
    private final boolean mutable;

    public ExtraData() {
        this(new CompoundTag(), true);
    }

    public ExtraData(CompoundTag tag) {
        this(tag, true);
    }

    private ExtraData(CompoundTag tag, boolean mutable) {
        this.tag = tag.copy();
        this.mutable = mutable;
    }

    /** Returns a detached tag suitable for codecs and NBT boundaries. */
    public @NotNull CompoundTag asTag() {
        return copyTag();
    }

    /** Returns a deep copy; callers cannot mutate this ExtraData through it. */
    public @NotNull CompoundTag copyTag() {
        return tag.copy();
    }

    public boolean contains(String key) {
        return tag.contains(key);
    }

    public int size() {
        return tag.size();
    }

    public <T> Optional<T> get(@NotNull ExtraData.Type<T> type) {
        if (!contains(type.key())) return Optional.empty();
        try {
            return Optional.ofNullable(type.getter().apply(this, type.key()));
        } catch (Exception e) {
            Exposure.LOGGER.error("Cannot get ExtraData entry: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public <T> T getOrDefault(@NotNull ExtraData.Type<T> type, T defaultValue) {
        if (!contains(type.key())) return defaultValue;
        try {
            @Nullable T value = type.getter().apply(this, type.key());
            return value != null ? value : defaultValue;
        } catch (Exception e) {
            Exposure.LOGGER.error("Cannot get ExtraData entry: {}", e.getMessage());
            return defaultValue;
        }
    }

    public <T> void put(Type<T> type, @NotNull T value) {
        Preconditions.checkNotNull(value, "value");
        ensureMutable();
        type.setter().accept(this, type.key(), value);
    }

    public <T> void remove(Type<T> type) {
        remove(type.key());
    }

    public @NotNull ExtraData copy() {
        return new ExtraData(tag);
    }

    public @NotNull ExtraData merge(ExtraData other) {
        return merge(other.tag);
    }

    public @NotNull ExtraData merge(CompoundTag other) {
        ensureMutable();
        for (String key : other.keySet()) {
            Tag value = other.get(key);
            if (value == null) continue;

            if (value instanceof CompoundTag incoming && tag.get(key) instanceof CompoundTag existing) {
                existing.merge(incoming);
            } else {
                tag.put(key, value.copy());
            }
        }
        return this;
    }

    public String getString(String key) {
        return tag.getStringOr(key, "");
    }

    public boolean getBoolean(String key) {
        return tag.getBooleanOr(key, false);
    }

    public int getInt(String key) {
        return tag.getIntOr(key, 0);
    }

    public long getLong(String key) {
        return tag.getLongOr(key, 0L);
    }

    public float getFloat(String key) {
        return tag.getFloatOr(key, 0.0F);
    }

    public double getDouble(String key) {
        return tag.getDoubleOr(key, 0.0D);
    }

    public ListTag getList(String key, int elementType) {
        ListTag list = tag.getListOrEmpty(key);
        if (!list.isEmpty() && list.stream().anyMatch(value -> value.getId() != elementType)) {
            return new ListTag();
        }
        return list;
    }

    public void put(String key, Tag value) {
        ensureMutable();
        tag.put(key, value);
    }

    public void putString(String key, String value) {
        ensureMutable();
        tag.putString(key, value);
    }

    public void putBoolean(String key, boolean value) {
        ensureMutable();
        tag.putBoolean(key, value);
    }

    public void putInt(String key, int value) {
        ensureMutable();
        tag.putInt(key, value);
    }

    public void putLong(String key, long value) {
        ensureMutable();
        tag.putLong(key, value);
    }

    public void putFloat(String key, float value) {
        ensureMutable();
        tag.putFloat(key, value);
    }

    public void putDouble(String key, double value) {
        ensureMutable();
        tag.putDouble(key, value);
    }

    public void remove(String key) {
        ensureMutable();
        tag.remove(key);
    }

    private void ensureMutable() {
        if (!mutable) {
            throw new UnsupportedOperationException("ExtraData.EMPTY is immutable");
        }
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof ExtraData extraData && tag.equals(extraData.tag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tag);
    }

    @Override
    public String toString() {
        return tag.toString();
    }

    public record Type<T>(String key, BiFunction<ExtraData, String, @Nullable T> getter,
                          TriConsumer<ExtraData, String, T> setter) {
        public static Type<String> string(String key) {
            return new Type<>(key, ExtraData::getString, ExtraData::putString);
        }

        public static Type<Boolean> bool(String key) {
            return new Type<>(key, ExtraData::getBoolean, ExtraData::putBoolean);
        }

        public static Type<Integer> intVal(String key) {
            return new Type<>(key, ExtraData::getInt, ExtraData::putInt);
        }

        public static Type<Long> longVal(String key) {
            return new Type<>(key, ExtraData::getLong, ExtraData::putLong);
        }

        public static Type<Float> floatVal(String key) {
            return new Type<>(key, ExtraData::getFloat, ExtraData::putFloat);
        }

        public static Type<Double> doubleVal(String key) {
            return new Type<>(key, ExtraData::getDouble, ExtraData::putDouble);
        }

        public static <T extends StringRepresentable> Type<T> stringRepresentable(
                String key, Function<String, @Nullable T> deserializeFunction) {
            return new Type<>(key,
                    (data, k) -> deserializeFunction.apply(data.getString(k)),
                    (data, k, value) -> data.putString(k, value.getSerializedName()));
        }

        public static Type<Vec3> vec3(String key) {
            return new Type<>(key,
                    (data, k) -> {
                        ListTag pos = data.getList(k, DoubleTag.TAG_DOUBLE);
                        return new Vec3(pos.getDoubleOr(0, 0.0D), pos.getDoubleOr(1, 0.0D), pos.getDoubleOr(2, 0.0D));
                    },
                    (data, k, value) -> {
                        ListTag pos = new ListTag();
                        pos.add(DoubleTag.valueOf(value.x()));
                        pos.add(DoubleTag.valueOf(value.y()));
                        pos.add(DoubleTag.valueOf(value.z()));
                        data.put(k, pos);
                    });
        }

        public static Type<Identifier> resourceLocation(String key) {
            return new Type<>(key,
                    (data, k) -> Identifier.parse(data.getString(k)),
                    (data, k, value) -> data.putString(k, value.toString()));
        }

        public static <T> Type<List<T>> list(
                String key, int tagType, Function<Tag, T> extractFunc, Function<T, Tag> packFunc) {
            return new Type<>(key,
                    (data, k) -> data.getList(k, tagType).stream().map(extractFunc).toList(),
                    (data, k, value) -> {
                        ListTag list = new ListTag();
                        list.addAll(value.stream().map(packFunc).toList());
                        data.put(k, list);
                    });
        }

        public static <T> Type<List<T>> stringBasedList(
                String key, Function<String, T> extractFunc, Function<T, String> packFunc) {
            return list(key, Tag.TAG_STRING,
                    tag -> extractFunc.apply(tag.asString().orElse("")),
                    value -> StringTag.valueOf(packFunc.apply(value)));
        }
    }
}

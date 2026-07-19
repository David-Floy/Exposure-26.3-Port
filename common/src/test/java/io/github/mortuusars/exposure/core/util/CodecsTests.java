package io.github.mortuusars.exposure.core.util;

import com.google.gson.JsonArray;
import com.mojang.serialization.JsonOps;
import io.github.mortuusars.exposure.util.Codecs;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CodecsTests {
    @Test
    void intArrayCodecJson() {
        int[] array = new int[]{-1, 0, 42, Integer.MAX_VALUE};

        JsonArray jsonArray = Codecs.intArrayCodec(0, 256).encodeStart(JsonOps.INSTANCE, array).getOrThrow().getAsJsonArray();

        assertEquals(-1, jsonArray.get(0).getAsInt());
        assertEquals(0, jsonArray.get(1).getAsInt());
        assertEquals(42, jsonArray.get(2).getAsInt());
        assertEquals(Integer.MAX_VALUE, jsonArray.get(3).getAsInt());
        assertEquals(4, jsonArray.size());
    }

    @Test
    void intArrayCodecNbt() {
        int[] array = new int[]{-1, 0, 42, Integer.MAX_VALUE};

        ListTag tag = assertInstanceOf(ListTag.class,
                Codecs.intArrayCodec(0, 256).encodeStart(NbtOps.INSTANCE, array).getOrThrow());

        assertEquals(-1, assertInstanceOf(IntTag.class, tag.get(0)).intValue());
        assertEquals(0, assertInstanceOf(IntTag.class, tag.get(1)).intValue());
        assertEquals(42, assertInstanceOf(IntTag.class, tag.get(2)).intValue());
        assertEquals(Integer.MAX_VALUE, assertInstanceOf(IntTag.class, tag.get(3)).intValue());
        assertEquals(4, tag.size());
        assertArrayEquals(array, Codecs.intArrayCodec(0, 256).parse(NbtOps.INSTANCE, tag).getOrThrow());
    }

    @Test
    void byteArrayCodecJson() {
        byte[] array = new byte[]{-1, 0, 42, (byte) 0xFF};

        JsonArray jsonArray = Codecs.byteArrayCodec(0, 256).encodeStart(JsonOps.INSTANCE, array).getOrThrow().getAsJsonArray();

        assertEquals(-1, jsonArray.get(0).getAsByte());
        assertEquals(0, jsonArray.get(1).getAsByte());
        assertEquals(42, jsonArray.get(2).getAsByte());
        assertEquals((byte)0xFF, jsonArray.get(3).getAsByte());
        assertEquals(4, jsonArray.size());
    }

    @Test
    void byteArrayCodecNbt() {
        byte[] array = new byte[]{-1, 0, 42, (byte) 0xFF};

        ListTag tag = assertInstanceOf(ListTag.class,
                Codecs.byteArrayCodec(0, 256).encodeStart(NbtOps.INSTANCE, array).getOrThrow());

        assertEquals((byte) -1, assertInstanceOf(ByteTag.class, tag.get(0)).byteValue());
        assertEquals((byte) 0, assertInstanceOf(ByteTag.class, tag.get(1)).byteValue());
        assertEquals((byte) 42, assertInstanceOf(ByteTag.class, tag.get(2)).byteValue());
        assertEquals((byte) 0xFF, assertInstanceOf(ByteTag.class, tag.get(3)).byteValue());
        assertEquals(4, tag.size());
        assertArrayEquals(array, Codecs.byteArrayCodec(0, 256).parse(NbtOps.INSTANCE, tag).getOrThrow());
    }
}

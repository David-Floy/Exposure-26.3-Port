package io.github.mortuusars.exposure.client.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.renderer.PostChainConfig;
import net.minecraft.client.renderer.UniformValue;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShaderResourceTests {
    @Test
    void colorProcessorUniformBlockMatchesTheEngineStd140Writer() throws Exception {
        Map<String, Integer> shaderOffsets = std140Offsets(uniformBlockMembers(
                resourceText(COLOR_PROCESSOR_SHADER), "ColorConfig"));

        List<Path> postEffects;
        try (Stream<Path> files = Files.list(Path.of(ShaderResourceTests.class.getResource("/assets/exposure/post_effect").toURI()))) {
            postEffects = files.filter(file -> file.toString().endsWith(".json")).sorted().toList();
        }
        assertFalse(postEffects.isEmpty(), "No Exposure post effects found");

        int checkedPasses = 0;
        for (Path file : postEffects) {
            JsonObject root;
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                root = JsonParser.parseReader(reader).getAsJsonObject();
            }
            PostChainConfig config = PostChainConfig.CODEC.parse(JsonOps.INSTANCE, root).getOrThrow();
            JsonArray rawPasses = root.getAsJsonArray("passes");
            for (int passIndex = 0; passIndex < config.passes().size(); passIndex++) {
                PostChainConfig.Pass pass = config.passes().get(passIndex);
                if (!pass.fragmentShaderId().toString().equals("minecraft:program/exposure_color_processor")) continue;

                List<UniformValue> values = pass.uniforms().get("ColorConfig");
                assertNotNull(values, file.getFileName() + " has no ColorConfig block");
                JsonArray rawValues = rawPasses.get(passIndex).getAsJsonObject()
                        .getAsJsonObject("uniforms").getAsJsonArray("ColorConfig");

                // Write exactly like PostPass does, then read every value back at the shader's std140 offset.
                ByteBuffer buffer = ByteBuffer.allocateDirect(256).order(ByteOrder.nativeOrder());
                Std140Builder builder = Std140Builder.intoBuffer(buffer);
                values.forEach(value -> value.writeTo(builder));

                for (int i = 0; i < rawValues.size(); i++) {
                    JsonObject raw = rawValues.get(i).getAsJsonObject();
                    String name = raw.get("name").getAsString();
                    Integer offset = shaderOffsets.get(name);
                    assertNotNull(offset, file.getFileName() + ": shader block has no member " + name);
                    JsonElement expected = raw.get("value");
                    float[] expectedValues = expected.isJsonArray()
                            ? toFloats(expected.getAsJsonArray()) : new float[]{expected.getAsFloat()};
                    for (int component = 0; component < expectedValues.length; component++) {
                        assertEquals(expectedValues[component], buffer.getFloat(offset + component * 4), 0.00001f,
                                file.getFileName() + ": " + name + "[" + component + "] is not where the shader reads it");
                    }
                }
                checkedPasses++;
            }
        }
        // Every Exposure post effect except invert (vanilla post/invert) is a colour processor filter.
        assertEquals(postEffects.size() - 1, checkedPasses, "Unexpected number of colour processor passes");
    }

    @Test
    void colorProcessorDeclaresVaryingLocationsRequiredBySpirvCompilation() {
        String shader = resourceText(COLOR_PROCESSOR_SHADER).replaceAll("\\s+", " ");

        assertTrue(shader.contains("#extension GL_ARB_separate_shader_objects : require"),
                "26.3 compiles post shaders to SPIR-V and requires separate shader objects");
        assertTrue(shader.contains("layout(location = 0) in vec2 texCoord;"),
                "The fragment input must match the location of core/screenquad's texCoord output");
        assertTrue(shader.contains("layout(location = 0) out vec4 fragColor;"),
                "Fragment outputs need an explicit location");
    }

    @Test
    void redFilterRetainsUpstreamArtValuesAndProductionUniformOrder() {
        JsonObject root = resourceJson("/assets/exposure/post_effect/red_filter.json");
        JsonObject pass = root.getAsJsonArray("passes").get(0).getAsJsonObject();
        JsonArray uniforms = pass.getAsJsonObject("uniforms").getAsJsonArray("ColorConfig");

        assertEquals(List.of("Mul", "Add", "Saturation", "Contrast"),
                uniforms.asList().stream()
                        .map(element -> element.getAsJsonObject().get("name").getAsString())
                        .toList());
        assertFloatArray(uniforms.get(0).getAsJsonObject().getAsJsonArray("value"), 1.0f, 0.2f, 0.25f);
        assertFloatArray(uniforms.get(1).getAsJsonObject().getAsJsonArray("value"), 0.02f, 0.01f, 0.01f);
        assertEquals(1.1f, uniforms.get(2).getAsJsonObject().get("value").getAsFloat(), 0.00001f);
        assertEquals(0.98f, uniforms.get(3).getAsJsonObject().get("value").getAsFloat(), 0.00001f);
    }

    private static final String COLOR_PROCESSOR_SHADER = "/assets/minecraft/shaders/program/exposure_color_processor.fsh";

    /** Returns {@code type name} pairs of a std140 uniform block in declaration order. */
    private static List<String[]> uniformBlockMembers(String shader, String blockName) {
        String code = shader.replaceAll("//[^\\r\\n]*", "");
        Matcher block = Pattern.compile("layout\\(std140\\)\\s*uniform\\s+" + blockName + "\\s*\\{([^}]*)}").matcher(code);
        assertTrue(block.find(), "Missing uniform block " + blockName);
        List<String[]> members = new ArrayList<>();
        Matcher member = Pattern.compile("(\\w+)\\s+(\\w+)\\s*;").matcher(block.group(1));
        while (member.find()) {
            members.add(new String[]{member.group(1), member.group(2)});
        }
        return members;
    }

    /** Byte offsets per the GLSL std140 rules for the scalar and vector types used by Exposure shaders. */
    private static Map<String, Integer> std140Offsets(List<String[]> members) {
        Map<String, Integer> offsets = new LinkedHashMap<>();
        int offset = 0;
        for (String[] member : members) {
            int alignment;
            int size;
            switch (member[0]) {
                case "float", "int" -> { alignment = 4; size = 4; }
                case "vec2" -> { alignment = 8; size = 8; }
                case "vec3" -> { alignment = 16; size = 12; }
                case "vec4" -> { alignment = 16; size = 16; }
                default -> throw new AssertionError("Unsupported std140 member type " + member[0]);
            }
            offset = (offset + alignment - 1) / alignment * alignment;
            offsets.put(member[1], offset);
            offset += size;
        }
        return offsets;
    }

    private static float[] toFloats(JsonArray array) {
        float[] values = new float[array.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = array.get(i).getAsFloat();
        }
        return values;
    }

    private static String resourceText(String path) {
        InputStream stream = ShaderResourceTests.class.getResourceAsStream(path);
        assertNotNull(stream, "Missing resource: " + path);
        try (stream) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (java.io.IOException exception) {
            throw new AssertionError("Cannot read resource: " + path, exception);
        }
    }

    private static JsonObject resourceJson(String path) {
        InputStream stream = ShaderResourceTests.class.getResourceAsStream(path);
        assertNotNull(stream, "Missing resource: " + path);
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (java.io.IOException exception) {
            throw new AssertionError("Cannot read resource: " + path, exception);
        }
    }

    private static void assertFloatArray(JsonArray actual, float... expected) {
        assertEquals(expected.length, actual.size());
        for (int index = 0; index < expected.length; index++) {
            assertEquals(expected[index], actual.get(index).getAsFloat(), 0.00001f);
        }
    }
}

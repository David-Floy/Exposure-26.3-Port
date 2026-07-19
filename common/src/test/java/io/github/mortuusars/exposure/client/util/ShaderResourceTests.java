package io.github.mortuusars.exposure.client.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShaderResourceTests {
    @Test
    void colorProcessorKeepsStd140Vec3PaddingBeforeFollowingUniforms() {
        String shader = resourceText("/assets/minecraft/shaders/program/exposure_color_processor.fsh")
                .replaceAll("//[^\\r\\n]*", "")
                .replaceAll("\\s+", " ");

        assertTrue(shader.contains("layout(std140) uniform ColorConfig { "
                        + "vec3 Mul; float _PaddingAfterMul; "
                        + "vec3 Add; float _PaddingAfterAdd; "
                        + "float Saturation; float Contrast; }"),
                "The 26.1.2 Std140Builder allocates a full 16-byte slot for every vec3; "
                        + "explicit padding must keep scalar offsets at 32 and 36 bytes.");
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

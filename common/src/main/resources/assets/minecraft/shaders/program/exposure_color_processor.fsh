#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D DiffuseSampler;

layout(location = 0) in vec2 texCoord;

// 26.3 writes a vec3 uniform as 12 bytes (standard std140), so the scalars pack directly after Add.
layout(std140) uniform ColorConfig {
    vec3 Mul;
    vec3 Add;
    float Saturation;
    float Contrast;
};

layout(location = 0) out vec4 fragColor;

// Duplicate of the legacy color-convolve pass with Exposure's contrast control.
void main() {
    vec4 inTexel = texture(DiffuseSampler, texCoord);

    vec3 rgb = inTexel.rgb * Mul + Add;

    vec3 gray = vec3(0.3, 0.59, 0.11);
    float luma = dot(rgb, gray);
    vec3 chroma = rgb - luma;
    rgb = (chroma * Saturation) + luma;

    rgb = (rgb - 0.5) * Contrast + 0.5;

    fragColor = vec4(rgb, 1.0);
}

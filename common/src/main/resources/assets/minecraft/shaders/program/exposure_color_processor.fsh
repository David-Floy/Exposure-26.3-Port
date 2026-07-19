#version 330

uniform sampler2D DiffuseSampler;

in vec2 texCoord;

layout(std140) uniform ColorConfig {
    vec3 Mul;
    float _PaddingAfterMul;
    vec3 Add;
    float _PaddingAfterAdd;
    float Saturation;
    float Contrast;
};

out vec4 fragColor;

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

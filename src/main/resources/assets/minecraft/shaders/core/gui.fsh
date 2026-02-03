#version 330

// Can't moj_import in things used during startup, when resource packs don't exist.
// This is a copy of dynamicimports.glsl
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec4 color = vertexColor;
    // color.a == 0.4 Top part of sidebar
    // color.a == 0.3 Rest of sidebar
    // ??????????? -S
    if ((color.a > 0.29 && color.a < 0.3) || color.a == 0.4) {
        discard;
    }
    if (color.a == 0.0) {
        discard;
    }
    fragColor = color * ColorModulator;
}
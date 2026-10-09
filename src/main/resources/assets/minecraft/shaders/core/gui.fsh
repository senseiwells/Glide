#version 330
#extension GL_ARB_separate_shader_objects : require

// Can't moj_import in things used during startup, when resource packs don't exist.
// This is a copy of dynamicimports.glsl
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    mat4 TextureMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
};

layout(location = 0) in vec4 vertexColor;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 color = vertexColor;
    // == Glide Start ==
    // color.a == 0.4 Top part of sidebar
    // color.a == 0.3 Rest of sidebar
    // ??????????? -S
    if ((color.a > 0.29 && color.a < 0.3) || color.a == 0.4) {
        discard;
    }
    // == Glide End ==
    if (color.a == 0.0) {
        discard;
    }
    fragColor = color * ColorModulator;
}

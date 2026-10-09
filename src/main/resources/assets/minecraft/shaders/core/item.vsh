#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:light.glsl>
#include <minecraft:fog.glsl>
#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>
#include <minecraft:sample_lightmap.glsl>
// == Glide Start ==
#include <glide:glide.glsl>
// == Glide End ==

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;
layout(location = 3) in ivec2 UV1;
layout(location = 4) in ivec2 UV2;
#ifdef GLINT_SPECIAL
layout(location = 5) in vec2 UV3;
#endif
layout(location = 6) in vec3 Normal;

// == Glide Start ==
uniform sampler2D Sampler0;
// == Glide End ==
#ifndef OIT_ALPHA_ONLY
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
#endif

// == Glide Start ==
layout(location = 0) out float sphericalVertexDistance;
layout(location = 1) out float cylindricalVertexDistance;
// == Glide End ==
layout(location = 2) out vec4 vertexColor;
#ifndef OIT_ALPHA_ONLY
layout(location = 3) out vec4 lightMapColor;
layout(location = 4) out vec4 overlayColor;
#endif

layout(location = 5) out vec2 texCoord0;
#ifdef GLINT
layout(location = 6) out vec2 texCoordGlint;
#endif

// == Glide Start ==
layout(location = 7) flat out int marker;
layout(location = 8) out float height;
layout(location = 9) out vec2 quadCoord;
layout(location = 10) flat out float anchorV;
// == Glide End ==

void main() {
    // == Glide Start ==
    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);
    // == Glide End ==

    vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, Color);
    #ifndef OIT_ALPHA_ONLY
    lightMapColor = sample_lightmap(Sampler2, UV2);
    overlayColor = texelFetch(Sampler1, UV1, 0);
    #endif

    texCoord0 = UV0;

    // == Glide Start ==
    vec3 pos = Position;
    int corner;
    marker = findMarker(Sampler0, texCoord0, corner);
    quadCoord = QUAD_CORNERS[corner < 0 ? gl_VertexIndex % 4 : corner];
    height = 0.0;

    if (marker == MARKER_THERMAL) {
        vec2 texel = 1.0 / vec2(textureSize(Sampler0, 0));
        texCoord0 += (1.0 - 2.0 * quadCoord) * texel;

        height = float(UV2.x) / 16.0 + float(UV2.y);
        if (quadCoord.y == 0.0) {
            pos.y += height;
        }
    } else if (marker == MARKER_BEACON_BEAM || marker == MARKER_BEACON_SHELL) {
        height = float(UV2.x) / 16.0 + float(UV2.y);
    }

    anchorV = texCoord0.y;
    // == Glide End ==

    #ifdef GLINT
    #ifdef GLINT_SPECIAL
    texCoordGlint = (TextureMat * vec4(UV3, 0.0, 1.0)).xy;
    #else
    texCoordGlint = (TextureMat * vec4(UV0, 0.0, 1.0)).xy;
    #endif
    #endif

    // == Glide Start ==
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
    // == Glide End ==
}

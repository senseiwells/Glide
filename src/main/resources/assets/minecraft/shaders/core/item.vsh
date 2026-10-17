#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:light.glsl>
#include <minecraft:fog.glsl>
#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>
#include <minecraft:sample_lightmap.glsl>
#include <glide:glide.glsl>

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;
layout(location = 3) in ivec2 UV1;
layout(location = 4) in ivec2 UV2;
#ifdef GLINT_SPECIAL
layout(location = 5) in vec2 UV3;
#endif
layout(location = 6) in vec3 Normal;

uniform sampler2D Sampler0;

#ifndef OIT_ALPHA_ONLY
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
#endif

layout(location = 0) out float sphericalVertexDistance;
layout(location = 1) out float cylindricalVertexDistance;

layout(location = 2) out vec4 vertexColor;
#ifndef OIT_ALPHA_ONLY
layout(location = 3) out vec4 lightMapColor;
layout(location = 4) out vec4 overlayColor;
#endif

layout(location = 5) out vec2 texCoord0;
#ifdef GLINT
layout(location = 6) out vec2 texCoordGlint;
#endif

layout(location = 7) flat out int objectType; //See glide.glsl
layout(location = 8) out float height;
layout(location = 9) out vec2 quadCoord;
layout(location = 10) flat out float anchorV;

void main() {
    vec3 pos = Position;
    
    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);
    
    vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, Color);
    #ifndef OIT_ALPHA_ONLY
    lightMapColor = sample_lightmap(Sampler2, UV2);
    overlayColor = texelFetch(Sampler1, UV1, 0);
    #endif
    
    texCoord0 = UV0;
    int corner;
    
    objectType = get_object_from_texture(Sampler0, texCoord0, corner);
    quadCoord = QUAD_CORNERS[corner < 0 ? gl_VertexIndex % 4 : corner];
    
    height = get_heights(objectType, UV2);
    
    if (objectType == THERMAL) {
        vec2 texel = 1.0 / vec2(textureSize(Sampler0, 0));
        texCoord0 += (1.0 - 2.0 * quadCoord) * texel;
        
        if (quadCoord.y == 0.0) {
            pos.y += height;
        }
    }
    
    anchorV = texCoord0.y;
    
    #ifdef GLINT
    #ifdef GLINT_SPECIAL
    texCoordGlint = (TextureMat * vec4(UV3, 0.0, 1.0)).xy;
    #else
    texCoordGlint = (TextureMat * vec4(UV0, 0.0, 1.0)).xy;
    #endif
    #endif
    
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
}
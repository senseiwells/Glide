#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:globals.glsl>

#include <minecraft:fog.glsl>
#include <minecraft:dynamictransforms.glsl>
#include <minecraft:oit.glsl>
#include <glide:glide.glsl>

uniform sampler2D Sampler0;

#ifdef GLINT
uniform sampler2D GlintSampler;
#endif

layout(location = 0) in float sphericalVertexDistance;
layout(location = 1) in float cylindricalVertexDistance;

layout(location = 2) in vec4 vertexColor;
#ifndef OIT_ALPHA_ONLY
layout(location = 3) in vec4 lightMapColor;
layout(location = 4) in vec4 overlayColor;
#endif
layout(location = 5) in vec2 texCoord0;
#ifdef GLINT
layout(location = 6) in vec2 texCoordGlint;
#endif

layout(location = 7) flat in int objectType; //See glide.glsl
layout(location = 8) in float height;
layout(location = 9) in vec2 quadCoord;
layout(location = 10) flat in float anchorV;

#ifndef OIT_ALPHA_ONLY
layout(location = 0) out vec4 fragColor;
#endif

#ifndef OIT_ALPHA_ONLY
vec4 calculateFinalColor(vec4 color) {
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);

    if (objectType == 0) { //Based on decomp the lightmap has *some* effect on boosts, but I can't be bothered to figure it out, assume it doesn't. -S
        color *= lightMapColor; 
    }

    #ifdef GLINT
    vec4 glintColor = GlintAlpha * texture(GlintSampler, texCoordGlint);// Glint color modulator?
    // Matches BlendFuntion.GLINT
    color.rgb += glintColor.rgb * glintColor.rgb;
    #endif

    #ifdef OIT_ACCUMULATE
    color = sampleColorForAccumulation(color);
    vec4 fogColor = vec4(FogColor.rgb * color.a, FogColor.a);
    #else
    vec4 fogColor = FogColor;
    #endif

    return apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, fogColor);
}
#endif

float spriteStart(float spriteHeight) {
    return texCoord0.y < anchorV ? anchorV - spriteHeight : anchorV;
}

float measuredHeight(mat2 dTexCoord, mat2 dQuadCoord) {
    float det = determinant(dQuadCoord);
    det = abs(det) < 1e-14 ? (det < 0.0 ? -1e-14 : 1e-14) : det;
    mat2 adjugate = mat2(dQuadCoord[1][1], -dQuadCoord[0][1], -dQuadCoord[1][0], dQuadCoord[0][0]);
    mat2 quadToTex = dTexCoord * adjugate / det;
    return abs(quadToTex[0].y) + abs(quadToTex[1].y);
}

vec4 scrolled(float start, float repeat, float scale, float offset) {
    float local = texCoord0.y - start;
    return textureLod(Sampler0, vec2(texCoord0.x, start + mod(local * scale + offset, repeat)), 0.0);
}

void main() {
    vec4 color = texture(Sampler0, texCoord0);
    #ifdef ALPHA_CUTOUT
    if (color.a < ALPHA_CUTOUT && objectType == 0) {
        discard;
    }
    #endif

    color *= vertexColor * ColorModulator;

    mat2 dTexCoord = mat2(dFdx(texCoord0), dFdy(texCoord0));
    mat2 dQuadCoord = mat2(dFdx(quadCoord), dFdy(quadCoord));
    float texel = 1.0 / float(textureSize(Sampler0, 0).y);
    
    if (objectType == THERMAL) {
        float repeat = 128.0 * texel;
        float start = spriteStart(repeat);
        color = scrolled(start, repeat, height / 8.0, GameTime * 64.0) * vec4(vec3(1.0), 2.0) * ColorModulator;
        color.a *= min((texCoord0.y - start) * height, 1.0);
        color.a /= 2.1;
        if (color.a < 0.1) {
            discard;
        }
    }
    if (objectType == BOOST || objectType == OMNIBOOST) {
        float repeat = 16.0 * texel;
        float repeats = measuredHeight(dTexCoord, dQuadCoord) / repeat > 3.0 ? 5.0 : 1.0;
        vec4 sampled = scrolled(spriteStart(repeat * repeats), repeat, 0.55, GameTime * 32.0);
        
        if (ivec4(round(sampled * 255.0)) == BOOSTCOLOR || ivec4(round(sampled * 255.0)) == OMNIBOOSTCOLOR) {
            discard;
        }

        color = sampled * vec4(vec3(1.0), 2.0) * ColorModulator;
        if (objectType == OMNIBOOST) {
            color.rgb *= vec3(1, (133.0 / 255.0), 0);
        } else {
            color.rgb *= vec3(1, (210.0 / 255.0), 0);
        }
        
        color.a /= max(sphericalVertexDistance / 20.0, 1.7);
        color.a /= 2.1;
        if (color.a < 0.1) {
            discard;
        }
    }
    if (objectType == BEACONBEAM || objectType == BEACONSHELL) {
        float repeat = 16.0 * texel;
        vec4 sampled = scrolled(spriteStart(repeat), repeat, height / 8.0, -GameTime * 128.0);
        if (ivec4(round(sampled * 255.0)) == BEACONBEAMCOLOR || ivec4(round(sampled * 255.0)) == BEACONSHELLCOLOR) {
            discard;
        }

        color = sampled * vec4(vec3(1.0), 2.0) * ColorModulator;
        if (objectType == BEACONSHELL) {
            color.a /= 5.0;
        }
    }
    
    #ifdef GLINT
    color.a = max(color.a, GlintAlpha);
    #endif

    #ifdef OIT_ALPHA_ONLY
    executeAlphaOnlyPhase(gl_FragCoord.z, color.a);
    #else
    fragColor = calculateFinalColor(color);
    #endif
}
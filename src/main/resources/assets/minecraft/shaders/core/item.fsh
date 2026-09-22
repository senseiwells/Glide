#version 330
#extension GL_ARB_separate_shader_objects : require

// == Glide Start ==
#include <minecraft:globals.glsl>
// == Glide End ==
#include <minecraft:fog.glsl>
#include <minecraft:dynamictransforms.glsl>
#include <minecraft:oit.glsl>
// == Glide Start ==
#include <glide:glide.glsl>
// == Glide End ==

uniform sampler2D Sampler0;

#ifdef GLINT
uniform sampler2D GlintSampler;
#endif

// == Glide Start ==
// Vanilla guards these behind OIT_ALPHA_ONLY. The marker fades below derive
// alpha from the distance, so it has to survive every OIT pass or the
// alpha-only passes would disagree with the accumulate pass.
layout(location = 0) in float sphericalVertexDistance;
layout(location = 1) in float cylindricalVertexDistance;
// == Glide End ==
layout(location = 2) in vec4 vertexColor;
#ifndef OIT_ALPHA_ONLY
layout(location = 3) in vec4 lightMapColor;
layout(location = 4) in vec4 overlayColor;
#endif
layout(location = 5) in vec2 texCoord0;
#ifdef GLINT
layout(location = 6) in vec2 texCoordGlint;
#endif

// == Glide Start ==
layout(location = 7) flat in int marker;
layout(location = 8) in float height;
layout(location = 9) in vec2 quadCoord;
layout(location = 10) flat in float anchorV;
// == Glide End ==

#ifndef OIT_ALPHA_ONLY
layout(location = 0) out vec4 fragColor;
#endif

// == Glide Start ==
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
// == Glide End ==

#ifndef OIT_ALPHA_ONLY
vec4 calculateFinalColor(vec4 color) {
    // == Glide Start ==
    // Marker geometry is emissive, it takes neither the overlay nor the lightmap.
    if (marker == MARKER_NONE) {
    // == Glide End ==
        color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
        color *= lightMapColor;
    // == Glide Start ==
    }
    // == Glide End ==

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

void main() {
    // == Glide Start ==
    vec4 color;

    if (marker != MARKER_NONE) {
        mat2 dTexCoord = mat2(dFdx(texCoord0), dFdy(texCoord0));
        mat2 dQuadCoord = mat2(dFdx(quadCoord), dFdy(quadCoord));
        float texel = 1.0 / float(textureSize(Sampler0, 0).y);

        if (marker == MARKER_THERMAL) {
            float repeat = THERMAL_REPEAT * texel;
            float start = spriteStart(repeat);
            color = scrolled(start, repeat, height / 8.0, GameTime * 64.0) * vec4(vec3(1.0), 2.0) * ColorModulator;
            color.a *= min((texCoord0.y - start) * height / (sphericalVertexDistance / 20.0), 1.0);
            color.a /= 2.1;
            if (color.a < 0.1) {
                discard;
            }
        } else if (marker == MARKER_BOOST || marker == MARKER_OMNI_BOOST) {
            float repeat = BOOST_REPEAT * texel;
            float repeats = measuredHeight(dTexCoord, dQuadCoord) / repeat > 3.0 ? 5.0 : 1.0;
            vec4 sampled = scrolled(spriteStart(repeat * repeats), repeat, 0.55, GameTime * 32.0);
            // The frame is part of the region we scroll, so drop it.
            if (isKey(sampled, BOOST_KEY) || isKey(sampled, OMNI_BOOST_KEY)) {
                discard;
            }

            color = sampled * vec4(vec3(1.0), 2.0) * ColorModulator;
            color.rgb *= marker == MARKER_OMNI_BOOST ? vec3(1.0, 133.0 / 255.0, 0.0) : vec3(1.0, 210.0 / 255.0, 0.0);
            color.a /= max(sphericalVertexDistance / 20.0, 1.7);
            color.a /= 2.1;
            if (color.a < 0.1) {
                discard;
            }
        } else {
            float repeat = BEACON_REPEAT * texel;
            vec4 sampled = scrolled(spriteStart(repeat), repeat, height / 8.0, -GameTime * 128.0);
            if (isKey(sampled, marker == MARKER_BEACON_BEAM ? BEACON_BEAM_KEY : BEACON_SHELL_KEY)) {
                discard;
            }

            color = sampled * vec4(vec3(1.0), 2.0) * ColorModulator;
            if (marker == MARKER_BEACON_SHELL) {
                color.a /= 5.0;
            }
        }
    } else {
    // == Glide End ==
        color = texture(Sampler0, texCoord0);
        #ifdef ALPHA_CUTOUT
        if (color.a < ALPHA_CUTOUT) {
            discard;
        }
        #endif

        color *= vertexColor * ColorModulator;
    // == Glide Start ==
    }
    // == Glide End ==

    #ifdef GLINT
    color.a = max(color.a, GlintAlpha);
    #endif

    #ifdef OIT_ALPHA_ONLY
    executeAlphaOnlyPhase(gl_FragCoord.z, color.a);
    #else
    fragColor = calculateFinalColor(color);
    #endif
}

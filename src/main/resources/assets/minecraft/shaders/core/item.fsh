#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
// == Glide Start ==
#moj_import <minecraft:globals.glsl>
// == Glide End ==

uniform sampler2D Sampler0;

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec4 vertexColor;
in vec4 lightMapColor;
in vec4 overlayColor;
in vec2 texCoord0;

// == Glide Start ==
const int MARKER_NONE = 0;
const int MARKER_THERMAL = 1;
const int MARKER_BOOST = 2;
const int MARKER_OMNI_BOOST = 3;
const int MARKER_BEACON_BEAM = 4;
const int MARKER_BEACON_SHELL = 5;

flat in int marker;
in float height;
in vec3 cornerTex1;
in vec3 cornerTex2;
in vec3 cornerTex3;
// == Glide End ==

out vec4 fragColor;

// == Glide Start ==
vec4 spriteBounds() {
    vec2 corner1 = cornerTex1.xy / cornerTex1.z;
    vec2 corner2 = cornerTex2.xy / cornerTex2.z;
    vec2 corner3 = cornerTex3.xy / cornerTex3.z;
    vec2 minUV = min(corner1, min(corner2, corner3));
    vec2 maxUV = max(corner1, max(corner2, corner3));
    return vec4(minUV, maxUV - minUV);
}

vec4 scrolled(vec4 bounds, float scale, float offset) {
    vec2 uv = texCoord0 - bounds.xy;
    uv.y = uv.y * scale + offset;
    return texture(Sampler0, mod(uv, bounds.zw) + bounds.xy);
}

bool isKey(vec4 color, ivec3 key) {
    return ivec3(round(color.rgb * 255.0)) == key;
}
// == Glide End ==

void main() {
    // == Glide Start ==
    if (marker != MARKER_NONE) {
        vec4 bounds = spriteBounds();
        vec4 color;

        if (marker == MARKER_THERMAL) {
            color = scrolled(bounds, height / 8.0, GameTime * 64.0) * vec4(vec3(1.0), 2.0) * ColorModulator;
            color.a *= min((texCoord0.y - bounds.y) * height / (sphericalVertexDistance / 20.0), 1.0);
            color.a /= 2.1;
            if (color.a < 0.1) {
                discard;
            }
        } else if (marker == MARKER_BOOST || marker == MARKER_OMNI_BOOST) {
            vec4 sampled = scrolled(bounds, 0.55, GameTime * 32.0);
            if (isKey(sampled, ivec3(255, 185, 0)) || isKey(sampled, ivec3(230, 100, 47))) {
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
            vec4 sampled = scrolled(bounds, height / 8.0, -GameTime * 128.0);
            if (isKey(sampled, marker == MARKER_BEACON_BEAM ? ivec3(0, 255, 0) : ivec3(0, 83, 255))) {
                discard;
            }

            color = sampled * vec4(vec3(1.0), 2.0) * ColorModulator;
            if (marker == MARKER_BEACON_SHELL) {
                color.a /= 5.0;
            }
        }

        fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
        return;
    }
    // == Glide End ==

    vec4 color = texture(Sampler0, texCoord0);
#ifdef ALPHA_CUTOUT
    if (color.a < ALPHA_CUTOUT) {
        discard;
    }
#endif

    color *= vertexColor * ColorModulator;
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
    color *= lightMapColor;

    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}

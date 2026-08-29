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

const float THERMAL_REPEAT = 128.0;
const float BOOST_REPEAT = 16.0;
const float BEACON_REPEAT = 16.0;

flat in int marker;
in float height;
in vec2 quadCoord;
flat in float anchorV;
// == Glide End ==

out vec4 fragColor;

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

bool isKey(vec4 color, ivec3 key) {
    return ivec3(round(color.rgb * 255.0)) == key;
}
// == Glide End ==

void main() {
    // == Glide Start ==
    mat2 dTexCoord = mat2(dFdx(texCoord0), dFdy(texCoord0));
    mat2 dQuadCoord = mat2(dFdx(quadCoord), dFdy(quadCoord));

    if (marker != MARKER_NONE) {
        float texel = 1.0 / float(textureSize(Sampler0, 0).y);
        vec4 color;

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
            float repeat = BEACON_REPEAT * texel;
            vec4 sampled = scrolled(spriteStart(repeat), repeat, height / 8.0, -GameTime * 128.0);
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

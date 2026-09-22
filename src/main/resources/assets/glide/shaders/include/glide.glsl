#ifndef GLIDE_GLSL
#define GLIDE_GLSL

const int MARKER_NONE = 0;
const int MARKER_THERMAL = 1;
const int MARKER_BOOST = 2;
const int MARKER_OMNI_BOOST = 3;
const int MARKER_BEACON_BEAM = 4;
const int MARKER_BEACON_SHELL = 5;

const ivec3 BOOST_KEY = ivec3(255, 185, 0);
const ivec3 OMNI_BOOST_KEY = ivec3(230, 100, 47);
const ivec3 BEACON_BEAM_KEY = ivec3(0, 255, 0);
const ivec3 BEACON_SHELL_KEY = ivec3(0, 83, 255);

const ivec2 THERMAL_KEY_RG = ivec2(201, 217);
const int THERMAL_KEY_B = 228;
const int THERMAL_CORNER_B = 224;

const float THERMAL_REPEAT = 128.0;
const float BOOST_REPEAT = 16.0;
const float BEACON_REPEAT = 16.0;

const vec2 QUAD_CORNERS[4] = vec2[4](
    vec2(0.0, 0.0),
    vec2(0.0, 1.0),
    vec2(1.0, 1.0),
    vec2(1.0, 0.0)
);

bool isKey(vec4 color, ivec3 key) {
    return ivec3(round(color.rgb * 255.0)) == key;
}

int findMarker(sampler2D tex, vec2 coord, out int corner) {
    ivec2 size = textureSize(tex, 0);
    ivec2 texel = clamp(ivec2(floor(coord * vec2(size))), ivec2(0), size - 1);
    ivec4 key = ivec4(round(texelFetch(tex, texel, 0) * 255.0));
    corner = -1;
    if (key.a == 255) {
        if (key.rg == THERMAL_KEY_RG && key.b >= THERMAL_CORNER_B && key.b <= THERMAL_KEY_B) {
            if (key.b < THERMAL_KEY_B) {
                corner = key.b - THERMAL_CORNER_B;
            }
            return MARKER_THERMAL;
        }
        if (key.rgb == BOOST_KEY) return MARKER_BOOST;
        if (key.rgb == OMNI_BOOST_KEY) return MARKER_OMNI_BOOST;
        if (key.rgb == BEACON_BEAM_KEY) return MARKER_BEACON_BEAM;
        if (key.rgb == BEACON_SHELL_KEY) return MARKER_BEACON_SHELL;
    }
    return MARKER_NONE;
}

#endif
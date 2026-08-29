#version 330

#moj_import <minecraft:light.glsl>
#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:sample_lightmap.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

// == Glide Start ==
uniform sampler2D Sampler0;
// == Glide End ==
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;

out float sphericalVertexDistance;
out float cylindricalVertexDistance;
out vec4 vertexColor;
out vec4 lightMapColor;
out vec4 overlayColor;

out vec2 texCoord0;

// == Glide Start ==
const int MARKER_NONE = 0;
const int MARKER_THERMAL = 1;
const int MARKER_BOOST = 2;
const int MARKER_OMNI_BOOST = 3;
const int MARKER_BEACON_BEAM = 4;
const int MARKER_BEACON_SHELL = 5;

const ivec2 THERMAL_KEY_RG = ivec2(201, 217);
const int THERMAL_KEY_B = 228;
const int THERMAL_CORNER_B = 224;

flat out int marker;
out float height;
out vec2 quadCoord;
flat out float anchorV;

const vec2 QUAD_CORNERS[4] = vec2[4](
    vec2(0.0, 0.0),
    vec2(0.0, 1.0),
    vec2(1.0, 1.0),
    vec2(1.0, 0.0)
);

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
        if (key.rgb == ivec3(255, 185, 0)) return MARKER_BOOST;
        if (key.rgb == ivec3(230, 100, 47)) return MARKER_OMNI_BOOST;
        if (key.rgb == ivec3(0, 255, 0)) return MARKER_BEACON_BEAM;
        if (key.rgb == ivec3(0, 83, 255)) return MARKER_BEACON_SHELL;
    }
    return MARKER_NONE;
}
// == Glide End ==

void main() {
    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);

    vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, Color);
    lightMapColor = sample_lightmap(Sampler2, UV2);
    overlayColor = texelFetch(Sampler1, UV1, 0);

    texCoord0 = UV0;

    // == Glide Start ==
    vec3 pos = Position;
    int corner;
    marker = findMarker(Sampler0, texCoord0, corner);
    quadCoord = QUAD_CORNERS[corner < 0 ? gl_VertexID % 4 : corner];
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

    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
}

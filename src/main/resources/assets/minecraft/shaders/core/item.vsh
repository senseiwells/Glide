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

flat out int marker;
out float height;
out vec3 cornerTex1;
out vec3 cornerTex2;
out vec3 cornerTex3;

// Glide's models are drawn from sprites that are framed with a solid key
// colour, and every quad of such a model has its corner UVs sitting on that
// frame. Sampling the texel under a vertex therefore tells us which of the
// animated models (if any) the quad belongs to.
// Returns MARKER_NONE for any sprite that isn't one of ours.
int findMarker(sampler2D tex, vec2 coord) {
    ivec2 size = textureSize(tex, 0);
    ivec2 texel = clamp(ivec2(floor(coord * vec2(size))), ivec2(0), size - 1);
    ivec4 key = ivec4(round(texelFetch(tex, texel, 0) * 255.0));
    if (key.a == 255) {
        if (key.rgb == ivec3(201, 217, 228)) return MARKER_THERMAL;
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
    int corner = gl_VertexID % 4;

    marker = findMarker(Sampler0, texCoord0);
    height = 0.0;

    if (marker == MARKER_THERMAL) {
        // Pull the corners a texel inside the frame, so the region we scroll
        // is the sprite itself rather than the key colour around it.
        vec2 texel = 1.0 / vec2(textureSize(Sampler0, 0));
        switch (corner) {
            case 0:
                texCoord0 += vec2(1.0, 1.0) * texel;
                break;
            case 1:
                texCoord0 += vec2(1.0, -1.0) * texel;
                break;
            case 2:
                texCoord0 += vec2(-1.0, -1.0) * texel;
                break;
            case 3:
                texCoord0 += vec2(-1.0, 1.0) * texel;
                break;
        }
    }

    // Each of these is only written by one vertex of a triangle, so dividing
    // the interpolated xy by the interpolated z recovers that vertex's UV in
    // the fragment shader, which gives us the quad's region of the atlas.
    cornerTex1 = vec3(0.0);
    cornerTex2 = vec3(0.0);
    cornerTex3 = vec3(0.0);
    if (corner == 0) cornerTex1 = vec3(texCoord0, 1.0);
    if (corner == 2) cornerTex2 = vec3(texCoord0, 1.0);
    if (gl_VertexID % 2 == 1) cornerTex3 = vec3(texCoord0, 1.0);

    // The models are a single block tall; the entity packs the height it
    // should be stretched to into its brightness override.
    if (marker == MARKER_THERMAL) {
        height = float(UV2.x) / 16.0 + float(UV2.y);
        if (corner == 0 || corner == 3) {
            pos.y += height;
        }
    } else if (marker == MARKER_BEACON_BEAM || marker == MARKER_BEACON_SHELL) {
        height = float(UV2.x) / 16.0 + float(UV2.y);
    }
    // == Glide End ==

    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
}

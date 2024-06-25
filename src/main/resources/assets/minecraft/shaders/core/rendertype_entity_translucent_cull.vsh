#version 150

#moj_import <light.glsl>
#moj_import <fog.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in vec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler0;
uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;

uniform vec3 Light0_Direction;
uniform vec3 Light1_Direction;

out float vertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;
out vec2 texCoord1;
out vec2 texCoord2;
out vec4 normal;
out float isThermal;
out float isBoost;
out float isBeaconShell;
out float isBeaconBeam;
out float height;
out vec3 cornerTex1;
out vec3 cornerTex2;
out vec3 cornerTex3;


void main() {
    vec3 pos = Position;

    vertexDistance = fog_distance(pos, FogShape);
    vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, Color) * texelFetch(Sampler2, UV2 / 16, 0);
    texCoord0 = UV0;
    texCoord1 = UV1;
    texCoord2 = UV2;
    normal = ProjMat * ModelViewMat * vec4(Normal, 0.0);

    isThermal = 0.0;
    if (ivec4(texture(Sampler0, texCoord0) * 255.0 + 0.5) == ivec4(201, 217, 228, 255)) {
        isThermal = 1.0;
        vec2 texel = 1.0 / textureSize(Sampler0, 0);
        switch (gl_VertexID % 4) {
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
    isBoost = 0.0;
    if (ivec4(texture(Sampler0, texCoord0) * 255.0 + 0.5) == ivec4(230, 100, 47, 255)) {
        isBoost = 1.0;
        vec2 texel = 1.0 / textureSize(Sampler0, 0);
    }
    isBeaconBeam = 0.0;
    if (ivec4(texture(Sampler0, texCoord0) * 255.0 + 0.5) == ivec4(0, 255, 0, 255)) {
        isBeaconBeam = 1.0;
        vec2 texel = 1.0 / textureSize(Sampler0, 0);
    }
    isBeaconShell = 0.0;
    if (ivec4(texture(Sampler0, texCoord0) * 255.0 + 0.5) == ivec4(0, 83, 255, 255)) {
        isBeaconShell = 1.0;
        vec2 texel = 1.0 / textureSize(Sampler0, 0);
    }


    cornerTex1 = vec3(0.0);
    cornerTex2 = vec3(0.0);
    cornerTex3 = vec3(0.0);
    if (gl_VertexID % 4 == 0) cornerTex1 = vec3(texCoord0, 1.0);
    if (gl_VertexID % 4 == 2) cornerTex2 = vec3(texCoord0, 1.0);
    if (gl_VertexID % 2 == 1) cornerTex3 = vec3(texCoord0, 1.0);

    if (isThermal > 0.5) {
        height = UV2.x / 16.0 + UV2.y;
        if (gl_VertexID % 4 == 0 || gl_VertexID % 4 == 3) {
            pos.y += height;
        }
    }
    if (isBoost > 0.5) {
        height = UV2.x / 16.0 + UV2.y;
    }
    if (isBeaconBeam > 0.5) {
        height = UV2.x / 16.0 + UV2.y;
    }
    if (isBeaconShell > 0.5) {
        height = UV2.x / 16.0 + UV2.y;
    }

    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
}

#version 150

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>

uniform sampler2D Sampler0;

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec4 vertexColor;
in vec4 spectatorVertexColor;
in vec2 texCoord0;
in vec2 texCoord1;
in vec4 normal;
in float isThermal;
in float isBoost;
in float isOmniBoost;
in float isBeaconShell;
in float isBeaconBeam;
in float height;
in vec3 cornerTex1;
in vec3 cornerTex2;
in vec3 cornerTex3;

out vec4 fragColor;

void main() {
    vec2 cornerUV1 = cornerTex1.xy / cornerTex1.z;
    vec2 cornerUV2 = cornerTex2.xy / cornerTex2.z;
    vec2 cornerUV3 = cornerTex3.xy / cornerTex3.z;
    vec2 minUV = min(cornerUV1, min(cornerUV2, cornerUV3));
    vec2 maxUV = max(cornerUV1, max(cornerUV2, cornerUV3));

    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;

    if (isThermal > 0.5) {
        maxUV -= minUV;
        vec2 uv = texCoord0 - minUV;
        uv.y *= height / 8.0;
        uv.y += GameTime * 64.0;
        color = texture(Sampler0, mod(uv, maxUV) + minUV) * vec4(vec3(1.0), 2.0) * ColorModulator;
        color.a *= min((texCoord0.y - minUV.y) * height / (sphericalVertexDistance / 20.0), 1.0);
        color.a /= 2.1;
        if (color.a < 0.1) {
            discard;
        }
    } else if (isBoost > 0.5 || isOmniBoost > 0.5) {
        maxUV -= minUV;
        vec2 uv = texCoord0 - minUV;
        uv.y *= 0.55;
        uv.y += GameTime * 32.0;

        color = texture(Sampler0, mod(uv, maxUV) + minUV) * vec4(vec3(1.0), 2.0) * ColorModulator;


        if (abs(normal.y) > 0.1) {
            color.a /= max(sphericalVertexDistance / 20.0, 1.7);
        } else {
            color.a /= max(sphericalVertexDistance / 20.0, 1.7);
        }


        if (color.r*255 == 230 && color.g*255 == 100 && color.b*255 == 47) {
            discard;
        }
        if (color.r*255 == 255 && color.g*255 == 185 && color.b*255 == 0) {
            discard;
        }

        if (isOmniBoost > 0.5) {
            color.rgb *= vec3(1, (133.0 / 255.0), 0);
        } else {
            color.rgb *= vec3(1, (210.0 / 255.0), 0);
        }
        color.a /= 2.1;
        if (color.a < 0.1) {
            discard;
        }
    } else if (isBeaconBeam > 0.5) {
        maxUV -= minUV;
        vec2 uv = texCoord0 - minUV;
        uv.y *= height / 8;
        uv.y -= GameTime * 128.0;

        color = texture(Sampler0, mod(uv, maxUV) + minUV) * vec4(vec3(1.0), 2.0) * ColorModulator;

        if (color.r*255 == 0 && color.g*255 == 255 && color.b*255 == 0) {
            discard;
        }
    } else if (isBeaconShell > 0.5) {
        maxUV -= minUV;
        vec2 uv = texCoord0 - minUV;
        uv.y *= height / 8;
        uv.y -= GameTime * 128.0;

        color = texture(Sampler0, mod(uv, maxUV) + minUV) * vec4(vec3(1.0), 2.0) * ColorModulator;

        color.a = color.a / 5;
        if (color.r*255 == 0 && color.g*255 == 83 && color.b*255 == 255) {
            discard;
        }
    } else {
        color = texture(Sampler0, texCoord0) * spectatorVertexColor * ColorModulator;
        if (sphericalVertexDistance < 16.0) { // Start of fadeout / Not a UI element. TODO: Add additional check for if it is a ring
            if (color.a < 0.1) {discard;} // Most players have a transparent second layer.
            color.a = (sphericalVertexDistance / 16.0); // Change divisor to match start of fade out.
        }
    }
    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
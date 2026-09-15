#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

#moj_import <minecraft:globals.glsl>
#moj_import <glide:glide.glsl>

uniform sampler2D Sampler0;

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;

flat in int objectType;
in vec3 cornerTex1;
in vec3 cornerTex2;
in vec3 cornerTex3;
in vec4 normal;
in float height;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0);
#ifdef ALPHA_CUTOUT
    if (color.a < ALPHA_CUTOUT && objectType == 0) {
        discard;
    }
#endif
    
    color *= vertexColor * ColorModulator;
    
     vec2 cornerUV1 = cornerTex1.xy / cornerTex1.z;
    vec2 cornerUV2 = cornerTex2.xy / cornerTex2.z;
    vec2 cornerUV3 = cornerTex3.xy / cornerTex3.z;
    vec2 minUV = min(cornerUV1, min(cornerUV2, cornerUV3));
    vec2 maxUV = max(cornerUV1, max(cornerUV2, cornerUV3));
    
    if (objectType == THERMAL) {
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
    }
    if (objectType == BOOST || objectType == OMNIBOOST) {
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


        if (ivec4(color * 255.0 + 0.5).rgb == OMNIBOOSTCOLOR.rgb || ivec4(color * 255.0 + 0.5).rgb == BOOSTCOLOR.rgb) {
            discard;
        }

        if (objectType == OMNIBOOST) {
            color.rgb *= vec3(1, (133.0 / 255.0), 0);
        } else {
            color.rgb *= vec3(1, (210.0 / 255.0), 0);
        }
        color.a /= 2.1;
        if (color.a < 0.1) {
            discard;
        }
    }
    if (objectType == BEACONBEAM || objectType == BEACONSHELL) {
        maxUV -= minUV;
        vec2 uv = texCoord0 - minUV;
        uv.y *= height / 8;
        uv.y -= GameTime * 128.0;

        color = texture(Sampler0, mod(uv, maxUV) + minUV) * vec4(vec3(1.0), 2.0) * ColorModulator;
            
        if (objectType == BEACONSHELL) {
            color.a = color.a / 5;
        }
        if (ivec4(color * 255.0 + 0.5).rgb == BEACONBEAMCOLOR.rgb || ivec4(color * 255.0 + 0.5).rgb == BEACONSHELLCOLOR.rgb ) {
            discard;
        }
    }
    
    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}

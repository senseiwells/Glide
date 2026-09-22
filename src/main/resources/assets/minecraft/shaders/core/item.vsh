#version 330

#moj_import <minecraft:light.glsl>
#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:sample_lightmap.glsl>

#moj_import <minecraft:globals.glsl>
#moj_import <glide:glide.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler2;

out float sphericalVertexDistance;
out float cylindricalVertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;


uniform sampler2D Sampler0;
flat out int objectType; //See glide.glsl
out vec3 cornerTex1;
out vec3 cornerTex2;
out vec3 cornerTex3;
out vec4 normal;
out float height;

void main() {
    vec3 pos = Position;
    normal = ProjMat * ModelViewMat * vec4(Normal, 0.0);

    sphericalVertexDistance = fog_spherical_distance(pos);
    cylindricalVertexDistance = fog_cylindrical_distance(pos);

    vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, Color) * sample_lightmap(Sampler2, UV2);

    texCoord0 = UV0;
    
    objectType = get_object_from_texture(texture(Sampler0, texCoord0));
    
    height = get_heights(objectType, UV2);
    
    if (objectType == THERMAL) {
        texCoord0 = get_thermal_texcoords((1.0 / textureSize(Sampler0, 0)), gl_VertexID, texCoord0);
        if (gl_VertexID % 4 == 0 || gl_VertexID % 4 == 3) {
            pos.y += height;
        }
    }
    
    
    cornerTex1 = vec3(0.0);
    cornerTex2 = vec3(0.0);
    cornerTex3 = vec3(0.0);
    if (gl_VertexID % 4 == 0) cornerTex1 = vec3(texCoord0, 1.0);
    if (gl_VertexID % 4 == 2) cornerTex2 = vec3(texCoord0, 1.0);
    if (gl_VertexID % 2 == 1) cornerTex3 = vec3(texCoord0, 1.0);
    
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
}

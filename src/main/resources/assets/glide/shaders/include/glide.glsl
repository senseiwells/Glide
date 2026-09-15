#version 330

#define THERMAL 1
#define BOOST 2
#define OMNIBOOST 3
#define BEACONBEAM 4
#define BEACONSHELL 5

#define THERMALCOLOR ivec4(201, 217, 228, 255)
#define BOOSTCOLOR ivec4(255, 185, 0, 255)
#define OMNIBOOSTCOLOR ivec4(230, 100, 47, 255)
#define BEACONBEAMCOLOR ivec4(0, 255, 0, 255)
#define BEACONSHELLCOLOR ivec4(0, 83, 255, 255)



int get_object_from_texture(vec4 color) {
	ivec4 scaledColor = ivec4(color * 255.0 + 0.5);
	
	if (scaledColor == THERMALCOLOR) {
		return THERMAL;
	}
	if (scaledColor == BOOSTCOLOR) {
		return BOOST;
	}
	if (scaledColor == OMNIBOOSTCOLOR) {
		return OMNIBOOST;
	}
	if (scaledColor == BEACONBEAMCOLOR) {
		return BEACONBEAM;
	}
	if (scaledColor == BEACONSHELLCOLOR) {
		return BEACONSHELL;
	}
	
	return 0;
}

vec2 get_thermal_texcoords(vec2 texel, int vertId, vec2 texCoord) {
        switch (vertId % 4) {
            case 0:
            return texCoord + vec2(1.0, 1.0) * texel;
            case 1:
            return texCoord + vec2(1.0, -1.0) * texel;
            case 2:
            return texCoord + vec2(-1.0, -1.0) * texel;
            case 3:
            return texCoord + vec2(-1.0, 1.0) * texel;
        }
        return texCoord;
}

float get_heights(int objectType, ivec2 uv) {
switch(objectType) {
	case THERMAL:
		return uv.x / 16.0 + uv.y;
	case BOOST:
		return uv.x / 50.0 + uv.y;
	case OMNIBOOST:
		return uv.x / 50.0 + uv.y;
	case BEACONBEAM:
		return uv.x / 16.0 + uv.y;
	case BEACONSHELL:
		return uv.x / 16.0 + uv.y;		
}
return 0.0;
}
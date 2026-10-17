#define THERMAL 1
#define BOOST 2
#define OMNIBOOST 3
#define BEACONBEAM 4
#define BEACONSHELL 5

#define THERMALCOLORRANGE ivec4[2](ivec4(201, 217, 224, 255), ivec4(201, 217, 228, 255))
#define BOOSTCOLOR ivec4(255, 185, 0, 255)
#define OMNIBOOSTCOLOR ivec4(230, 100, 47, 255)
#define BEACONBEAMCOLOR ivec4(0, 255, 0, 255)
#define BEACONSHELLCOLOR ivec4(0, 83, 255, 255)

#define QUAD_CORNERS vec2[4](vec2(0.0, 0.0), vec2(0.0, 1.0), vec2(1.0, 1.0), vec2(1.0, 0.0))

int get_object_from_texture(sampler2D tex, vec2 coord, out int corner) {
	corner = -1;
	
	ivec2 size = textureSize(tex, 0);
    	ivec2 texel = clamp(ivec2(floor(coord * vec2(size))), ivec2(0), size - 1);
    	ivec4 scaledColor = ivec4(round(texelFetch(tex, texel, 0) * 255.0));
	
	if (all(greaterThanEqual(scaledColor, THERMALCOLORRANGE[0])) && all(lessThanEqual(scaledColor, THERMALCOLORRANGE[1])) ) { //stupid vector comparision that I hate. -S
		if (scaledColor.b < THERMALCOLORRANGE[1].b) {
			corner = scaledColor.b - THERMALCOLORRANGE[0].b; //change this to be alphas later -> `corner = scaledColor.a` -S
		}
		
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

float get_heights(int objectType, ivec2 uv) {
switch(objectType) {
	case THERMAL:
		return uv.x / 16.0 + uv.y;
	case BOOST:
		return 0.0;
	case OMNIBOOST:
		return 0.0;
	case BEACONBEAM:
		return uv.x / 16.0 + uv.y;
	case BEACONSHELL:
		return uv.x / 16.0 + uv.y;		
}
return 0.0;
}
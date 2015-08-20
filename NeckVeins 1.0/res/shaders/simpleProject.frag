#version 330

in vec4 location_projectorspace;

in vec3 UV_frag; 

uniform sampler2D projectionTexture;

uniform float transparency = 0.3;

void main()
{
	vec4 color = vec4(1, 0, 0, transparency);
/*
	vec4 local = location_projectorspace;
	
	local.x = (local.x/local.w+1)/2;
	local.y = (local.y/local.w+1)/2;
	local.z = (local.z/local.w+1)/2;
	color.xyz = texture(projectionTexture, local.xy).xyz;
	color.w = float(local.x >= 0 && local.x <= 1 && local.y >= 0 && local.y <= 1) * transparency;*/
	
	color.xyz = texture(projectionTexture, UV_frag.xy).xyz;
	//color.xyz = UV_frag.xyz;
	gl_FragColor = color;
}	
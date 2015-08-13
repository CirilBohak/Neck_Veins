#version 330

in vec4 location_projectorspace;

in vec3 normal_projectorspace;

in vec3 lightDirection;

uniform sampler2DShadow depthmap;
uniform sampler2D projectionTexture;

void main()
{
	vec4 color = vec4(1, 0, 0, 1);

	vec4 local = location_projectorspace;
	
	local.x = (local.x/local.w+1)/2;
	local.y = (local.y/local.w+1)/2;
	local.z = (local.z/local.w+1)/2;
	
	if(	local.x >= 0 && local.x <= 1 &&
		local.y >= 0 && local.y <= 1)	
		{
			float d = texture(depthmap, local.xyz);
			float intensity = clamp(dot(lightDirection, normal_projectorspace), 0, 1);
			//float intensity = 1;
			color.xyz = max(d*intensity, 0.1) * texture(projectionTexture, local.xy).xyz;
		}
	gl_FragColor = color;
}	
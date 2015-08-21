#version 330

in vec4 location_projectorspace;

in vec3 normal_projectorspace;

in vec3 lightDirection;

uniform sampler2DShadow depthmap;
uniform sampler2D projectionTexture;
uniform float xRatio = 1;
uniform float yRatio = 1;

void main()
{
	vec4 color = vec4(1, 0, 0, 1);

	vec4 local = location_projectorspace;
	
	vec2 texcoords = local.xy;
	texcoords.x = ((texcoords.x / local.w)/xRatio+1)/2;
	texcoords.y = ((texcoords.y / local.w)/yRatio+1)/2;
	
	local.x = (local.x/local.w+1)/2;
	local.y = (local.y/local.w+1)/2;
	local.z = (local.z/local.w+1)/2;
	
	if(	local.x >= 0 && local.x <= 1 &&
		local.y >= 0 && local.y <= 1)	
		{
			float d = texture(depthmap, local.xyz);
			float intensity = clamp(dot(lightDirection, normal_projectorspace), 0, 1);
			//float intensity = 1;
			color.xyz = max(d*intensity, 0.1) * texture(projectionTexture, texcoords).xyz;
		}
	/*if(texcoords.x >= 0 && texcoords.x <= 1 && texcoords.y >= 0 && texcoords.y <= 1)
	{
		color.xy = texcoords;
		color.z = 0;
	}else{
		color.xyz = 0;
	}*/
	
	gl_FragColor = color;
}	
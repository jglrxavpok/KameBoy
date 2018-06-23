#version 330

uniform sampler2D diffuse;
uniform vec2 minUV;
uniform vec2 maxUV;
uniform vec3 textColor;
in vec2 pos;
out vec4 color;

void main() {
    vec2 deltaUV = maxUV - minUV;
    float x = pos.x;
    float y = 1.0-pos.y;
    vec4 read_color = texture(diffuse, vec2(x, y) * deltaUV + minUV) * vec4(textColor, 1.0);
    if(read_color.a < 0.1)
        discard;
    color = read_color;
}
#version 330

uniform sampler2D diffuse;
uniform vec2 scroll;
in vec2 pos;
out vec4 color;

void main() {
    float x = pos.x;
    float y = 1.0-pos.y;
    color = vec4(texture(diffuse, vec2(x, y)).rgb, 1.0);
}
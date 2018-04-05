#version 330

uniform sampler2D diffuse;
in vec2 pos;
out vec4 color;

void main() {
    color = vec4(texture(diffuse, vec2(pos.x, 1.0-pos.y)).rgb, 1.0);
}
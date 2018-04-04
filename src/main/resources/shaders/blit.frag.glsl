#version 330

uniform sampler2D diffuse;
in vec2 pos;
out vec4 color;

void main() {
    color = texture(diffuse, vec2(pos.x, 1.0-pos.y));
}
#version 330

layout (location = 0) in vec2 position;
out vec2 pos;

void main() {
    pos = position;
    gl_Position = vec4(position*2.0 - vec2(1.0), 0.0, 1.0);
}
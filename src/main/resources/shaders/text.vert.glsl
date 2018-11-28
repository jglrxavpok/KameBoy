layout (location = 0) in vec2 position;
out vec2 pos;

uniform vec2 screenSize;
uniform vec2 offset;
uniform float scale = 1.0;

void main() {
    pos = position;
    vec2 screenPos = position * scale*16.0 + offset;
    vec2 posInScreenSpace = (screenPos) / screenSize;
    gl_Position = vec4(posInScreenSpace*2.0 - vec2(1.0), 0.0, 1.0);
}
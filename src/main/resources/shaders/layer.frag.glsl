uniform sampler2D diffuse;
in vec2 pos;
out vec4 color;

void main() {
    float x = pos.x;
    float y = 1.0-pos.y;
    vec4 texColor = texture(diffuse, vec2(x*160.0/256.0, y*143.0/256.0));
    if(texColor.a < 0.1)
        discard;
    color = texColor;
}
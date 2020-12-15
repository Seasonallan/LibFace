attribute vec4 position;
attribute vec4 color;
varying vec4 varyingColor;

void main() {
  varyingColor = color;
  gl_Position = position;
}
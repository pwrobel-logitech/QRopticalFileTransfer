#include <SDL2/SDL.h>
#include <SDL2/SDL_opengl.h>
#include <stdio.h>
#include <string>
#include <iostream>
#include <cmath>
#include <vector>

namespace glrenderer {

	//OpenGL context
	extern SDL_GLContext gContext;
	extern GLuint mtex0id;
	extern SDL_Surface *surf;

	//Call only from the render thread only !
	bool initGL(int sx, int sy);//init displaylists and pass window size
    void renderGL(int w, int h, const char* buff);
	void set_viewport_size(int x, int y);
    void setup_projection();
    void setup_scene();


}

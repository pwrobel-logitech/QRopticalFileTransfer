#include <SDL2/SDL.h>
#include <SDL2/SDL_opengl.h>
#include <stdio.h>
#include <string>
#include <iostream>
#include <cmath>
#include <vector>


#define DLOG printf
#ifdef RELEASE
#define DLOG(fmt, ...) (0)
#endif

//Window initial dimension constants
const int SCREEN_WIDTH = 660;
const int SCREEN_HEIGHT = 660;
const int STATUSBAR_HEIGHT = 45;
const int STATUSBAR_EPSILON = 5; //this is for the statusbar margins - real bar has 2*espilon less height

namespace glrenderer {

	//OpenGL context
	extern SDL_GLContext gContext;
	extern GLuint mtex0id;
	extern SDL_Surface *surf;
    extern SDL_Surface *progressbar_surf;

    extern double total_progress_to_draw;
    extern bool is_timeout_to_draw;
    extern std::string add_text_to_draw;

	//Call only from the render thread only !
	bool initGL(int sx, int sy);//init displaylists and pass window size
    void renderGL(int w, int h, const char* buff);
    void drawbar();
	void set_viewport_size(int x, int y);
    void setup_projection();
    void setup_scene();


}


namespace globals {
    extern std::string binpath;
    extern double startupsecs;
    extern double curr_time_left;

    extern const char* text_startupsequence;// = "Startup sequence ";
    extern std::string current_filename;
}


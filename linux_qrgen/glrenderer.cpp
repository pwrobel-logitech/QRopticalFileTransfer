#include "glrenderer.h"

SDL_GLContext glrenderer::gContext;

double default_fovX = 90;//degrees
int resX, resY;

void glrenderer::setup_projection(){

	//glViewport(0, 0, glrenderer::myworld.resX, glrenderer::myworld.resY);
	//Initialize Projection Matrix
	glMatrixMode( GL_PROJECTION );
	glLoadIdentity();
    glOrtho(-1.0, 1.0, -1.0, 1.0, -1.0, 1.0);
    //gluPerspective(90.0, 1 , 0.1, 10.0);

}

void glrenderer::set_viewport_size(int x, int y){
    resX = x;
    resY = y;
}

bool glrenderer::initGL(int sx, int sy)
{

    Uint32 rmask, gmask, bmask, amask;

    /* SDL interprets each pixel as a 32-bit number, so our masks must depend
       on the endianness (byte order) of the machine */

    rmask = 0xff000000;
    gmask = 0x00ff0000;
    bmask = 0x0000ff00;
    amask = 0x000000ff;

    glrenderer::surf = SDL_CreateRGBSurface(0, 1024, 1024, 32,
                                      rmask, gmask, bmask, amask);

	glEnable(GL_DEPTH_TEST);
	glMatrixMode( GL_MODELVIEW );
	glLoadIdentity();
	return true;
}

void render_subscreen()
{
		
	GLenum error = GL_NO_ERROR;
    glrenderer::setup_projection();
    glViewport(0,0,resX,resY);
	//Initialize Modelview Matrix
	glMatrixMode( GL_MODELVIEW );

}

void glrenderer::renderGL(int w, int h, const char* buff)
{
	
	//Clear color buffer and depth buffer
	glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);
	//clear color
    glClearColor( 1.f, 1.0f, 1.0f, 1.f );
	glMatrixMode( GL_MODELVIEW );
	glLoadIdentity();
    //draw here..


    glEnable( GL_TEXTURE_2D );

    //glColor3f(1.0f, 0.0f, 0.0f);
    float x=0.7, y=0.7;

    int Mode = GL_RGBA;

    SDL_LockSurface(glrenderer::surf);

    memset(glrenderer::surf->pixels, 0x7f, glrenderer::surf->w * glrenderer::surf->h * 4);

    for(int j=0;j<h;j++)
        for (int i=0;i<w;i++)
        {
            ((char*)glrenderer::surf->pixels)[(i+glrenderer::surf->pitch*j/4)*4] = buff[i+j*w];
            ((char*)glrenderer::surf->pixels)[(i+glrenderer::surf->pitch*j/4)*4+1] = buff[i+j*w];
            ((char*)glrenderer::surf->pixels)[(i+glrenderer::surf->pitch*j/4)*4+2] = buff[i+j*w];
            ((char*)glrenderer::surf->pixels)[(i+glrenderer::surf->pitch*j/4)*4+3] = buff[i+j*w];
        }




    glTexImage2D(GL_TEXTURE_2D, 0, Mode, glrenderer::surf->w, glrenderer::surf->h,
                 0, Mode, GL_UNSIGNED_BYTE, glrenderer::surf->pixels);

    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

    SDL_UnlockSurface(glrenderer::surf);

    float tex_fractw = (float) w / (float)glrenderer::surf->w;
    float tex_fracth = (float) h / (float)glrenderer::surf->h;

    glBegin(GL_QUADS);
        glTexCoord2f(0,tex_fracth);
        glVertex2f(-x,-y);
        glTexCoord2f(tex_fractw,tex_fracth);
        glVertex2f(x,-y);
        glTexCoord2f(tex_fractw,0);
        glVertex2f(x,y);
        glTexCoord2f(0,0);
        glVertex2f(-x,y);
    glEnd();

	glFlush();
	glFinish();
}


void glrenderer::setup_scene(){
	glColor3d(1.0, 1.0, 1.0);

}

GLuint glrenderer::mtex0id = 0;
SDL_Surface *glrenderer::surf;


#include "glrenderer.h"
#include <SDL2/SDL_ttf.h>

SDL_GLContext glrenderer::gContext;

double default_fovX = 90;//degrees
int resX, resY;

void glrenderer::setup_projection(){

	//glViewport(0, 0, glrenderer::myworld.resX, glrenderer::myworld.resY);
	//Initialize Projection Matrix
    glMatrixMode(GL_PROJECTION);
    glLoadIdentity();
    glOrtho(0.0f, SCREEN_WIDTH, 0.0f, SCREEN_HEIGHT+STATUSBAR_HEIGHT, 0.0f, 1.0f);
    //gluPerspective(90.0, 1 , 0.1, 10.0);

}

double glrenderer::total_progress_to_draw;
bool glrenderer::is_timeout_to_draw;
std::string glrenderer::add_text_to_draw;

void glrenderer::set_viewport_size(int x, int y){
    resX = x;
    resY = y;
}

TTF_Font* Sans;// = TTF_OpenFont("/usr/share/fonts/truetype/freefont/FreeSans.ttf", 24);

bool glrenderer::initGL(int sx, int sy)
{
    if (TTF_Init() < 0) {
        printf("Failed to initialize the SDL TTF module !");
    }
    Sans = TTF_OpenFont("/usr/share/fonts/truetype/freefont/FreeSans.ttf", 24);

    Uint32 rmask, gmask, bmask, amask;

    /* SDL interprets each pixel as a 32-bit number, so our masks must depend
       on the endianness (byte order) of the machine */

    rmask = 0xff000000;
    gmask = 0x00ff0000;
    bmask = 0x0000ff00;
    amask = 0x000000ff;

    glrenderer::surf = SDL_CreateRGBSurface(0, 512, 512, 32,
                                      rmask, gmask, bmask, amask);

    glrenderer::progressbar_surf = SDL_CreateRGBSurface(0, SCREEN_WIDTH-2*STATUSBAR_EPSILON,
                                                        STATUSBAR_HEIGHT-2*STATUSBAR_EPSILON, 32,
                                                        rmask, gmask, bmask, amask);

	glEnable(GL_DEPTH_TEST);
	glMatrixMode( GL_MODELVIEW );
	glLoadIdentity();
	return true;
}

int rmask = 0xff000000;
int gmask = 0x00ff0000;
int bmask = 0x0000ff00;
int amask = 0x000000ff;


void glrenderer::drawbar(){

    //SDL_Surface *winsurf = SDL_GetWindowSurface(win);


    //this opens a font style and sets a size

    SDL_Color White = {255, 0, 0};
    // this is the color in rgb format, maxing out all would give you the color white, and it will be your text's color

    SDL_Surface* surfaceMessage = TTF_RenderText_Solid(Sans, "asdfasdfasdf", White);
    // as TTF_RenderText_Solid could only be used on SDL_Surface then you have to create the surface first

    char *txtbuff = new char [glrenderer::progressbar_surf->w * glrenderer::progressbar_surf->h * 4];
    for (int i = 0; i < glrenderer::progressbar_surf->w * glrenderer::progressbar_surf->h * 4; i++)
        txtbuff[i] = 0;
    //SDL_LockSurface(surfaceMessage);

    //memcpy(txtbuff, surfaceMessage->pixels, surfaceMessage->w * surfaceMessage->h);
            //memset(surfaceMessage->pixels, 0x24, surfaceMessage->w * surfaceMessage->h);
    //SDL_UnlockSurface(surfaceMessage);

    //SDL_FillRect(surfaceMessage, NULL, SDL_MapRGB(surfaceMessage->format, 255, 0, 0));


    SDL_FillRect(glrenderer::progressbar_surf, NULL, SDL_MapRGB(glrenderer::progressbar_surf->format, 255, 255, 255));

    SDL_LockSurface(glrenderer::progressbar_surf);
    //SDL_LockSurface(surfaceMessage);

    //memset(glrenderer::progressbar_surf->pixels,
    //       0xf0, glrenderer::progressbar_surf->w * glrenderer::progressbar_surf->h * 4);
    //memset(glrenderer::progressbar_surf->pixels,
    //       0xe0, glrenderer::progressbar_surf->w * glrenderer::progressbar_surf->h * 2);

    int w =glrenderer::progressbar_surf->w;
    int h =glrenderer::progressbar_surf->h;



    for(int j=0;j<h;j++)
        for (int i=0;i<w*glrenderer::total_progress_to_draw;i++)
        {
            ((char*)glrenderer::progressbar_surf->pixels)[(i+glrenderer::progressbar_surf->pitch*j/4)*4] = 0;
            ((char*)glrenderer::progressbar_surf->pixels)[(i+glrenderer::progressbar_surf->pitch*j/4)*4+1] = 0;
            ((char*)glrenderer::progressbar_surf->pixels)[(i+glrenderer::progressbar_surf->pitch*j/4)*4+2] = 0xff;
            ((char*)glrenderer::progressbar_surf->pixels)[(i+glrenderer::progressbar_surf->pitch*j/4)*4+3] = 0xff;
        }

    //memset(glrenderer::progressbar_surf->pixels,
    //       0x0, SCREEN_WIDTH*STATUSBAR_HEIGHT);

    //memcpy(glrenderer::progressbar_surf->pixels, txtbuff,
    //       glrenderer::progressbar_surf->w * glrenderer::progressbar_surf->h * 1);

    //memcpy(glrenderer::progressbar_surf->pixels, surfaceMessage->pixels, surfaceMessage->w * surfaceMessage->h);

    //SDL_UnlockSurface(surfaceMessage);
    SDL_UnlockSurface(glrenderer::progressbar_surf);

    //SDL_FillRect(surfaceMessage, NULL, SDL_MapRGB(surfaceMessage->format, 255, 0, 0));

    SDL_Rect dst;
    dst.x = 200;
    dst.y = 7;

    Uint32 rmask, gmask, bmask, amask;

    /* SDL interprets each pixel as a 32-bit number, so our masks must depend
       on the endianness (byte order) of the machine */

    rmask = 0xff000000;
    gmask = 0x00ff0000;
    bmask = 0x0000ff00;
    amask = 0x000000ff;


     //SDL_Surface* tst = SDL_CreateRGBSurface(0, 40, 40, 32,
     //                                 rmask, gmask, bmask, amask);
     //SDL_FillRect(tst, NULL, SDL_MapRGB(tst->format, 0, 255, 0));

    SDL_BlitSurface(surfaceMessage, NULL, glrenderer::progressbar_surf, &dst);

    SDL_FreeSurface(surfaceMessage);


    //SDL_SetSurfaceBlendMode(progressbar_surf, SDL_BLENDMODE_ADD);

    glMatrixMode(GL_PROJECTION);
    glLoadIdentity();
    glOrtho(0.0f, SCREEN_WIDTH, 0.0f, SCREEN_HEIGHT+STATUSBAR_HEIGHT, 0.0f, 1.0f);

    glMatrixMode( GL_MODELVIEW );
    glLoadIdentity();

    //glColor3f(1.0f, 0.0f, 0.0f);
    float x=SCREEN_WIDTH-2*STATUSBAR_EPSILON, y=STATUSBAR_HEIGHT-2*STATUSBAR_EPSILON ;

    int Mode = GL_RGBA;


    glTexImage2D(GL_TEXTURE_2D, 0, Mode, glrenderer::progressbar_surf->w, glrenderer::progressbar_surf->h,
                 0, Mode, GL_UNSIGNED_BYTE, glrenderer::progressbar_surf->pixels);

    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);


    float yoffset = SCREEN_HEIGHT+STATUSBAR_EPSILON;
    float xoffset = STATUSBAR_EPSILON;

    float tex_fractw = 1.0f;
    float tex_fracth = 1.0f;

    glBegin(GL_QUADS);
        glTexCoord2f(0,tex_fracth);
        glVertex2f(0+xoffset,0+yoffset);
        glTexCoord2f(tex_fractw,tex_fracth);
        glVertex2f(x+xoffset,0+yoffset);
        glTexCoord2f(tex_fractw,0);
        glVertex2f(x+xoffset,y+yoffset);
        glTexCoord2f(0,0);
        glVertex2f(0+xoffset,y+yoffset);
    glEnd();

    delete []txtbuff;

    //SDL_BlitSurface(progressbar_surf, NULL, winsurf, NULL);

}

void render_subscreen()
{
		
	GLenum error = GL_NO_ERROR;
    glrenderer::setup_projection();
    glViewport(0,0, SCREEN_WIDTH, SCREEN_HEIGHT+STATUSBAR_HEIGHT);
	//Initialize Modelview Matrix
	glMatrixMode( GL_MODELVIEW );

}

void glrenderer::renderGL(int w, int h, const char* buff)
{
    render_subscreen();
	
	//Clear color buffer and depth buffer
	glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);
	//clear color
    glClearColor( 1.0f, 1.0f, 1.0f, 1.f );
	glMatrixMode( GL_MODELVIEW );
	glLoadIdentity();
    //draw here..


    glEnable( GL_TEXTURE_2D );

    //glColor3f(1.0f, 0.0f, 0.0f);
    float x=SCREEN_WIDTH, y=SCREEN_HEIGHT;

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

    SDL_UnlockSurface(glrenderer::surf);
    SDL_Surface* tst2 = SDL_CreateRGBSurface(0, 4, 4, 32,
                                     rmask, gmask, bmask, amask);
    SDL_FillRect(tst2, NULL, SDL_MapRGB(tst2->format, 0, 255, 0));


/*

    TTF_Font* Sans = TTF_OpenFont("/usr/share/fonts/truetype/freefont/FreeSans.ttf", 24);
    //this opens a font style and sets a size

    SDL_Color White = {0, 0, 0};
    // this is the color in rgb format, maxing out all would give you the color white, and it will be your text's color

    SDL_Surface* surfaceMessage = TTF_RenderText_Solid(Sans, "asdfasdfasdf", White);
    // as TTF_RenderText_Solid could only be used on SDL_Surface then you have to create the surface first




    SDL_BlitSurface(surfaceMessage, NULL, glrenderer::surf, NULL);
*/

    glTexImage2D(GL_TEXTURE_2D, 0, Mode, glrenderer::surf->w, glrenderer::surf->h,
                 0, Mode, GL_UNSIGNED_BYTE, glrenderer::surf->pixels);

    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);



    float tex_fractw = (float) w / (float)glrenderer::surf->w;
    float tex_fracth = (float) h / (float)glrenderer::surf->h;

    float scrfrac = 0.7;
    float offsetx = x*(1.0 - scrfrac) / 2.0;
    float offsety = y*(1.0 - scrfrac) / 2.0;

    glBegin(GL_QUADS);
        glTexCoord2f(0,tex_fracth);
        glVertex2f(0+offsetx,0+offsety);
        glTexCoord2f(tex_fractw,tex_fracth);
        glVertex2f(x*scrfrac+offsetx,0+offsety);
        glTexCoord2f(tex_fractw,0);
        glVertex2f(x*scrfrac+offsetx,y*scrfrac+offsety);
        glTexCoord2f(0,0);
        glVertex2f(0+offsetx,y*scrfrac+offsety);
    glEnd();


    glrenderer::drawbar();

	glFlush();
	glFinish();
}


void glrenderer::setup_scene(){
	glColor3d(1.0, 1.0, 1.0);

}

GLuint glrenderer::mtex0id = 0;
SDL_Surface *glrenderer::surf;
SDL_Surface *glrenderer::progressbar_surf;


//From the tclap library :
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

#include <string>
#include <iostream>
#include <algorithm>
#include "tclap/CmdLine.h"

#include "glrenderer.h"
#include "SDL2/SDL.h"
#include <stdio.h>

using namespace TCLAP;
using namespace std;

//Window initial dimension constants
const int SCREEN_WIDTH = 640;
const int SCREEN_HEIGHT = 640;

bool is_fullscreen = false;
//Starts up SDL and creates window
bool init(bool is_fullscreen);
void PrintEvent(const SDL_Event * event);
//For debugging purposes
void PrintEvent(const SDL_Event * event);

//Frees media and shuts down SDL
void close();

//The window we'll be rendering to
SDL_Window* gWindow = NULL;

int screen_counter = 0;

//current window size
int sizeX = 0;
int sizeY = 0;

bool is_displaychange_requested = false;
bool is_fullscreenchange_requested = false;
bool is_requested_reread_winsize = false;

void create_thread();
static int MyThread(void *ptr);
int draw_frame();
bool is_thread_running = true;
SDL_Thread *thread;
SDL_mutex *mutex;

//lock sdl mutex
bool lock_mutex(){
    if (SDL_LockMutex(mutex) == 0) {
        return true;
    } else {
        fprintf(stderr, "Couldn't lock mutex\n");
        return false;
    }
};

//unlock sdl mutex
bool unlock_mutex(){
    SDL_UnlockMutex(mutex);
    return true;///fix this latex to check if it succeeds
};

//invalidate logics - invalidate = request for a redraw in a separate thread
bool is_screen_valid = true;
void invalidate_screen(){
    if(is_screen_valid){
        lock_mutex();
        is_screen_valid = false;
        unlock_mutex();
    }
}


void create_thread(){
    mutex = SDL_CreateMutex();
    if (!mutex) {
        printf( "Couldn't create mutex\n");
        return;
    }
    //create a thread, pass it a thread pointer function
    thread = SDL_CreateThread(MyThread, "RendererThr", (void *)NULL);
    if (NULL == thread) {
        printf("\nSDL_CreateThread failed: %s\n", SDL_GetError());
    }
}

bool init(bool is_fullscreen)
{
    //gl sdl stuff
    printf("Started to initialize opengl context \n");
    glrenderer::gContext = SDL_GL_CreateContext( gWindow );
    if( glrenderer::gContext == NULL )
    {
        printf( "OpenGL context could not be created! SDL Error: %s\n", SDL_GetError() );
    }
    else
    {
        SDL_GetWindowSize(gWindow, &sizeX, &sizeY);
        printf("Got window size x: %d, y: %d \n", sizeX, sizeY);
        if( !glrenderer::initGL(sizeX, sizeY) )
        {
            printf( "Unable to initialize OpenGL!\n" );
        }
        //render gl
        glrenderer::renderGL();
    }
    int numdisplays = SDL_GetNumVideoDisplays();
    printf("Numdisplays %d \n", numdisplays);
    if(numdisplays <= 0)
    {
        printf("Error in the number of the displays \n");
        exit(0);
    }
    return true;
}

int MyThread(void *ptr)
{
    int cnt = 0;
    lock_mutex();

    if(!init(false))
    {
        printf( "Failed to initialize!\n" );
    }
    unlock_mutex();
    draw_frame();
    while(is_thread_running) {
        lock_mutex();
        if(is_requested_reread_winsize)
        {
            SDL_GetWindowSize(gWindow, &sizeX, &sizeY);
            if( !glrenderer::initGL(sizeX, sizeY) )
            {
                printf( "Unable to initialize OpenGL!\n" );
            }

            draw_frame();
        }
        if(is_fullscreenchange_requested)
        {
            draw_frame();
            is_fullscreenchange_requested = false;
        }
        unlock_mutex();

        lock_mutex();
        draw_frame();
        unlock_mutex();

        //printf("\nThread counter: %d", cnt);
        SDL_Delay(1);
        if (!is_screen_valid){
            draw_frame();
            is_screen_valid = true;
        }
        cnt++;
    }
    return cnt;
}


void request_reread_win_size(){
    lock_mutex();
    is_requested_reread_winsize = true;
    unlock_mutex();
}

void request_fulscreen_change(){
    lock_mutex();
    is_fullscreenchange_requested = true;
    unlock_mutex();
}


void request_display_change(){
    lock_mutex();
    is_displaychange_requested = true;
    unlock_mutex();
}

int draw_frame(){
    lock_mutex();
    SDL_GetWindowSize(gWindow, &sizeX, &sizeY);
    glrenderer::set_viewport_size(sizeX, sizeY);
    glrenderer::renderGL();
    SDL_GL_SwapWindow( gWindow );
    unlock_mutex();
    return 0;
}

void close()
{
    //Destroy window
    SDL_DestroyWindow( gWindow );
    gWindow = NULL;
    //Quit SDL subsystems
    SDL_Quit();
}

SDL_Event event;

void do_SDL_setup(){

    //Initialization flag
    bool success = true;
    //Initialize SDL
    if( SDL_Init( SDL_INIT_VIDEO ) < 0 )
    {
        printf( "SDL could not initialize! SDL Error: %s\n", SDL_GetError() );
        success = false;
    }
    //Create window
    gWindow = SDL_CreateWindow( "GL_RENDERER", SDL_WINDOWPOS_CENTERED_DISPLAY(screen_counter),
    SDL_WINDOWPOS_CENTERED_DISPLAY(screen_counter),
    SCREEN_WIDTH, SCREEN_HEIGHT, SDL_WINDOW_SHOWN|SDL_WINDOW_OPENGL|SDL_WINDOW_RESIZABLE );
    if( gWindow == NULL )
    {
        printf( "Window could not be created! SDL Error: %s\n", SDL_GetError() );
        success = false;
    }
    create_thread();
    lock_mutex();
    unlock_mutex();
    //Main loop flag
    bool quit = false;

    //Event handler
    SDL_Event e;

    //While application is running
        while( !quit )
        {
            //Handle events on queue
            while( SDL_PollEvent( &e ) != 0 )
            {
                PrintEvent(&e);
                switch (e.type)
                {
                    case SDL_WINDOWEVENT:
                        printf("Window event \n");
                        invalidate_screen();
                        break;
                    case SDL_MOUSEMOTION:
                        break;
                    case SDL_QUIT:
                        quit = true;
                    case SDL_KEYDOWN:
                        if(e.key.keysym.sym == SDLK_ESCAPE)
                            quit = true; //quit
                        break;
                }
            }
        }
        is_thread_running = false;
        int threadReturnValue;
        //wait for thread to finish - join thread
        SDL_WaitThread(thread, &threadReturnValue);
        printf("Thread returned value: %d \n", threadReturnValue);
        SDL_DestroyMutex(mutex);
        //Free resources and close SDL
        close();

}


void PrintEvent(const SDL_Event * event)
{
    if (event->type == SDL_WINDOWEVENT) {
        switch (event->window.event) {
        case SDL_WINDOWEVENT_SHOWN:
            printf("Window %d shown \n", event->window.windowID);
             break;
        case SDL_WINDOWEVENT_HIDDEN:
            printf("Window %d hidden \n", event->window.windowID);
            break;
        case SDL_WINDOWEVENT_EXPOSED:
            printf("Window %d exposed \n", event->window.windowID);
            break;
        case SDL_WINDOWEVENT_MOVED:
            printf("Window %d moved to %d,%d \n",
                event->window.windowID, event->window.data1,
                event->window.data2);
            break;
        case SDL_WINDOWEVENT_RESIZED:
            printf("Window %d resized to %dx%d \n",
                event->window.windowID, event->window.data1,
                event->window.data2);
            break;
        case SDL_WINDOWEVENT_MINIMIZED:
            printf("Window %d minimized \n", event->window.windowID);
            break;
        case SDL_WINDOWEVENT_MAXIMIZED:
            printf("Window %d maximized \n", event->window.windowID);
            break;
        case SDL_WINDOWEVENT_RESTORED:
            printf("Window %d restored \n", event->window.windowID);
            break;
        case SDL_WINDOWEVENT_ENTER:
            printf("Mouse entered window %d \n",
                event->window.windowID);
            break;
        case SDL_WINDOWEVENT_LEAVE:
            printf("Mouse left window %d \n", event->window.windowID);
            break;
        case SDL_WINDOWEVENT_FOCUS_GAINED:
            printf("Window %d gained keyboard focus \n",
                event->window.windowID);
            break;
        case SDL_WINDOWEVENT_FOCUS_LOST:
            printf("Window %d lost keyboard focus \n",
                event->window.windowID);
            break;
        case SDL_WINDOWEVENT_CLOSE:
            printf("Window %d closed \n", event->window.windowID);
            break;
        default:
            printf("Window %d got unknown event %d \n",
                event->window.windowID, event->window.event);
            break;
        }
    }
}

int main(int argc, char** argv)
{
	try {  

    CmdLine cmd("Give it a file to send as a set of QR frames.", ' ', "0.9");

    ValueArg<int> QRsizeArg("q", "qrsize", "Bytes capacity of single QR frame", false, 31, "int");
    cmd.add( QRsizeArg );

    //SwitchArg reverseSwitch("r","reverse","Print name backwards", false);
    //cmd.add( reverseSwitch );

    UnlabeledMultiArg<string> multi("fnames", "File list, space-separated",
                                    true, "File/Space-separated list of files");
    cmd.add( multi );

	// Parse the args.
	cmd.parse( argc, argv );

	// Get the value parsed by each arg. 
    //string name = nameArg.getValue();
    //bool reverseName = reverseSwitch.getValue();
    vector<string>  fileNames = multi.getValue();

    printf("QR size : %d\n", QRsizeArg.getValue());

    for(vector<string>::iterator it = fileNames.begin(); it!=fileNames.end(); it++){
        std::cout << *it << std::endl;
    }

	} catch (ArgException &e)  // catch any exceptions
    {
        cerr << "error: " << e.error() << " for arg " << e.argId() << endl;
        exit(0);
    }

    do_SDL_setup();

}



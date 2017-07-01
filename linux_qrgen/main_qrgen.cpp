//From the tclap library :
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
#include <ctype.h>
#include <string>
#include <iostream>
#include <algorithm>
#include "tclap/CmdLine.h"

#include "glrenderer.h"
#include "SDL2/SDL.h"
#include <stdio.h>

#include <qr_frame_producer.h>

using namespace TCLAP;
using namespace std;




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

///////////////////////////system info
std::string executable_path;

//////////////////////////// QR info
//current QR buffer size
int QRbuffw=0, QRbuffh=0;
char* QRbuffer;
unsigned int Nframe=0;
unsigned int Nheader_frame=0;

Qr_frame_producer* frame_producer = NULL;
vector<string> fileNames;
int qrbytesize = 0;
int current_file_index = 0;
int initTime = 5;
int targetFPS = 14;
int maxAllowedErrorPercent = 50;
////////////////////////////

///////consts resulting from the above QR info
double timeframe_delay; //delay between two consecutive frames = 1/fps
///

//general globals regarding the state of the program at the current moment
double current_ms_time = 0;
bool is_in_header_generation_mode = true; //when header frames are generated, keep this to true
bool is_last_frame_for_curr_file_done = false;
//


bool is_displaychange_requested = false;
bool is_fullscreenchange_requested = false;
bool is_requested_reread_winsize = false;

inline bool does_file_exist(const std::string& name) {
    if (FILE *file = fopen(name.c_str(), "r")) {
        fclose(file);
        return true;
    } else {
        return false;
    }
}

void create_thread();
static int MyThread(void *ptr);
int draw_frame();
bool is_thread_running = true;
SDL_Thread *thread;
SDL_mutex *mutex;

double get_current_ms_time(){
    struct timeval tp;
    gettimeofday(&tp, NULL);
    long int ms = tp.tv_sec * 1000 + tp.tv_usec / 1000;
    return ((double) ms);
}


int syscommand(string aCommand, string & result) {
    FILE * f;
    if ( !(f = popen( aCommand.c_str(), "r" )) ) {
            cout << "Can not open file" << endl;
            return -1;
        }
        const int BUFSIZE = 4096;
        char buf[ BUFSIZE ];
        if (fgets(buf,BUFSIZE,f)!=NULL) {
            result = buf;
        }
        pclose( f );
        return 1;
    }


string getBundleName () {
    pid_t procpid = getpid();
    stringstream toCom;
    toCom << "cat /proc/" << procpid << "/comm";
    string fRes="";
    syscommand(toCom.str(),fRes);
    size_t last_pos = fRes.find_last_not_of(" \n\r\t") + 1;
    if (last_pos != string::npos) {
        fRes.erase(last_pos);
    }
    return fRes;
}


string getBundlePath () {
pid_t procpid = getpid();
string appName = getBundleName();
stringstream command;
command <<  "readlink /proc/" << procpid << "/exe | sed \"s/\\(\\/" << appName << "\\)$//\"";
string fRes;
syscommand(command.str(),fRes);
return fRes;
}

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

int curr_data_frame_num = 0;
int total_data_frame_num = 0;
char* pathseparator = "/";

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


double first_frame_draw_time;

int produce_next_QR_frame_to_buffer(){
    if(current_file_index < fileNames.size()){
        if(Nframe == 0){
            if(frame_producer == NULL){
                frame_producer = new Qr_frame_producer;
                std::string path = executable_path;
                if ((fileNames[current_file_index].c_str())[0] == (pathseparator)[0])
                    path = std::string("");
                frame_producer->set_external_file_info(fileNames[current_file_index].c_str(),
                                                       path.c_str(),
                                                       qrbytesize,
                                                       ((double)maxAllowedErrorPercent)/100.0,
                                                       511);
                globals::current_filename = std::string((fileNames[current_file_index]).c_str());
                std::string fullfilename(path+std::string(pathseparator)+fileNames[current_file_index]);
                bool exist = does_file_exist(fullfilename);
                if (!exist){
                    fprintf(stderr, "File %s does not exist!\n", fullfilename.c_str());
                    frame_producer = NULL;
                    current_file_index++;
                    Nframe = 0;
                    curr_data_frame_num = 0;
                    is_in_header_generation_mode = true;
                    glrenderer::is_timeout_to_draw = true;
                    first_frame_draw_time = get_current_ms_time();
                    if (current_file_index >= fileNames.size())
                        return 2;
                    else
                        return 1;
                }
            }

        }
        if(frame_producer){
            int status = frame_producer->produce_next_qr_grayscale_image_to_mem(&QRbuffer, &QRbuffw);
            DLOG("Got status from the frame producer : %d\n", status);
            QRbuffh = QRbuffw;
            Nframe++;
            if (status == 1){
                SDL_Delay(3000);
                delete frame_producer;
                frame_producer = NULL;
                current_file_index++;
                Nframe = 0;
                curr_data_frame_num = 0;
                is_in_header_generation_mode = true;
                glrenderer::is_timeout_to_draw = true;
                first_frame_draw_time = get_current_ms_time();
                return 1;
            }
        }
        return 0;
    } else {
        if(frame_producer != NULL)
            delete frame_producer;
        return 2;
    }
}



bool init(bool is_fullscreen)
{
    //gl sdl stuff
    DLOG("Started to initialize opengl context \n");
    glrenderer::gContext = SDL_GL_CreateContext( gWindow );
    if( glrenderer::gContext == NULL )
    {
        fprintf(stderr, "OpenGL context could not be created! SDL Error: %s\n", SDL_GetError() );
    }
    else
    {
        SDL_GetWindowSize(gWindow, &sizeX, &sizeY);
        DLOG("Got window size x: %d, y: %d \n", sizeX, sizeY);
        if( !glrenderer::initGL(sizeX, sizeY) )
        {
            fprintf( stderr, "Unable to initialize OpenGL!\n" );
        }
        //render gl
        glrenderer::renderGL(QRbuffw, QRbuffh, QRbuffer);
    }
    int numdisplays = SDL_GetNumVideoDisplays();
    DLOG("Numdisplays %d \n", numdisplays);
    if(numdisplays <= 0)
    {
        fprintf(stderr, "Error in the number of the displays \n");
        exit(0);
    }
    return true;
}

bool exit_app = false;


int MyThread(void *ptr)
{
    double last_time = get_current_ms_time();

    int cnt = 0;
    lock_mutex();

    if(!init(false))
    {
        fprintf(stderr, "Failed to initialize!\n" );
    }


    curr_data_frame_num = 0;
    produce_next_QR_frame_to_buffer();
    draw_frame();
    cnt++;
    //curr_data_frame_num++;
    unlock_mutex();

    first_frame_draw_time = get_current_ms_time();

    while(is_thread_running) {


        if (is_requested_reread_winsize)
        {
            SDL_GetWindowSize(gWindow, &sizeX, &sizeY);
            if( !glrenderer::initGL(sizeX, sizeY) )
            {
                fprintf(stderr, "Unable to initialize OpenGL!\n" );
            }

            draw_frame();
        }

        if (is_in_header_generation_mode){
            int currt = get_current_ms_time();
            double tl = ((get_current_ms_time() - first_frame_draw_time)/1000.0);
            glrenderer::total_progress_to_draw =  tl / globals::startupsecs;
            globals::curr_time_left = initTime - tl;
            if (globals::curr_time_left < 0.0)
                globals::curr_time_left = 0.0;
            if (get_current_ms_time() >= first_frame_draw_time + initTime * 1000.0){
                is_in_header_generation_mode = false;
                glrenderer::is_timeout_to_draw = false;
                frame_producer->tell_no_more_generating_header();
            }
        }

        //printf("\nThread counter: %d", cnt);
        double tdiff = get_current_ms_time() - last_time;
        if(tdiff >= timeframe_delay){
            last_time = get_current_ms_time();
            // redraw frame


            lock_mutex();


            if (!is_in_header_generation_mode){
                glrenderer::total_progress_to_draw = 0;
                int totframes = frame_producer->tell_how_much_frames_will_be_generated();
                int nf = curr_data_frame_num;
                if (nf > totframes-1)
                    nf = totframes-1;
                if (totframes > 1)
                    glrenderer::total_progress_to_draw = ((double)curr_data_frame_num) / (totframes-1);
                if (glrenderer::total_progress_to_draw>1.0)
                    glrenderer::total_progress_to_draw=1.0;

            }

            int status = produce_next_QR_frame_to_buffer();

            if (!is_in_header_generation_mode){
                curr_data_frame_num++;
            }

            if(status == 0)
                draw_frame();
            if (status == 2){
                is_thread_running = false;
            }
            is_screen_valid = true;

            unlock_mutex();
            cnt++;

        }
        //do the delay

        //

        SDL_Delay(1);
        //if(cnt==90)
          //  frame_producer->tell_no_more_generating_header();
        //if (!is_screen_valid){

        //}

    }
    lock_mutex();
    exit_app = true;
    unlock_mutex();
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
    //glrenderer::set_viewport_size(sizeX, sizeY);
    glrenderer::renderGL(QRbuffw, QRbuffh, QRbuffer);


    //SDL_UpdateWindowSurface(gWindow);
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
        fprintf(stderr, "SDL could not initialize! SDL Error: %s\n", SDL_GetError() );
        success = false;
    }
    //Create window
    gWindow = SDL_CreateWindow( "Optical file transfer upload", SDL_WINDOWPOS_CENTERED_DISPLAY(screen_counter),
    SDL_WINDOWPOS_CENTERED_DISPLAY(screen_counter),
    SCREEN_WIDTH, SCREEN_HEIGHT + STATUSBAR_HEIGHT, SDL_WINDOW_SHOWN|SDL_WINDOW_OPENGL|SDL_WINDOW_RESIZABLE );
    if( gWindow == NULL )
    {
        fprintf(stderr, "Window could not be created! SDL Error: %s\n", SDL_GetError() );
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
            lock_mutex();
            if (exit_app)
                quit = true;
            unlock_mutex();
            //Handle events on queue
            while( SDL_PollEvent( &e ) != 0 )
            {
                lock_mutex();
                if (exit_app)
                    quit = true;
                unlock_mutex();
                PrintEvent(&e);
                switch (e.type)
                {
                    case SDL_WINDOWEVENT:
                        DLOG("Window event \n");
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
        DLOG("Thread returned value: %d \n", threadReturnValue);
        SDL_DestroyMutex(mutex);
        //Free resources and close SDL
        close();

}


void PrintEvent(const SDL_Event * event)
{
    if (event->type == SDL_WINDOWEVENT) {
        switch (event->window.event) {
        case SDL_WINDOWEVENT_SHOWN:
            DLOG("Window %d shown \n", event->window.windowID);
             break;
        case SDL_WINDOWEVENT_HIDDEN:
            DLOG("Window %d hidden \n", event->window.windowID);
            break;
        case SDL_WINDOWEVENT_EXPOSED:
            DLOG("Window %d exposed \n", event->window.windowID);
            break;
        case SDL_WINDOWEVENT_MOVED:
            DLOG("Window %d moved to %d,%d \n",
                event->window.windowID, event->window.data1,
                event->window.data2);
            break;
        case SDL_WINDOWEVENT_RESIZED:
            DLOG("Window %d resized to %dx%d \n",
                event->window.windowID, event->window.data1,
                event->window.data2);
            break;
        case SDL_WINDOWEVENT_MINIMIZED:
            DLOG("Window %d minimized \n", event->window.windowID);
            break;
        case SDL_WINDOWEVENT_MAXIMIZED:
            DLOG("Window %d maximized \n", event->window.windowID);
            break;
        case SDL_WINDOWEVENT_RESTORED:
            DLOG("Window %d restored \n", event->window.windowID);
            break;
        case SDL_WINDOWEVENT_ENTER:
            DLOG("Mouse entered window %d \n",
                event->window.windowID);
            break;
        case SDL_WINDOWEVENT_LEAVE:
            DLOG("Mouse left window %d \n", event->window.windowID);
            break;
        case SDL_WINDOWEVENT_FOCUS_GAINED:
            DLOG("Window %d gained keyboard focus \n",
                event->window.windowID);
            break;
        case SDL_WINDOWEVENT_FOCUS_LOST:
            DLOG("Window %d lost keyboard focus \n",
                event->window.windowID);
            break;
        case SDL_WINDOWEVENT_CLOSE:
            DLOG("Window %d closed \n", event->window.windowID);
            break;
        default:
            DLOG("Window %d got unknown event %d \n",
                event->window.windowID, event->window.event);
            break;
        }
    }
}

double clamp(double val, double lower, double upper) {
  return std::max(lower, std::min(val, upper));
}

int clamp(int val, int lower, int upper) {
  return std::max(lower, std::min(val, upper));
}

int main(int argc, char** argv)
{

    globals::binpath = std::string(argv[0]);
    int posslash = globals::binpath.length()-1;
    while (isalpha(globals::binpath.c_str()[globals::binpath.length()-1])
           || isdigit(globals::binpath.c_str()[globals::binpath.length()-1])){
        globals::binpath.resize (globals::binpath.size () - 1);
    }

    DLOG ("Global path is the %s\n", globals::binpath.c_str());

	try {  

    CmdLine cmd("Give it a file to send as a set of QR frames.", ' ', "0.9");

    ValueArg<int> QRsizeArg("q", "qrsize", "Bytes capacity of single QR frame, range 90-1600", false, 585, "int");
    cmd.add( QRsizeArg );

    ValueArg<int> QRtargetFPS("s", "fpsvalue", "Target FPS, range 5-60", false, 17, "int");
    cmd.add( QRtargetFPS );

    ValueArg<int> MaxErrorLevel("e", "percent_maxallowederror", "Maximum allowed error, range 20%-80%", false, 50, "int");
    cmd.add( MaxErrorLevel );

    ValueArg<int> QRinitTime("t", "initialframetime", "Time duration(s) of the startup sequence - range 3-10s", false, 6, "int");
    cmd.add( QRinitTime );

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
    fileNames = multi.getValue();
    qrbytesize = QRsizeArg.getValue();
    initTime = QRinitTime.getValue();
    targetFPS = QRtargetFPS.getValue();
    maxAllowedErrorPercent = MaxErrorLevel.getValue();

    qrbytesize = clamp (qrbytesize, 90, 1600);
    initTime = clamp (initTime, 3, 10);
    targetFPS = clamp (targetFPS, 5, 60);
    if (maxAllowedErrorPercent % 5 != 0)
        maxAllowedErrorPercent -= (maxAllowedErrorPercent % 5);
    maxAllowedErrorPercent = clamp (maxAllowedErrorPercent, 20, 80);


    timeframe_delay = 1000.0 / ((double) targetFPS);


    globals::startupsecs = (double)initTime;

    DLOG("QR size : %d\n", QRsizeArg.getValue());

    for(vector<string>::iterator it = fileNames.begin(); it!=fileNames.end(); it++){
#ifndef RELEASE
        std::cout << *it << std::endl;
#endif
    }

	} catch (ArgException &e)  // catch any exceptions
    {
        cerr << "error: " << e.error() << " for arg " << e.argId() << endl;
        exit(0);
    }

    /*
    string tmp = getBundlePath();
    for(int i=1; i<tmp.size(); i++){
        if(tmp[i] == '\n'){
            tmp[i] = 0;
            if(tmp[i-1] == '/')
                tmp[i-1] = 0;
        }
    }
    executable_path = std::string(tmp.c_str());
    */

    char cwd[1024];
    getcwd(cwd, sizeof(cwd));

    if(cwd[strlen(cwd) - 1] == '/')
      cwd[strlen(cwd) - 1] = 0;
    executable_path = std::string(cwd);

    DLOG("path : %s \n", executable_path.c_str());
    if (qrbytesize < 16)
        qrbytesize = 16;

    frame_producer = NULL;

    //frame_producer.set_external_file_info("textmy.txt", "/repos/qr/", qrbytesize);

    do_SDL_setup();

}



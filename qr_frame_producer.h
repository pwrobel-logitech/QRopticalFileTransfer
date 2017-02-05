#include <stdio.h>
#include <stdlib.h>
#include <memory.h>
#include <time.h>
#include <stdio.h>
#include <unistd.h>
#include <sys/time.h>
#include <string>
#include "open_rs_encoder.h"
#include "fileutil/fileops.h"

class Qr_frame_producer
{
public:
    Qr_frame_producer(const char* file);
    ~Qr_frame_producer();
    //-1 = error, 0 - ok, 1 - finished
    int produce_next_qr_image_to_file(const char* imagepath);
private:
    void setup_encoder();
    std::string filename_;
    Encoder* encoder_;
};



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

class Qr_frame_producer;

Qr_frame_producer* global_frame_producer;

class Qr_frame_producer
{
public:
    Qr_frame_producer(const char* file);
    ~Qr_frame_producer();
    //-1 = error, 0 - ok, 1 - finished
    int produce_next_qr_image_to_file(const char* imagepath);
    static Qr_frame_producer* getQr_frame_producer(){ //get singleton
        return global_frame_producer;
    }
private:
    void setup_encoder();
    ///we will pass it for the encoder as the data feeding callback
    static int needData(FileChunk*) ;
    std::string filename_;
    Encoder* encoder_;
    uint16_t total_chars_per_QR_;
};




#include <stdio.h>
#include <stdlib.h>
#include <memory.h>
#include <time.h>
#include <stdio.h>
#include <unistd.h>
#include <sys/time.h>
#include <string.h>
#include "open_rs_encoder.h"


class Qr_frame_producer
{
public:
    Qr_frame_producer(const char* file);
    ~Qr_frame_producer();
    //-1 = error, 0 - ok, 1 - finished
    // image template - "dump_%d" = will generate dump_1, dump_2, dump_3.. files
    int produce_next_qr_image_to_file(const char* imagetemplate);
    int produce_next_qr_grayscale_image_to_mem(char** produced_image, int *produced_width);

    static int needData(FileChunk*);
    static char* file;
private:
    void setup_encoder();
    ///we will pass it for the encoder as the data feeding callback

    std::string filename_;
    Encoder* encoder_;
    uint16_t total_chars_per_QR_;
};




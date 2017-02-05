
#include <stdio.h>
#include <stdlib.h>
#include <memory.h>
#include <time.h>
#include <stdio.h>
#include <unistd.h>
#include <sys/time.h>
#include <string>

#include "qr_frame_producer.h"

int main(int argc, char **argv){
    if(argc>1)
        printf("Parsing %s file\n", argv[1]);
    else
        exit(0);


    Qr_frame_producer frame_producer(argv[1]);

    frame_producer.produce_next_qr_image_to_file("k.bmp");



    return 1;
}

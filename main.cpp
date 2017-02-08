
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

    {
        uint32_t n_possible_symbols_dst = 256;
        uint32_t n_possible_symbols_src = 511;
        uint32_t count_symbols_src = 32;
        printf("Test util count bytes, count_symbols_to_fit(n_possible_symbols_dst = %d, n_possible_symbols_src = %d, count_symbols_src = %d : %d\n",
                n_possible_symbols_dst,
                n_possible_symbols_src,
                count_symbols_src,
                utils::count_symbols_to_fit(n_possible_symbols_dst, n_possible_symbols_src, count_symbols_src));
    }

    Qr_frame_producer frame_producer(argv[1]);
    global_frame_producer = &frame_producer;

    frame_producer.produce_next_qr_image_to_file("k.bmp");



    return 1;
}

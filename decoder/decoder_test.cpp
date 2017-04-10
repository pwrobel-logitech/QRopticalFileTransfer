#include "qr_frame_decoder.h"
#include "string.h"

#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <math.h>

int main(int argc, char** argv){

    QR_frame_decoder framedecoder;

    FILE *f;
    char namebuf[100];


    for (int j = 0; j<7*20+511*2; j++) {
        snprintf(namebuf, sizeof(namebuf), "QRNE_%d_frame", j);
        f = fopen(namebuf, "rb");
        if (f == NULL)
            continue;
        // obtain file size
        fseek (f , 0 , SEEK_END);
        int fsize = ftell (f);
        rewind (f);
        char *buf = new char[fsize];
        if (fread (buf, 1, fsize, f) != fsize)
            printf("Error reading frame in the qr decoder test! \n");
        int width = (int)sqrt(fsize);
        framedecoder.send_next_grayscale_qr_frame(buf, width, width);
        delete []buf;

        fclose(f);
    };


    framedecoder.tell_no_more_qr();

    framedecoder.print_current_header();
    framedecoder.print_current_maindata();

    return 1;
}

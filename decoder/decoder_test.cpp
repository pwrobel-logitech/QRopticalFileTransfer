#include "qr_frame_decoder.h"
#include "string.h"

#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <math.h>

int main(int argc, char** argv){

    QR_frame_decoder framedecoder;
    framedecoder.tell_file_generation_path("/home/pwrobel/");

    FILE *f;
    char namebuf[100];

    immediate_status last_status;

    for (int j = 0; j<7*20+511*30; j++) {
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
        immediate_status status = framedecoder.send_next_grayscale_qr_frame(buf, width, width);
        last_status = status;
        if ((status == ALREADY_CORRECTLY_TRANSFERRED) || (status == ERRONEUS_HASH_WRONG) ||
             (status == ERRONEUS)){
            delete []buf;
            fclose(f);
            break;
        }
        delete []buf;

        fclose(f);
    };

    bool is_critical_err = false;
    if((last_status == ERRONEUS_HASH_WRONG) || (last_status == ERRONEUS)){
        is_critical_err = true;
    }

    if (!is_critical_err){
        framedecoder.tell_no_more_qr();

        printf("Got file : \n");
        framedecoder.print_current_header();
        framedecoder.print_current_maindata();
        printf("\n");
    } else {
        printf("ERROR - transmition failed - too many errors or file sanity check failed\n");
    }

    return 1;
}

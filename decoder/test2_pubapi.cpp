#include "../public_decoder_api.h"


#include "string.h"

#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <math.h>

int main(int argc, char** argv){


 initialize_decoder();
 set_decoded_file_path("/home/pwrobel/");

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
     immediate_status status = send_next_grayscale_buffer_to_decoder(buf, width, width);
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
     tell_decoder_no_more_qr();
     printf("Got file : \n");
     printf("\n");
 } else {
     printf("ERROR - transmition failed - too many errors or file sanity check failed\n");
 }

 deinitialize_decoder();
}

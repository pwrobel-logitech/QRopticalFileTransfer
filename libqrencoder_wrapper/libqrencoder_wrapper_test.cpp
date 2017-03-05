#include "libqrencoder_wrapper.h"
#include "string.h"

#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>


void test_gen_qr_and_decode(){
    char* in = new char[14];
    snprintf(in, 5+1, "aBcdeFGhij");

    char* out_qr_bmp;
    int outw;
    printf("Buff START %s \n", in);
    for (int i = 0 ;i < 5 ; i++){
        printf("%d\n", in[i]);
    }
    generate_qr_greyscale_bitmap_data((const unsigned char*)in, 5, &out_qr_bmp, &outw, 3);


/*
    printf("\n");
    for (int i = 0; i<outw; i++) {
        for (int j = 0; j<outw; j++) {
            printf("%01x ", 0xf & out_qr_bmp[i*outw+j]);
        }
        printf("\n");
    }
*/

    int out_datal;
    char *outdata;
    generate_data_from_qr_greyscalebuffer(&out_datal, &outdata, out_qr_bmp,
                                               outw);

    printf("libzxing returned outl %d\n", out_datal);
    for (int i = 0 ;i < 5 ; i++){
        printf("%d\n", outdata[i]);
    }

    delete []in;
}


int main(){
  const unsigned char* data = (const unsigned char*)
          "In computing, plain text is the data (e.g. file contents) "
          "that represent only characters of readable material but not its"
          "Composes a string with the same text that would be printed"
          ;
  int len = strlen((const char*)data);
  int out_len;
  char* out_data;
  int max_target_width = 600;
  init_libqrencoder(max_target_width*max_target_width*2);

  generate_image_data(data, len, &out_data, &out_len, &max_target_width);

  FILE *my_file = fopen("dumpQR.jpg", "wb");
  fwrite(out_data, out_len , 1, my_file);
  fclose(my_file);

  finish_libqrencoder();

  init_libqrencoder(1200*1200*2);

  char str[40];
  for(int i = 1; i<10; i++){

    snprintf(str, sizeof(str), "dumpQR_sizemult%d.jpg", i);


    generate_small_image_data_margin(data, len, &out_data, &out_len, &max_target_width, i, 3);

    my_file = fopen(str, "wb");
    fwrite(out_data, out_len , 1, my_file);
    fclose(my_file);


  }

  test_gen_qr_and_decode();


  finish_libqrencoder();
  return 1;
}

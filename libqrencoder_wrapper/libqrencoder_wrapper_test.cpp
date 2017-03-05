#include "libqrencoder_wrapper.h"
#include "string.h"

#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>


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
  finish_libqrencoder();
  return 1;
}

#include "libqrencoder_wrapper.h"
#include "string.h"

#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <jpeglib.h>


int main(){
  const unsigned char* data = (const unsigned char*)"In computing, plain text is the data (e.g. file contents) that represent only characters of readable material but not its";
  int len = strlen((const char*)data);
  int out_len;
  char** out_data;
  int max_target_width = 600;
  init_libqrencoder(max_target_width*max_target_width*2);
  generate_image_data(data, len, out_data, &out_len, &max_target_width);
  finish_libqrencoder();
  return 1;
}

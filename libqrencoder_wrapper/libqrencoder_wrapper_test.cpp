#include "libqrencoder_wrapper.h"
#include "string.h"

int main(){
  const unsigned char* data = (const unsigned char*)"abcQQdef";
  int len = strlen((const char*)data);
  int out_len;
  unsigned char** out_data;
  generate_image_data(data, len, out_data, &out_len);
  return 1;
}

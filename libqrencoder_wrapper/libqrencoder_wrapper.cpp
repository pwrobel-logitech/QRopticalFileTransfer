
#include "libqrencoder_wrapper.h"
#include "stdlib.h"
#include "stdio.h"

void init_libqrencoder(int size){

}

void finish_libqrencoder(){

}

void generate_image_data(const unsigned char* input_data, int input_length, unsigned char** out_image_data, int *out_image_data_size){
  QRcode *generatedQR = NULL;
  generatedQR = QRcode_encodeData(input_length, input_data, 1, QR_ECLEVEL_M);
  if (generatedQR != NULL){
      *out_image_data = generatedQR->data;
      *out_image_data_size = generatedQR->width * generatedQR->width;
  }
  FILE *my_file = fopen("dump11", "wb");
  fwrite(*out_image_data, *out_image_data_size, 1, my_file);
  fclose(my_file);
}




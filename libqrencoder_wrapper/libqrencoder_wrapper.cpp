
#include "libqrencoder_wrapper.h"
#include "qrencode.h"


void init_libqrencoder(int size){

}


void finish_libqrencoder(){

}

void generate_image_data(const unsigned char* input_data, int input_length, char* out_image_data, int *out_image_data_size){
  QRcode_encodeData(input_length, input_data, 1, QR_ECLEVEL_M);
}




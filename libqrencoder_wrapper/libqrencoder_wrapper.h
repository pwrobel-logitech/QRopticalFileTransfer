#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include "public_decoder_api.h"

extern "C" {

// init libqrencoder - prepare to use with data of given fixed size
void init_libqrencoder(int size);

// deallocate all the resources needed for the libqrencoder module
void finish_libqrencoder();

// wrapper function to generate grayscale QR image
void generate_qr_greyscale_bitmap_data(const unsigned char* input_data,
                                       int input_length,
                                       char** out_image_data,
                                       int *out_image_data_width,
                                       int margin);

}
